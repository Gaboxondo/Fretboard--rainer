param(
    [string]$BumpType = "auto",
    [string]$CustomVersion = "",
    [string]$CommitMessage = "",
    [string]$BranchName = "",
    [switch]$DryRun = $false
)

# 1. Leer versión actual de pom.xml
[xml]$pom = Get-Content pom.xml
$currentVersion = $pom.project.version
Write-Host "Versión actual en pom.xml: $currentVersion"

# Validar formato SemVer (X.Y.Z)
if ($currentVersion -match '^(\d+)\.(\d+)\.(\d+)') {
    $major = [int]$Matches[1]
    $minor = [int]$Matches[2]
    $patch = [int]$Matches[3]
} else {
    Write-Error "Formato de versión en pom.xml no válido para SemVer: $currentVersion"
    exit 1
}

# 2. Si no se especificó BranchName o es 'main', intentar detectar la rama de origen del PR vía GitHub CLI
$sourceBranch = $BranchName
if ([string]::IsNullOrWhiteSpace($sourceBranch) -or $sourceBranch -eq "main" -or $sourceBranch -eq "refs/heads/main") {
    try {
        if (Get-Command gh -ErrorAction SilentlyContinue) {
            $prBranch = gh pr list --state merged --limit 1 --json headRefName --jq ".[0].headRefName" 2>$null
            if (-not [string]::IsNullOrWhiteSpace($prBranch)) {
                $sourceBranch = $prBranch.Trim()
                Write-Host "Rama de origen detectada desde GitHub PR: $sourceBranch"
            }
        }
    } catch {
        # Fallback a commit message
    }
}

# 3. Determinar el tipo de incremento y si corresponde generar release
$shouldRelease = $false
$determinedBump = $BumpType
$newVersion = ""

if (-not [string]::IsNullOrWhiteSpace($CustomVersion)) {
    $shouldRelease = $true
    $determinedBump = "custom"
    $newVersion = $CustomVersion.TrimStart('v')
} elseif ($determinedBump -in @("major", "minor", "patch")) {
    $shouldRelease = $true
} elseif ($determinedBump -eq "none") {
    $shouldRelease = $false
    $determinedBump = "none"
} else {
    # Modo 'auto': analizar archivos modificados y mensaje de commit / rama
    $haystack = "$sourceBranch $BranchName $CommitMessage".ToLower()
    Write-Host "Analizando contexto para autoversionado: '$haystack'"

    # 3.1. Verificar si hay cambios en archivos de código fuente o empaquetado
    $hasSourceChanges = $false
    try {
        $changedFiles = git diff-tree --no-commit-id --name-only -r HEAD 2>$null
        if ($changedFiles) {
            foreach ($f in $changedFiles) {
                if ($f -match '^src/main/' -or $f -match '^installer/' -or $f -eq 'pom.xml') {
                    $hasSourceChanges = $true
                    break
                }
            }
            if (-not $hasSourceChanges) {
                Write-Host "No hay cambios en código fuente (src/main/, installer/, pom.xml). Archivos en commit: $($changedFiles -join ', ')"
            }
        } else {
            $hasSourceChanges = $true
        }
    } catch {
        $hasSourceChanges = $true
    }

    # 3.2. Clasificación según archivos y Conventional Commits
    if (-not $hasSourceChanges) {
        Write-Host "No se detectaron cambios en archivos de código fuente (src/main/, installer/, pom.xml). Omitiendo release."
        $determinedBump = "none"
        $shouldRelease = $false
    } elseif ($haystack -match 'breaking change' -or $haystack -match '\b(feat|fix)(\([^)]+\))?!:') {
        $determinedBump = "major"
        $shouldRelease = $true
    } elseif ($haystack -match 'feature/' -or $haystack -match 'feat/' -or $haystack -match '\bfeat(\([^)]+\))?:') {
        $determinedBump = "minor"
        $shouldRelease = $true
    } elseif ($haystack -match 'bug/' -or $haystack -match 'bugfix/' -or $haystack -match 'hotfix/' -or $haystack -match 'fix/' -or $haystack -match '\bfix(\([^)]+\))?:' -or $haystack -match '\bperf(\([^)]+\))?:') {
        $determinedBump = "patch"
        $shouldRelease = $true
    } elseif ($haystack -match '\b(chore|docs|style|test|ci|build|refactor)(\([^)]+\))?:') {
        Write-Host "Commit identificado como tipo de mantenimiento/docs/tests ($($Matches[1])). No requiere nueva versión."
        $determinedBump = "none"
        $shouldRelease = $false
    } else {
        # Cambios de código generales sin prefijo convencional específico
        $determinedBump = "patch"
        $shouldRelease = $true
    }
}

# 4. Si no corresponde release, omitir cálculo e incremento
if (-not $shouldRelease -or $determinedBump -eq "none") {
    Write-Host "Resultado: NO se genera versión nueva (SHOULD_RELEASE=false, versión mantenida en $currentVersion)."
    if ($env:GITHUB_OUTPUT) {
        "SHOULD_RELEASE=false" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
        "NEW_VERSION=$currentVersion" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
        "BUMP_TYPE=none" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
        "CURRENT_VERSION=$currentVersion" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
    }
    return $currentVersion
}

# 5. Calcular nueva versión para release
if ([string]::IsNullOrWhiteSpace($newVersion)) {
    switch ($determinedBump.ToLower()) {
        "major" {
            $major++
            $minor = 0
            $patch = 0
        }
        "minor" {
            $minor++
            $patch = 0
        }
        "patch" {
            $patch++
        }
        default {
            $patch++
        }
    }
    $newVersion = "$major.$minor.$patch"
}

Write-Host "Tipo de incremento: $determinedBump"
Write-Host "Nueva versión calculada: $newVersion"

if ($DryRun) {
    Write-Host "Modo DryRun: No se modifican archivos."
    return $newVersion
}

# 5. Actualizar pom.xml
$pomContent = Get-Content pom.xml -Raw
$updatedPomContent = [regex]::Replace(
    $pomContent,
    '(<groupId>com\.threadboardlearn</groupId>\s*<artifactId>fretboard-trainer</artifactId>\s*<version>)[^<]+(</version>)',
    "`${1}$newVersion`${2}"
)
Set-Content -Path pom.xml -Value $updatedPomContent -Encoding UTF8
Write-Host "pom.xml actualizado a $newVersion"

# 6. Actualizar README.md y docs/es/README.md
$readmeFiles = @("README.md", "docs/es/README.md")
foreach ($rPath in $readmeFiles) {
    if (Test-Path $rPath) {
        $readme = Get-Content $rPath -Raw
        $readme = [regex]::Replace($readme, 'Release-v[0-9.]+-orange', "Release-v$newVersion-orange")
        $readme = [regex]::Replace($readme, 'fretboard-trainer-[0-9.]+\.jar', "fretboard-trainer-$newVersion.jar")
        Set-Content -Path $rPath -Value $readme -Encoding UTF8
        Write-Host "$rPath actualizado a versión $newVersion"
    }
}

# 7. Exportar a GitHub Output si estamos en un runner de GitHub Actions
if ($env:GITHUB_OUTPUT) {
    "SHOULD_RELEASE=true" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
    "NEW_VERSION=$newVersion" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
    "BUMP_TYPE=$determinedBump" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
    "CURRENT_VERSION=$currentVersion" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
}

return $newVersion
