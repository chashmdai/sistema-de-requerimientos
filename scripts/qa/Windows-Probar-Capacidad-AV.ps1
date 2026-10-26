[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
$id=[guid]::NewGuid().ToString('N')
$latest='C:\req\evidencias\capacidad-av-windows-2026-10-26.json'
$evidencePath="C:\req\evidencias\capacidad-av-$id.json"
# Conservar incluso un intento parcial anterior: nunca sustituir su unica evidencia.
if(Test-Path -LiteralPath $latest){Copy-Item -LiteralPath $latest -Destination "C:\req\evidencias\capacidad-av-anterior-$id.json"}
$available=[uint64](Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory*1024
$evidence=[ordered]@{scope='QA_AV_CAPACIDAD';utc=[DateTimeOffset]::UtcNow.ToString('o');state='PREFLIGHT';availablePhysicalBytes=$available;requiredFreeBytes=4GB;plannedJobLimitBytes=1536MB;cpuLimitSeconds=45;deadlineMilliseconds=90000;processLimit=1;scannerExecuted=$false;fullDocumentalRuntime=$false;root=$null;results=@();failure=$null;evidence=$evidencePath}
function Save-Evidence {
 $json=$evidence|ConvertTo-Json -Depth 9
 [IO.File]::WriteAllText($evidencePath,$json)
 [IO.File]::WriteAllText($latest,$json)
 if($evidence.root){[IO.File]::WriteAllText((Join-Path $evidence.root 'resultado-capacidad.json'),$json)}
}
if($available -lt 4GB){$evidence.state='BLOQUEADO_RECURSOS';Save-Evidence;Get-Content -LiteralPath $evidencePath;return}
$root='C:\req\testdata\isolation-'+$id
for($p='C:\req\testdata';$p;$p=Split-Path -Parent $p){if((Get-Item -LiteralPath $p -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Reparse point no admitido'}}
$null=New-Item -ItemType Directory -Path $root
$evidence.root=$root
Save-Evidence
try{
 foreach($n in @('Canario','Aislamiento')){
  $compile=@(& C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /nologo /platform:x64 /target:exe ('/out:'+(Join-Path $root ($n+'.exe'))) (Join-Path $PSScriptRoot ('windows-isolation\'+$n+'.cs')) 2>&1)
  $compile|Set-Content -LiteralPath (Join-Path $root ($n+'-compilacion.txt'))
  if($LASTEXITCODE){throw 'Compilacion de sonda fallida'}
 }
 $failed=$false
 foreach($mode in @('av-capacity-clean','av-capacity-eicar','av-unavailable')){
  $text=((& (Join-Path $root 'Aislamiento.exe') $root $mode none 90000)-join '').Trim();$brokerExit=$LASTEXITCODE
  [IO.File]::WriteAllText((Join-Path $root ($mode+'-broker.txt')),$text)
  $expected=switch($mode){'av-capacity-clean'{0}'av-capacity-eicar'{1}'av-unavailable'{2}}
  $fields=@{};foreach($field in ($text -split ';')){if($field -match '^([^=]+)=(.*)$'){$fields[$Matches[1]]=$Matches[2]}}
  $work=Join-Path $root ('work-'+$mode);$inputPath=Join-Path $work 'input.txt';$logs=@()
  foreach($name in @('salida.log','stderr.log')){$path=Join-Path $work $name;if(Test-Path -LiteralPath $path){$logs+=@{stream=if($name -eq 'salida.log'){'stdout'}else{'stderr'};path=$path;bytes=(Get-Item -LiteralPath $path).Length;sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()}}}
  $childAlive=$false;if($fields.pid -match '^\d+$'){$childAlive=$null -ne (Get-Process -Id ([int]$fields.pid) -ErrorAction SilentlyContinue)}
  $inputExists=Test-Path -LiteralPath $inputPath -PathType Leaf
  $correct=$brokerExit -eq 0 -and $fields.exit -eq [string]$expected -and $fields.timeout -eq 'False' -and $fields.profileDeleted -eq 'true' -and $fields.appContainer -eq 'true' -and $fields.capabilities -eq '0' -and $fields.processLimit -eq '1' -and $fields.killOnJobClose -eq 'true' -and -not $childAlive
  $diagnostic=if($logs.Count){($logs|ForEach-Object {Get-Content -LiteralPath $_.path -Raw}) -join "`n"}else{''}
  if($mode -eq 'av-capacity-clean'){$correct=$correct -and $diagnostic.Contains('input.txt: OK') -and $inputExists}
  if($mode -eq 'av-capacity-eicar'){$correct=$correct -and $diagnostic.Contains('Eicar-Test-Signature FOUND') -and $inputExists}
  if($mode -eq 'av-unavailable'){$correct=$correct -and $diagnostic.Contains('cl_load(): No such file or directory:') -and -not $diagnostic.Contains('input.txt: OK')}
  $evidence.results+=@{mode=$mode;expectedScannerExit=$expected;brokerExit=$brokerExit;fields=$fields;correct=[bool]$correct;orphan=$childAlive;inputExists=$inputExists;inputBytes=if($inputExists){(Get-Item -LiteralPath $inputPath).Length}else{$null};inputSha256=if($inputExists){(Get-FileHash -LiteralPath $inputPath -Algorithm SHA256).Hash.ToLowerInvariant()}else{$null};logs=$logs;temporaries=@(Get-ChildItem -LiteralPath $work -Recurse -Force|Select-Object Name,Length,Attributes)}
  if($fields.pid){$evidence.scannerExecuted=$true}
  if(-not $correct){$failed=$true}
  Save-Evidence
  # No continuar si la frontera o su limpieza fallan; un rechazo de contenido no es un fallo de la frontera.
  if($brokerExit -ne 0 -or $childAlive -or $fields.profileDeleted -ne 'true'){throw 'Fallo de aislamiento o limpieza; revisar evidencia del intento'}
 }
 $allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544','S-1-3-0')
 foreach($base in @($root,'C:\req\bin\clamav-1.4.6','C:\req\storage\antivirus-defs')){
  foreach($item in (@(Get-Item -LiteralPath $base)+@(Get-ChildItem -LiteralPath $base -Recurse -Force))){foreach($rule in (Get-Acl -LiteralPath $item.FullName).Access){if($rule.AccessControlType -eq 'Allow' -and $rule.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'Permiso residual de sonda'}}}
 }
 $evidence.aclResidualAllowCount=0
 $evidence.state=if($failed){'FALLO_ESCENARIO'}else{'SCANNER_PROBADO_NO_INTEGRADO'}
}catch{$evidence.state='FALLO_ENSAYO';$evidence.failure=$_.Exception.GetType().Name;Save-Evidence;throw ('Ensayo no acreditado; evidencia: '+$evidencePath)}
Save-Evidence
Get-Content -LiteralPath $evidencePath
if($evidence.state -ne 'SCANNER_PROBADO_NO_INTEGRADO'){throw ('Escenario no acreditado; evidencia: '+$evidencePath)}
