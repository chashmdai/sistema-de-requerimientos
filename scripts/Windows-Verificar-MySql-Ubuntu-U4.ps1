#Requires -Version 7.4
[CmdletBinding()]
param([Parameter(Mandatory)][string]$Evidencia,[ValidateSet('ESTADO','ABRIR','CERRAR')][string]$Control='ESTADO',[switch]$EnsayoSinteticoCoordinado)
$ErrorActionPreference='Stop'
if(-not $IsWindows -or -not ([IO.Path]::GetFullPath($Evidencia)).StartsWith('C:\req\evidencias\')){throw 'Solo evidencia privada U4'}
if($Control -eq 'ABRIR' -and -not $EnsayoSinteticoCoordinado){throw 'Apertura requiere ventana sintetica coordinada'}
foreach($identity in @('app','migrator')){
 $credential=Import-Clixml -LiteralPath "C:\req\config\mysql-u4-ubuntu-$identity.credential.xml"
 if($credential -isnot [pscredential] -or $credential.UserName -cne "siger_u4_ubuntu_$identity"){throw 'Identidad U4 requerida'}
 $start=[Diagnostics.ProcessStartInfo]::new('C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe')
 $start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 $repo=Split-Path $PSScriptRoot
 foreach($a in @('--enable-native-access=ALL-UNNAMED','-XX:-UsePerfData','-Xmx128m','--class-path',"$repo\siger-institucional\backend\target\classes;C:\req\bin\lib\*",(Join-Path $PSScriptRoot 'qa\VerificarMySqlUbuntuU4.java'))){$start.ArgumentList.Add($a)}
 $process=[Diagnostics.Process]::Start($start)
 try{
  $out=$process.StandardOutput.ReadToEndAsync();$err=$process.StandardError.ReadToEndAsync()
  $payload=@{username=$credential.UserName;password=$credential.GetNetworkCredential().Password;control=$Control;syntheticCoordinated=[bool]$EnsayoSinteticoCoordinado}|ConvertTo-Json -Compress
  $process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$credential=$null
  if(-not $process.WaitForExit(45000)){$process.Kill($true);throw 'Preflight MySQL U4 vencido'}
  $lines=$out.GetAwaiter().GetResult() -split '\r?\n';$null=$err.GetAwaiter().GetResult()
  $result=($lines|Where-Object {$_.StartsWith('{')}|Select-Object -Last 1)|ConvertFrom-Json
  if($process.ExitCode -ne 0 -or $result.ok -eq $false){throw 'Preflight MySQL U4 rechazado; detalles reservados omitidos'}
  $result|ConvertTo-Json -Depth 5|Set-Content -LiteralPath (Join-Path $Evidencia "mysql-u4-$identity.json")
  @{identity=$identity;tls=$result.tls;qaDenied=$result.siger_institucional_windows_qaAccessDenied;historicalDenied=$result.siger_institucional_windows_labAccessDenied;inbox=$result.inbox;ackPending=$result.ackPending;competingConsumerRejected=$result.competingConsumerRejected;locksReleasedAndReacquired=$result.locksReleasedAndReacquired;originBound=$result.originBound;custodyReady=$result.privateNtfsCustodyReady;control=$result.control}|ConvertTo-Json -Depth 5
 }finally{if(-not $process.HasExited){$process.Kill($true)};$process.Dispose();$payload=$null;$credential=$null}
}
