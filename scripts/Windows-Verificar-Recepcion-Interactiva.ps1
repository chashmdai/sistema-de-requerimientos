[CmdletBinding()]
param([Parameter(Mandatory)][string]$Run,[Parameter(Mandatory)][string]$Esperado,[Parameter(Mandatory)][string]$Evidencia)
$ErrorActionPreference='Stop'
function Private-File([string]$Path){
 $p=[IO.Path]::GetFullPath($Path);if(-not $p.StartsWith('C:\req\',[StringComparison]::OrdinalIgnoreCase)){throw 'Fuera de QA'}
 for($c=$p;$c;$c=[IO.Path]::GetDirectoryName($c)){if(Test-Path -LiteralPath $c){if((Get-Item -LiteralPath $c -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlace no admitido'}}}
 $allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')
 foreach($r in (Get-Acl -LiteralPath $p).Access){if($r.AccessControlType -eq 'Allow' -and -not ($r.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'Archivo no privado'}}
}
if(-not $IsWindows -or $Run -notmatch '^C:\\req\\testdata\\runtime-qa-[a-f0-9]{32}$' -or -not ([IO.Path]::GetFullPath($Evidencia)).StartsWith('C:\req\evidencias\') -or -not ([IO.Path]::GetFullPath($Esperado)).StartsWith('C:\req\evidencias\')){throw 'Solo verificacion QA Windows'}
foreach($p in @($Run,$Esperado,$Evidencia)){Private-File $p}
$state=Get-Content -LiteralPath (Join-Path $Run 'run.json') -Raw|ConvertFrom-Json
if($state.database -ne 'siger_institucional_windows_qa' -or $state.realGraph -or $state.archivo -notmatch '^C:\\req\\testdata\\runtime-qa-[a-f0-9]{32}\\archivo$'){throw 'Runtime QA distinto'}
$jar='C:\req\bin\siger-'+$state.artifactSHA256.Substring(0,16)+'.jar';Private-File $jar
if((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $state.artifactSHA256){throw 'Artefacto distinto'}
$classes='C:\req\scratch\verificar-interactivo-'+[guid]::NewGuid().ToString('N');New-Item -ItemType Directory -Path $classes|Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::OpenRead($jar)
try{foreach($entry in $zip.Entries|Where-Object {$_.FullName -match '^BOOT-INF/classes/[A-Za-z0-9_/$.-]+\.class$'}){$target=Join-Path $classes $entry.FullName.Substring(17);if(-not ([IO.Path]::GetFullPath($target)).StartsWith($classes+'\') -or $entry.FullName.Contains('..')){throw 'Entrada JAR invalida'};[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target))|Out-Null;[IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$false)}}finally{$zip.Dispose()}
$jdk='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin';$cp=$classes+';C:\req\bin\lib\*'
& (Join-Path $jdk 'javac.exe') -cp $cp -d $classes (Join-Path $PSScriptRoot 'qa\VerificarRecepcionInteractiva.java') *> (Join-Path $Evidencia 'verificador-interactivo-compilacion.log')
if($LASTEXITCODE){throw 'No compilo verificador de lectura'}
& (Join-Path $jdk 'javap.exe') -cp $cp -c cl.nexbyte.siger.institucional.formulario.RecuperarFormulario cl.nexbyte.siger.institucional.robustez.BaseJdbc *> (Join-Path $Evidencia 'secuencia-transaccional-artefacto.txt')
if($LASTEXITCODE){throw 'No se pudo inspeccionar secuencia del artefacto'}
$expected=Get-Content -LiteralPath $Esperado -Raw|ConvertFrom-Json -AsHashtable
$ownPath=Join-Path $Run 'secrets\institucional-trust.json';$publicPath=Join-Path $Run 'secrets\publico-trust.json';$credPath='C:\req\config\mysql-windows-qa-app.credential.xml'
foreach($p in @($ownPath,$publicPath,$credPath)){Private-File $p}
$process=$null;$payload=$null;$credential=$null
try{
 $credential=Import-Clixml -LiteralPath $credPath;if($credential.UserName -ne 'siger_windows_qa_app'){throw 'Solo cuenta DML QA'}
 $inputData=@{user=$credential.UserName;password=$credential.GetNetworkCredential().Password;storage=$state.archivo;operation=$expected.operation;contentSHA256=$expected.contentSHA256;provenanceSHA256=$expected.provenanceSHA256;documentVersion=$expected.documentVersion;attachments=$expected.attachments;ownTrust=(Get-Content -LiteralPath $ownPath -Raw|ConvertFrom-Json);publicTrust=(Get-Content -LiteralPath $publicPath -Raw|ConvertFrom-Json)}
 $start=[Diagnostics.ProcessStartInfo]::new((Join-Path $jdk 'java.exe'));$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 foreach($arg in @('--enable-native-access=ALL-UNNAMED','-XX:-UsePerfData','-Xmx128m','-cp',$cp,'VerificarRecepcionInteractiva')){$start.ArgumentList.Add($arg)}
 $process=[Diagnostics.Process]::Start($start);$out=$process.StandardOutput.ReadToEndAsync();$err=$process.StandardError.ReadToEndAsync();$payload=$inputData|ConvertTo-Json -Depth 8 -Compress;$process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$inputData.Clear();$credential=$null
 if(-not $process.WaitForExit(45000)){$process.Kill($true);throw 'Verificacion de lectura vencida'};$null=$err.GetAwaiter().GetResult();$safe=@($out.GetAwaiter().GetResult() -split "`r?`n"|Where-Object {$_.StartsWith('{')})|Select-Object -Last 1
 if($process.ExitCode -ne 0 -or -not $safe){throw 'Recepcion no acreditada; valores privados omitidos'}
 $result=$safe|ConvertFrom-Json;if(-not $result.ok){throw 'Recepcion no acreditada'};$result|ConvertTo-Json -Depth 8|Set-Content -LiteralPath (Join-Path $Evidencia 'verificacion-institucional-interactiva.json');$safe
}finally{if($process){if(-not $process.HasExited){$process.Kill($true)};$process.Dispose()};$payload=$null;$credential=$null;if($inputData){$inputData.Clear()}}
