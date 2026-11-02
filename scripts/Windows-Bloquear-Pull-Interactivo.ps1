[CmdletBinding()]
param([Parameter(Mandatory)][string]$Run,[Parameter(Mandatory)][string]$Evidencia)
$ErrorActionPreference='Stop'
if(-not $IsWindows -or $Run -notmatch '^C:\\req\\testdata\\runtime-qa-[a-f0-9]{32}$' -or -not ([IO.Path]::GetFullPath($Evidencia)).StartsWith('C:\req\evidencias\')){throw 'Solo QA Windows'}
$state=Get-Content -LiteralPath (Join-Path $Run 'run.json') -Raw|ConvertFrom-Json
if($state.database -ne 'siger_institucional_windows_qa' -or $state.origin -notmatch '^https://localhost:808[01]$' -or $state.automaticPublicRecovery -or $state.integrationProfile -notmatch '^C:\\req\\config\\formulario-wsl-e2e-[a-f0-9]{32}\\perfil\.json$'){throw 'Runtime no corresponde a la preparacion interactiva'}
$path=Join-Path ([IO.Path]::GetDirectoryName($state.integrationProfile)) 'conexion.json.dpapi'
$allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')
for($p=$path;$p;$p=[IO.Path]::GetDirectoryName($p)){if((Get-Item -LiteralPath $p -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlace no permitido'}}
foreach($r in (Get-Acl -LiteralPath $path).Access){if($r.AccessControlType -eq 'Allow' -and -not ($r.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'ACL no privada'}}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$bytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($path),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
try{
 $credential=[Text.Encoding]::UTF8.GetString($bytes)|ConvertFrom-Json;$headers=@{Authorization=('Bearer '+$credential.operationalToken)}
 $before=Invoke-RestMethod -Uri ($state.origin+'/operacion/v1/control') -Headers $headers -MaximumRedirection 0 -TimeoutSec 10
 if($before.recuperar){$headers['If-Match']='"'+$before.revision+'"';$body=@{aceptarNuevos=$before.aceptarNuevos;recuperar=$false;contingencia=$before.contingencia;motivo='PRUEBA_LOCAL'}|ConvertTo-Json -Compress;$after=Invoke-RestMethod -Uri ($state.origin+'/operacion/v1/control') -Method Put -Headers $headers -Body $body -ContentType 'application/json' -MaximumRedirection 0 -TimeoutSec 10}else{$after=$before}
 if($after.recuperar){throw 'Control de recuperacion no cerrado'}
 @{phase='B';before=$before;after=$after;automaticRecovery=$false;publicPulls=0;awaitHumanSubmission=$true;subsequentHumanRecoveryAuthorizationRequired=$true}|ConvertTo-Json -Depth 4|Set-Content -LiteralPath (Join-Path $Evidencia 'control-siger-fase-b.json')
 @{pullEnabled=$false;automaticRecovery=$false;revision=$after.revision;publicPulls=0}|ConvertTo-Json -Compress
}catch{throw 'No se acredito el cierre del pull QA. Valores privados y detalle HTTP omitidos.'}
finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes);$credential=$null;if($headers){$headers.Clear()}}
