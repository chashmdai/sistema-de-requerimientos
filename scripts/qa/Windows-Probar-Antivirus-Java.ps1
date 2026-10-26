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
# El compilador termina antes de medir/ejecutar el adaptador. Mismos límites; no mantener javac
# dentro del proceso antivirus por el modo source-file de java (el backend tampoco lo hace).
$qaClasses=Join-Path $root 'qa-classes';$null=New-Item -ItemType Directory -Path $qaClasses
& 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\javac.exe' -J-Xmx256m -J-XX:-UsePerfData ('-J-Djava.io.tmpdir='+$javaTemp) -cp ($classes+';C:\req\bin\lib\*') -d $qaClasses (Join-Path $PSScriptRoot 'AntivirusWindowsReal.java') *> (Join-Path $root 'javac.txt')
if($LASTEXITCODE -ne 0){throw 'No se pudo compilar el harness; conservar log privado, no ejecutar scanner'}
$launchAvailable=[uint64](Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory*1024
@{utc=[DateTimeOffset]::UtcNow.ToString('o');availablePhysicalBeforeCompileBytes=$available;availablePhysicalBeforeLaunchBytes=$launchAvailable;requiredFreeBytes=4GB;plannedJobLimitBytes=1536MB;javaHeapLimitBytes=256MB;cpuLimitSeconds=45;deadlineMilliseconds=90000}|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $root 'preflight.json')
if($launchAvailable -lt 4GB){$json=@{scope='JAVA_AV_WINDOWS_REAL_NO_RUNTIME_COMPLETO';state='BLOQUEADO_RECURSOS';root=$root;availablePhysicalBytes=$launchAvailable;requiredFreeBytes=4GB;scannerExecuted=$false;fullDocumentalRuntime=$false}|ConvertTo-Json;[IO.File]::WriteAllText($record,$json);$json;return}
$json=@(& 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe' --enable-native-access=ALL-UNNAMED -XX:-UsePerfData -Xmx256m ('-Djava.io.tmpdir='+$javaTemp) -cp ($qaClasses+';'+$classes+';C:\req\bin\lib\*') AntivirusWindowsReal $root 2> (Join-Path $root 'java-stderr.txt'))
$exit=$LASTEXITCODE
$json|Set-Content -LiteralPath (Join-Path $root 'java-stdout.txt')
if(Test-Path -LiteralPath (Join-Path $root 'resultado.json')){Copy-Item -LiteralPath (Join-Path $root 'resultado.json') -Destination $record}
else{@{scope='JAVA_AV_WINDOWS_REAL_NO_RUNTIME_COMPLETO';state='FALLO_HARNESS';root=$root;exit=$exit;fullDocumentalRuntime=$false}|ConvertTo-Json|Set-Content -LiteralPath $record}
Get-Content -LiteralPath $record
if($exit -ne 0){throw ('Candidato AV no acreditado; revisar evidencia privada: '+$record)}
