param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$path='C:\req\config\windows-qa-localhost-tls.json.dpapi';$public='C:\req\config\windows-qa-localhost.cer'
if(Test-Path -LiteralPath $path){
 if(-not (Test-Path -LiteralPath $public)){throw 'TLS QA incompleto, conservar antes de corregir'}
 $cert=[Security.Cryptography.X509Certificates.X509Certificate2]::new([IO.File]::ReadAllBytes($public))
 if(-not (Test-Path ('Cert:\CurrentUser\Root\'+$cert.Thumbprint)) -or $cert.NotAfter -lt (Get-Date).AddDays(1)){throw 'TLS QA requiere revision; no se renueva silenciosamente'}
 @{existing=$true;thumbprint=$cert.Thumbprint;trust='CurrentUser Root, leaf localhost only'}|ConvertTo-Json -Compress
 return
}
if(Test-Path -LiteralPath $public){throw 'Certificado previo sin DPAPI; no reemplazar'}
$rsa=[Security.Cryptography.RSA]::Create(2048)
try{
 $request=[Security.Cryptography.X509Certificates.CertificateRequest]::new('CN=localhost SIGER Windows QA',$rsa,[Security.Cryptography.HashAlgorithmName]::SHA256,[Security.Cryptography.RSASignaturePadding]::Pkcs1)
 $request.CertificateExtensions.Add([Security.Cryptography.X509Certificates.X509BasicConstraintsExtension]::new($false,$false,0,$true))
 $request.CertificateExtensions.Add([Security.Cryptography.X509Certificates.X509KeyUsageExtension]::new([Security.Cryptography.X509Certificates.X509KeyUsageFlags]::DigitalSignature -bor [Security.Cryptography.X509Certificates.X509KeyUsageFlags]::KeyEncipherment,$true))
 $eku=[Security.Cryptography.OidCollection]::new();$eku.Add([Security.Cryptography.Oid]::new('1.3.6.1.5.5.7.3.1'))|Out-Null
 $request.CertificateExtensions.Add([Security.Cryptography.X509Certificates.X509EnhancedKeyUsageExtension]::new($eku,$true))
 $san=[Security.Cryptography.X509Certificates.SubjectAlternativeNameBuilder]::new();$san.AddDnsName('localhost');$san.AddIpAddress([Net.IPAddress]::Loopback);$san.AddIpAddress([Net.IPAddress]::IPv6Loopback);$request.CertificateExtensions.Add($san.Build())
 $cert=$request.CreateSelfSigned([DateTimeOffset]::UtcNow.AddMinutes(-5),[DateTimeOffset]::UtcNow.AddDays(30))
 $password=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(30))
 $pfx=$cert.Export([Security.Cryptography.X509Certificates.X509ContentType]::Pfx,$password)
 try{$clear=[Text.Encoding]::UTF8.GetBytes((@{pfx=[Convert]::ToBase64String($pfx);password=$password}|ConvertTo-Json -Compress));try{[IO.File]::WriteAllBytes($path,[Security.Cryptography.ProtectedData]::Protect($clear,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($clear)}}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($pfx);$password=$null}
 [IO.File]::WriteAllBytes($public,$cert.Export([Security.Cryptography.X509Certificates.X509ContentType]::Cert))
 $store=[Security.Cryptography.X509Certificates.X509Store]::new('Root','CurrentUser')
 try{$store.Open([Security.Cryptography.X509Certificates.OpenFlags]::ReadWrite);$store.Add([Security.Cryptography.X509Certificates.X509Certificate2]::new([IO.File]::ReadAllBytes($public)))}finally{$store.Close()}
 @{created=$true;thumbprint=$cert.Thumbprint;notAfter=$cert.NotAfter.ToString('o');trust='CurrentUser Root, leaf localhost only';privateKey='DPAPI CurrentUser';newMachineTrust=$false}|ConvertTo-Json|Tee-Object C:\req\evidencias\tls-localhost-qa-2026-10-26.json
}finally{$rsa.Dispose()}
