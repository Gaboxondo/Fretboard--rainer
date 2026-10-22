Add-Type -AssemblyName System.Drawing

$sourcePath = "C:\Proyectos\ThreadBoardlearn\src\main\resources\icons\app-icon.png"
$resDir = "C:\Proyectos\ThreadBoardlearn\android\app\src\main\res"

if (-not (Test-Path $sourcePath)) {
    Write-Error "Source icon not found at $sourcePath"
    exit 1
}

$sourceImg = [System.Drawing.Bitmap]::FromFile($sourcePath)

function Resize-Image {
    param(
        [System.Drawing.Bitmap]$Source,
        [int]$TargetWidth,
        [int]$TargetHeight,
        [bool]$IsRound = $false
    )

    $target = New-Object System.Drawing.Bitmap $TargetWidth, $TargetHeight
    $graphics = [System.Drawing.Graphics]::FromImage($target)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    if ($IsRound) {
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddEllipse(0, 0, $TargetWidth, $TargetHeight)
        $graphics.SetClip($path)
    }

    $graphics.DrawImage($Source, 0, 0, $TargetWidth, $TargetHeight)
    $graphics.Dispose()
    return $target
}

function Create-AdaptiveForeground {
    param(
        [System.Drawing.Bitmap]$Source,
        [int]$CanvasSize = 432,
        [int]$IconSize = 288
    )

    $target = New-Object System.Drawing.Bitmap $CanvasSize, $CanvasSize
    $graphics = [System.Drawing.Graphics]::FromImage($target)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    $offset = ($CanvasSize - $IconSize) / 2
    $graphics.DrawImage($Source, [float]$offset, [float]$offset, [float]$IconSize, [float]$IconSize)
    $graphics.Dispose()
    return $target
}

# 1. Copiar a drawable/app_logo.png
$drawableDir = Join-Path $resDir "drawable"
if (-not (Test-Path $drawableDir)) { New-Item -ItemType Directory -Path $drawableDir -Force }

$logoPath = Join-Path $drawableDir "app_logo.png"
$sourceImg.Save($logoPath, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "Created $logoPath"

# 2. Adaptive Foreground (432x432 con icono centrado a 288x288 dentro de la zona segura)
$foregroundImg = Create-AdaptiveForeground -Source $sourceImg -CanvasSize 432 -IconSize 288
$foregroundPath = Join-Path $drawableDir "ic_launcher_foreground.png"
$foregroundImg.Save($foregroundPath, [System.Drawing.Imaging.ImageFormat]::Png)
$foregroundImg.Dispose()
Write-Host "Created $foregroundPath"

# 3. Mipmap standard & round icons
$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($entry in $sizes.GetEnumerator()) {
    $dirName = $entry.Key
    $size = $entry.Value
    $targetDir = Join-Path $resDir $dirName
    if (-not (Test-Path $targetDir)) { New-Item -ItemType Directory -Path $targetDir -Force }

    # Standard
    $standardImg = Resize-Image -Source $sourceImg -TargetWidth $size -TargetHeight $size -IsRound $false
    $standardPath = Join-Path $targetDir "ic_launcher.png"
    $standardImg.Save($standardPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $standardImg.Dispose()

    # Round
    $roundImg = Resize-Image -Source $sourceImg -TargetWidth $size -TargetHeight $size -IsRound $true
    $roundPath = Join-Path $targetDir "ic_launcher_round.png"
    $roundImg.Save($roundPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $roundImg.Dispose()

    Write-Host "Generated ${dirName}: $size x $size"
}

$sourceImg.Dispose()
Write-Host "All Android icons generated successfully!"
