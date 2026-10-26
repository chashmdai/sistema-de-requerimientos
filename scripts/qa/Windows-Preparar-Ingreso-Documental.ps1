param([Parameter(Mandatory)][string]$Fixtures)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$root='C:\req\testdata\runtime-qa-'+[guid]::NewGuid().ToString('N');$null=New-Item -ItemType Directory -Path $root
if([IO.Path]::GetFullPath($Fixtures) -notmatch '^C:\\req\\testdata\\documental-win-[a-f0-9]{32}\\fixtures$'){throw 'Solo fixtures sinteticos'}
$jdk='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin';$classes=Join-Path $root 'classes';$cp='C:\req\repo\siger-institucional\backend\target\classes;C:\req\bin\lib\*'
& (Join-Path $jdk 'javac.exe') -J-Xmx256m -J-XX:-UsePerfData -cp $cp -d $classes (Join-Path $PSScriptRoot 'IngresoDocumentalWindows.java') *> (Join-Path $root 'compile.log');if($LASTEXITCODE){throw 'Fixture QA no compilado'}
$c=Import-Clixml C:\req\config\mysql-windows-qa-app.credential.xml;if($c.UserName -ne 'siger_windows_qa_app'){throw 'Solo DML QA'}
$data=@{user=$c.UserName;password=$c.GetNetworkCredential().Password}
try{
 foreach($entry in @(@('institucional-key.pem','key'),@('institucional-trust.json','own'),@('publico-trust.json','trust'))){$bytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes(('C:\req\config\origen-dpapi\'+$entry[0]+'.dpapi')),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser);try{$text=[Text.Encoding]::UTF8.GetString($bytes);$data[$entry[1]]=if($entry[1] -eq 'key'){$text}else{$text|ConvertFrom-Json}}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes);$text=$null}}
 $s=[Diagnostics.ProcessStartInfo]::new((Join-Path $jdk 'java.exe'));$s.UseShellExecute=$false;$s.CreateNoWindow=$true;$s.RedirectStandardInput=$true;$s.RedirectStandardOutput=$true;$s.RedirectStandardError=$true
 foreach($arg in @('--enable-native-access=ALL-UNNAMED','-Xmx256m','-XX:-UsePerfData',('-Djava.io.tmpdir='+$root),'-cp',($classes+';'+$cp),'cl.nexbyte.siger.institucional.formulario.IngresoDocumentalWindows',$root,$Fixtures)){$s.ArgumentList.Add($arg)}
 $p=[Diagnostics.Process]::Start($s);try{$payload=$data|ConvertTo-Json -Depth 5 -Compress;$p.StandardInput.Write($payload);$p.StandardInput.Close();$payload=$null;$data.Clear();$o=$p.StandardOutput.ReadToEndAsync();$e=$p.StandardError.ReadToEndAsync();if(-not $p.WaitForExit(60000)){$p.Kill();throw 'Fixture QA vencido'};if($p.ExitCode){$o.Result.Trim();throw 'Fixture QA no acreditado; detalles privados omitidos'};$o.Result.Trim();$root|Set-Content C:\req\scratch\ultimo-ingreso-doc-qa.txt}finally{$p.Dispose()}
}finally{$data.Clear();$c=$null;$payload=$null}
