Add-Type -AssemblyName System.Drawing

$root = Join-Path $PSScriptRoot "..\src\main\resources\assets\infwhat"
$guiPath = Join-Path $root "textures\gui\container\inf_source_io.png"
$itemDir = Join-Path $root "textures\item"
$itemPath = Join-Path $itemDir "infinite_source_config_replicator.png"
New-Item -ItemType Directory -Force -Path $itemDir | Out-Null

$bitmap = [System.Drawing.Bitmap]::new(184, 178, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

function Fill-Rect([int]$x, [int]$y, [int]$width, [int]$height, [string]$hex) {
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($hex))
    $graphics.FillRectangle($brush, $x, $y, $width, $height)
    $brush.Dispose()
}

function Draw-Panel([int]$x, [int]$y, [int]$width, [int]$height, [string]$body) {
    Fill-Rect $x $y $width $height "#05090D"
    Fill-Rect ($x + 1) ($y + 1) ($width - 2) ($height - 2) "#80939D"
    Fill-Rect ($x + 2) ($y + 2) ($width - 4) ($height - 4) "#111A20"
    Fill-Rect ($x + 3) ($y + 3) ($width - 6) ($height - 6) $body
    Fill-Rect ($x + 2) ($y + 2) ($width - 4) 1 "#C0D1D7"
    Fill-Rect ($x + 2) ($y + 2) 1 ($height - 4) "#A3B7C0"
    Fill-Rect ($x + 2) ($y + $height - 3) ($width - 4) 1 "#05090D"
    Fill-Rect ($x + $width - 3) ($y + 2) 1 ($height - 4) "#05090D"
}

function Draw-Slot([int]$x, [int]$y) {
    Fill-Rect $x $y 18 18 "#060A0E"
    Fill-Rect ($x + 1) ($y + 1) 16 16 "#80919A"
    Fill-Rect ($x + 2) ($y + 2) 14 14 "#11191E"
    Fill-Rect ($x + 3) ($y + 3) 12 12 "#3A4850"
    Fill-Rect ($x + 2) ($y + 2) 14 1 "#A5B5BC"
    Fill-Rect ($x + 2) ($y + 2) 1 14 "#8799A2"
    Fill-Rect ($x + 2) ($y + 15) 14 1 "#0A1014"
    Fill-Rect ($x + 15) ($y + 2) 1 14 "#0A1014"
}

# Shared 184 x 178 I/O page texture; both machine screens use these exact coordinates.
Fill-Rect 0 0 184 178 "#030609"
Fill-Rect 1 1 182 176 "#70858F"
Fill-Rect 2 2 180 174 "#0A1116"
Fill-Rect 3 3 178 172 "#26343D"
Fill-Rect 4 4 176 170 "#111A20"
Fill-Rect 5 5 174 168 "#202C34"
Draw-Panel 7 5 170 80 "#19262E"
Fill-Rect 8 7 2 76 "#35C7C3"
Fill-Rect 10 7 1 76 "#177B84"
Fill-Rect 12 19 159 1 "#435B66"
Fill-Rect 12 20 159 1 "#10191F"
Draw-Panel 159 6 16 14 "#202D35"
Draw-Panel 9 20 166 15 "#202D35"
Draw-Panel 9 36 45 15 "#202D35"
Draw-Panel 52 36 101 15 "#202D35"
Draw-Panel 154 36 21 15 "#202D35"
for ($row = 0; $row -lt 3; $row++) {
    $y = 51 + ($row * 11)
    Draw-Panel 9 $y 82 11 "#202D35"
    Draw-Panel 93 $y 82 11 "#202D35"
}
Draw-Panel 6 96 172 75 "#17232A"
for ($row = 0; $row -lt 3; $row++) {
    for ($column = 0; $column -lt 9; $column++) {
        Draw-Slot (10 + ($column * 18)) (98 + ($row * 18))
    }
}
for ($column = 0; $column -lt 9; $column++) {
    Draw-Slot (10 + ($column * 18)) 152
}
Fill-Rect 3 3 2 2 "#C0D1D7"
Fill-Rect 181 3 2 2 "#C0D1D7"
Fill-Rect 3 173 2 2 "#34464F"
Fill-Rect 181 173 2 2 "#34464F"
$graphics.Dispose()
$bitmap.Save($guiPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()

# Pixel-art configuration card in the mod's existing steel/teal technology palette.
$icon = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
function Set-Pixel([int]$x, [int]$y, [string]$hex) {
    $icon.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($hex))
}
function Fill-PixelRect([int]$x, [int]$y, [int]$width, [int]$height, [string]$hex) {
    for ($py = $y; $py -lt ($y + $height); $py++) {
        for ($px = $x; $px -lt ($x + $width); $px++) { Set-Pixel $px $py $hex }
    }
}
Fill-PixelRect 5 1 6 1 "#C4D2D7"
Fill-PixelRect 3 2 10 1 "#718791"
Fill-PixelRect 2 3 12 10 "#10191F"
Fill-PixelRect 3 3 10 1 "#AABCC3"
Fill-PixelRect 2 4 1 8 "#748A93"
Fill-PixelRect 13 4 1 8 "#05090D"
Fill-PixelRect 3 12 10 1 "#05090D"
Fill-PixelRect 4 5 8 6 "#263740"
# Two offset data sheets and a central cyan link form a compact copy/config glyph.
Fill-PixelRect 5 6 4 1 "#17262E"
Fill-PixelRect 5 7 1 3 "#46D5D0"
Fill-PixelRect 6 7 3 1 "#2DA5AD"
Fill-PixelRect 8 8 1 3 "#46D5D0"
Fill-PixelRect 6 9 2 1 "#7EE5DF"
Fill-PixelRect 10 6 1 1 "#94AAB2"
Fill-PixelRect 10 8 1 1 "#94AAB2"
Fill-PixelRect 10 10 1 1 "#94AAB2"
Set-Pixel 3 5 "#5AE0D9"
Set-Pixel 12 11 "#2DABB3"
Set-Pixel 4 13 "#657B85"
Set-Pixel 11 13 "#657B85"
$icon.Save($itemPath, [System.Drawing.Imaging.ImageFormat]::Png)
$icon.Dispose()

Write-Output "Generated $guiPath and $itemPath"
