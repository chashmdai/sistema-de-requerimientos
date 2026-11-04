#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[ValidateSet('recepcion','sintetico')][string]$Instance='recepcion')
. "$PSScriptRoot\Session-Smr.ps1"
. "$PSScriptRoot\ArchiveCrypto.ps1"
$session=New-SmrSession $Root $Instance;$state=Suspend-SmrConsumer $session
$base="$Root\instances\$Instance";$config=Get-Content -Raw "$base\instance.json"|ConvertFrom-Json;$platform=Get-Content -Raw "$Root\mysql\platform.json"|ConvertFrom-Json
$backup=Join-Path $Root ('backups\'+$Instance+'-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8));$stage=Join-Path $backup 'staging';[IO.Directory]::CreateDirectory($stage)|Out-Null
$db=Read-SmrJson "$Root\secrets\$($config.schema)_backup.dpapi";$cnf="$stage\client.cnf"
@"
[client]
host=127.0.0.1
port=$($platform.port)
user=$($db.username)
password=$($db.password)
ssl-mode=VERIFY_IDENTITY
ssl-ca=$($Root.Replace('\','/'))/secrets/tls/ca.pem
"@|Set-Content -LiteralPath $cnf
try{
 & "$($platform.home)\bin\mysqldump.exe" "--defaults-extra-file=$cnf" --single-transaction --skip-lock-tables --skip-add-locks --no-tablespaces --set-gtid-purged=OFF "--result-file=$stage\database.sql" $config.schema 2> "$backup\dump.err.log"
 if($LASTEXITCODE){throw 'Dump no acreditado'}
}finally{$db.Clear();Remove-Item -LiteralPath $cnf}
Copy-Item -LiteralPath "$base\store\objects" -Destination "$stage\objects" -Recurse
Copy-Item -LiteralPath "$base\instance.json" -Destination "$stage\instance.json"
[IO.Directory]::CreateDirectory("$stage\trust")|Out-Null
Copy-Item -LiteralPath $(if($config.synthetic){"$base\private\producer-trust.json"}else{"$Root\config\formulario-signing-trust.json"}) -Destination "$stage\trust\producer.json"
if($config.synthetic){$ack=Read-SmrJson "$base\private\ack.dpapi";[IO.File]::WriteAllText("$stage\trust\ack.json",(@{$ack.keyId=$ack.publicPem}|ConvertTo-Json));$ack.Clear()}else{Copy-Item -LiteralPath "$Root\public\ack\ack-trust.json" -Destination "$stage\trust\ack.json"}
$files=@(Get-ChildItem $stage -Recurse -File|ForEach-Object {@{path=[IO.Path]::GetRelativePath($stage,$_.FullName).Replace('\','/');size=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash.ToLowerInvariant()}})
@{atUtc=[DateTime]::UtcNow.ToString('o');instance=$Instance;schema=$config.schema;ackPending=$state.ackPendientes;records=$state.registrosDurables;sourceRelease=$config.release;files=$files}|ConvertTo-Json -Depth 8|Set-Content -LiteralPath "$stage\manifest.json"
[IO.Compression.ZipFile]::CreateFromDirectory($stage,"$backup\custodia.zip",'Optimal',$false)
$keyPath="$Root\recovery\backup-key.dpapi";if(-not(Test-Path -LiteralPath $keyPath)){Protect-SmrJson @{key=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(64))} $keyPath}
$secret=Read-SmrJson $keyPath;$key=[Convert]::FromBase64String($secret.key)
try{Protect-SmrArchive "$backup\custodia.zip" "$backup\custodia.smrbackup" $key;Unprotect-SmrArchive "$backup\custodia.smrbackup" "$backup\verification.zip" $key;if((Get-FileHash "$backup\custodia.zip").Hash -ne (Get-FileHash "$backup\verification.zip").Hash){throw 'Backup cifrado no reproduce los bytes'}}finally{[Array]::Clear($key);$secret.Clear()}
@{sha256=(Get-FileHash "$backup\custodia.smrbackup").Hash.ToLowerInvariant();encrypted=$true;integrityVerified=$true;restoreTested=$false;consumerRemainsPaused=$true}|ConvertTo-Json|Set-Content "$backup\backup.json"
# Retirar solo copias transitorias recién creadas, verificadas dentro de esta raíz de backup.
$resolved=[IO.Path]::GetFullPath($stage);if(-not $resolved.StartsWith([IO.Path]::GetFullPath("$Root\backups")+'\') -or [IO.Path]::GetFileName($resolved) -ne 'staging'){throw 'Ruta de limpieza rechazada'}
Remove-Item -LiteralPath $resolved -Recurse;Remove-Item -LiteralPath "$backup\custodia.zip","$backup\verification.zip"
Invoke-SmrPost $session '/nucleo/logout' $null|Out-Null
Write-Output $backup
