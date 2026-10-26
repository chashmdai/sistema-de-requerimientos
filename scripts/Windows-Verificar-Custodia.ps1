param()
$ErrorActionPreference='Stop'
$root='C:\req'
$manifest=Get-Content "$root\evidencias\manifest-documentos.json" -Raw | ConvertFrom-Json
$total=0
foreach($entry in $manifest.allObjects) {
 $base=[IO.Path]::GetFullPath("$root\storage\archivo\objects")+[IO.Path]::DirectorySeparatorChar
 $file=[IO.Path]::GetFullPath((Join-Path $base $entry.path))
 if (-not $file.StartsWith($base,[StringComparison]::OrdinalIgnoreCase)) {throw 'Ruta fuera de custodia'}
 $item=Get-Item -LiteralPath $file
 if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0 -or $item.Length -ne $entry.size) {throw 'Objeto invalido'}
 for($parent=$item.Directory;$parent -and $parent.FullName.StartsWith($root,[StringComparison]::OrdinalIgnoreCase);$parent=$parent.Parent) {if (($parent.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {throw 'Reparse point no permitido'}}
 if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256) {throw 'Integridad no coincide'}
 $total+=$item.Length
}
$identity=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
$allowed=@($identity,'S-1-5-18','S-1-5-32-544','S-1-3-0')
$paths=@(Get-Item $root)+(Get-ChildItem "$root\config","$root\storage","$root\scratch" -Recurse -Force)
foreach($item in $paths) {
 if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {throw 'Enlace no permitido'}
 $acl=Get-Acl -LiteralPath $item.FullName
 foreach($rule in $acl.Access) {if ($rule.AccessControlType -eq 'Allow' -and $rule.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed) {throw 'ACL no restringida'}}
}
$rootAcl=Get-Acl $root
if (-not $rootAcl.AreAccessRulesProtected) {throw 'Raiz hereda permisos externos'}
@{objects=$manifest.allObjects.Count;referenced=$manifest.referenced.Count;bytes=$total;sha256Verified=$true;aclCheckedObjects=$paths.Count;rootInheritanceDisabled=$true;unexpectedAllowAces=0}|ConvertTo-Json