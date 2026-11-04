#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[Parameter(Mandatory)][string]$Destination)
. "$PSScriptRoot\ArchiveCrypto.ps1"
if(Test-Path -LiteralPath $Destination){throw 'Destino cifrado debe ser nuevo'}
$credential=Get-Credential -UserName 'SMR-transferencia-cifrada' -Message 'Frase privada de recuperación (mínimo 20 caracteres). Entregar separadamente por canal aprobado.'
if(-not $credential -or $credential.Password.Length -lt 20){throw 'Frase de recuperación insuficiente'}
$ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($credential.Password);$salt=[Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
$temp=Join-Path $Root ('secrets\export-'+[guid]::NewGuid().ToString('N')+'.json')
try{
 $pass=[Text.Encoding]::UTF8.GetBytes([Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr));$key=[Security.Cryptography.Rfc2898DeriveBytes]::Pbkdf2($pass,$salt,200000,[Security.Cryptography.HashAlgorithmName]::SHA256,64)
 $data=@{version=1;ack=(Read-SmrJson "$Root\secrets\ack-identity.dpapi");ownTrust=(Get-Content -Raw "$Root\public\ack\ack-trust.json"|ConvertFrom-Json -AsHashtable);source=(Get-Content -Raw "$Root\instances\recepcion\instance.json"|ConvertFrom-Json -AsHashtable)}
 if(Test-Path "$Root\instances\recepcion\private\producer-token.dpapi"){$data.bearer=Read-SmrJson "$Root\instances\recepcion\private\producer-token.dpapi"}
 foreach($name in @('backup-key','ack-recovery-key')){if(Test-Path "$Root\recovery\$name.dpapi"){$data[$name]=Read-SmrJson "$Root\recovery\$name.dpapi"}}
 [IO.File]::WriteAllText($temp,($data|ConvertTo-Json -Depth 15 -Compress));Set-SmrPrivate $temp
 Protect-SmrArchive $temp $Destination $key;Set-SmrPrivate $Destination
 @{version=1;kdf='PBKDF2-HMAC-SHA256';iterations=200000;salt=[Convert]::ToBase64String($salt);encryption='AES-256-CBC-HMAC-SHA256';sha256=(Get-FileHash -LiteralPath $Destination).Hash.ToLowerInvariant()}|ConvertTo-Json|Set-Content -LiteralPath ($Destination+'.json');Set-SmrPrivate ($Destination+'.json')
 Write-Output 'Exportación local cifrada creada. Trasladar solo con autorización; frase por canal separado. No copiar DPAPI como sustituto de reprovisión.'
}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr);if($pass){[Array]::Clear($pass)};if($key){[Array]::Clear($key)};if(Test-Path -LiteralPath $temp){Remove-Item -LiteralPath $temp};$credential=$null;$data=$null}
