[CmdletBinding()]
param([string]$Perfil='C:\req\config\formulario-wsl-integracion',[Parameter(Mandatory)][string]$Evidencia,[string]$RuntimeRoot='/home/nexbyteproveedor/.local/share/siger/nna-local-20261023')
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
# Solo lectura en WSL. No reclama entregas ni confirma registros. Nunca lee claves privadas del productor.
function Private-File([string]$Path){
 $p=[IO.Path]::GetFullPath($Path);if(-not $p.StartsWith('C:\req\',[StringComparison]::OrdinalIgnoreCase)){throw 'Ruta privada fuera de laboratorio'}
 $cursor=$p;while($cursor){if(Test-Path -LiteralPath $cursor){if((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlace no admitido'}};$cursor=[IO.Path]::GetDirectoryName($cursor)}
 if(Test-Path -LiteralPath $p){$allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544');foreach($r in (Get-Acl -LiteralPath $p).Access){if($r.AccessControlType -eq 'Allow' -and -not ($r.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'ACL privada no comprobada'}}}
}
function Source-Bytes([string]$Name){$p='C:\req\config\origen-dpapi\'+$Name+'.dpapi';Private-File $p;return ,([Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($p),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))}
function Wsl-Path([string]$Path){if(-not $Path.StartsWith('/home/nexbyteproveedor/.local/share/siger/') -or $Path.Contains('..')){throw 'Ruta de confianza no admitida'};return '\\wsl.localhost\Ubuntu-24.04'+$Path.Replace('/','\')}
function Public-File([string]$Path,[byte[]]$Bytes){Private-File $Path;if(Test-Path -LiteralPath $Path){if([Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([IO.File]::ReadAllBytes($Path))) -ne [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($Bytes))){throw 'Perfil existente distinto; revisar rotacion sin sobrescribir'}}else{[IO.File]::WriteAllBytes($Path,$Bytes)};Private-File $Path}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$Perfil=[IO.Path]::GetFullPath($Perfil);$Evidencia=[IO.Path]::GetFullPath($Evidencia);Private-File $Perfil;Private-File $Evidencia
if($RuntimeRoot -notmatch '^/home/nexbyteproveedor/\.local/share/siger/(nna-local-20261023|e2e-siger-[a-f0-9]{32})$' -or -not $Evidencia.StartsWith('C:\req\evidencias\')){throw 'Solo origen y evidencia de laboratorio'}
if($RuntimeRoot.EndsWith('/nna-local-20261023')){if($Perfil -ne 'C:\req\config\formulario-wsl-integracion'){throw 'Conservar perfil original'}}elseif($Perfil -notmatch '^C:\\req\\config\\formulario-wsl-e2e-[a-f0-9]{32}$'){throw 'QA requiere copia DPAPI independiente'}
$null=New-Item -ItemType Directory -Path $Perfil,$Evidencia -Force;Private-File $Perfil;Private-File $Evidencia
$source=Wsl-Path $RuntimeRoot
$process=$null;$payload=$null;$keyBytes=$null;$secretBytes=$null
try{
 $runtime=Get-Content -LiteralPath (Join-Path $source 'active-runtime.json') -Raw|ConvertFrom-Json
 if($runtime.config -ne ($RuntimeRoot+'/application.properties') -or (Get-FileHash -LiteralPath (Join-Path $source 'application.properties') -Algorithm SHA256).Hash.ToLowerInvariant() -ne $runtime.config_sha256){throw 'Configuracion activa distinta; revisar sin modificar el productor'}
 $properties=@{};foreach($line in [IO.File]::ReadAllLines((Join-Path $source 'application.properties'))){$kv=$line.Split('=',2);if($kv.Count -eq 2 -and $kv[0] -in @('publico.integracion.token','publico.comprobante.trusted-keys','publico.comprobante.institutional-trusted-keys','publico.comprobante.key-id')){$properties[$kv[0]]=$kv[1].Trim()}}
 $token=$properties['publico.integracion.token'];if($token -notmatch '^[A-Za-z0-9_-]{32,400}$'){throw 'Credencial tecnica no disponible'}
 $publicTrust=Get-Content -LiteralPath (Wsl-Path $properties['publico.comprobante.trusted-keys']) -Raw|ConvertFrom-Json -AsHashtable
 $publicId=$properties['publico.comprobante.key-id'];if(-not $publicTrust.ContainsKey($publicId)){throw 'Clave activa publica no confiada'}
 $institutionalTrust=Get-Content -LiteralPath (Wsl-Path $properties['publico.comprobante.institutional-trusted-keys']) -Raw|ConvertFrom-Json -AsHashtable
 $original=Source-Bytes 'publico-trust.json';try{$merged=[Text.Encoding]::UTF8.GetString($original)|ConvertFrom-Json -AsHashtable}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($original)}
 foreach($id in $publicTrust.Keys){if($merged.ContainsKey($id) -and $merged[$id].Trim() -ne $publicTrust[$id].Trim()){throw 'Colision de identificador de clave publica'};$merged[$id]=$publicTrust[$id]}
 $own=Source-Bytes 'institucional-trust.json';try{$ownTrust=[Text.Encoding]::UTF8.GetString($own)|ConvertFrom-Json -AsHashtable}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($own)}
 if($ownTrust.Count -ne 1){throw 'Seleccion de clave institucional requiere revision'};$ownId=@($ownTrust.Keys)[0]
 $ca=Join-Path $Perfil 'rootCA.pem';Public-File $ca ([IO.File]::ReadAllBytes('\\wsl.localhost\Ubuntu-24.04\home\nexbyteproveedor\.local\share\siger\sandbox-https\rootCA.pem'))
 Public-File (Join-Path $Perfil 'publico-trust.json') ([Text.Encoding]::UTF8.GetBytes(($merged|ConvertTo-Json -Compress)))
 $credentialPath=Join-Path $Perfil 'conexion.json.dpapi';Private-File $credentialPath
 if(Test-Path -LiteralPath $credentialPath){$secretBytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($credentialPath),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser);$credential=[Text.Encoding]::UTF8.GetString($secretBytes)|ConvertFrom-Json;if($credential.token -cne $token -or $credential.operationalToken -notmatch '^[A-Za-z0-9_-]{64}$'){throw 'Perfil privado distinto; revisar rotacion'}}else{
  $credential=@{token=$token;operationalToken=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLowerInvariant()};$secretBytes=[Text.Encoding]::UTF8.GetBytes(($credential|ConvertTo-Json -Compress));[IO.File]::WriteAllBytes($credentialPath,[Security.Cryptography.ProtectedData]::Protect($secretBytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser));Private-File $credentialPath
 }
 $java='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin';$classes='C:\req\scratch\sonda-formulario-'+[guid]::NewGuid().ToString('N');$null=New-Item -ItemType Directory -Path $classes
 & (Join-Path $java 'javac.exe') -cp 'C:\req\repo\siger-institucional\backend\target\classes;C:\req\bin\lib\*' -d $classes 'C:\req\repo\scripts\qa\PrepararConexionFormulario.java' 2>&1 | Set-Content -LiteralPath (Join-Path $Evidencia 'sonda-compilacion.log');if($LASTEXITCODE -ne 0){throw 'No compilo la sonda'}
 $keyBytes=Source-Bytes 'institucional-key.pem'
 $payload=@{origin='https://formulario.defensorianinez.gob.cl';token=$token;ca=$ca;publicTrust=(Join-Path $Perfil 'publico-trust.json');publicKeyId=$publicId;institutionalTrust=$institutionalTrust;institutionalKeyId=$ownId;institutionalPrivateKey=[Text.Encoding]::UTF8.GetString($keyBytes)}|ConvertTo-Json -Depth 5 -Compress
 $start=[Diagnostics.ProcessStartInfo]::new((Join-Path $java 'java.exe'));$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true;$start.WorkingDirectory=$classes
 foreach($arg in @('-XX:-UsePerfData','-Xmx128m','-cp',($classes+';C:\req\repo\siger-institucional\backend\target\classes;C:\req\bin\lib\*'),'PrepararConexionFormulario')){$start.ArgumentList.Add($arg)}
 $process=[Diagnostics.Process]::Start($start);$outTask=$process.StandardOutput.ReadToEndAsync();$errTask=$process.StandardError.ReadToEndAsync();$process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null
 if(-not $process.WaitForExit(45000)){$process.Kill($true);throw 'Sonda vencida'}
 $result=$outTask.GetAwaiter().GetResult()|ConvertFrom-Json;$null=$errTask.GetAwaiter().GetResult()
 if($process.ExitCode -ne 0 -or -not $result.technicalAuthenticationReady -or -not $result.institutionalSigningTrustedOffline){throw 'Sonda no acreditada; sin recuperacion ni ACK'}
 $result|ConvertTo-Json -Depth 6|Set-Content -LiteralPath (Join-Path $Evidencia 'conexion-solo-lectura.json')
 $profile=@{origin='https://formulario.defensorianinez.gob.cl';ca=$ca;publicTrust=(Join-Path $Perfil 'publico-trust.json');credential=$credentialPath;publicKeyId=$publicId;publicJarSHA256=$runtime.sha256;publicRecoveryEnabled=$false;automaticRecovery=$false;technicalAuthenticationReady=$true;institutionalSigningTrustedOffline=$true;checkedAt=[DateTimeOffset]::UtcNow.ToString('O')}
 $profile|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $Perfil 'perfil.json');Private-File (Join-Path $Perfil 'perfil.json')
 @{ready=$true;profile=(Join-Path $Perfil 'perfil.json');publicRecoveryEnabled=$false;claims=0;acksSent=0;evidence=$Evidencia}|ConvertTo-Json -Compress
}catch{throw 'Preparacion de conexion no acreditada. Valores privados y excepciones HTTP omitidos; no se reclamo ni confirmo una entrega.'}
finally{if($process){if(-not $process.HasExited){$process.Kill($true)};$process.Dispose()};foreach($bytes in @($keyBytes,$secretBytes)){if($bytes){[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes)}};$payload=$null;$token=$null;$credential=$null;if($properties){$properties.Clear()}}
