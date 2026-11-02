[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
# Visor local, solo lectura. No se invoca desde Codex y no imprime/exporta la contraseña.
if(-not $IsWindows -or [Threading.Thread]::CurrentThread.GetApartmentState() -ne 'STA'){throw 'Ejecutar con PowerShell 7 Windows -STA'}
$path='C:\req\config\origen-dpapi\nexbyte-cuenta.json.dpapi'
$allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')
for($p=$path;$p;$p=[IO.Path]::GetDirectoryName($p)){if((Get-Item -LiteralPath $p -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlace no permitido'}}
foreach($r in (Get-Acl -LiteralPath $path).Access){if($r.AccessControlType -eq 'Allow' -and -not ($r.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'ACL privada no comprobada'}}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$bytes=$null;$credential=$null;$form=$null;$passwordBox=$null
try{
 $bytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($path),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
 $credential=[Text.Encoding]::UTF8.GetString($bytes)|ConvertFrom-Json
 if($credential.usuario -ne 'nexbyte' -or [string]::IsNullOrWhiteSpace($credential.password)){throw 'Cuenta QA no disponible'}
 $form=[Windows.Forms.Form]::new();$form.Text='Acceso privado SIGER QA';$form.ClientSize=[Drawing.Size]::new(530,210);$form.StartPosition='CenterScreen';$form.FormBorderStyle='FixedDialog';$form.MaximizeBox=$false;$form.MinimizeBox=$false
 $label=[Windows.Forms.Label]::new();$label.Text="Usuario QA: nexbyte`r`nCredencial existente; no se modifica ninguna cuenta.";$label.AutoSize=$true;$label.Location=[Drawing.Point]::new(18,16);$form.Controls.Add($label)
 $passwordBox=[Windows.Forms.TextBox]::new();$passwordBox.Location=[Drawing.Point]::new(18,66);$passwordBox.Size=[Drawing.Size]::new(490,28);$passwordBox.ReadOnly=$true;$passwordBox.UseSystemPasswordChar=$true;$passwordBox.Text=$credential.password;$form.Controls.Add($passwordBox)
 $show=[Windows.Forms.CheckBox]::new();$show.Text='Mostrar temporalmente para ingresar en SIGER';$show.AutoSize=$true;$show.Location=[Drawing.Point]::new(18,108);$show.Add_CheckedChanged({$passwordBox.UseSystemPasswordChar=-not $show.Checked});$form.Controls.Add($show)
 $note=[Windows.Forms.Label]::new();$note.Text='Consulta local bajo tu usuario Windows. No pegar en chat ni informes.';$note.AutoSize=$true;$note.Location=[Drawing.Point]::new(18,143);$form.Controls.Add($note)
 $close=[Windows.Forms.Button]::new();$close.Text='Cerrar';$close.Location=[Drawing.Point]::new(420,173);$close.Add_Click({$form.Close()});$form.Controls.Add($close)
 $credential=$null;$null=$form.ShowDialog()
}catch{throw 'No se pudo abrir el visor privado QA; no se modificaron cuentas ni se mostraron valores en consola.'}
finally{if($passwordBox){$passwordBox.Clear()};if($form){$form.Dispose()};if($bytes){[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes)};$credential=$null}
