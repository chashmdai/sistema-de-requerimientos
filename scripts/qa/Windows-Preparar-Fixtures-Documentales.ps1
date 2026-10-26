[CmdletBinding()]
param([Parameter(Mandatory)][string]$Python)
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
$repo=Split-Path (Split-Path $PSScriptRoot)
& (Join-Path $repo 'scripts\Windows-Preparar-Herramientas-Documentales.ps1')|Out-Null
$image=(& (Join-Path $repo 'scripts\Windows-Preparar-Inspector-Oficina.ps1')|ConvertFrom-Json)
$root='C:\req\testdata\documental-win-'+[guid]::NewGuid().ToString('N')
foreach($name in @('fixtures','jobs','evidence')){$null=New-Item -ItemType Directory -Path (Join-Path $root $name)}
& $Python (Join-Path $repo 'scripts\Generar-Fixtures-Documentales.py') (Join-Path $root 'fixtures');if($LASTEXITCODE){throw 'Fixtures no generados'}
& $Python (Join-Path $PSScriptRoot 'Generar-Fixtures-Windows-Extendidos.py') (Join-Path $root 'fixtures');if($LASTEXITCODE){throw 'Fixtures extendidos no generados'}
$f=Join-Path $root 'fixtures';$exe='C:\req\bin\ffmpeg-8.1-shared\bin\ffmpeg.exe'
foreach($ext in @('jpg','webp')){& $exe -v error -threads 1 -f lavfi -i 'color=c=blue:s=32x32:d=0.1' -frames:v 1 -threads 1 (Join-Path $f ('medio.'+$ext));if($LASTEXITCODE){throw 'Fixture imagen no generado'}}
foreach($ext in @('mp3','wav','m4a','ogg')){& $exe -v error -threads 1 -f lavfi -i 'sine=frequency=440:duration=0.2:sample_rate=16000' -ac 1 -threads 1 (Join-Path $f ('medio.'+$ext));if($LASTEXITCODE){throw 'Fixture audio no generado'}}
foreach($ext in @('mp4','mov','webm')){$codec=if($ext -eq 'webm'){'libvpx'}else{'libopenh264'};& $exe -v error -threads 1 -f lavfi -i 'color=c=blue:s=32x32:r=5:d=0.2' -an -c:v $codec -threads 1 -pix_fmt yuv420p (Join-Path $f ('medio.'+$ext));if($LASTEXITCODE){throw 'Fixture video no generado'}}
$config=@{
 antivirus='C:/req/bin/clamav-1.4.6';bases='C:/req/storage/antivirus-defs';'antivirus.manifest'='C:/req/evidencias/clamav-portable-2026-10-26.json'
 qpdf='C:/req/bin/qpdf-12.4.2';'qpdf.manifest'='C:/req/config/qpdf-windows-manifest.json';poppler='C:/req/bin/poppler-26.09.0-0';'poppler.manifest'='C:/req/config/poppler-windows-manifest.json'
 ffmpeg='C:/req/bin/ffmpeg-8.1-shared';'ffmpeg.manifest'='C:/req/config/ffmpeg-windows-manifest.json';java=$image.root;'java.manifest'=$image.manifest;trabajos=(Join-Path $root 'jobs');evidencia=(Join-Path $root 'evidence')
}
$config.GetEnumerator()|Sort-Object Key|ForEach-Object {$_.Key+'='+$_.Value.Replace('\','/')}|Set-Content -LiteralPath (Join-Path $root 'windows.properties')
('windows.servicios='+$root.Replace('\','/')+'/windows.properties')|Set-Content -LiteralPath (Join-Path $root 'test.properties')
$root|Set-Content C:\req\scratch\ultimo-documental-win.txt
@{root=$root;scope='FIXTURES_SINTETICOS_WINDOWS';python='SOLO_GENERADOR_QA_NO_RUNTIME';javaImage=$image.root}|ConvertTo-Json -Compress
