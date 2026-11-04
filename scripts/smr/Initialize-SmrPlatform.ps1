#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[string]$JavaHome='',[string]$MySqlHome='C:\Program Files\MySQL\MySQL Server 8.0',[int]$MySqlPort=13316)
. "$PSScriptRoot\Common.ps1"
Assert-SmrPrivate $Root
$platform=Join-Path $Root 'mysql';$tls=Join-Path $Root 'secrets\tls'
if(Test-Path -LiteralPath "$platform\platform.json"){Write-Output 'Plataforma existente; no modificada';return}
if(-not $JavaHome){$JavaHome=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../runtime/java25'))};if(-not(Test-Path "$JavaHome\bin\java.exe")){throw 'Java25 portable explícito requerido'}
if((Test-Path -LiteralPath $platform) -or (Get-NetTCPConnection -LocalPort $MySqlPort -State Listen -ErrorAction SilentlyContinue)){throw 'Colisión MySQL SMR; inspeccionar sin borrar'}
foreach($p in @($platform,"$platform\data",$tls)){[IO.Directory]::CreateDirectory($p)|Out-Null}
$hash=[Security.Cryptography.HashAlgorithmName]::SHA256;$pad=[Security.Cryptography.RSASignaturePadding]::Pkcs1
$caKey=[Security.Cryptography.RSA]::Create(3072);$caReq=[Security.Cryptography.X509Certificates.CertificateRequest]::new('CN=SMR isolated local CA',$caKey,$hash,$pad)
$caReq.CertificateExtensions.Add([Security.Cryptography.X509Certificates.X509BasicConstraintsExtension]::new($true,$false,0,$true))
$caReq.CertificateExtensions.Add([Security.Cryptography.X509Certificates.X509KeyUsageExtension]::new([Security.Cryptography.X509Certificates.X509KeyUsageFlags]::KeyCertSign,$true))
$ca=$caReq.CreateSelfSigned([DateTimeOffset]::UtcNow.AddMinutes(-5),[DateTimeOffset]::UtcNow.AddYears(2))
$key=[Security.Cryptography.RSA]::Create(3072);$req=[Security.Cryptography.X509Certificates.CertificateRequest]::new('CN=localhost',$key,$hash,$pad)
$san=[Security.Cryptography.X509Certificates.SubjectAlternativeNameBuilder]::new();$san.AddDnsName('localhost');$san.AddIpAddress([Net.IPAddress]::Parse('127.0.0.1'));$req.CertificateExtensions.Add($san.Build())
$req.CertificateExtensions.Add([Security.Cryptography.X509Certificates.X509BasicConstraintsExtension]::new($false,$false,0,$true))
$eku=[Security.Cryptography.OidCollection]::new();$eku.Add([Security.Cryptography.Oid]::new('1.3.6.1.5.5.7.3.1'))|Out-Null;$req.CertificateExtensions.Add([Security.Cryptography.X509Certificates.X509EnhancedKeyUsageExtension]::new($eku,$false))
$leaf=$req.Create($ca,[DateTimeOffset]::UtcNow.AddMinutes(-3),[DateTimeOffset]::UtcNow.AddYears(1),[Security.Cryptography.RandomNumberGenerator]::GetBytes(16))
$server=[Security.Cryptography.X509Certificates.RSACertificateExtensions]::CopyWithPrivateKey($leaf,$key)
[IO.File]::WriteAllText("$tls\ca.pem",$ca.ExportCertificatePem());[IO.File]::WriteAllText("$tls\server.pem",$server.ExportCertificatePem());[IO.File]::WriteAllText("$tls\server-key.pem",$key.ExportRSAPrivateKeyPem())
[IO.File]::WriteAllBytes("$Root\config\smr-local-ca.cer",$ca.Export([Security.Cryptography.X509Certificates.X509ContentType]::Cert))
$tlsPassword=New-SmrSecret;[IO.File]::WriteAllBytes("$tls\localhost.p12",$server.Export([Security.Cryptography.X509Certificates.X509ContentType]::Pkcs12,$tlsPassword));Protect-SmrJson @{password=$tlsPassword} "$Root\secrets\tls-password.dpapi"
# Confianza solo del usuario actual y únicamente la nueva CA SMR; no cambia certificados existentes/globales.
Import-Certificate -FilePath "$Root\config\smr-local-ca.cer" -CertStoreLocation Cert:\CurrentUser\Root|Out-Null
& "$JavaHome\bin\keytool.exe" -importcert -noprompt -alias smr-local-ca -file "$tls\ca.pem" -keystore "$Root\config\mysql-trust.p12" -storetype PKCS12 -storepass public-trust 2> "$Root\logs\keytool.log"
if($LASTEXITCODE){throw 'Truststore SMR no creado'}
$rootPassword=New-SmrSecret;Protect-SmrJson @{username='root';password=$rootPassword} "$Root\secrets\mysql-admin.dpapi"
$sql=[Collections.Generic.List[string]]::new();$sql.Add("ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';")
foreach($schema in @('smr_recepcion','smr_sintetico','smr_restore')){
 $sql.Add("CREATE DATABASE $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
 foreach($kind in @('app','migrator','backup')){
  $user="${schema}_$kind";$password=New-SmrSecret;Protect-SmrJson @{username=$user;password=$password} "$Root\secrets\${schema}_$kind.dpapi"
  $sql.Add("CREATE USER '$user'@'localhost' IDENTIFIED BY '$password' REQUIRE SSL;")
  $grants=switch($kind){'app'{'SELECT,INSERT,UPDATE,DELETE'};'backup'{'SELECT,SHOW VIEW'};default{'SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,DROP,INDEX,REFERENCES,CREATE VIEW'}}
  $sql.Add("GRANT $grants ON $schema.* TO '$user'@'localhost';")
 }
}
$init="$platform\initialize.sql";[IO.File]::WriteAllLines($init,$sql,[Text.UTF8Encoding]::new($false));Set-SmrPrivate $init
$dat=($platform+'\data').Replace('\','/');$t=$tls.Replace('\','/')
@"
[mysqld]
basedir=$($MySqlHome.Replace('\','/'))
datadir=$dat
bind-address=127.0.0.1
port=$MySqlPort
mysqlx=OFF
skip-name-resolve=OFF
require-secure-transport=ON
ssl-ca=$t/ca.pem
ssl-cert=$t/server.pem
ssl-key=$t/server-key.pem
tls-version=TLSv1.2,TLSv1.3
innodb-buffer-pool-size=128M
max-connections=24
innodb-flush-log-at-trx-commit=1
log-error=$($platform.Replace('\','/'))/mysql-error.log
pid-file=$($platform.Replace('\','/'))/mysqld.pid
"@|Set-Content -LiteralPath "$platform\my.ini"
& "$MySqlHome\bin\mysqld.exe" "--defaults-file=$platform\my.ini" --initialize-insecure 2> "$Root\logs\mysql-initialize.log"
if($LASTEXITCODE){throw 'Inicialización MySQL SMR falló; conservar estado parcial'}
$process=Start-Process -FilePath "$MySqlHome\bin\mysqld.exe" -ArgumentList @("`"--defaults-file=$platform\my.ini`"","`"--init-file=$init`"") -WindowStyle Hidden -PassThru
$ready=$false;for($n=0;$n -lt 30;$n++){if($process.HasExited){throw 'MySQL SMR terminó; ver log privado'};if(Get-NetTCPConnection -LocalPort $MySqlPort -State Listen -ErrorAction SilentlyContinue){$ready=$true;break};Start-Sleep -Milliseconds 500};if(-not $ready){throw 'MySQL SMR no está disponible'}
# El init-file ya fue leído antes de aceptar conexiones. Se retira únicamente este secreto transitorio nuevo.
Remove-Item -LiteralPath $init
@{port=$MySqlPort;pid=(Get-NetTCPConnection -LocalPort $MySqlPort -State Listen).OwningProcess;home=$MySqlHome;config="$platform\my.ini";scope='SOLO_SMR';serviceInstalled=$false;caThumbprint=$ca.Thumbprint}|ConvertTo-Json|Set-Content -LiteralPath "$platform\platform.json"
$sql.Clear();$password=$null;$rootPassword=$null;$tlsPassword=$null;$key.Dispose();$caKey.Dispose()
Write-Output "MySQL SMR iniciado en 127.0.0.1:$MySqlPort; PID $($process.Id). MySQL80 preservado."
