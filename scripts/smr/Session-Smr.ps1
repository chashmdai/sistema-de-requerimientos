. "$PSScriptRoot\Common.ps1"
function New-SmrSession([string]$Root,[string]$Instance){
 $base=Join-Path $Root "instances\$Instance";$config=Get-Content -Raw "$base\instance.json"|ConvertFrom-Json;$credential=Read-SmrJson "$base\private\access.dpapi"
 $url="https://localhost:$($config.port)";$web=[Microsoft.PowerShell.Commands.WebRequestSession]::new();$csrf=Invoke-RestMethod "$url/nucleo/csrf" -WebSession $web
 try{Invoke-WebRequest "$url/nucleo/login" -Method Post -WebSession $web -Headers @{$csrf.headerName=$csrf.token} -Body @{username=$credential.username;password=$credential.password}|Out-Null}finally{$credential.Clear()}
 return @{url=$url;web=$web}
}
function Invoke-SmrPost($Session,[string]$Path,$Body,$Headers=@{}){
 $csrf=Invoke-RestMethod "$($Session.url)/nucleo/csrf" -WebSession $Session.web;$Headers[$csrf.headerName]=$csrf.token
 $params=@{Uri="$($Session.url)$Path";Method='Post';WebSession=$Session.web;Headers=$Headers;ContentType='application/json';SkipHeaderValidation=$true};if($null -ne $Body){$params.Body=$Body|ConvertTo-Json -Depth 8 -Compress}
 try{return Invoke-RestMethod @params}finally{$Session.web.Headers.Remove('If-Match');$Session.web.Headers.Remove($csrf.headerName)}
}
function Suspend-SmrConsumer($Session){
 $state=Invoke-RestMethod "$($Session.url)/nucleo/smr/estado" -WebSession $Session.web
 if($state.control.recuperar){Invoke-SmrPost $Session '/nucleo/smr/control' @{recuperar=$false;motivo='MANTENIMIENTO'} @{'If-Match'=[string]$state.control.revision}|Out-Null}
 $deadline=[DateTime]::UtcNow.AddMinutes(5)
 do{$state=Invoke-RestMethod "$($Session.url)/nucleo/smr/estado" -WebSession $Session.web;if(-not $state.control.recuperar -and -not $state.enCurso){return $state};Start-Sleep -Milliseconds 500}while([DateTime]::UtcNow -lt $deadline)
 throw 'No se acreditó drenaje; conservar proceso'
}
