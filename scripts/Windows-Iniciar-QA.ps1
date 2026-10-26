[CmdletBinding()]
param([Parameter(Mandatory)][string]$Jar,[ValidateRange(30,1800)][int]$Seconds=600,[ValidateSet(8080,8081)][int]$Port=8081,[string]$Archivo='')
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
# Es un runtime PRELIMINAR QA, sin workers ni recuperacion externa. No acepta el historico.
function Private-File([string]$Path){
 $allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')
 $p=[IO.Path]::GetFullPath($Path)
 if(-not $p.StartsWith('C:\req\',[StringComparison]::OrdinalIgnoreCase)){throw 'Archivo fuera de laboratorio'}
 $cursor=$p
 while($cursor){if(Test-Path -LiteralPath $cursor){if((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlace no admitido'}};$cursor=[IO.Path]::GetDirectoryName($cursor)}
 foreach($rule in (Get-Acl -LiteralPath $p).Access){if($rule.AccessControlType -eq 'Allow' -and -not ($rule.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $rule.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'ACL privada no comprobada'}}
}
function Source-Bytes([string]$Name){$p='C:\req\config\origen-dpapi\'+$Name+'.dpapi';Private-File $p;return ,([Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($p),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))}
$artifact=[IO.Path]::GetFullPath($Jar)
if([IO.Path]::GetDirectoryName($artifact) -ne 'C:\req\bin' -or [IO.Path]::GetFileName($artifact) -notmatch '^siger-([a-f0-9]{16})\.jar$'){throw 'Solo JAR por SHA instalado fuera de target'}
$prefix=$Matches[1];Private-File $artifact
$hash=(Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash.ToLowerInvariant()
if(-not $hash.StartsWith($prefix)){throw 'Artefacto distinto de su nombre SHA'}
if(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue){throw 'Puerto ocupado; no se interviene otro servicio'}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$root='C:\req\testdata\runtime-qa-'+[guid]::NewGuid().ToString('N')
$secret=Join-Path $root 'secrets';$null=New-Item -ItemType Directory -Path $secret
Private-File $secret
if($Archivo){$Archivo=[IO.Path]::GetFullPath($Archivo);if($Archivo -notmatch '^C:\\req\\testdata\\runtime-qa-[a-f0-9]{32}\\archivo$' -or -not (Test-Path -LiteralPath $Archivo -PathType Container)){throw 'Solo custodia QA previa'};Private-File $Archivo}else{$Archivo=Join-Path $root 'archivo'}
$process=$null;$files=[Collections.Generic.List[string]]::new();$payload=$null;$dbCredential=$null
try{
 foreach($name in @('institucional-key.pem','institucional-trust.json','publico-trust.json','notificaciones-allowlist.txt')){
  [byte[]]$bytes=Source-Bytes $name
  try{$target=Join-Path $secret $name;$files.Add($target);[IO.File]::WriteAllBytes($target,$bytes);Private-File $target}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes)}
 }
 $ownTrust=Get-Content -LiteralPath (Join-Path $secret 'institucional-trust.json') -Raw|ConvertFrom-Json
 $keyIds=@($ownTrust.PSObject.Properties.Name)
 if($keyIds.Count -ne 1 -or $keyIds[0] -notmatch '^[A-Za-z0-9_-]{1,64}$'){throw 'Seleccion de clave de comprobante requiere revision'}
 $tlsPath='C:\req\config\windows-qa-localhost-tls.json.dpapi';Private-File $tlsPath
 $tlsBytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($tlsPath),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
 try{$tls=[Text.Encoding]::UTF8.GetString($tlsBytes)|ConvertFrom-Json;$pfx=[Convert]::FromBase64String($tls.pfx);try{$target=Join-Path $secret 'servidor.p12';$files.Add($target);[IO.File]::WriteAllBytes($target,$pfx);Private-File $target}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($pfx)}}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($tlsBytes)}
 $credFile='C:\req\config\mysql-windows-qa-app.credential.xml';Private-File $credFile;$dbCredential=Import-Clixml -LiteralPath $credFile
 if($dbCredential -isnot [pscredential] -or $dbCredential.UserName -ne 'siger_windows_qa_app'){throw 'Solo identidad DML QA'}
 $config=@{
  'siger.windows.modo'='PRELIMINAR_SIN_DOCUMENTAL';'server.port'="$Port"
  'institucional.db.url'='jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000&characterEncoding=UTF-8'
  'institucional.db.user'=$dbCredential.UserName;'institucional.db.password'=$dbCredential.GetNetworkCredential().Password
  'server.ssl.key-store'=([uri](Join-Path $secret 'servidor.p12')).AbsoluteUri;'server.ssl.key-store-password'=$tls.password
  'institucional.comprobante.key-id'=$keyIds[0]
  'institucional.comprobante.private-key'=(Join-Path $secret 'institucional-key.pem')
  'institucional.comprobante.own-trusted-keys'=(Join-Path $secret 'institucional-trust.json')
  'institucional.comprobante.trusted-keys'=(Join-Path $secret 'publico-trust.json')
  'institucional.documentos.directorio'=$Archivo
  'institucional.configuracion-privada.directorio'=$secret
  'institucional.notificaciones.correo.allowlist'=(Join-Path $secret 'notificaciones-allowlist.txt')
  'institucional.notificaciones.correo.campana'='windows_qa_simulado'
 }
 $start=[Diagnostics.ProcessStartInfo]::new('C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe')
 $start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true;$start.WorkingDirectory=$root
 $start.Environment.Clear();foreach($key in @('SystemRoot','windir','USERPROFILE','LOCALAPPDATA')){if([Environment]::GetEnvironmentVariable($key)){$start.Environment[$key]=[Environment]::GetEnvironmentVariable($key)}}
 $start.Environment['TEMP']=$root;$start.Environment['TMP']=$root
 foreach($arg in @('--enable-native-access=ALL-UNNAMED','-XX:-UsePerfData','-Xmx384m',('-Djava.io.tmpdir='+$root),'-Dloader.main=cl.nexbyte.siger.institucional.windows.ArranqueWindows','-cp',$artifact,'org.springframework.boot.loader.launch.PropertiesLauncher')){$start.ArgumentList.Add($arg)}
 $process=[Diagnostics.Process]::Start($start)
 $out=[IO.FileStream]::new((Join-Path $root 'backend.log'),[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::Read);$err=[IO.FileStream]::new((Join-Path $root 'backend-error.log'),[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::Read)
 $outTask=$process.StandardOutput.BaseStream.CopyToAsync($out);$errTask=$process.StandardError.BaseStream.CopyToAsync($err)
 $payload=$config|ConvertTo-Json -Compress;$process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$config.Clear();$dbCredential=$null;$tls=$null
 $state=@{scope='PRELIMINAR_SIN_DOCUMENTAL';root=$root;archivo=$Archivo;pid=$process.Id;origin="https://localhost:$Port";artifactSHA256=$hash;database='siger_institucional_windows_qa';realGraph=$false;publicRecovery=$false;documentWorkers=$false;secretPaths=$files.ToArray()}
 $state|ConvertTo-Json -Depth 4|Set-Content -LiteralPath (Join-Path $root 'run.json')
 $root|Set-Content C:\req\scratch\ultimo-runtime-qa.txt
 $watch=[Diagnostics.Stopwatch]::StartNew()
 while(-not $process.HasExited -and $watch.Elapsed.TotalSeconds -lt $Seconds -and -not (Test-Path -LiteralPath (Join-Path $root 'stop'))){Start-Sleep -Milliseconds 200}
}finally{
 if($process){if(-not $process.HasExited){$process.Kill();if(-not $process.WaitForExit(10000)){throw 'Proceso QA no terminado; conservar secretos privados para diagnostico'}}
  try{$null=$outTask.GetAwaiter().GetResult();$null=$errTask.GetAwaiter().GetResult()}finally{if($out){$out.Dispose()};if($err){$err.Dispose()};$process.Dispose()}}
 # Solo archivos efimeros exactos de este proceso, sin borrado recursivo ni historial.
 foreach($file in $files){if(Test-Path -LiteralPath $file){Private-File $file;Remove-Item -LiteralPath $file}}
 $payload=$null;$dbCredential=$null
}
@{runtime=$root;stopped=$true;ephemeralPrivateFilesRemoved=$true;scope='PRELIMINAR_SIN_DOCUMENTAL'}|ConvertTo-Json -Compress
