[CmdletBinding()]
param([Parameter(Mandatory)][string]$Run,[switch]$AutorizadoE2ELocal)
$ErrorActionPreference='Stop'
# Preparado para una campaña futura autorizada. No se invoca desde los preparadores ni el arranque.
if(-not $IsWindows -or -not $AutorizadoE2ELocal){throw 'Requiere autorizacion expresa de la prueba E2E local'}
$Run=[IO.Path]::GetFullPath($Run)
if($Run -notmatch '^C:\\req\\testdata\\runtime-qa-[a-f0-9]{32}$'){throw 'Solo runtime QA'}
$state=Get-Content -LiteralPath (Join-Path $Run 'run.json') -Raw|ConvertFrom-Json
if(-not $state.publicRecovery -or $state.automaticPublicRecovery -or $state.realGraph -or -not $state.documentWorkers -or $state.database -ne 'siger_institucional_windows_qa' -or $state.origin -notmatch '^https://localhost:808[01]$'){throw 'Runtime no habilitado para una recuperacion manual aislada'}
if($state.root -ne $Run -or (Test-Path -LiteralPath (Join-Path $Run 'stop'))){throw 'Run detenido o distinto'}
$listener=Get-NetTCPConnection -State Listen -LocalPort ([uri]$state.origin).Port -ErrorAction Stop
if(@($listener).Count -ne 1 -or $listener.LocalAddress -ne '127.0.0.1' -or $listener.OwningProcess -ne $state.pid -or (Get-Process -Id $state.pid -ErrorAction Stop).Path -ne 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe'){throw 'Proceso y listener distintos del runtime QA'}
$profilePath=$state.integrationProfile
if(-not $profilePath -or $profilePath -notmatch '^C:\\req\\config\\formulario-wsl-(integracion|e2e-[a-f0-9]{32})\\perfil\.json$'){throw 'Perfil no vinculado al runtime'}
$credentialPath=Join-Path ([IO.Path]::GetDirectoryName($profilePath)) 'conexion.json.dpapi'
$allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')
for($p=$credentialPath;$p;$p=[IO.Path]::GetDirectoryName($p)){if((Get-Item -LiteralPath $p -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlace no admitido'}}
foreach($r in (Get-Acl -LiteralPath $credentialPath).Access){if($r.AccessControlType -eq 'Allow' -and -not ($r.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'Credencial sin ACL privada'}}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$bytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($credentialPath),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
try{
 $credential=[Text.Encoding]::UTF8.GetString($bytes)|ConvertFrom-Json;$headers=@{Authorization=('Bearer '+$credential.operationalToken)}
 $control=Invoke-RestMethod -Uri ($state.origin+'/operacion/v1/control') -Headers $headers -MaximumRedirection 0 -TimeoutSec 10
 if(-not $control.recuperar -or $control.contingencia -ne 'NORMAL'){throw 'Control operacional cerrado; no se modifica automaticamente'}
 # Una sola solicitud. Ante timeout no se repite: verificar COM y conciliacion antes de reintentar.
 $result=Invoke-RestMethod -Method Post -Uri ($state.origin+'/operacion/v1/formulario-completo/recuperar') -Headers $headers -MaximumRedirection 0 -TimeoutSec 300
 @{scope='E2E_LOCAL_MANUAL_UNA_INVOCACION';recuperadas=$result.recuperadas;confirmadas=$result.confirmadas;fallidas=$result.fallidas;solapada=$result.solapada;automaticRetry=$false}|ConvertTo-Json -Compress
}catch{throw 'Recuperacion no acreditada. Sin reenvio automatico: conciliar el estado institucional y publico. Detalle HTTP omitido.'}
finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes);$credential=$null;if($headers){$headers.Clear()}}
