[CmdletBinding()]
param([Parameter(Mandatory)][string]$Run,[ValidateSet('preflight','e2e','offline')][string]$Mode='preflight')
$ErrorActionPreference='Stop'
$base=[IO.Path]::GetFullPath('C:\req\testdata')+[IO.Path]::DirectorySeparatorChar
if(-not [IO.Path]::GetFullPath($Run).StartsWith($base,[StringComparison]::OrdinalIgnoreCase)){throw 'Solo run QA privado'}
$credential=$null;$child=$null
try {
 $credential=Import-Clixml -LiteralPath 'C:\req\config\mysql-windows-qa-app.credential.xml'
 if($credential -isnot [pscredential] -or $credential.UserName -ne 'siger_windows_qa_app'){throw 'Solo app QA'}
 $i=[Diagnostics.ProcessStartInfo]::new('C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe')
 $i.UseShellExecute=$false;$i.CreateNoWindow=$true;$i.RedirectStandardInput=$true;$i.RedirectStandardOutput=$true;$i.RedirectStandardError=$true
 foreach($a in @('--enable-native-access=ALL-UNNAMED','-XX:-UsePerfData','-Xmx256m','-cp',((Join-Path $Run 'classes')+';'+(Join-Path $Run 'institucional-classes')+';C:\req\bin\lib\*'),'cl.nexbyte.siger.institucional.formulario.RecepcionSinAgenteWindows',$Mode,$Run)){$i.ArgumentList.Add($a)}
 $child=[Diagnostics.Process]::Start($i)
 $out=$child.StandardOutput.ReadToEndAsync();$err=$child.StandardError.ReadToEndAsync()
 $child.StandardInput.Write((@{username=$credential.UserName;password=$credential.GetNetworkCredential().Password}|ConvertTo-Json -Compress));$child.StandardInput.Close();$credential=$null
 if(-not $child.WaitForExit(180000)){$child.Kill($true);throw 'Timeout QA'}
 $text=$out.GetAwaiter().GetResult();$text|Set-Content -LiteralPath (Join-Path $Run ($Mode+'.log')) -Encoding utf8
 $safe=$text -split '\r?\n'|Where-Object {$_.StartsWith('{')};$safe
 if($child.ExitCode -ne 0){throw 'Prueba no completada; revisar diagnóstico acotado'}
}finally{$credential=$null;if($child){try{if(-not $child.HasExited){$child.Kill($true)}}finally{$child.Dispose()}}}
