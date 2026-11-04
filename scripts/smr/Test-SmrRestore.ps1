#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[Parameter(Mandatory)][string]$Backup,[ValidateSet('sintetico','recepcion')][string]$Instance='sintetico')
. "$PSScriptRoot\ArchiveCrypto.ps1"
$instance=Get-Content -Raw "$Root\instances\$Instance\instance.json"|ConvertFrom-Json;$platform=Get-Content -Raw "$Root\mysql\platform.json"|ConvertFrom-Json
$dest=Join-Path $Root ('tests\restore-'+[guid]::NewGuid().ToString('N'));[IO.Directory]::CreateDirectory($dest)|Out-Null
$secret=Read-SmrJson "$Root\recovery\backup-key.dpapi";$key=[Convert]::FromBase64String($secret.key)
try{Unprotect-SmrArchive "$Backup\custodia.smrbackup" "$dest\custodia.zip" $key}finally{[Array]::Clear($key);$secret.Clear()}
$zip=[IO.Compression.ZipFile]::OpenRead("$dest\custodia.zip");$extract="$dest\extract";[IO.Directory]::CreateDirectory($extract)|Out-Null
try{foreach($entry in $zip.Entries){$path=[IO.Path]::GetFullPath((Join-Path $extract $entry.FullName));if(-not $path.StartsWith($extract+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Ruta peligrosa en backup'}}}finally{$zip.Dispose()}
[IO.Compression.ZipFile]::ExtractToDirectory("$dest\custodia.zip",$extract)
$manifest=Get-Content -Raw "$extract\manifest.json"|ConvertFrom-Json
foreach($f in $manifest.files){if((Get-FileHash -LiteralPath (Join-Path $extract $f.path)).Hash.ToLowerInvariant() -ne $f.sha256){throw 'Hash de backup incorrecto'}}
$target=& "$PSScriptRoot\New-SmrRestoreTarget.ps1" -Root $Root
if($target -notmatch '^smr_restore_[a-f0-9]{8}$'){throw 'Destino de restore no acreditado'}
# Solo directivas DDL de vistas de mysqldump: mapear definidor/esquema al destino nuevo.
# Nunca transformar INSERT, cuerpos JSON, firmas, folios o originales.
$mapped="$dest\restore.sql";$reader=[IO.StreamReader]::new("$extract\database.sql");$writer=[IO.StreamWriter]::new($mapped,$false,[Text.UTF8Encoding]::new($false))
try{while($null -ne ($line=$reader.ReadLine())){if($line -match '^/\*!50013 DEFINER=`[A-Za-z0-9_]+`@`localhost` SQL SECURITY DEFINER \*/$'){$line='/*!50013 DEFINER=CURRENT_USER SQL SECURITY DEFINER */'};if($line.StartsWith('/*!50001 VIEW ')){$line=$line.Replace(('`'+$manifest.schema+'`.'),('`'+$target+'`.'))};$writer.WriteLine($line)}}finally{$reader.Dispose();$writer.Dispose()}
$credential=Read-SmrJson "$Root\secrets\${target}_migrator.dpapi";$cnf="$dest\client.cnf"
@"
[client]
host=127.0.0.1
port=$($platform.port)
user=$($credential.username)
password=$($credential.password)
ssl-mode=VERIFY_IDENTITY
ssl-ca=$($Root.Replace('\','/'))/secrets/tls/ca.pem
"@|Set-Content -LiteralPath $cnf
try{
 $count=& "$($platform.home)\bin\mysql.exe" "--defaults-extra-file=$cnf" --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$target'";if($LASTEXITCODE -or $count -ne '0'){throw 'Restore exige esquema nuevo vacío; no borrar tablas'}
 $si=[Diagnostics.ProcessStartInfo]::new("$($platform.home)\bin\mysql.exe");$si.UseShellExecute=$false;$si.CreateNoWindow=$true;$si.RedirectStandardInput=$true;$si.RedirectStandardOutput=$true;$si.RedirectStandardError=$true
 foreach($a in @("--defaults-extra-file=$cnf",'--binary-mode',"--database=$target")){$si.ArgumentList.Add($a)}
 $proc=[Diagnostics.Process]::Start($si);$out=$proc.StandardOutput.ReadToEndAsync();$err=$proc.StandardError.ReadToEndAsync();$input=[IO.File]::OpenRead($mapped);try{$input.CopyTo($proc.StandardInput.BaseStream)}catch{Write-Verbose 'Importador SQL cerró la entrada; consultar diagnóstico privado'}finally{$input.Dispose();try{$proc.StandardInput.Close()}catch{}}
 $proc.WaitForExit();[IO.File]::WriteAllText("$dest\restore.err.log",$err.GetAwaiter().GetResult());$null=$out.GetAwaiter().GetResult();if($proc.ExitCode){throw 'Restore SQL falló; conservar estado parcial'}
}finally{$credential.Clear();Remove-Item -LiteralPath $cnf}
$store="$dest\store";[IO.Directory]::CreateDirectory($store)|Out-Null;Copy-Item -LiteralPath "$extract\objects" -Destination "$store\objects" -Recurse
$db=Read-SmrJson "$Root\secrets\${target}_app.dpapi";$ack=if($instance.synthetic){Read-SmrJson "$Root\instances\$Instance\private\ack.dpapi"}else{Read-SmrJson "$Root\secrets\ack-identity.dpapi"}
$p=@{url=$instance.dbUrl.Replace('/'+$instance.schema+'?',"/$target`?");user=$db.username;password=$db.password;store=$store;producerTrust="$extract\trust\producer.json";ownTrust="$extract\trust\ack.json";keyId=$ack.keyId;publicPem=$ack.publicPem;privatePem=$ack.privatePem;origin=$instance.origin}
Invoke-SmrJava "$($instance.javaHome)\bin\java.exe" @('--enable-native-access=ALL-UNNAMED','-XX:-UsePerfData','-Xmx192m','--class-path',"$($instance.release)\tools\core.jar;$($instance.release)\lib\*","$PSScriptRoot\testing\VerifyRestore.java") ($p|ConvertTo-Json -Compress) "$dest\verify.log" "$dest\verify.err.log"|Out-Null
$last=Get-Content "$dest\verify.log"|Where-Object {$_ -match '^\{"'}|Select-Object -Last 1;$result=$last|ConvertFrom-Json
if($result.records -ne $manifest.records -or -not $result.exclusiveConsumerVerified){throw 'Restore no acreditado'}
$result|ConvertTo-Json|Set-Content -LiteralPath "$Root\evidence\backup-restore-$Instance.json"
if($Instance -eq 'sintetico'){$result|ConvertTo-Json|Set-Content -LiteralPath "$Root\evidence\backup-restore.json"}
@{schema=$target;store=$store;dbUrl=$p.url;sourceConfiguration="$extract\instance.json";consumerEnabled=$false}|ConvertTo-Json|Set-Content -LiteralPath "$dest\restore-target.json"
$metadata=Get-Content -Raw "$Backup\backup.json"|ConvertFrom-Json;$metadata.restoreTested=$true;$metadata|ConvertTo-Json|Set-Content "$Backup\backup.json"
$p.Clear();$db.Clear();$ack.Clear();Write-Output "Restore aislado MySQL/NTFS: $($result.records) Comunicaciones, $($result.originals) originales, firmas, hashes, Flyway y exclusión verificados. Sin PULL público. Destino: $dest\restore-target.json"
