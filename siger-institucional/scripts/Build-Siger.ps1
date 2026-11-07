#Requires -Version 7.4
[CmdletBinding()]
param([string]$JavaHome=$env:JAVA_HOME,[string]$Maven='',[string]$TestDirectory='',
      [ValidateRange(0,8)][int]$TestPart=0,[ValidateRange(1,8)][int]$TestParts=2,[string]$CoverageDirectory='',
      [switch]$IntegrarParticiones)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if($IntegrarParticiones -and ($TestPart -or -not $CoverageDirectory)){throw 'La integración requiere evidencia de todas las particiones; no admite selección parcial.'}
if($IntegrarParticiones){& (Join-Path $PSScriptRoot 'Test-CiCoverage.ps1') -Mode Aggregate -Directory $CoverageDirectory -Parts $TestParts}
if(Get-ChildItem Env:|Where-Object {$_.Name -like 'ROBUST_DB_*' -or $_.Name -like 'DOCUMENTAL_REAL_*'}){throw 'Build aislado H2: retirar del proceso las variables de MySQL/workers reales; no utilizar la QA integrada.'}
if(-not $JavaHome){throw 'Seleccionar Java25 con -JavaHome; no se modifica la configuración global.'}
$java=Join-Path $JavaHome $(if($IsWindows){'bin/java.exe'}else{'bin/java'})
if(-not(Test-Path -LiteralPath $java)){throw 'Java seleccionado no encontrado'}
$version=(& $java --version 2>&1)-join ' ';if($LASTEXITCODE -or $version -notmatch '(?:openjdk|java) 25(?:[.\s-])'){throw 'Este repositorio requiere Java25 explícito.'}
if(-not $Maven){$Maven=(Get-Command $(if($IsWindows){'mvn.cmd'}else{'mvn'}) -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source}
if(-not $TestPart){
 $npm=(Get-Command $(if($IsWindows){'npm.cmd'}else{'npm'}) -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
 $nodeVersion=(& node --version);if($LASTEXITCODE -or $nodeVersion -notmatch '^v24\.'){throw 'Utilizar Node24 con su npm y package-lock versionado.'}
}
if(-not $TestDirectory){$base=if($IsWindows){$env:LOCALAPPDATA}else{[IO.Path]::GetTempPath()};$TestDirectory=Join-Path $base ('siger-build-'+[guid]::NewGuid().ToString('N'))}
$TestDirectory=[IO.Path]::GetFullPath($TestDirectory)
if($TestDirectory.StartsWith($repo+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -or (Test-Path -LiteralPath $TestDirectory)){throw 'Temporales nuevos, privados y fuera de Git requeridos.'}
[IO.Directory]::CreateDirectory($TestDirectory)|Out-Null
if($IsWindows){
 $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User;$acl=[Security.AccessControl.DirectorySecurity]::new();$acl.SetOwner($sid);$acl.SetAccessRuleProtection($true,$false)
 foreach($id in @($sid.Value,'S-1-5-18','S-1-5-32-544')){$rule=[Security.AccessControl.FileSystemAccessRule]::new([Security.Principal.SecurityIdentifier]::new($id),'FullControl','ContainerInherit,ObjectInherit','None','Allow');$acl.AddAccessRule($rule)}
 Set-Acl -LiteralPath $TestDirectory -AclObject $acl
}else{[IO.File]::SetUnixFileMode($TestDirectory,([IO.UnixFileMode]::UserRead -bor [IO.UnixFileMode]::UserWrite -bor [IO.UnixFileMode]::UserExecute))}
$oldJava=$env:JAVA_HOME;$oldMaven=$env:MAVEN_OPTS
try{
 $env:JAVA_HOME=$JavaHome;$env:MAVEN_OPTS='-Xmx256m -XX:-UsePerfData'
 if(-not $TestPart){
  Push-Location (Join-Path $PSScriptRoot '../front')
  try{& $npm ci;if($LASTEXITCODE){throw 'npm ci no completado'};foreach($step in @('lint','test','build')){& $npm run $step;if($LASTEXITCODE){throw "Frontend no correcto: $step"}}}finally{Pop-Location}
 }
 Push-Location (Join-Path $PSScriptRoot '../backend')
 try{
  $argLine='--enable-native-access=ALL-UNNAMED -Xmx256m -XX:-UsePerfData -Djava.io.tmpdir="'+$TestDirectory+'"'
  if($TestPart){
   if($TestPart -gt $TestParts){throw 'Partición inválida'}
   & $Maven -B -ntp ('-DargLine='+$argLine) ('-Djava.io.tmpdir='+$TestDirectory) clean test-compile
   if($LASTEXITCODE){throw 'Compilación previa al inventario no completada'}
   if(-not $CoverageDirectory){$CoverageDirectory=Join-Path $TestDirectory 'coverage'}
   $coverageScript=Join-Path $PSScriptRoot 'Test-CiCoverage.ps1'
   $plan=& $coverageScript -Mode Plan -Directory $CoverageDirectory -Part $TestPart -Parts $TestParts -JavaHome $JavaHome
   $selectors=@($plan.selected|ForEach-Object {$_.Replace('.','/')+'.java'}) -join ','
   Write-Output "Reparto $($plan.allocation_algorithm), huella $($plan.allocation_sha256); estimación heurística $($plan.estimated_ms) ms, sin omitir clases ni cambiar límites."
   Write-Output "Ejecutando partición $TestPart/${TestParts}: $($plan.selected.Count) de $($plan.all.Count) clases JUnit."
   & $Maven -B -ntp ('-DargLine='+$argLine) ('-Djava.io.tmpdir='+$TestDirectory) ('-Dtest='+$selectors) verify
   $buildExit=$LASTEXITCODE
   & $coverageScript -Mode Validate -Directory $CoverageDirectory -Part $TestPart -Parts $TestParts
   if($buildExit){throw 'Pruebas de partición no completadas; consultar Surefire'}
  }elseif($IntegrarParticiones){
   # Las pruebas ya constan completas/disjuntas para ESTE HEAD/run/OS. Integrar los
   # recursos web realmente verificados sin volver a ejecutar Surefire ni excluir tests.
   & $Maven -B -ntp ('-Djava.io.tmpdir='+$TestDirectory) clean compile jar:jar spring-boot:repackage
  }else{& $Maven -B -ntp ('-DargLine='+$argLine) ('-Djava.io.tmpdir='+$TestDirectory) clean verify}
  if($LASTEXITCODE){throw 'Build H2 no correcto; consultar Surefire. No reintentar sobre MySQL integrado.'}
 }finally{Pop-Location}
 if(-not $TestPart){& (Join-Path $PSScriptRoot 'Test-IntegracionWeb.ps1')}
 Write-Output ('Compilación terminada. Temporales de prueba conservados: '+$TestDirectory)
 Write-Output 'Las tres pruebas históricas de estrés están etiquetadas stress y excluidas por defecto. Los workers reales se califican separadamente.'
}finally{$env:JAVA_HOME=$oldJava;$env:MAVEN_OPTS=$oldMaven}
