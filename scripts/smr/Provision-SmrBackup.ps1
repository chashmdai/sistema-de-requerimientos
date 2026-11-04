#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr')
. "$PSScriptRoot\Common.ps1"
$platform=Get-Content -Raw "$Root\mysql\platform.json"|ConvertFrom-Json
if($platform.scope -ne 'SOLO_SMR' -or $platform.port -ne 13316 -or [IO.Path]::GetFullPath($platform.config) -ne "$Root\mysql\my.ini"){throw 'Provisión solo en motor SMR nuevo'}
$admin=Read-SmrJson "$Root\secrets\mysql-admin.dpapi";$cnf=Join-Path $Root ('secrets\backup-provision-'+[guid]::NewGuid().ToString('N')+'.cnf')
@"
[client]
host=127.0.0.1
port=$($platform.port)
user=$($admin.username)
password=$($admin.password)
ssl-mode=VERIFY_IDENTITY
ssl-ca=$($Root.Replace('\','/'))/secrets/tls/ca.pem
"@|Set-Content -LiteralPath $cnf
try{foreach($schema in @('smr_sintetico','smr_recepcion','smr_restore')){
 $path="$Root\secrets\${schema}_backup.dpapi";if(Test-Path -LiteralPath $path){continue};$password=New-SmrSecret;Protect-SmrJson @{username="${schema}_backup";password=$password} $path
 $sql="CREATE USER '${schema}_backup'@'localhost' IDENTIFIED BY '$password' REQUIRE SSL; GRANT SELECT,SHOW VIEW ON $schema.* TO '${schema}_backup'@'localhost';"
 $si=[Diagnostics.ProcessStartInfo]::new("$($platform.home)\bin\mysql.exe");$si.UseShellExecute=$false;$si.CreateNoWindow=$true;$si.RedirectStandardInput=$true;$si.RedirectStandardOutput=$true;$si.RedirectStandardError=$true;$si.ArgumentList.Add("--defaults-extra-file=$cnf")
 $proc=[Diagnostics.Process]::Start($si);$out=$proc.StandardOutput.ReadToEndAsync();$err=$proc.StandardError.ReadToEndAsync();$proc.StandardInput.Write($sql);$proc.StandardInput.Close();$proc.WaitForExit();$null=$out.GetAwaiter().GetResult();$null=$err.GetAwaiter().GetResult();if($proc.ExitCode){throw 'Provisión backup parcial; inspeccionar sin regenerar credencial'}
};Write-Output 'Identidades backup SELECT/SHOW VIEW propias de SMR creadas; runtime DML preservado.'}finally{Remove-Item -LiteralPath $cnf;$admin.Clear();$password=$null;$sql=$null}
