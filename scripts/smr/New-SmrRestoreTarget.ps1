#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr')
. "$PSScriptRoot\Common.ps1"
$platform=Get-Content -Raw "$Root\mysql\platform.json"|ConvertFrom-Json
if($platform.scope -ne 'SOLO_SMR' -or $platform.port -ne 13316 -or [IO.Path]::GetFullPath($platform.config) -ne "$Root\mysql\my.ini"){throw 'Destino exclusivamente en motor SMR nuevo'}
$schema='smr_restore_'+[guid]::NewGuid().ToString('N').Substring(0,8);$sql=[Collections.Generic.List[string]]::new();$sql.Add("CREATE DATABASE $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
foreach($kind in @('app','migrator')){$password=New-SmrSecret;$user="${schema}_$kind";Protect-SmrJson @{username=$user;password=$password} "$Root\secrets\${schema}_$kind.dpapi";$grants=if($kind -eq 'app'){'SELECT,INSERT,UPDATE,DELETE'}else{'SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,DROP,INDEX,REFERENCES,CREATE VIEW'};$sql.Add("CREATE USER '$user'@'localhost' IDENTIFIED BY '$password' REQUIRE SSL;");$sql.Add("GRANT $grants ON $schema.* TO '$user'@'localhost';")}
$admin=Read-SmrJson "$Root\secrets\mysql-admin.dpapi";$cnf=Join-Path $Root ('secrets\restore-provision-'+[guid]::NewGuid().ToString('N')+'.cnf')
@"
[client]
host=127.0.0.1
port=$($platform.port)
user=$($admin.username)
password=$($admin.password)
ssl-mode=VERIFY_IDENTITY
ssl-ca=$($Root.Replace('\','/'))/secrets/tls/ca.pem
"@|Set-Content -LiteralPath $cnf
try{$si=[Diagnostics.ProcessStartInfo]::new("$($platform.home)\bin\mysql.exe");$si.UseShellExecute=$false;$si.CreateNoWindow=$true;$si.RedirectStandardInput=$true;$si.RedirectStandardOutput=$true;$si.RedirectStandardError=$true;$si.ArgumentList.Add("--defaults-extra-file=$cnf");$proc=[Diagnostics.Process]::Start($si);$out=$proc.StandardOutput.ReadToEndAsync();$err=$proc.StandardError.ReadToEndAsync();$proc.StandardInput.Write(($sql -join "`n"));$proc.StandardInput.Close();$proc.WaitForExit();$null=$out.GetAwaiter().GetResult();$null=$err.GetAwaiter().GetResult();if($proc.ExitCode){throw 'Destino de restore parcial; conservar e inspeccionar'};Write-Output $schema}finally{Remove-Item -LiteralPath $cnf;$admin.Clear();$password=$null;$sql.Clear()}
