[CmdletBinding()]
param([Parameter(Mandatory)][string]$Evidencia)
$ErrorActionPreference='Stop'
if(-not $IsWindows -or -not ([IO.Path]::GetFullPath($Evidencia)).StartsWith('C:\req\evidencias\')){throw 'Solo evidencia Windows de laboratorio'}
# Solo SELECT/SHOW sobre QA; no regenera fixtures ni altera operation_control.
$credential=Import-Clixml -LiteralPath C:\req\config\mysql-windows-qa-app.credential.xml
if($credential -isnot [pscredential] -or $credential.UserName -ne 'siger_windows_qa_app'){throw 'Solo identidad QA DML'}
$start=[Diagnostics.ProcessStartInfo]::new('C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe')
$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
foreach($arg in @('-XX:-UsePerfData','--class-path','C:\req\bin\lib\*',(Join-Path $PSScriptRoot 'WindowsQaConexion.java'))){$start.ArgumentList.Add($arg)}
$process=[Diagnostics.Process]::Start($start)
try{
 $payload=@{username=$credential.UserName;password=$credential.GetNetworkCredential().Password;audit=$true}|ConvertTo-Json -Compress
 $process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$credential=$null
 $out=$process.StandardOutput.ReadToEndAsync();$err=$process.StandardError.ReadToEndAsync()
 if(-not $process.WaitForExit(30000)){$process.Kill($true);throw 'Preflight QA vencido'}
 $null=$err.GetAwaiter().GetResult();if($process.ExitCode -ne 0){throw 'Preflight QA no acreditado; detalle remoto omitido'}
 $result=$out.GetAwaiter().GetResult()|ConvertFrom-Json
 if($result.schema -ne 'siger_institucional_windows_qa' -or $result.user -ne 'siger_windows_qa_app@localhost' -or $result.tls -notmatch '^TLSv1\.[23]$'){throw 'Identidad o TLS QA no acreditados'}
 foreach($grant in $result.grants){if($grant -notmatch '^GRANT USAGE ON \*\.\* TO '){$on=$grant.IndexOf(' ON ');if($on -lt 0 -or -not $grant.Substring($on).StartsWith(' ON `siger_institucional_windows_qa`.* TO ') -or @($grant.Substring(6,$on-6).Split(', ')|Where-Object {$_ -notin @('SELECT','INSERT','UPDATE','DELETE')}).Count){throw 'Privilegios QA distintos de DML'}}}
 $result|ConvertTo-Json -Depth 6|Set-Content -LiteralPath (Join-Path $Evidencia 'mysql-qa-solo-lectura.json')
 @{ok=$true;schema=$result.schema;tls=$result.tls;dmlOnly=$true;ackPending=$result.ackPending;duplicatedCom=$result.duplicatedCom;graphAcceptedQa=$result.graphAcceptedQa;readOnly=$true}|ConvertTo-Json -Compress
}finally{if(-not $process.HasExited){$process.Kill($true)};$process.Dispose();$payload=$null;$credential=$null}
