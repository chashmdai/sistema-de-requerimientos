#Requires -Version 7.4
[CmdletBinding()]param([Parameter(Mandatory)][string]$Release,[string]$Evidence='')
. "$PSScriptRoot\Common.ps1"
function Snapshot-SmrGlobalJava {
 return @{machineJavaHome=[Environment]::GetEnvironmentVariable('JAVA_HOME','Machine');userJavaHome=[Environment]::GetEnvironmentVariable('JAVA_HOME','User');machinePath=[Environment]::GetEnvironmentVariable('PATH','Machine');userPath=[Environment]::GetEnvironmentVariable('PATH','User');resolvedJava=(Get-Command java.exe).Source;resolvedJavaHash=(Get-FileHash (Get-Command java.exe).Source).Hash}
}
$before=Snapshot-SmrGlobalJava;$manifest=Get-Content -Raw "$Release\manifest.json"|ConvertFrom-Json
foreach($f in (Get-Content -Raw "$Release\runtime-manifest.json"|ConvertFrom-Json)){if((Get-FileHash -LiteralPath (Join-Path $Release $f.path)).Hash.ToLowerInvariant() -ne $f.sha256){throw 'Runtime portable alterado'}}
$java=Join-Path ([IO.Path]::GetFullPath($Release)) 'runtime\java25\bin\java.exe'
if((Get-FileHash $java).Hash.ToLowerInvariant() -ne $manifest.javaExeSha256){throw 'Java25 no coincide'}
$si=[Diagnostics.ProcessStartInfo]::new($java);$si.UseShellExecute=$false;$si.CreateNoWindow=$true;$si.RedirectStandardOutput=$true;$si.RedirectStandardError=$true;$si.ArgumentList.Add('--version')
# Solo el proceso de prueba se inicia sin JAVA_HOME y sin Java en PATH. No se cambian variables de Windows/usuario/sesión.
$si.Environment.Remove('JAVA_HOME')|Out-Null;$si.Environment['PATH']=Join-Path $env:SystemRoot 'System32';$p=[Diagnostics.Process]::Start($si);$o=$p.StandardOutput.ReadToEndAsync();$e=$p.StandardError.ReadToEndAsync();$p.WaitForExit();$version=$o.GetAwaiter().GetResult()+$e.GetAwaiter().GetResult();if($p.ExitCode -or $version -notmatch 'openjdk 25\.'){throw 'Runtime aislado no ejecuta Java25'}
$after=Snapshot-SmrGlobalJava;foreach($k in $before.Keys){if($before[$k] -cne $after[$k]){throw 'Java global o configuración global cambió'}}
$result=@{globalUnchanged=$true;portableJava25WithoutJavaHomeOrJavaPath=$true;globalJavaPath=$after.resolvedJava;portableJavaPath=$java;portableVersion=$version.Trim();java8VmTested=$false;scope='WINDOWS_DESARROLLO_REAL_VM_PENDIENTE'}
if($Evidence){$result|ConvertTo-Json|Set-Content -LiteralPath $Evidence};Write-Output 'Java25 portable verificado sin JAVA_HOME/Java en PATH del hijo; Java global y variables persistentes intactos.'
