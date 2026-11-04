#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[string]$NssmPath='',[string]$PowerShellPath='',[Parameter(Mandatory)][pscredential]$ServiceCredential)
. "$PSScriptRoot\Common.ps1"
if(-not([Security.Principal.WindowsPrincipal]::new([Security.Principal.WindowsIdentity]::GetCurrent())).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){throw 'Se requiere administrador para registrar SOLO SMR; no instalado'}
$name='SMRRecepcion';if(Get-Service -Name $name -ErrorAction SilentlyContinue){throw 'Servicio ya existente: inspeccionar; no sobrescribir'}
$c=Get-Content -Raw "$Root\instances\recepcion\instance.json"|ConvertFrom-Json
if(-not $NssmPath){$NssmPath=Join-Path $c.release 'runtime\service\nssm.exe'};if(-not $PowerShellPath){$PowerShellPath=Join-Path $c.release 'runtime\pwsh\pwsh.exe'}
$sid=([Security.Principal.NTAccount]::new($ServiceCredential.UserName)).Translate([Security.Principal.SecurityIdentifier]).Value
if($sid -ne $c.ownerSid){throw 'DPAPI debe reprovisionarse bajo la cuenta efectiva antes de instalar'}
foreach($file in @($NssmPath,$PowerShellPath,"$($c.javaHome)\bin\java.exe")){if(-not(Test-Path -LiteralPath $file)){throw 'Ejecutable explícito requerido'}}
if([IO.Path]::GetFullPath($c.javaHome) -ne (Join-Path ([IO.Path]::GetFullPath($c.release)) 'runtime\java25')){throw 'Java25 debe ser propio del paquete SMR'}
$platform=Get-Content -Raw "$Root\mysql\platform.json"|ConvertFrom-Json
if($platform.scope -eq 'SOLO_SMR'){
 if(Get-Service SMRMySQL -ErrorAction SilentlyContinue){$service=Get-CimInstance Win32_Service -Filter "Name='SMRMySQL'";if(-not $service.PathName.Contains($platform.config)){throw 'SMRMySQL existente de otro destino'}}else{
  & "$($platform.home)\bin\mysqld.exe" --install SMRMySQL "--defaults-file=$($platform.config)";if($LASTEXITCODE){throw 'Registro de motor propio no acreditado'}
  Set-Service -Name SMRMySQL -StartupType Manual
 }
}
# La cuenta y su password pasan al SCM como objeto privado, nunca a argumentos de proceso.
New-Service -Name $name -BinaryPathName "`"$NssmPath`"" -Credential $ServiceCredential -StartupType Manual -DisplayName 'SMR recepción independiente'|Out-Null
& $NssmPath set $name Application $PowerShellPath;if($LASTEXITCODE){throw 'Configurar servicio pendiente'}
& $NssmPath set $name AppDirectory $Root
& $NssmPath set $name AppParameters "-NoProfile -NonInteractive -File `"$($c.release)\scripts\Run-Smr.ps1`" -Root `"$Root`" -Instance recepcion"
& $NssmPath set $name AppExit Default Restart
& $NssmPath set $name AppRestartDelay 10000
if($platform.scope -eq 'SOLO_SMR'){& $NssmPath set $name DependOnService SMRMySQL}
# Nunca establecer AppEnvironment, JAVA_HOME ni PATH; el launcher elige runtime/java25 explícitamente.
Write-Output 'Servicio SMRRecepcion registrado en Manual, sin iniciarlo. Calificar cuenta/DPAPI/drenaje con TI antes de activar inicio automático.'
