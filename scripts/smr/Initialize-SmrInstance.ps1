#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[ValidateSet('recepcion','sintetico')][string]$Instance,[Parameter(Mandatory)][string]$Release,[int]$Port=0)
. "$PSScriptRoot\Common.ps1"
Assert-SmrPrivate $Root;$base=Join-Path $Root "instances\$Instance"
if(Test-Path -LiteralPath "$base\instance.json"){Write-Output 'Instancia ya configurada; no sobrescrita';return}
if(-not $Port){$Port=if($Instance -eq 'sintetico'){18443}else{18444}}
if(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue){throw 'Puerto UI ocupado'}
foreach($p in @($base,"$base\private","$base\store","$base\logs","$base\producer")){[IO.Directory]::CreateDirectory($p)|Out-Null}
$JavaHome=Join-Path ([IO.Path]::GetFullPath($Release)) 'runtime\java25';$java=Join-Path $JavaHome 'bin\java.exe';if(-not(Test-Path -LiteralPath $java)){throw 'El paquete requiere Java25 portable propio'};$schema="smr_$Instance";if($Instance -eq 'sintetico'){
 foreach($role in @('ack','producer')){
  $keys=(Invoke-SmrJava $java @('-XX:-UsePerfData',"$PSScriptRoot\IdentitySmr.java") '') -split '\r?\n'|Where-Object {$_}
  $identity=@{keyId="smr-test-$role-v1";privatePem="-----BEGIN PRIVATE KEY-----`n$($keys[0])`n-----END PRIVATE KEY-----`n";publicPem="-----BEGIN PUBLIC KEY-----`n$($keys[1])`n-----END PUBLIC KEY-----`n"}
  Protect-SmrJson $identity "$base\private\$role.dpapi"
 }
 $producer=Read-SmrJson "$base\private\producer.dpapi";[IO.File]::WriteAllText("$base\private\producer-trust.json",(@{$producer.keyId=$producer.publicPem}|ConvertTo-Json))
 Protect-SmrJson @{token=(New-SmrSecret)} "$base\private\producer-token.dpapi"
}
$password=New-SmrSecret
Protect-SmrJson @{username='smr.pruebas';password=$password} "$base\private\access.dpapi"
Protect-SmrJson @{cuenta=@{usuario='smr.pruebas';nombre='Operador local de pruebas SMR';correo="smr.$Instance@example.invalid";cargo='Pruebas técnicas locales';unidad=$null;sede=$null;password=$password};lectura=$true;operacion=$true} "$base\private\bootstrap.dpapi"
$platform=Get-Content -Raw "$Root\mysql\platform.json"|ConvertFrom-Json
$url="jdbc:mysql://127.0.0.1:$($platform.port)/${schema}?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/$($Root.Replace('\','/'))/config/mysql-trust.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000&characterEncoding=UTF-8"
$config=@{instance=$Instance;release=[IO.Path]::GetFullPath($Release);javaHome=$JavaHome;port=$Port;schema=$schema;dbUrl=$url;installationId=[guid]::NewGuid().ToString();origin=$(if($Instance -eq 'sintetico'){'https://localhost:19443'}else{'https://formulario.defensorianinez.gob.cl'});consumerEnabled=($Instance -eq 'sintetico');synthetic=($Instance -eq 'sintetico');ownerSid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value}
$config|ConvertTo-Json|Set-Content -LiteralPath "$base\instance.json"
$password=$null;$identity=$null;$keys=$null
Write-Output "Instancia $Instance configurada para https://localhost:$Port/; credencial únicamente en visor privado."
