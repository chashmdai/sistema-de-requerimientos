#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[ValidateSet('recepcion','sintetico')][string]$Instance='recepcion')
. "$PSScriptRoot\Common.ps1"
$base="$Root\instances\$Instance";$c=Get-Content -Raw "$base\instance.json"|ConvertFrom-Json;$db=Read-SmrJson "$Root\secrets\$($c.schema)_migrator.dpapi"
$p=@{'institucional.db.url'=$c.dbUrl;'institucional.db.user'=$db.username;'institucional.db.password'=$db.password}
Invoke-SmrJava (Join-Path $c.javaHome 'bin\java.exe') @('-XX:-UsePerfData','-Xmx192m','-jar',"$($c.release)\smr-rc1.jar",'--smr-migrate') ($p|ConvertTo-Json -Compress) "$base\logs\migration.log" "$base\logs\migration.err.log"|Out-Null
$p.Clear();$db.Clear();Write-Output "Migraciones SMR completas en $($c.schema); runtime usa otra identidad DML."
