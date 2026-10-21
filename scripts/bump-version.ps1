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

# 3. Determinar el tipo de incremento
$determinedBump = $BumpType

if ($determinedBump -eq "auto" -or [string]::IsNullOrWhiteSpace($determinedBump)) {
    $haystack = "$sourceBranch $BranchName $CommitMessage".ToLower()
    Write-Host "Analizando contexto para autoversionado: '$haystack'"

    if ($haystack -match 'feature/' -or $haystack -match 'feat/' -or $haystack -match '\bfeat(\([^)]+\))?:') {
        $determinedBump = "minor"
    } elseif ($haystack -match 'bug/' -or $haystack -match 'bugfix/' -or $haystack -match 'hotfix/' -or $haystack -match 'fix/' -or $haystack -match '\bfix(\([^)]+\))?:') {
        $determinedBump = "patch"
    } else {
        # Por defecto ante cambios generales, sube el tercer número (patch)
        $determinedBump = "patch"
    }
}

# 4. Calcular nueva versión
if (-not [string]::IsNullOrWhiteSpace($CustomVersion)) {
    $newVersion = $CustomVersion.TrimStart('v')
} else {
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

# 6. Actualizar README.md
if (Test-Path README.md) {
    $readme = Get-Content README.md -Raw
    $readme = [regex]::Replace($readme, 'Release-v[0-9.]+-orange', "Release-v$newVersion-orange")
    $readme = [regex]::Replace($readme, 'fretboard-trainer-[0-9.]+\.jar', "fretboard-trainer-$newVersion.jar")
    Set-Content -Path README.md -Value $readme -Encoding UTF8
    Write-Host "README.md actualizado a versión $newVersion"
}

# 7. Exportar a GitHub Output si estamos en un runner de GitHub Actions
if ($env:GITHUB_OUTPUT) {
    "NEW_VERSION=$newVersion" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
    "BUMP_TYPE=$determinedBump" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
    "CURRENT_VERSION=$currentVersion" | Out-File -FilePath $env:GITHUB_OUTPUT -Append -Encoding utf8
}

return $newVersion
