[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
$id=[guid]::NewGuid().ToString('N')
$record="C:\req\evidencias\antivirus-java-$id.json"
$available=[uint64](Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory*1024
if($available -lt 4GB){
 $json=@{scope='JAVA_AV_WINDOWS_REAL_NO_RUNTIME_COMPLETO';state='BLOQUEADO_RECURSOS';utc=[DateTimeOffset]::UtcNow.ToString('o');availablePhysicalBytes=$available;requiredFreeBytes=4GB;plannedJobLimitBytes=1536MB;scannerExecuted=$false;fullDocumentalRuntime=$false}|ConvertTo-Json
 [IO.File]::WriteAllText($record,$json);$json;return
}
$repo=Split-Path (Split-Path $PSScriptRoot)
$classes=Join-Path $repo 'siger-institucional\backend\target\classes'
if(-not (Test-Path -LiteralPath (Join-Path $classes 'cl\nexbyte\siger\institucional\documental\AntivirusDocumentoWindows.class'))){throw 'Compilar primero el candidato AV con Java25; no se reutiliza un JAR activo'}
$root='C:\req\testdata\av-java-'+$id
for($p='C:\req\testdata';$p;$p=Split-Path -Parent $p){if((Get-Item -LiteralPath $p -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Raiz QA no admitida'}}
$null=New-Item -ItemType Directory -Path $root
$javaTemp=Join-Path $root 'java-temp';$null=New-Item -ItemType Directory -Path $javaTemp
[IO.File]::WriteAllText('C:\req\scratch\ultimo-av-java.txt',$root)
$json=@(& 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe' --enable-native-access=ALL-UNNAMED -XX:-UsePerfData -Xmx256m ('-Djava.io.tmpdir='+$javaTemp) -cp ($classes+';C:\req\bin\lib\*') (Join-Path $PSScriptRoot 'AntivirusWindowsReal.java') $root 2> (Join-Path $root 'java-stderr.txt'))
$exit=$LASTEXITCODE
$json|Set-Content -LiteralPath (Join-Path $root 'java-stdout.txt')
if(Test-Path -LiteralPath (Join-Path $root 'resultado.json')){Copy-Item -LiteralPath (Join-Path $root 'resultado.json') -Destination $record}
else{@{scope='JAVA_AV_WINDOWS_REAL_NO_RUNTIME_COMPLETO';state='FALLO_HARNESS';root=$root;exit=$exit;fullDocumentalRuntime=$false}|ConvertTo-Json|Set-Content -LiteralPath $record}
Get-Content -LiteralPath $record
if($exit -ne 0){throw ('Candidato AV no acreditado; revisar evidencia privada: '+$record)}
