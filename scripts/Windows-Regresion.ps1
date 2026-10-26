param([ValidateSet('H2','Frontend','Package')][string]$Mode='H2')
$ErrorActionPreference='Stop'
if (-not $IsWindows) {throw 'Solo Windows nativo'}
$repo=Split-Path $PSScriptRoot
$backend=Join-Path $repo 'siger-institucional\backend'
$front=Join-Path $repo 'siger-institucional\front'
$excluded=@('vectorInteroperableYDuplicadosConcurrentesTienenUnaConfirmacionEstable','doscientosFoliosDistintosBajoConcurrenciaYFormatoHistoricoEstable','consumidorParaleloAcotadoConservaUnicidadYConfirmaCadaRegistro')
$source=Get-Content (Join-Path $backend 'src\test\java\cl\nexbyte\siger\institucional\robustez\RecepcionInstitucionalJdbcTest.java') -Raw
foreach($name in $excluded) {if ($source -notmatch ('void\s+'+[regex]::Escape($name)+'\s*\(')) {throw 'Filtro de carga no comprobado'}}
# Never pass Windows integrated data to tests: their fixtures deliberately delete data.
if (Get-ChildItem Env: | Where-Object { $_.Name -like 'ROBUST_DB_*' -or $_.Name -like 'DOCUMENTAL_REAL_*' }) {throw 'Retirar configuracion de motor/worker ajeno del proceso QA'}
[IO.Directory]::CreateDirectory('C:\req\scratch\tests') | Out-Null
$oldJava=$env:JAVA_HOME;$oldTmp=$env:TMP;$oldTemp=$env:TEMP;$oldOptions=$env:JAVA_TOOL_OPTIONS
try {
 $env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
 $env:TMP='C:\req\scratch\tests';$env:TEMP=$env:TMP
 if ($Mode -eq 'Frontend') {
  Push-Location $front
  try {foreach($step in @('test','lint','build')) {& npm.cmd run $step *> "C:\req\logs\frontend-$step-windows.log";if ($LASTEXITCODE -ne 0) {throw 'Verificacion frontend no completada'}}} finally {Pop-Location}
 } else {
  Push-Location $backend
  try {
   if ($Mode -eq 'H2') {& C:\maven\bin\mvn.cmd -B -ntp '-Djava.io.tmpdir=C:\req\scratch\tests' ('-Dtest=*,!RecepcionInstitucionalJdbcTest#'+($excluded -join '+')) test *> C:\req\logs\backend-h2-windows.log}
   else {& C:\maven\bin\mvn.cmd -B -ntp -DskipTests package *> C:\req\logs\package-windows-final.log}
   $code=$LASTEXITCODE
  } finally {Pop-Location}
  if ($code -ne 0) {throw 'Bateria no correcta: consultar evidencia local, sin confundir con Linux'}
 }
} finally {$env:JAVA_HOME=$oldJava;$env:TMP=$oldTmp;$env:TEMP=$oldTemp;$env:JAVA_TOOL_OPTIONS=$oldOptions}