[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
$id=[guid]::NewGuid().ToString('N');$record="C:\req\evidencias\junit-av-real-$id.json"
$available=[uint64](Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory*1024
if($available -lt 4GB){$json=@{scope='JUNIT_AV_REAL_WINDOWS';state='BLOQUEADO_RECURSOS';availablePhysicalBytes=$available;requiredFreeBytes=4GB;scannerExecuted=$false;fullDocumentalRuntime=$false}|ConvertTo-Json;[IO.File]::WriteAllText($record,$json);$json;return}
$repo=Split-Path (Split-Path $PSScriptRoot)
$backend=Join-Path $repo 'siger-institucional\backend'
$report=Join-Path $backend 'target\surefire-reports\TEST-cl.nexbyte.siger.institucional.formulario.ServiciosDocumentalesRealesTest.xml'
$xml=[xml](Get-Content -LiteralPath $report -Raw)
$cp=($xml.testsuite.properties.property|Where-Object name -eq 'surefire.test.class.path').value
$engine=@($cp -split ';'|Where-Object {$_ -match 'junit-platform-engine-[0-9.]+\.jar$'})
if($engine.Count -ne 1){throw 'Classpath JUnit no comprobado; compilar test-compile con Maven'}
$version=[regex]::Match((Split-Path -Leaf $engine[0]),'^junit-platform-engine-([0-9.]+)\.jar$').Groups[1].Value
$platform=Split-Path (Split-Path (Split-Path $engine[0]))
$launcher=Join-Path $platform ("junit-platform-launcher\$version\junit-platform-launcher-$version.jar")
if(-not (Test-Path -LiteralPath $launcher)){throw 'Falta launcher JUnit de la misma version; no descargar ni mezclar automaticamente'}
$cp+=';'+$launcher
$root='C:\req\testdata\av-junit-'+$id
for($p='C:\req\testdata';$p;$p=Split-Path -Parent $p){if((Get-Item -LiteralPath $p -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Raiz QA no valida'}}
$null=New-Item -ItemType Directory -Path $root
foreach($name in @('tmp','classes','jobs','evidence','fixtures')){$null=New-Item -ItemType Directory -Path (Join-Path $root $name)}
[IO.File]::WriteAllText('C:\req\scratch\ultimo-av-junit.txt',$root)
[IO.File]::WriteAllBytes((Join-Path $root 'fixtures\control-antivirus.txt'),[Convert]::FromBase64String('WDVPIVAlQEFQWzRcUFpYNTQoUF4pN0NDKTd9JEVJQ0FSLVNUQU5EQVJELUFOVElWSVJVUy1URVNULUZJTEUhJEgrSCo='))
$config=Join-Path $root 'real.properties'
@('institucional.documental.antivirus-proveedor=WINDOWS_CLAMAV','windows.manifest=C:/req/evidencias/clamav-portable-2026-10-26.json','windows.tools=C:/req/bin/clamav-1.4.6','windows.definitions=C:/req/storage/antivirus-defs',('windows.jobs='+((Join-Path $root 'jobs').Replace('\','/'))),('windows.evidence='+((Join-Path $root 'evidence').Replace('\','/'))))|Set-Content -LiteralPath $config
$jdk='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin'
& (Join-Path $jdk 'javac.exe') -J-Xmx256m -J-XX:-UsePerfData -cp $cp -d (Join-Path $root 'classes') (Join-Path $PSScriptRoot 'ControlAntivirusJUnitWindows.java') *> (Join-Path $root 'javac.txt')
if($LASTEXITCODE){throw 'Compilacion de launcher JUnit fallida; conservar evidencia'}
$beforeConfig=$env:DOCUMENTAL_REAL_CONFIG;$beforeFixtures=$env:DOCUMENTAL_REAL_FIXTURES
try{$env:DOCUMENTAL_REAL_CONFIG=$config;$env:DOCUMENTAL_REAL_FIXTURES=Join-Path $root 'fixtures'; & (Join-Path $jdk 'java.exe') --enable-native-access=ALL-UNNAMED -XX:-UsePerfData -Xmx256m ('-Djava.io.tmpdir='+ (Join-Path $root 'tmp')) -cp ((Join-Path $root 'classes')+';'+$cp) ControlAntivirusJUnitWindows $root > (Join-Path $root 'stdout.txt') 2> (Join-Path $root 'stderr.txt');$exit=$LASTEXITCODE}finally{$env:DOCUMENTAL_REAL_CONFIG=$beforeConfig;$env:DOCUMENTAL_REAL_FIXTURES=$beforeFixtures}
if(Test-Path -LiteralPath (Join-Path $root 'junit-resultado.json')){Copy-Item -LiteralPath (Join-Path $root 'junit-resultado.json') -Destination $record;Get-Content -LiteralPath $record}else{throw ('Launcher no concluyo; revisar '+$root)}
if($exit){throw ('Control real no acreditado; revisar '+$record)}
