#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[Parameter(Mandatory)][string]$InputFile)
. "$PSScriptRoot\ArchiveCrypto.ps1"
if(-not(Test-Path -LiteralPath $Root)){[IO.Directory]::CreateDirectory($Root)|Out-Null;Set-SmrPrivate $Root};Assert-SmrPrivate $Root
foreach($d in @('secrets','recovery','public\ack','instances\recepcion\private')){[IO.Directory]::CreateDirectory((Join-Path $Root $d))|Out-Null}
if(Test-Path "$Root\secrets\ack-identity.dpapi"){throw 'Identidad destino ya existe; no sobrescribir ni rotar'}
$m=Get-Content -Raw -LiteralPath ($InputFile+'.json')|ConvertFrom-Json
if($m.kdf -ne 'PBKDF2-HMAC-SHA256' -or $m.iterations -ne 200000 -or (Get-FileHash -LiteralPath $InputFile).Hash.ToLowerInvariant() -ne $m.sha256){throw 'Paquete de recuperación no válido'}
$credential=Get-Credential -UserName 'SMR-transferencia-cifrada' -Message 'Frase privada recibida por canal separado'
$ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($credential.Password);$temp=Join-Path $Root ('secrets\import-'+[guid]::NewGuid().ToString('N')+'.json')
try{
 $pass=[Text.Encoding]::UTF8.GetBytes([Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr));$key=[Security.Cryptography.Rfc2898DeriveBytes]::Pbkdf2($pass,[Convert]::FromBase64String($m.salt),200000,[Security.Cryptography.HashAlgorithmName]::SHA256,64)
 Unprotect-SmrArchive $InputFile $temp $key;$data=Get-Content -Raw -LiteralPath $temp|ConvertFrom-Json -AsHashtable
 if($data.version -ne 1 -or $data.ack.keyId -notmatch '^[A-Za-z0-9_-]{1,64}$' -or $data.ack.publicPem -notlike '-----BEGIN PUBLIC KEY-----*' -or $data.ack.privatePem -notlike '-----BEGIN PRIVATE KEY-----*'){throw 'Identidad importada incompatible'}
 $data.ack.ownerSid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value;Protect-SmrJson $data.ack "$Root\secrets\ack-identity.dpapi"
 [IO.File]::WriteAllText("$Root\public\ack\ack-trust.json",($data.ownTrust|ConvertTo-Json));[IO.File]::WriteAllText("$Root\public\ack\ack-public.pem",$data.ack.publicPem)
 if($data.bearer){Protect-SmrJson $data.bearer "$Root\instances\recepcion\private\producer-token.dpapi"}
 foreach($name in @('backup-key','ack-recovery-key')){if($data.ContainsKey($name)){Protect-SmrJson $data[$name] "$Root\recovery\$name.dpapi"}}
 @{installationId=$data.source.installationId;origin=$data.source.origin;sourceSchema=$data.source.schema;pullEnabled=$false;importedUtc=[DateTime]::UtcNow.ToString('o')}|ConvertTo-Json|Set-Content -LiteralPath "$Root\migration.json"
 Write-Output 'Secretos reprotegidos por DPAPI bajo cuenta/destino actual. TLS y MySQL requieren provisión propia; PULL no habilitado.'
}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr);if($pass){[Array]::Clear($pass)};if($key){[Array]::Clear($key)};if(Test-Path -LiteralPath $temp){Remove-Item -LiteralPath $temp};$credential=$null;$data=$null}
