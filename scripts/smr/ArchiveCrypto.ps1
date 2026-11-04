. "$PSScriptRoot\Common.ps1"
if(-not('SmrBackupCrypto' -as [type])){Add-Type 'public static class SmrBackupCrypto { public static bool Equal(byte[] a, byte[] b) { return System.Security.Cryptography.CryptographicOperations.FixedTimeEquals(a,b); } }'}
function Protect-SmrArchive([string]$InputFile,[string]$OutputFile,[byte[]]$Key){
 if($Key.Length -ne 64 -or (Test-Path -LiteralPath $OutputFile)){throw 'Clave/formato de backup inválido'}
 $aes=[Security.Cryptography.Aes]::Create();$aes.Key=$Key[0..31];$aes.GenerateIV();$input=[IO.File]::OpenRead($InputFile);$output=[IO.File]::Create($OutputFile)
 try{$header=[Text.Encoding]::ASCII.GetBytes('SMRBK001');$output.Write($header,0,$header.Length);$output.Write($aes.IV,0,16);$crypto=[Security.Cryptography.CryptoStream]::new($output,$aes.CreateEncryptor(),'Write',$true);try{$input.CopyTo($crypto);$crypto.FlushFinalBlock()}finally{$crypto.Dispose()}}finally{$input.Dispose();$output.Dispose();$aes.Dispose()}
 $hmac=[Security.Cryptography.HMACSHA256]::new([byte[]]$Key[32..63]);$stream=[IO.File]::OpenRead($OutputFile);try{$tag=$hmac.ComputeHash($stream)}finally{$stream.Dispose();$hmac.Dispose()}
 $stream=[IO.File]::Open($OutputFile,'Append','Write');try{$stream.Write($tag,0,$tag.Length)}finally{$stream.Dispose()}
}
function Unprotect-SmrArchive([string]$InputFile,[string]$OutputFile,[byte[]]$Key){
 if($Key.Length -ne 64 -or (Test-Path -LiteralPath $OutputFile)){throw 'Destino de restore debe ser nuevo'}
 $stream=[IO.File]::OpenRead($InputFile);$hmac=[Security.Cryptography.HMACSHA256]::new([byte[]]$Key[32..63]);$buffer=[byte[]]::new(65536)
 try{if($stream.Length -lt 72){throw 'Backup incompleto'};$remaining=$stream.Length-32;while($remaining -gt 0){$n=$stream.Read($buffer,0,[int][Math]::Min($buffer.Length,$remaining));if($n -le 0){throw 'Backup incompleto'};$null=$hmac.TransformBlock($buffer,0,$n,$buffer,0);$remaining-=$n};$null=$hmac.TransformFinalBlock([byte[]]::new(0),0,0);$tag=[byte[]]::new(32);$stream.ReadExactly($tag,0,32);if(-not [SmrBackupCrypto]::Equal($tag,$hmac.Hash)){throw 'Integridad HMAC del backup inválida'}
  $stream.Position=0;$header=[byte[]]::new(8);$stream.ReadExactly($header,0,8);if([Text.Encoding]::ASCII.GetString($header) -ne 'SMRBK001'){throw 'Formato no admitido'};$iv=[byte[]]::new(16);$stream.ReadExactly($iv,0,16)
  $aes=[Security.Cryptography.Aes]::Create();$aes.Key=$Key[0..31];$aes.IV=$iv;$out=[IO.File]::Create($OutputFile);$crypto=[Security.Cryptography.CryptoStream]::new($out,$aes.CreateDecryptor(),'Write',$true)
  try{$remaining=$stream.Length-24-32;while($remaining -gt 0){$n=$stream.Read($buffer,0,[int][Math]::Min($buffer.Length,$remaining));if($n -le 0){throw 'Backup incompleto'};$crypto.Write($buffer,0,$n);$remaining-=$n};$crypto.FlushFinalBlock()}finally{$crypto.Dispose();$out.Dispose();$aes.Dispose()}
 }finally{$stream.Dispose();$hmac.Dispose();[Array]::Clear($buffer)}
}
