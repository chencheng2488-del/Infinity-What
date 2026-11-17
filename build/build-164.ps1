# Builds the 1.6.4 Forge jar with the manual MCP + SpecialSource toolchain.
# All javac/specialsource arguments use relative paths so the space in the project path
# never reaches a native command line.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$jdk = 'D:\Java\jdk8u504-b01'
$classes = 'build\classes'
$libs = 'build\libs'
$fat = 'build\fat'
$stage = 'build\stage'
$devJar = 'build\libs\infwhat-1.6.4-forge-2.2.0-dev.jar'
$finalJar = 'build\libs\infwhat-1.6.4-forge-2.2.0.jar'
$cp = '..\..\..\libs\minecraft-1.6.4-mcp.jar;..\..\..\libs\forge-1.6.4-mcp.jar;..\..\..\libs\lwjgl-2.9.1.jar;..\..\..\libs\Thaumcraft4.1.0g.jar'

foreach ($dir in @('build\classes', 'build\libs', 'build\fat', 'build\stage')) {
    if (Test-Path $dir) { Remove-Item -Recurse -Force $dir }
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

# Every source is reachable from these entry points through -sourcepath.
$entryPoints = @(
    'com\bandybus\infwhat\InfinityWhat.java',
    'com\bandybus\infwhat\client\ClientProxy.java',
    'com\bandybus\infwhat\client\gui\GuiInfItemGenerator.java',
    'com\bandybus\infwhat\client\gui\GuiInfFluidGenerator.java',
    'com\bandybus\infwhat\client\gui\GuiOrdoUsurpatoryMatrix.java',
    'com\bandybus\infwhat\client\gui\AbstractArcaneMachineGui.java',
    'com\bandybus\infwhat\client\gui\GuiArcaneAspectReplicator.java',
    'com\bandybus\infwhat\client\gui\GuiArcaneEssentiaEmanator.java',
    'com\bandybus\infwhat\container\GuiHandlerRouter.java',
    'com\bandybus\infwhat\container\AbstractArcaneMachineContainer.java',
    'com\bandybus\infwhat\container\ArcaneAspectReplicatorContainer.java',
    'com\bandybus\infwhat\container\ArcaneEssentiaEmanatorContainer.java',
    'com\bandybus\infwhat\integration\thaumcraft4\ThaumcraftGuiHandler.java',
    'com\bandybus\infwhat\integration\thaumcraft4\ThaumcraftRecipeIntegration.java'
)
Push-Location 'src\main\java'
& "$jdk\bin\javac.exe" -encoding UTF-8 -source 1.6 -target 1.6 -nowarn `
    -bootclasspath 'D:\Java\jdk7\jre\lib\rt.jar' `
    -classpath $cp -sourcepath '.' -d "..\..\..\$classes" @entryPoints
if ($LASTEXITCODE -ne 0) { Pop-Location; throw 'javac failed' }
Pop-Location

Copy-Item -Path 'src\main\resources\*' -Destination $classes -Recurse -Force

& "$jdk\bin\jar.exe" cf $devJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'dev jar failed' }

# The fat jar gives SpecialSource the full Minecraft hierarchy, which is required to map
# inherited members written through a subclass reference.
Push-Location $fat
& "$jdk\bin\jar.exe" xf "..\..\libs\minecraft-1.6.4-mcp.jar"
& "$jdk\bin\jar.exe" xf "..\..\libs\forge-1.6.4-mcp.jar"
& "$jdk\bin\jar.exe" xf "..\..\$devJar"
Pop-Location
& "$jdk\bin\jar.exe" cf 'build\fat.jar' -C $fat .
if ($LASTEXITCODE -ne 0) { throw 'fat jar failed' }

& "$jdk\bin\java.exe" -jar 'libs\SpecialSource-1.11.0-shaded.jar' -i 'build\fat.jar' -o 'build\fat-remapped.jar' -m 'libs\mcp-to-srg.srg'
if ($LASTEXITCODE -ne 0) { throw 'reobf failed' }

Push-Location $stage
& "$jdk\bin\jar.exe" xf "..\fat-remapped.jar" com assets mcmod.info logo.png
# The fat jar also contains the Minecraft and Forge asset trees; only our namespace ships.
Get-ChildItem 'assets' -Directory | Where-Object { $_.Name -ne 'infwhat' } |
    Remove-Item -Recurse -Force
Pop-Location
& "$jdk\bin\jar.exe" cf $finalJar -C $stage .
if ($LASTEXITCODE -ne 0) { throw 'final jar failed' }

$hash = (Get-FileHash -Algorithm SHA256 $finalJar)
Write-Output ("{0}  {1} bytes  SHA256 {2}" -f $finalJar, (Get-Item $finalJar).Length, $hash.Hash)
