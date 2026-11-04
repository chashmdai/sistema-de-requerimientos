#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[ValidateSet('recepcion','sintetico')][string]$Instance='sintetico')
. "$PSScriptRoot\Common.ps1"
Add-Type -AssemblyName System.Windows.Forms
$p=Read-SmrJson "$Root\instances\$Instance\private\access.dpapi";$c=Get-Content -Raw "$Root\instances\$Instance\instance.json"|ConvertFrom-Json
$f=[Windows.Forms.Form]::new();$f.Text='Acceso local SMR';$f.Width=560;$f.Height=230;$f.StartPosition='CenterScreen';$f.TopMost=$true
$label=[Windows.Forms.Label]::new();$label.Text="https://localhost:$($c.port)/`r`nUsuario: $($p.username)";$label.SetBounds(15,15,510,50);$f.Controls.Add($label)
$input=[Windows.Forms.TextBox]::new();$input.Text=$p.password;$input.UseSystemPasswordChar=$true;$input.ReadOnly=$true;$input.SetBounds(15,75,510,30);$f.Controls.Add($input)
$check=[Windows.Forms.CheckBox]::new();$check.Text='Mostrar contraseña local';$check.SetBounds(15,115,300,30);$check.Add_CheckedChanged({$input.UseSystemPasswordChar=-not $check.Checked});$f.Controls.Add($check)
try{$f.ShowDialog()|Out-Null}finally{$input.Text='';$p.Clear();$f.Dispose()}
