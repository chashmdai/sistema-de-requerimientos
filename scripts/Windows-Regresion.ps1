param([ValidateSet('H2','MySQL','Frontend','Package')][string]$Mode='H2')
$ErrorActionPreference='Stop'
if (-not $IsWindows) {throw 'Solo Windows nativo'}
$repo=Split-Path $PSScriptRoot;$backend=Join-Path $repo 'siger-institucional\backend';$front=Join-Path $repo 'siger-institucional\front'
$excluded=@('vectorInteroperableYDuplicadosConcurrentesTienenUnaConfirmacionEstable','doscientosFoliosDistintosBajoConcurrenciaYFormatoHistoricoEstable','consumidorParaleloAcotadoConservaUnicidadYConfirmaCadaRegistro')
$source=Get-Content (Join-Path $backend 'src\test\java\cl\nexbyte\siger\institucional\robustez\RecepcionInstitucionalJdbcTest.java') -Raw
foreach($name in $excluded) {if ($source -notmatch ('void\s+'+[regex]::Escape($name)+'\s*\(')) {throw 'Filtro de carga no comprobado'}}
if (Get-ChildItem Env:|Where-Object {$_.Name -like 'ROBUST_DB_*' -or $_.Name -like 'DOCUMENTAL_REAL_*'}) {throw 'No mezclar configuracion QA con motores/workers ajenos'}
[IO.Directory]::CreateDirectory('C:\req\scratch\tests')|Out-Null
$oldJava=$env:JAVA_HOME;$oldTmp=$env:TMP;$oldTemp=$env:TEMP;$oldOptions=$env:JAVA_TOOL_OPTIONS
try {
 $env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot';$env:TMP='C:\req\scratch\tests';$env:TEMP=$env:TMP;$env:JAVA_TOOL_OPTIONS=($oldOptions+' -XX:-UsePerfData').Trim()
 if($Mode -eq 'MySQL') {
  foreach($role in @('app','migrator')) {
   $file="C:\req\config\mysql-windows-qa-$role.credential.xml"
   if(-not (Test-Path -LiteralPath $file)) {throw 'Falta provisioning administrativo QA y DPAPI; no se toca la copia historica'}
   $cred=Import-Clixml -LiteralPath $file
   if($cred -isnot [pscredential] -or $cred.UserName -ne "siger_windows_qa_$role") {throw 'Identidad QA invalida'}
   $start=[Diagnostics.ProcessStartInfo]::new("$env:JAVA_HOME\bin\java.exe");$start.UseShellExecute=$false;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true;$start.CreateNoWindow=$true
   foreach($arg in @('--class-path','C:\req\bin\lib\*',(Join-Path $PSScriptRoot 'WindowsQaConexion.java'))) {$start.ArgumentList.Add($arg)}
   $p=[Diagnostics.Process]::Start($start)
   try{$payload=@{username=$cred.UserName;password=$cred.GetNetworkCredential().Password}|ConvertTo-Json -Compress;$p.StandardInput.Write($payload);$p.StandardInput.Close();$payload=$null;$stdout=$p.StandardOutput.ReadToEndAsync();$stderr=$p.StandardError.ReadToEndAsync();if(-not $p.WaitForExit(30000)){$p.Kill($true);throw 'QA JDBC timeout'};if($p.ExitCode -ne 0){throw 'QA JDBC no verificado; detalle remoto omitido'};$stdout.Result.Trim()|Set-Content "C:\req\evidencias\qa-jdbc-$role.json"}finally{$p.Dispose()}
   if($role -eq 'app'){$env:ROBUST_DB_USER=$cred.UserName;$env:ROBUST_DB_PASSWORD=$cred.GetNetworkCredential().Password}else{$env:ROBUST_DB_MIGRATION_USER=$cred.UserName;$env:ROBUST_DB_MIGRATION_PASSWORD=$cred.GetNetworkCredential().Password};$cred=$null
  }
  $env:ROBUST_DB_URL='jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000&characterEncoding=UTF-8'
 }
 if($Mode -eq 'Frontend') {Push-Location $front;try{foreach($step in @('test','lint','build')){& npm.cmd run $step *> "C:\req\logs\frontend-$step-windows.log";if($LASTEXITCODE -ne 0){throw 'Frontend no correcto'}}}finally{Pop-Location}}
 else {Push-Location $backend;try{
   if($Mode -in @('H2','MySQL')) {& C:\maven\bin\mvn.cmd -B -ntp '-Djava.io.tmpdir=C:\req\scratch\tests' '-DargLine=--enable-native-access=ALL-UNNAMED -XX:-UsePerfData -Djava.io.tmpdir=C:\req\scratch\tests' ('-Dtest=*,!RecepcionInstitucionalJdbcTest#'+($excluded -join '+')) test *> "C:\req\logs\backend-$($Mode.ToLower())-windows.log"}
   else{& C:\maven\bin\mvn.cmd -B -ntp -DskipTests package *> C:\req\logs\package-windows-final.log};$code=$LASTEXITCODE
  }finally{Pop-Location};if($code -ne 0){throw 'Bateria no correcta: consultar resultados locales'}}
}finally{Get-ChildItem Env:|Where-Object {$_.Name -like 'ROBUST_DB_*'}|ForEach-Object {Remove-Item -LiteralPath ('Env:\'+$_.Name)};$env:JAVA_HOME=$oldJava;$env:TMP=$oldTmp;$env:TEMP=$oldTemp;$env:JAVA_TOOL_OPTIONS=$oldOptions}