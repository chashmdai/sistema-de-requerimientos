[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
# No acepta ejecutables/documentos/rutas externos ni cambia servicios o politicas.
$root='C:\req\testdata\isolation-'+[guid]::NewGuid().ToString('N')
$null=New-Item -ItemType Directory -Path $root
$compiler='C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe'
foreach($name in @('Canario','Aislamiento')){
 & $compiler /nologo /platform:x64 /target:exe ('/out:'+(Join-Path $root ($name+'.exe'))) (Join-Path $PSScriptRoot ('windows-isolation\'+$name+'.cs'))
 if($LASTEXITCODE -ne 0){throw 'Compilacion de sonda no correcta'}
}
[IO.File]::WriteAllText((Join-Path $root 'canario-externo.txt'),'SINTETICO')
$exe=Join-Path $root 'Aislamiento.exe'
$results=[Collections.Generic.List[object]]::new()
$listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,0)
$listener.Start()
try{
 # Positivo: el mismo listener acepta al proceso confiable antes del negativo.
 $control=[Net.Sockets.TcpClient]::new();$control.Connect('127.0.0.1',$listener.LocalEndpoint.Port)
 $accepted=$listener.AcceptTcpClient();$accepted.Dispose();$control.Dispose()
 $argsProbe='"'+$root+'\canario-externo.txt" "'+$root+'\write-denied.txt" '+$listener.LocalEndpoint.Port
 foreach($mode in @('probe','memory','cpu','timeout','crash','noop')){
  $limit=if($mode -eq 'cpu'){10000}else{4000}
  $line=& $exe $root $mode $argsProbe $limit
  if($LASTEXITCODE -ne 0){throw ('Fallo de API en sonda '+$mode)}
  $text=($line -join '').Trim()
  $expected=switch($mode){'probe'{'exit=0;timeout=False'}'memory'{'exit=0;timeout=False'}'cpu'{'exit=3221225540;timeout=False'}'timeout'{'exit=124;timeout=True'}'crash'{'exit=37;timeout=False'}'noop'{'exit=90;timeout=False'}}
  if(-not $text.Contains($expected) -or -not $text.EndsWith(';profileDeleted=true')){throw ('Resultado de sonda no comprobado: '+$mode)}
  if($text -notmatch ';pid=([0-9]+);'){throw 'Falta identidad de proceso'}
  $childId=[int]$Matches[1]
  if(Get-Process -Id $childId -ErrorAction SilentlyContinue){throw 'Hijo no terminado'}
  $results.Add(@{mode=$mode;result=$text;orphan=$false})
 }
 if($listener.Pending()){throw 'La conexion aislada alcanzo el listener'}
 if([IO.File]::ReadAllText((Join-Path $root 'canario-externo.txt')) -ne 'SINTETICO' -or (Test-Path -LiteralPath (Join-Path $root 'write-denied.txt'))){throw 'Canario externo alterado'}
}finally{$listener.Stop()}
$allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544','S-1-3-0')
foreach($item in (@(Get-Item -LiteralPath $root)+@(Get-ChildItem -LiteralPath $root -Recurse -Force))){foreach($rule in (Get-Acl -LiteralPath $item.FullName).Access){if($rule.AccessControlType -eq 'Allow' -and $rule.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'La sonda dejo permisos de paquete residuales'}}}
$evidence=@{date='2026-10-26';root=$root;scope='SONDA_QA_NO_BROKER';capabilities=0;controlledListenerPositive=$true;controlledListenerReachedByChild=$false;results=$results;realParsersExecuted=0;realAntivirusExecuted=0;workerSecurityEquivalence=$false;warnings=@('TCP acotado sin conexion; no prueba de todos los protocolos o destinos','No prueba cuota de disco, DLL/parser reales ni rechazo de malware','No runtime documental ni servicio habilitados')}
$evidence | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $root 'resultado.json')
$evidence | ConvertTo-Json -Depth 8 | Set-Content C:\req\evidencias\sonda-proceso-appcontainer-2026-10-26.json
@{scope=$evidence.scope;probes=$results.Count;root=$root;workerSecurityEquivalence=$false}|ConvertTo-Json -Compress
