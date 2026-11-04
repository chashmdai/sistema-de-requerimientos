#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr')
. "$PSScriptRoot\Common.ps1"
$path="$Root\instances\recepcion\private\producer-token.dpapi"
if(Test-Path -LiteralPath $path){throw 'Bearer ya presente: no sobrescribir ni rotar por rutina'}
$secret=Read-Host 'Bearer existente entregado por Ubuntu (entrada privada; no pegar en chat)' -AsSecureString
$ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try{$plain=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr);if($plain -notmatch '^[A-Za-z0-9_-]{32,400}$'){throw 'Formato contractual no admitido'};Protect-SmrJson @{token=$plain} $path}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr);$plain=$null;$secret.Dispose()}
Write-Output 'Bearer protegido con DPAPI. Consumidor público permanece desactivado.'
