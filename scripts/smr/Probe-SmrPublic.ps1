#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[switch]$WithBearer)
. "$PSScriptRoot\Common.ps1"
$c=Get-Content -Raw "$Root\instances\recepcion\instance.json"|ConvertFrom-Json
$p=@{'institucional.formulario-completo.origen-publico'=$c.origin;'institucional.formulario-completo.ca-certificado'="$Root\config\public-edge-root-ca.crt"}
if($WithBearer){$token=Read-SmrJson "$Root\instances\recepcion\private\producer-token.dpapi";$p['institucional.formulario-completo.token']=$token.token}
$result=Invoke-SmrJava "$($c.javaHome)\bin\java.exe" @('-XX:-UsePerfData','-Xmx128m','-jar',"$($c.release)\smr-rc1.jar",'--smr-probe') ($p|ConvertTo-Json -Compress) "$Root\logs\public-probe.log" "$Root\logs\public-probe.err.log"
$p.Clear();$result.Trim()|Set-Content -LiteralPath "$Root\public\SONDA_PUBLICA.json";Write-Output $result
