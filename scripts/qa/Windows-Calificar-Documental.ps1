[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
$available=[uint64](Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory*1024
if($available -lt 4GB){@{scope='CANARIO_LANZADOR_JAVA_NO_PARSER';state='BLOQUEADO_RECURSOS';availablePhysicalBytes=$available;requiredFreeBytes=4GB}|ConvertTo-Json;return}
$root='C:\req\testdata\calificacion-doc-'+[guid]::NewGuid().ToString('N');$null=New-Item -ItemType Directory -Path $root
foreach($name in @('tools','defs','jobs','evidence','tmp','classes')){$null=New-Item -ItemType Directory -Path (Join-Path $root $name)}
[IO.File]::WriteAllText('C:\req\scratch\ultima-calificacion-doc.txt',$root)
& C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /nologo /platform:x64 /target:exe ('/out:'+ (Join-Path $root 'tools\clamscan.exe')) (Join-Path $PSScriptRoot 'CanarioDocumentalWindows.cs') *> (Join-Path $root 'csc.txt')
if($LASTEXITCODE){throw 'Canario no compila'}
$repo=Split-Path (Split-Path $PSScriptRoot);$cp=(Join-Path $repo 'siger-institucional\backend\target\classes')+';C:\req\bin\lib\*';$jdk='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin'
& (Join-Path $jdk 'javac.exe') -J-Xmx256m -J-XX:-UsePerfData -cp $cp -d (Join-Path $root 'classes') (Join-Path $PSScriptRoot 'CalificarDocumentalWindows.java') *> (Join-Path $root 'javac.txt')
if($LASTEXITCODE){throw 'Harness de calificacion no compila'}
& (Join-Path $jdk 'java.exe') --enable-native-access=ALL-UNNAMED -XX:-UsePerfData -Xmx256m ('-Djava.io.tmpdir='+ (Join-Path $root 'tmp')) -cp ((Join-Path $root 'classes')+';'+$cp) CalificarDocumentalWindows $root > (Join-Path $root 'stdout.txt') 2> (Join-Path $root 'stderr.txt')
$exit=$LASTEXITCODE
if(Test-Path (Join-Path $root 'qualification.json')){Copy-Item (Join-Path $root 'qualification.json') ('C:\req\evidencias\calificacion-doc-'+ (Split-Path -Leaf $root)+'.json');Get-Content (Join-Path $root 'qualification.json')}
if($exit){throw ('Calificacion no acreditada; conservar '+$root)}
