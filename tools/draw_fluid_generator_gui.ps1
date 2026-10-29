Add-Type -AssemblyName System.Drawing

$outputPath = Join-Path $PSScriptRoot "..\src\main\resources\assets\infwhat\textures\gui\container\inf_fluid_generator.png"
$bitmap = [System.Drawing.Bitmap]::new(368, 178, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
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

function Draw-Page([int]$offset, [bool]$ioPage) {
    Fill-Rect $offset 0 184 178 "#030609"
    Fill-Rect ($offset + 1) 1 182 176 "#70858F"
    Fill-Rect ($offset + 2) 2 180 174 "#0A1116"
    Fill-Rect ($offset + 3) 3 178 172 "#26343D"
    Fill-Rect ($offset + 4) 4 176 170 "#111A20"
    Fill-Rect ($offset + 5) 5 174 168 "#202C34"

    Draw-Panel ($offset + 7) 5 170 80 "#19262E"
    Fill-Rect ($offset + 8) 7 2 76 "#35C7C3"
    Fill-Rect ($offset + 10) 7 1 76 "#177B84"
    Fill-Rect ($offset + 12) 19 159 1 "#435B66"
    Fill-Rect ($offset + 12) 20 159 1 "#10191F"

    if ($ioPage) {
        # I/O page: title/close, output switch, rate selector, then six direction toggles.
        Draw-Panel ($offset + 159) 6 16 14 "#202D35"
        Draw-Panel ($offset + 9) 20 166 15 "#202D35"
        Draw-Panel ($offset + 9) 36 19 15 "#202D35"
        Draw-Panel ($offset + 28) 36 126 15 "#202D35"
        Draw-Panel ($offset + 154) 36 21 15 "#202D35"
        for ($row = 0; $row -lt 3; $row++) {
            $y = 51 + ($row * 11)
            Draw-Panel ($offset + 9) $y 82 11 "#202D35"
            Draw-Panel ($offset + 93) $y 82 11 "#202D35"
        }
    }
    else {
        # Fluid tank frame and the two separated main-page actions.
        Draw-Panel ($offset + 23) 20 22 46 "#14222A"
        Fill-Rect ($offset + 25) 22 18 42 "#060B10"
        Draw-Panel ($offset + 9) 67 82 16 "#202D35"
        Draw-Panel ($offset + 93) 67 82 16 "#202D35"
    }

    # Same inventory geometry as the item-generator GUI: slot hitboxes align with this artwork.
    Draw-Panel ($offset + 6) 96 172 75 "#17232A"
    for ($row = 0; $row -lt 3; $row++) {
        for ($column = 0; $column -lt 9; $column++) {
            Draw-Slot ($offset + 10 + ($column * 18)) (98 + ($row * 18))
        }
    }
    for ($column = 0; $column -lt 9; $column++) {
        Draw-Slot ($offset + 10 + ($column * 18)) 152
    }

    Fill-Rect ($offset + 3) 3 2 2 "#C0D1D7"
    Fill-Rect ($offset + 181) 3 2 2 "#C0D1D7"
    Fill-Rect ($offset + 3) 173 2 2 "#34464F"
    Fill-Rect ($offset + 181) 173 2 2 "#34464F"
}

Draw-Page 0 $false
Draw-Page 184 $true
$graphics.Dispose()
$bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()

Write-Output "Generated $outputPath (368 x 178)"
