[CmdletBinding()]
param([Parameter(Mandatory)][string]$Run,[ValidateSet('estado','habilitar','pausar')][string]$Accion='estado',[switch]$AutorizadoLaboratorio)
$ErrorActionPreference='Stop'
if(-not $IsWindows -or $Run -notmatch '^C:\\req\\testdata\\runtime-qa-[a-f0-9]{32}$'){throw 'Solo runtime Windows QA'}
$state=Get-Content -LiteralPath (Join-Path $Run 'run.json') -Raw|ConvertFrom-Json
if($state.database -ne 'siger_institucional_windows_qa' -or -not $state.automaticPublicRecovery -or -not $state.manualHumanReview -or $state.realGraph -or $state.integrationProfile -notmatch '^C:\\req\\config\\formulario-wsl-e2e-[a-f0-9]{32}\\perfil\.json$' -or $state.origin -notmatch '^https://localhost:808[01]$'){throw 'No es el consumidor automatico QA'}
$listener=Get-NetTCPConnection -LocalPort ([uri]$state.origin).Port -State Listen -ErrorAction Stop
if(@($listener).Count -ne 1 -or $listener.OwningProcess -ne $state.pid -or $listener.LocalAddress -ne '127.0.0.1'){throw 'Proceso/listener distintos'}
$path=Join-Path ([IO.Path]::GetDirectoryName($state.integrationProfile)) 'conexion.json.dpapi'
$allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')
for($p=$path;$p;$p=[IO.Path]::GetDirectoryName($p)){if((Get-Item -LiteralPath $p -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlace no permitido'}}
foreach($r in (Get-Acl -LiteralPath $path).Access){if($r.AccessControlType -eq 'Allow' -and -not ($r.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'ACL no privada'}}
if($Accion -ne 'estado' -and -not $AutorizadoLaboratorio){throw 'Cambio operacional requiere autorizacion de laboratorio'}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$bytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($path),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
try{
 $credential=[Text.Encoding]::UTF8.GetString($bytes)|ConvertFrom-Json;$headers=@{Authorization=('Bearer '+$credential.operationalToken)}
 $control=Invoke-RestMethod -Uri ($state.origin+'/operacion/v1/control') -Headers $headers -MaximumRedirection 0 -TimeoutSec 10
 if($Accion -ne 'estado'){$desired=$Accion -eq 'habilitar';if($control.recuperar -ne $desired){if($desired -and (-not $control.aceptarNuevos -or $control.contingencia -ne 'NORMAL')){throw 'Recepcion institucional no habilitada'};$headers['If-Match']='"'+$control.revision+'"';$body=@{aceptarNuevos=$control.aceptarNuevos;recuperar=$desired;contingencia=$control.contingencia;motivo='PRUEBA_LOCAL'}|ConvertTo-Json -Compress;$control=Invoke-RestMethod -Uri ($state.origin+'/operacion/v1/control') -Method Put -Headers $headers -Body $body -ContentType application/json -MaximumRedirection 0 -TimeoutSec 10}}
 $scheduler=Invoke-RestMethod -Uri ($state.origin+'/operacion/v1/formulario-completo/estado') -Headers $headers -MaximumRedirection 0 -TimeoutSec 10
 @{control=$control;consumidor=$scheduler;automatico=$true;origin=$state.origin;run=$Run;manualRecoveryInvocations=0}|ConvertTo-Json -Depth 5
}catch{throw 'Estado automatico QA no acreditado; detalles privados y HTTP omitidos'}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes);$credential=$null;if($headers){$headers.Clear()}}
