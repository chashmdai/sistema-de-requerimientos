#Requires -Version 7.4
[CmdletBinding()]
param([Parameter(Mandatory)][string]$JavaHome,[Parameter(Mandatory)][string]$Maven,[Parameter(Mandatory)][string]$MySqlBin,[ValidateSet("Completo","NucleoU2","ContinuidadU2","EquipoAgenda","Colaboraciones","Subrogancias","RevisionExcepcional","CierreExcepcional","Reservas","DocumentoAislado","Bitacora","BitacoraRollback","BitacoraEjecutor","IngresoGestion","FixtureJdbc","Territorio","Rectificaciones","AvisosU2","AvisosGestion","AvisosGestionMixto","AvisosGestionSeguridad","CorreoMaterializado","AutoridadMigracion","Clasificacion","ClasificacionReintento","Supervision","SupervisionRuta")][string]$Seleccion="Completo")
$ErrorActionPreference='Stop'
$MySqlBin=[IO.Path]::GetFullPath($MySqlBin)
if(-not $IsWindows){throw 'Este auxiliar provisiona solo una instancia Windows nativa descartable.'}
if(Get-ChildItem Env:|Where-Object {$_.Name -like 'ROBUST_DB_*' -or $_.Name -like 'DOCUMENTAL_REAL_*' -or $_.Name -eq 'SIGER_U15_TEST_MYSQL_PASSWORD' -or $_.Name -eq 'MYSQL_PWD'}){throw 'Retirar del proceso las variables de otros entornos; no se reutilizan recursos ni credenciales.'}
$java=Join-Path $JavaHome 'bin/java.exe';$version=(& $java --version 2>&1)-join ' '
if($LASTEXITCODE -or $version -notmatch '(?:openjdk|java) 25(?:[.\s-])'){throw 'Seleccionar Java25 explícito.'}
foreach($exe in @('mysqld.exe','mysqladmin.exe')){if(-not(Test-Path -LiteralPath (Join-Path $MySqlBin $exe))){throw 'Faltan binarios MySQL; no se instala ni modifica un servicio.'}}
$base=Join-Path $env:LOCALAPPDATA ('siger-autoridad-mysql-'+[guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($base)|Out-Null
$sid=[Security.Principal.WindowsIdentity]::GetCurrent().User;$acl=[Security.AccessControl.DirectorySecurity]::new();$acl.SetOwner($sid);$acl.SetAccessRuleProtection($true,$false)
foreach($id in @($sid.Value,'S-1-5-18','S-1-5-32-544')){$acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new([Security.Principal.SecurityIdentifier]::new($id),'FullControl','ContainerInherit,ObjectInherit','None','Allow'))}
Set-Acl -LiteralPath $base -AclObject $acl
$dir=Join-Path $base 'data';[IO.Directory]::CreateDirectory($dir)|Out-Null
$password=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$schema='siger_u15_test_'+[guid]::NewGuid().ToString('N');$init=Join-Path $base 'init.sql';$client=Join-Path $base 'client.cnf'
Set-Content -LiteralPath $init -Encoding utf8 -Value ("ALTER USER 'root'@'localhost' IDENTIFIED BY '$password';`nCREATE USER 'root'@'127.0.0.1' IDENTIFIED BY '$password';`nGRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1';`nCREATE DATABASE $($schema)_14 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_151 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_152 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_area CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_153 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_materia CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_154 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_contenido CHARACTER SET utf8mb4;")
Add-Content -LiteralPath $init -Encoding utf8 -Value ("CREATE DATABASE $($schema)_155 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_compartir CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_156 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_u2 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_157 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_158 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_159 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_1510 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_1511 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_1512 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_1513 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_reserva CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_bitprev CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_1515 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_1514 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_docsolo CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_docprev CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_excepcion CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_subrog CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_equipo CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_continuidad CHARACTER SET utf8mb4;")
Add-Content -LiteralPath $init -Encoding utf8 -Value ("CREATE DATABASE $($schema)_correo CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_correoprev CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_1516 CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_clasificacion CHARACTER SET utf8mb4;`nCREATE DATABASE $($schema)_supervision CHARACTER SET utf8mb4;")
# --no-defaults debe ser el primer argumento: nunca leer my.ini ni usar MySQL80.
& (Join-Path $MySqlBin 'mysqld.exe') --no-defaults --initialize-insecure ('--basedir='+[IO.Path]::GetFullPath((Join-Path $MySqlBin '..'))) ('--datadir='+$dir) --innodb-buffer-pool-size=64M ('--log-error='+$base+'\initialize.log')
if($LASTEXITCODE){throw 'Inicialización aislada no completada; evidencia privada conservada.'}
# Un puerto sin listener también puede estar reservado por Windows. Verificar bind real,
# sin cambiar exclusiones/firewall ni conectar a servicios existentes. mysqld vuelve a validar al arrancar.
$port=$null
for($attempt=0;$attempt -lt 32;$attempt++){
 $candidate=Get-Random -Minimum 55000 -Maximum 61000
 $probe=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,$candidate);$probe.ExclusiveAddressUse=$true
 try{$probe.Start();$port=$candidate;break}catch [Net.Sockets.SocketException]{}finally{$probe.Stop()}
}
if($null -eq $port){throw 'No hay un puerto efímero aislado disponible; no se modifican reservas o servicios.'}
Set-Content -LiteralPath $client -Encoding utf8 -Value ("[client]`nuser=root`npassword=$password`nhost=127.0.0.1`nprotocol=TCP`nport=$port")

