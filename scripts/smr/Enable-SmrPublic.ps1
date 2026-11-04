#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[Parameter(Mandatory)][string]$ApprovalFile)
. "$PSScriptRoot\Common.ps1"
Assert-SmrPrivate $ApprovalFile;$a=Get-Content -Raw -LiteralPath $ApprovalFile|ConvertFrom-Json
foreach($flag in @('custodiaEnEstePc','todaColaElegible','receptorUnico','respaldoVerificado','confianzaAckInstalada','pullUbuntuAbierto')){if($a.$flag -cne $true){throw "Confirmación concreta pendiente: $flag"}}
if(-not $a.referencia -or [DateTimeOffset]::Parse($a.ventanaHastaUtc) -le [DateTimeOffset]::UtcNow){throw 'Ventana/referencia de autorización requerida'}
$manifest=Get-Content -Raw "$Root\public\ack\manifest.json"|ConvertFrom-Json
if($a.ackTrustSha256 -ne $manifest.trustJsonSha256){throw 'Confianza ACK no corresponde a esta identidad'}
if(-not(Test-Path "$Root\instances\recepcion\private\producer-token.dpapi")){throw 'Falta Bearer privado'}
$c=Get-Content -Raw "$Root\instances\recepcion\instance.json"|ConvertFrom-Json
if(Get-NetTCPConnection -LocalPort $c.port -State Listen -ErrorAction SilentlyContinue){throw 'Pausar/drenar y detener únicamente recepción antes de cambiar su configuración'}
$probe=Get-Content -Raw "$Root\public\SONDA_PUBLICA.json"|ConvertFrom-Json
if($probe.sinCredencial -ne 401 -or $probe.credencialInvalida -ne 401 -or $probe.autenticada -ne 404){throw 'Falta sonda Java válida 401/401/404'}
Copy-Item -LiteralPath $ApprovalFile -Destination "$Root\instances\recepcion\autorizacion-publica.json" -ErrorAction Stop
$c.consumerEnabled=$true;$c|ConvertTo-Json|Set-Content -LiteralPath "$Root\instances\recepcion\instance.json"
Write-Output 'Transporte público configurado. Iniciar solo SMR recepción y activar el consumidor mediante la interfaz autenticada en la ventana autorizada.'
