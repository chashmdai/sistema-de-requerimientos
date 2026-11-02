#Requires -Version 7.4
[CmdletBinding()]
param([Parameter(Mandatory)][string]$CredencialAdministrativa,[ValidateSet('INSPECCIONAR','PROVISIONAR')][string]$Accion='INSPECCIONAR')
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows'}
# Usar únicamente un PSCredential administrativo autorizado para este aprovisionamiento.
# No modifica el archivo administrativo ni lo entrega al runtime.
$rootPath=[IO.Path]::GetFullPath($CredencialAdministrativa)
$allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')
function Assert-Private([string]$Path){
 for($p=[IO.Path]::GetFullPath($Path);$p;$p=[IO.Path]::GetDirectoryName($p)){if(Test-Path -LiteralPath $p){if((Get-Item -LiteralPath $p -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlace no permitido'}}}
 foreach($r in (Get-Acl -LiteralPath $Path).Access){if($r.AccessControlType -eq 'Allow' -and -not ($r.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'ACL no privada'}}
}
Assert-Private $rootPath
$rootCredential=Import-Clixml -LiteralPath $rootPath
if($rootCredential -isnot [pscredential] -or $rootCredential.UserName -cne 'root'){throw 'Identidad administrativa incompatible'}
$payload=@{accion=$Accion;username='root';password=$rootCredential.GetNetworkCredential().Password}
$process=$null
try{
 if($Accion -eq 'PROVISIONAR'){
  Assert-Private C:\req\config
  foreach($identity in @('app','migrator')){
   $path="C:\req\config\mysql-u4-ubuntu-$identity.credential.xml"
   if(Test-Path -LiteralPath $path){throw 'Credencial U4 ya existe; no sobrescribir ni rotar'}
  }
  foreach($identity in @('app','migrator')){
   $password=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLowerInvariant()
   $credential=[pscredential]::new("siger_u4_ubuntu_$identity",(ConvertTo-SecureString $password -AsPlainText -Force))
   $path="C:\req\config\mysql-u4-ubuntu-$identity.credential.xml"
   # Persistir DPAPI antes del DDL para conservar recuperación frente a fallo parcial.
   $credential|Export-Clixml -LiteralPath $path;Assert-Private $path
   $payload[$identity+'Password']=$password;$password=$null;$credential=$null
  }
 }
 $start=[Diagnostics.ProcessStartInfo]::new('C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe')
 $start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 foreach($a in @('-XX:-UsePerfData','-Xmx128m','--class-path','C:\req\bin\lib\*',(Join-Path $PSScriptRoot 'qa\ProvisionarMySqlUbuntuU4.java'))){$start.ArgumentList.Add($a)}
 $process=[Diagnostics.Process]::Start($start);$out=$process.StandardOutput.ReadToEndAsync();$err=$process.StandardError.ReadToEndAsync()
 $text=$payload|ConvertTo-Json -Compress;$process.StandardInput.Write($text);$process.StandardInput.Close();$text=$null;$payload.Clear();$rootCredential=$null
 if(-not $process.WaitForExit(45000)){$process.Kill($true);throw 'Administracion U4 vencida; conservar estado parcial'}
 $result=$out.GetAwaiter().GetResult()|ConvertFrom-Json;$null=$err.GetAwaiter().GetResult()
 if($process.ExitCode -ne 0 -or -not $result.ok){$result|ConvertTo-Json;throw 'Aprovisionamiento no acreditado; sin limpieza automatica de estado parcial'}
 $result|ConvertTo-Json
}finally{
 if($process){if(-not $process.HasExited){$process.Kill($true)};$process.Dispose()}
 $payload.Clear();$rootCredential=$null;$text=$null;$password=$null;$credential=$null
}
