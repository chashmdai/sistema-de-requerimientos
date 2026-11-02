#Requires -Version 7.4
[CmdletBinding()]
param(
 [Parameter(Mandatory)][string]$Jar,
 [ValidateSet('SONDA','MIGRAR','CICLO')][string]$Accion='SONDA',
 [string]$Perfil='C:\req\config\formulario-ddn-ubuntu-u4\perfil.json',
 [switch]$EnsayoSinteticoCoordinado
)
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows'}
if($Accion -eq 'CICLO' -and -not $EnsayoSinteticoCoordinado){throw 'CICLO requiere ventana sintetica coordinada con Ubuntu'}
function Assert-Private([string]$Path){
 $p=[IO.Path]::GetFullPath($Path)
 if(-not $p.StartsWith('C:\req\',[StringComparison]::OrdinalIgnoreCase)){throw 'Ruta fuera del laboratorio'}
 for($cursor=$p;$cursor;$cursor=[IO.Path]::GetDirectoryName($cursor)){
  if(Test-Path -LiteralPath $cursor){if((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlaces no permitidos'}}
 }
 $allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')
 foreach($r in (Get-Acl -LiteralPath $p).Access){if($r.AccessControlType -eq 'Allow' -and -not ($r.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'ACL no privada'}}
}
$artifact=[IO.Path]::GetFullPath($Jar)
if($artifact -notmatch '^C:\\req\\bin\\siger-([a-f0-9]{16})\.jar$'){throw 'Solo artefacto inmutable fuera de target'}
$prefix=$Matches[1];Assert-Private $artifact
if(-not (Get-FileHash -LiteralPath $artifact).Hash.ToLowerInvariant().StartsWith($prefix)){throw 'SHA del artefacto incompatible'}
if([IO.Path]::GetFullPath($Perfil) -ne 'C:\req\config\formulario-ddn-ubuntu-u4\perfil.json'){throw 'Solo perfil U4 independiente'}
Assert-Private $Perfil
$profile=Get-Content -LiteralPath $Perfil -Raw|ConvertFrom-Json
if($profile.scope -ne 'ENSAYO_SINTETICO_U4_DDN_UBUNTU' -or $profile.origin -ne 'https://formulario.defensorianinez.gob.cl:25443' -or $profile.deployedSha -ne '099c3b16e71d57b335496c3a112daccfa38831db' -or $profile.archivo -notmatch '^C:\\req\\testdata\\u4-ddn-ubuntu-[a-f0-9]{32}\\archivo$'){throw 'Perfil U4 rechazado'}
$config=@{accion=$Accion}
$tokenBytes=$null;$keyBytes=$null;$process=$null;$credential=$null
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
try{
 if($Accion -ne 'MIGRAR'){
  foreach($name in @('ca','publicTrust','ownTrust','credential')){
   $path=$profile.$name
   if([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($path)) -ne [IO.Path]::GetDirectoryName($Perfil)){throw 'Material fuera del perfil U4'}
   Assert-Private $path
  }
  $tokenBytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($profile.credential),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
  $token=([Text.Encoding]::UTF8.GetString($tokenBytes)|ConvertFrom-Json).token
  $keyPath='C:\req\config\origen-dpapi\institucional-key.pem.dpapi';Assert-Private $keyPath
  $keyBytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($keyPath),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
  foreach($name in @('origin','ca','caSHA256','publicTrust','publicTrustSHA256','ownTrust','keyId','archivo')){$config[$name]=[string]$profile.$name}
  $config.token=$token;$config.privateKey=[Text.Encoding]::UTF8.GetString($keyBytes);$token=$null
 }
 if($Accion -ne 'SONDA'){
  $credPath=if($Accion -eq 'MIGRAR'){'C:\req\config\mysql-u4-ubuntu-migrator.credential.xml'}else{'C:\req\config\mysql-u4-ubuntu-app.credential.xml'}
  Assert-Private $credPath;$credential=Import-Clixml -LiteralPath $credPath
  $expected=if($Accion -eq 'MIGRAR'){'siger_u4_ubuntu_migrator'}else{'siger_u4_ubuntu_app'}
  if($credential -isnot [pscredential] -or $credential.UserName -cne $expected){throw 'Identidad U4 requerida'}
  $config.dbUser=$credential.UserName;$config.dbPassword=$credential.GetNetworkCredential().Password
  $config.dbUrl='jdbc:mysql://127.0.0.1:3306/siger_u4_ddn_ubuntu?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000&characterEncoding=UTF-8'
 }
 $start=[Diagnostics.ProcessStartInfo]::new('C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe')
 $start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 $start.WorkingDirectory=[IO.Path]::GetDirectoryName($Perfil)
 $start.Environment.Clear();foreach($name in @('SystemRoot','windir','USERPROFILE','LOCALAPPDATA')){if([Environment]::GetEnvironmentVariable($name)){$start.Environment[$name]=[Environment]::GetEnvironmentVariable($name)}}
 $start.Environment['TEMP']=[IO.Path]::GetDirectoryName($Perfil);$start.Environment['TMP']=$start.Environment['TEMP']
 foreach($a in @('--enable-native-access=ALL-UNNAMED','-XX:-UsePerfData','-Xmx256m','-Dloader.main=cl.nexbyte.siger.institucional.windows.EnsayoFormularioUbuntu','-cp',$artifact,'org.springframework.boot.loader.launch.PropertiesLauncher')){$start.ArgumentList.Add($a)}
 $process=[Diagnostics.Process]::Start($start);$out=$process.StandardOutput.ReadToEndAsync();$err=$process.StandardError.ReadToEndAsync()
 $payload=$config|ConvertTo-Json -Compress;$process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$config.Clear();$credential=$null
 if(-not $process.WaitForExit(360000)){$process.Kill($true);throw 'Ensayo vencido; conservar base/custodia para conciliacion'}
 $result=$out.GetAwaiter().GetResult()|ConvertFrom-Json;$null=$err.GetAwaiter().GetResult()
 if($process.ExitCode -ne 0){throw 'Ensayo U4 rechazado; detalles privados omitidos. No afecta consumidor QA.'}
 $result|ConvertTo-Json -Depth 6
}finally{
 if($process){if(-not $process.HasExited){$process.Kill($true)};$process.Dispose()}
 foreach($bytes in @($tokenBytes,$keyBytes)){if($bytes){[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes)}}
 $config.Clear();$payload=$null;$credential=$null;$token=$null
}
