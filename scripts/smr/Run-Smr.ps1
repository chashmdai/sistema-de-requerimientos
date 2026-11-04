#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[ValidateSet('recepcion','sintetico')][string]$Instance='recepcion')
. "$PSScriptRoot\Common.ps1"
$base=Join-Path $Root "instances\$Instance";Assert-SmrPrivate $base;$c=Get-Content -Raw "$base\instance.json"|ConvertFrom-Json
$launcherLock=[IO.File]::Open("$base\launcher.lock",'OpenOrCreate','ReadWrite','None')
if($c.ownerSid -ne [Security.Principal.WindowsIdentity]::GetCurrent().User.Value){throw 'Reaprovisionar DPAPI bajo la identidad efectiva'}
$jar=Join-Path $c.release 'smr-rc1.jar';$manifest=Get-Content -Raw (Join-Path $c.release 'manifest.json')|ConvertFrom-Json
if([IO.Path]::GetFullPath($c.javaHome) -ne (Join-Path ([IO.Path]::GetFullPath($c.release)) 'runtime\java25')){throw 'Runtime Java SMR debe pertenecer al mismo paquete'}
if((Get-FileHash (Join-Path $c.javaHome 'bin\java.exe')).Hash.ToLowerInvariant() -ne $manifest.javaExeSha256){throw 'Runtime Java SMR no coincide'}
if((Get-FileHash -LiteralPath $jar).Hash.ToLowerInvariant() -ne $manifest.jarSha256){throw 'Artefacto SMR no coincide'}
if(Get-NetTCPConnection -LocalPort $c.port -State Listen -ErrorAction SilentlyContinue){throw 'Ya existe listener; no lanzar duplicado'}
$private="$base\private";$run=Join-Path $private ('run-'+[guid]::NewGuid().ToString('N'));[IO.Directory]::CreateDirectory($run)|Out-Null
$ack=if($c.synthetic){Read-SmrJson "$private\ack.dpapi"}else{Read-SmrJson "$Root\secrets\ack-identity.dpapi"}
$db=Read-SmrJson "$Root\secrets\$($c.schema)_app.dpapi";$tls=Read-SmrJson "$Root\secrets\tls-password.dpapi"
[IO.File]::WriteAllText("$run\ack-private.pem",$ack.privatePem);[IO.File]::WriteAllText("$run\ack-trust.json",(@{$ack.keyId=$ack.publicPem}|ConvertTo-Json))
if(-not $c.synthetic){Copy-Item -LiteralPath "$Root\public\ack\ack-trust.json" -Destination "$run\ack-trust.json" -Force}
Copy-Item -LiteralPath $(if($c.synthetic){"$private\producer-trust.json"}else{"$Root\config\formulario-signing-trust.json"}) -Destination "$run\producer-trust.json"
if(-not(Test-Path -LiteralPath "$private\bootstrap.json.aplicado")){$bootstrap=Read-SmrJson "$private\bootstrap.dpapi";[IO.File]::WriteAllText("$private\bootstrap.json",($bootstrap|ConvertTo-Json -Depth 10));$bootstrap.Clear()}
$p=@{'institucional.db.url'=$c.dbUrl;'institucional.db.user'=$db.username;'institucional.db.password'=$db.password;'server.port'=[string]$c.port;'server.address'='127.0.0.1';'server.ssl.key-store'="file:$Root/secrets/tls/localhost.p12";'server.ssl.key-store-password'=$tls.password;'institucional.configuracion-privada.directorio'=$private;'institucional.documentos.directorio'="$base\store";'institucional.comprobante.key-id'=$ack.keyId;'institucional.comprobante.private-key'="$run\ack-private.pem";'institucional.comprobante.own-trusted-keys'="$run\ack-trust.json";'institucional.comprobante.trusted-keys'="$run\producer-trust.json";'institucional.formulario-completo.origen-publico'=$c.origin;'institucional.formulario-completo.habilitado'=([string][bool]$c.consumerEnabled).ToLowerInvariant();'institucional.smr.instalacion-id'=$c.installationId;'institucional.smr.sintetico'=([string][bool]$c.synthetic).ToLowerInvariant();'institucional.smr.bootstrap.archivo'="$private\bootstrap.json"}
if($c.consumerEnabled){$conn=Read-SmrJson "$private\producer-token.dpapi";$p['institucional.formulario-completo.token']=$conn.token;$p['institucional.formulario-completo.ca-certificado']=if($c.synthetic){"$Root\secrets\tls\ca.pem"}else{"$Root\config\public-edge-root-ca.crt"}}
if(-not $c.synthetic){$p['institucional.smr.autorizacion.archivo']="$base\autorizacion-publica.json"}
if($c.PSObject.Properties.Name -contains 'dbPrincipal'){$p['institucional.db.principal']=$c.dbPrincipal}
$si=[Diagnostics.ProcessStartInfo]::new((Join-Path $c.javaHome 'bin\java.exe'));$si.UseShellExecute=$false;$si.CreateNoWindow=$true;$si.RedirectStandardInput=$true;$si.RedirectStandardOutput=$true;$si.RedirectStandardError=$true
foreach($a in @('--enable-native-access=ALL-UNNAMED','-XX:-UsePerfData','-Xmx256m','-jar',$jar,'--smr')){$si.ArgumentList.Add($a)}
$process=[Diagnostics.Process]::Start($si);$stamp=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ');$out=[IO.File]::Open("$base\logs\app-$stamp.log",'Create','Write','Read');$err=[IO.File]::Open("$base\logs\app-$stamp.err.log",'Create','Write','Read')
$outTask=$process.StandardOutput.BaseStream.CopyToAsync($out);$errTask=$process.StandardError.BaseStream.CopyToAsync($err)
try{
 $process.StandardInput.Write(($p|ConvertTo-Json -Compress));$process.StandardInput.Close();$p.Clear();$db.Clear();$ack.Clear();$tls.Clear()
 @{pid=$process.Id;launcherPid=$PID;jar=$jar;startedUtc=[DateTime]::UtcNow.ToString('o');instance=$Instance;port=$c.port;privateRun=$run;serviceInstalled=$false}|ConvertTo-Json|Set-Content -LiteralPath "$base\run.json"
 $process.WaitForExit();$outTask.GetAwaiter().GetResult();$errTask.GetAwaiter().GetResult()
 if($process.ExitCode){throw 'SMR terminó con error; consultar logs privados'}
}finally{
 $out.Dispose();$err.Dispose();if(Test-Path -LiteralPath "$private\bootstrap.json.aplicado"){[IO.File]::WriteAllText("$private\bootstrap.json.aplicado",'{"applied":true}')}
 if($process.HasExited -and (Test-Path -LiteralPath "$run\ack-private.pem")){Remove-Item -LiteralPath "$run\ack-private.pem"}
 $launcherLock.Dispose()
}
