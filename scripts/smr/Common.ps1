#Requires -Version 7.4
$ErrorActionPreference='Stop'
function Set-SmrPrivate([string]$Path){
 $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User
 $acl=if((Get-Item -LiteralPath $Path).PSIsContainer){[Security.AccessControl.DirectorySecurity]::new()}else{[Security.AccessControl.FileSecurity]::new()}
 $acl.SetOwner($sid);$acl.SetAccessRuleProtection($true,$false)
 foreach($id in @($sid.Value,'S-1-5-18','S-1-5-32-544')){
  $inherit=if((Get-Item -LiteralPath $Path).PSIsContainer){'ContainerInherit,ObjectInherit'}else{'None'}
  $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new([Security.Principal.SecurityIdentifier]::new($id),'FullControl',$inherit,'None','Allow'))
 };Set-Acl -LiteralPath $Path -AclObject $acl
}
function Assert-SmrPrivate([string]$Path){
 $allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')
 for($p=[IO.Path]::GetFullPath($Path);$p;$p=[IO.Path]::GetDirectoryName($p)){if(Test-Path -LiteralPath $p){if((Get-Item -Force -LiteralPath $p).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlace no admitido'}}}
 foreach($r in (Get-Acl -LiteralPath $Path).Access){if($r.AccessControlType -eq 'Allow' -and -not($r.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'ACL privada requerida'}}
}
function Protect-SmrJson($Value,[string]$Path){
 if(Test-Path -LiteralPath $Path){throw 'No sobrescribir secretos existentes'}
 $bytes=[Text.Encoding]::UTF8.GetBytes(($Value|ConvertTo-Json -Depth 20 -Compress))
 try{[IO.File]::WriteAllBytes($Path,[Security.Cryptography.ProtectedData]::Protect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser));Set-SmrPrivate $Path}finally{[Array]::Clear($bytes)}
}
function Read-SmrJson([string]$Path){Assert-SmrPrivate $Path;$bytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($Path),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser);try{return [Text.Encoding]::UTF8.GetString($bytes)|ConvertFrom-Json -AsHashtable}finally{[Array]::Clear($bytes)}}
function New-SmrSecret {return [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLowerInvariant()}
function Invoke-SmrJava([string]$Java,[string[]]$Arguments,[string]$InputJson,[string]$OutputLog='',[string]$ErrorLog=''){
 $si=[Diagnostics.ProcessStartInfo]::new($Java);$si.UseShellExecute=$false;$si.CreateNoWindow=$true;$si.RedirectStandardInput=$true;$si.RedirectStandardOutput=$true;$si.RedirectStandardError=$true
 foreach($a in $Arguments){$si.ArgumentList.Add($a)};$p=[Diagnostics.Process]::Start($si)
 $out=$p.StandardOutput.ReadToEndAsync();$err=$p.StandardError.ReadToEndAsync();$p.StandardInput.Write($InputJson);$p.StandardInput.Close();$InputJson=$null
 $p.WaitForExit();$o=$out.GetAwaiter().GetResult();$e=$err.GetAwaiter().GetResult()
 if($OutputLog){[IO.File]::WriteAllText($OutputLog,$o)};if($ErrorLog){[IO.File]::WriteAllText($ErrorLog,$e)}
 if($p.ExitCode){throw "Java SMR falló ($($p.ExitCode)); consultar el log privado"};return $o
}
