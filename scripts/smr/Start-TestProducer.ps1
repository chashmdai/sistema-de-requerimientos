#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[switch]$Foreground)
. "$PSScriptRoot\Common.ps1"
$base="$Root\instances\sintetico";$c=Get-Content -Raw "$base\instance.json"|ConvertFrom-Json
if(-not $Foreground){
 if(Get-NetTCPConnection -LocalPort 19443 -State Listen -ErrorAction SilentlyContinue){throw 'Productor ya escucha'}
 $pwsh=Join-Path $c.release 'runtime\pwsh\pwsh.exe';if(-not(Test-Path -LiteralPath $pwsh)){$pwsh=(Get-Process -Id $PID).Path};$startup=New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ShowWindow=[uint16]0}
 $r=Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{CommandLine="`"$pwsh`" -NoProfile -File `"$PSCommandPath`" -Root `"$Root`" -Foreground";ProcessStartupInformation=$startup};if($r.ReturnValue){throw 'Productor no iniciado'};Write-Output "Launcher productor sintético: PID $($r.ProcessId)";return
}
$producer=Read-SmrJson "$base\private\producer.dpapi";$ack=Read-SmrJson "$base\private\ack.dpapi";$token=Read-SmrJson "$base\private\producer-token.dpapi";$tls=Read-SmrJson "$Root\secrets\tls-password.dpapi"
$p=@{root="$base\producer";port=19443;token=$token.token;keyId=$producer.keyId;privatePem=$producer.privatePem;ackKeyId=$ack.keyId;ackPublicPem=$ack.publicPem;tls="$Root\secrets\tls\localhost.p12";tlsPassword=$tls.password}
Invoke-SmrJava (Join-Path $c.javaHome 'bin\java.exe') @('-XX:-UsePerfData','-Xmx128m','--class-path',"$($c.release)\tools\core.jar;$($c.release)\lib\*","$PSScriptRoot\testing\LocalProducer.java") ($p|ConvertTo-Json -Compress) "$base\logs\producer.log" "$base\logs\producer.err.log"|Out-Null