$serverArgs=@('--no-defaults',('--datadir='+$dir),('--port='+$port),'--bind-address=127.0.0.1','--mysqlx=OFF','--skip-log-bin','--innodb-buffer-pool-size=64M','--innodb-log-buffer-size=8M','--performance-schema=OFF','--max-connections=20','--skip-name-resolve',('--tmpdir='+$dir),'--secure-file-priv=NULL',('--log-error='+$base+'\server.log'),('--pid-file='+$base+'\server.pid'),('--init-file='+$init))
function Test-OwnListener {
 $listeners=@(Get-NetTCPConnection -State Listen -LocalAddress '127.0.0.1' -LocalPort $port -ErrorAction SilentlyContinue)
 if($listeners.Count -ne 1){return $false}
 $listenerOwner=Get-CimInstance Win32_Process -Filter ('ProcessId='+[uint32]$listeners[0].OwningProcess)
 return $listenerOwner -and $listenerOwner.ExecutablePath -eq (Join-Path $MySqlBin 'mysqld.exe') -and $listenerOwner.CommandLine -and $listenerOwner.CommandLine.Contains('--datadir='+$dir)
}
$proc=$null;$oldJava=$env:JAVA_HOME;$oldMaven=$env:MAVEN_OPTS;$oldLogin=$env:MYSQL_TEST_LOGIN_FILE
try{
 # Redirigir el login-path del cliente al ensayo, sin leer .mylogin.cnf compartido.
 # https://dev.mysql.com/doc/mysql-installation-excerpt/8.0/en/environment-variables.html
 $env:MYSQL_TEST_LOGIN_FILE=Join-Path $base 'sin-login-compartido.cnf'
 $proc=Start-Process -FilePath (Join-Path $MySqlBin 'mysqld.exe') -ArgumentList ($serverArgs|ForEach-Object {'"'+$_+'"'}) -WindowStyle Hidden -PassThru
 $ready=$false
 for($i=0;$i -lt 100;$i++){$proc.Refresh();if($proc.HasExited){throw 'La instancia propia no arrancó; consultar log privado.'};if(Test-OwnListener){& (Join-Path $MySqlBin 'mysqladmin.exe') ('--defaults-file='+$client) --protocol=TCP --host=127.0.0.1 ('--port='+$port) --user=root status *> $null;if($LASTEXITCODE -eq 0){$ready=$true;break}};Start-Sleep -Milliseconds 200}
 if(-not $ready){throw 'La instancia propia no aceptó autenticación dentro del plazo.'}
 $env:SIGER_U15_TEST_MYSQL_PASSWORD=$password;$env:JAVA_HOME=$JavaHome;$env:MAVEN_OPTS='-Xmx256m -XX:-UsePerfData'
 Push-Location (Join-Path $PSScriptRoot '../backend')
 $tests=if($Seleccion -eq 'Completo'){'SupervisionClasificacionTest,ClasificacionComunicacionesTest,CorreoMaterializadoTest,CorreoMigracionTest,AvisosGestionTest,AvisosNucleoTest,RectificacionesIngresoTest,TerritorioInstitucionalTest,IngresoGestionTest,BitacoraGestionTest,BitacoraMigracionTest,DocumentoAisladoTest,DocumentoAnteriorMigracionTest,AccesoReservadoTest,RevisionExcepcionalTest,SubroganciasTest,ColaboracionContinuidadTest,AutoridadMigracionTest,AreasMigracionTest,MateriasMigracionTest,ContenidoMigracionTest,ComparticionMigracionTest,NucleoU2Test,ContinuidadU2Test,ContinuidadMigracionTest,EquipoAgendaTest,EquipoMigracionTest'}elseif($Seleccion -eq 'NucleoU2'){'NucleoU2Test'}elseif($Seleccion -eq 'ContinuidadU2'){'ContinuidadU2Test'}elseif($Seleccion -eq 'CierreExcepcional'){'RevisionExcepcionalTest#habilitacionEspecificaConfirmaSinJefaturaNiAccesoAHermanosYConservaReq'}elseif($Seleccion -eq 'SupervisionRuta'){'SupervisionClasificacionTest#actuanteRevocadoConSoloSupervisionRecibeRutaDeMetadatos+avisoLlegaASupervisorNominalSinAliasYSeRetiraAlLiberar'}elseif($Seleccion -eq 'Supervision'){'SupervisionClasificacionTest,DocumentoAisladoTest#registro19PreservaFacultad17ConUnidadFuncionalDistintaDeMateria,ClasificacionComunicacionesTest#vencerUltimaCoordinacionNoReclasificaLaCapturaComoCentral'}elseif($Seleccion -eq 'ClasificacionReintento'){'ClasificacionComunicacionesTest#reintentosConfirmadosNoReabrenTomaFinalizadaNiDuplicanSol'}elseif($Seleccion -eq 'Clasificacion'){'ClasificacionComunicacionesTest'}elseif($Seleccion -eq 'AutoridadMigracion'){'AutoridadMigracionTest'}elseif($Seleccion -eq 'CorreoMaterializado'){'CorreoMaterializadoTest,CorreoMigracionTest'}elseif($Seleccion -eq 'AvisosGestionSeguridad'){'AvisosGestionTest#reasignacionAvisaSalienteConReferenciaMinimaSinRestituirCasoYTareas+acuseManualActualizaPendienteSinAlterarAsignacionNiCrearGestiones,ContinuidadU2Test#revocarJefaturaOrigenCierraAceptacionDeTransferencia,SubroganciasTest#solicitudesPreviasContinuanConSubroganciaYRetornoSinFalsearAutor'}elseif($Seleccion -eq 'AvisosGestionMixto'){'AvisosGestionTest#responsableDeCasoColaboradorRecibeCierreReqSinExpedienteMixto'}elseif($Seleccion -eq 'AvisosGestion'){'AvisosGestionTest'}elseif($Seleccion -eq 'AvisosU2'){'AvisosNucleoTest'}elseif($Seleccion -eq 'Rectificaciones'){'RectificacionesIngresoTest'}elseif($Seleccion -eq 'Territorio'){'TerritorioInstitucionalTest,DocumentoAisladoTest#registro18PreservaLaFacultad17YLosRespaldosSinConcederCaso'}elseif($Seleccion -eq 'IngresoGestion'){'IngresoGestionTest'}elseif($Seleccion -eq 'FixtureJdbc'){'DocumentoAnteriorMigracionTest,BitacoraMigracionTest'}elseif($Seleccion -eq 'BitacoraEjecutor'){'BitacoraGestionTest#jefaturaRectificaSinAtribuirseLaGestionDelProfesionalNiInventarEjecutorHistorico+reqSinCasoUsaLaMismaHistoriaYNoCreaCasoNiRequerimiento+cadenaAlteradaConCicloFallaCerradaSinAtribuirUnEjecutor'}elseif($Seleccion -eq 'BitacoraRollback'){'BitacoraGestionTest#fallaAtomicaRevierteActuacionRevisionComandoYEvento'}elseif($Seleccion -eq 'Bitacora'){'BitacoraGestionTest,BitacoraMigracionTest,AutoridadMigracionTest,DocumentoAisladoTest#tiObtieneSoloElDocumentoExpresamenteConcedidoSinCasoNiHermanos'}elseif($Seleccion -eq 'DocumentoAislado'){'DocumentoAisladoTest,DocumentoAnteriorMigracionTest,AccesoReservadoTest,AutoridadMigracionTest'}elseif($Seleccion -eq 'Reservas'){'AccesoReservadoTest,AutoridadMigracionTest'}elseif($Seleccion -eq 'RevisionExcepcional'){'RevisionExcepcionalTest,AutoridadMigracionTest'}elseif($Seleccion -eq 'Subrogancias'){'SubroganciasTest'}elseif($Seleccion -eq 'Colaboraciones'){'ColaboracionContinuidadTest'}else{'EquipoAgendaTest'}
 try{& $Maven -B -ntp ('-Dtest='+$tests) ('-Dsiger.test.autoridad.mysql=jdbc:mysql://127.0.0.1:'+$port+'/'+$schema) ('-DargLine=--enable-native-access=ALL-UNNAMED -Xmx256m -XX:-UsePerfData -Djava.io.tmpdir='+$base) test *> (Join-Path $base 'tests.log');if($LASTEXITCODE){throw 'Falló la validación MySQL aislada; ver tests.log privado.'}}finally{Pop-Location}
 $testLog=Get-Content -LiteralPath (Join-Path $base 'tests.log') -Raw
 if($testLog -notmatch '\[INFO\] BUILD SUCCESS' -or $testLog -notmatch 'Tests run: [1-9][0-9]*, Failures: 0, Errors: 0, Skipped: 0'){throw 'La salida no acredita ejecución MySQL completa; consultar tests.log.'}
 Write-Output ('MySQL descartable: selección '+$Seleccion+'; totales reales a continuación.')
 Write-Output (([regex]::Matches($testLog,'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+') | Select-Object -Last 1).Value)
}finally{
 try{
 $env:SIGER_U15_TEST_MYSQL_PASSWORD=$null;$env:JAVA_HOME=$oldJava;$env:MAVEN_OPTS=$oldMaven
 if($proc){
  if(Test-OwnListener){& (Join-Path $MySqlBin 'mysqladmin.exe') ('--defaults-file='+$client) --protocol=TCP --host=127.0.0.1 ('--port='+$port) --user=root shutdown *> $null}
  $proc.WaitForExit(10000)|Out-Null
  # mysqld en Windows puede lanzar un hijo. Solo detener PIDs con el datadir único que este auxiliar creó.
  $own=@(Get-CimInstance Win32_Process -Filter "Name='mysqld.exe'"|Where-Object {$_.CommandLine -and $_.CommandLine.Contains('--datadir='+$dir) -and $_.ExecutablePath -eq (Join-Path $MySqlBin 'mysqld.exe')})
  foreach($p in $own){Stop-Process -Id $p.ProcessId;Wait-Process -Id $p.ProcessId -Timeout 10 -ErrorAction SilentlyContinue}
 }
 }finally{$env:MYSQL_TEST_LOGIN_FILE=$oldLogin}
 Write-Output ('Evidencia privada conservada en '+$base+'; no se borran bases ni se toca un servicio.')
}
