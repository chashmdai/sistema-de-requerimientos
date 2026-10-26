param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
$cred=Import-Clixml C:\req\config\mysql-windows-qa-app.credential.xml
if($cred -isnot [pscredential] -or $cred.UserName -ne 'siger_windows_qa_app'){throw 'Solo identidad QA DML'}
$start=[Diagnostics.ProcessStartInfo]::new('C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe')
$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
foreach($a in @('-XX:-UsePerfData','--class-path','C:\req\bin\lib\*',(Join-Path (Split-Path $PSScriptRoot) 'WindowsQaConexion.java'))){$start.ArgumentList.Add($a)}
$p=[Diagnostics.Process]::Start($start)
try{
 $payload=@{username=$cred.UserName;password=$cred.GetNetworkCredential().Password;audit=$true}|ConvertTo-Json -Compress
 $p.StandardInput.Write($payload);$p.StandardInput.Close();$payload=$null;$cred=$null
 $out=$p.StandardOutput.ReadToEndAsync();$err=$p.StandardError.ReadToEndAsync()
 if(-not $p.WaitForExit(30000)){$p.Kill();throw 'Audit QA timeout'}
 if($p.ExitCode -ne 0){throw 'Audit QA no valido; detalle remoto omitido'}
 $out.Result.Trim()|Tee-Object C:\req\evidencias\auditoria-runtime-qa-2026-10-26.json
}finally{$p.Dispose();$cred=$null;$payload=$null}
