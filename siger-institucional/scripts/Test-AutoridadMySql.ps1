#Requires -Version 7.4
[CmdletBinding()]
param([Parameter(Mandatory)][string]$JavaHome,[Parameter(Mandatory)][string]$Maven,[Parameter(Mandatory)][string]$MySqlBin)
$ErrorActionPreference='Stop'
$MySqlBin=[IO.Path]::GetFullPath($MySqlBin)
if(-not $IsWindows){throw 'Este auxiliar provisiona solo una instancia Windows nativa descartable.'}
if(Get-ChildItem Env:|Where-Object {$_.Name -like 'ROBUST_DB_*' -or $_.Name -like 'DOCUMENTAL_REAL_*' -or $_.Name -eq 'SIGER_U15_TEST_MYSQL_PASSWORD'}){throw 'Retirar del proceso las variables de otros entornos; no se reutilizan recursos ni credenciales.'}
$java=Join-Path $JavaHome 'bin/java.exe';$version=(& $java --version 2>&1)-join ' '
if($LASTEXITCODE -or $version -notmatch '(?:openjdk|java) 25(?:[.\s-])'){throw 'Seleccionar Java25 explícito.'}
foreach($exe in @('mysqld.exe','mysqladmin.exe')){if(-not(Test-Path -LiteralPath (Join-Path $MySqlBin $exe))){throw 'Faltan binarios MySQL; no se instala ni modifica un servicio.'}}
$base=Join-Path $env:LOCALAPPDATA ('siger-autoridad-mysql-'+[guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($base)|Out-Null
$sid=[Security.Principal.WindowsIdentity]::GetCurrent().User;$acl=[Security.AccessControl.DirectorySecurity]::new();$acl.SetOwner($sid);$acl.SetAccessRuleProtection($true,$false)
foreach($id in @($sid.Value,'S-1-5-18','S-1-5-32-544')){$acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new([Security.Principal.SecurityIdentifier]::new($id),'FullControl','ContainerInherit,ObjectInherit','None','Allow'))}
Set-Acl -LiteralPath $base -AclObject $acl
$dir=Join-Path $base 'data';[IO.Directory]::CreateDirectory($dir)|Out-Null
$port=Get-Random -Minimum 55000 -Maximum 61000
if(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue){throw 'Puerto efímero ocupado; ejecutar nuevamente crea otro destino.'}
$password=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$schema='siger_u15_test_'+[guid]::NewGuid().ToString('N');$init=Join-Path $base 'init.sql';$client=Join-Path $base 'client.cnf'
Set-Content -LiteralPath $init -Encoding utf8 -Value ("ALTER USER 'root'@'localhost' IDENTIFIED BY '$password';`nCREATE USER 'root'@'127.0.0.1' IDENTIFIED BY '$password';`nGRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1';`nCREATE DATABASE $($schema)_14 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_151 CHARACTER SET utf8mb4;")
Set-Content -LiteralPath $client -Encoding utf8 -Value ("[client]`nuser=root`npassword=$password`nhost=127.0.0.1`nprotocol=TCP`nport=$port")
# --no-defaults debe ser el primer argumento: nunca leer my.ini ni usar MySQL80.
& (Join-Path $MySqlBin 'mysqld.exe') --no-defaults --initialize-insecure ('--basedir='+[IO.Path]::GetFullPath((Join-Path $MySqlBin '..'))) ('--datadir='+$dir) --innodb-buffer-pool-size=64M ('--log-error='+$base+'\initialize.log')
if($LASTEXITCODE){throw 'Inicialización aislada no completada; evidencia privada conservada.'}
$serverArgs=@('--no-defaults',('--datadir='+$dir),('--port='+$port),'--bind-address=127.0.0.1','--mysqlx=OFF','--skip-log-bin','--innodb-buffer-pool-size=64M','--innodb-log-buffer-size=8M','--performance-schema=OFF','--max-connections=20','--skip-name-resolve',('--tmpdir='+$dir),'--secure-file-priv=NULL',('--log-error='+$base+'\server.log'),('--pid-file='+$base+'\server.pid'),('--init-file='+$init))
$proc=$null;$oldJava=$env:JAVA_HOME;$oldMaven=$env:MAVEN_OPTS
try{
 $proc=Start-Process -FilePath (Join-Path $MySqlBin 'mysqld.exe') -ArgumentList ($serverArgs|ForEach-Object {'"'+$_+'"'}) -WindowStyle Hidden -PassThru
 $ready=$false
 for($i=0;$i -lt 100;$i++){$proc.Refresh();if($proc.HasExited){throw 'La instancia propia no arrancó; consultar log privado.'};& (Join-Path $MySqlBin 'mysqladmin.exe') ('--defaults-file='+$client) status *> $null;if($LASTEXITCODE -eq 0){$ready=$true;break};Start-Sleep -Milliseconds 200}
 if(-not $ready){throw 'La instancia propia no aceptó autenticación dentro del plazo.'}
 $env:SIGER_U15_TEST_MYSQL_PASSWORD=$password;$env:JAVA_HOME=$JavaHome;$env:MAVEN_OPTS='-Xmx256m -XX:-UsePerfData'
 Push-Location (Join-Path $PSScriptRoot '../backend')
 try{& $Maven -B -ntp '-Dtest=AutoridadMigracionTest' ('-Dsiger.test.autoridad.mysql=jdbc:mysql://127.0.0.1:'+$port+'/'+$schema) ('-DargLine=--enable-native-access=ALL-UNNAMED -Xmx256m -XX:-UsePerfData -Djava.io.tmpdir='+$base) test *> (Join-Path $base 'tests.log');if($LASTEXITCODE){throw 'Falló AutoridadMigracionTest; ver tests.log privado.'}}finally{Pop-Location}
 Write-Output 'MySQL descartable: dos rutas de migración y circuito transaccional correctos; tres tests sin omisiones.'
}finally{
 $env:SIGER_U15_TEST_MYSQL_PASSWORD=$null;$env:JAVA_HOME=$oldJava;$env:MAVEN_OPTS=$oldMaven
 if($proc){
  & (Join-Path $MySqlBin 'mysqladmin.exe') ('--defaults-file='+$client) shutdown *> $null
  $proc.WaitForExit(10000)|Out-Null
  # mysqld en Windows puede lanzar un hijo. Solo detener PIDs con el datadir único que este auxiliar creó.
  $own=@(Get-CimInstance Win32_Process -Filter "Name='mysqld.exe'"|Where-Object {$_.CommandLine -and $_.CommandLine.Contains('--datadir='+$dir) -and $_.ExecutablePath -eq (Join-Path $MySqlBin 'mysqld.exe')})
  foreach($p in $own){Stop-Process -Id $p.ProcessId;Wait-Process -Id $p.ProcessId -Timeout 10 -ErrorAction SilentlyContinue}
 }
 Write-Output ('Evidencia privada conservada en '+$base+'; no se borran bases ni se toca un servicio.')
}
