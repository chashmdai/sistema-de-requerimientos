#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[int]$ExpectedRecords=6,[int]$ExpectedNna=3)
. "$PSScriptRoot\Session-Smr.ps1"
$s=New-SmrSession $Root 'sintetico';$state=Invoke-RestMethod "$($s.url)/nucleo/smr/estado" -WebSession $s.web
if(-not $state.control.recuperar){Invoke-SmrPost $s '/nucleo/smr/control' @{recuperar=$true;motivo='PRUEBA_LOCAL'} @{'If-Match'=[string]$state.control.revision}|Out-Null}
$deadline=[DateTime]::UtcNow.AddSeconds(55)
do{$state=Invoke-RestMethod "$($s.url)/nucleo/smr/estado" -WebSession $s.web;if($state.registrosDurables -eq $ExpectedRecords -and $state.ackPendientes -eq 0){break};Start-Sleep -Seconds 1}while([DateTime]::UtcNow -lt $deadline)
if($state.registrosDurables -ne $ExpectedRecords -or $state.ackPendientes -ne 0){$state|Select-Object registrosDurables,ackPendientes,consumidor|ConvertTo-Json -Depth 5;throw 'PULL/ACK sintético incompleto'}
$list=Invoke-RestMethod "$($s.url)/nucleo/smr/comunicaciones" -WebSession $s.web
$result=@();$count=0
foreach($row in $list.items){
 $detail=Invoke-RestMethod "$($s.url)/nucleo/smr/comunicaciones/$($row.id)" -WebSession $s.web
 $expected=if($row.tipo -eq 'NNA'){'1.1'}else{'1.0'}
 if($detail.recepcion.registro.version -ne $expected -or $detail.entrega.comunicacion.ampliacionCompatible.unicode -ne 'ñ · María José · 漢字 · 🎈' -or $null -ne $detail.entrega.comunicacion.ampliacionCompatible.valorNulo){throw 'Documento/ACK no conservado íntegro'}
 if($row.tipo -ne 'ADULTO_CU' -and $detail.entrega.comunicacion.personas[0].autenticada){throw 'Autenticación inventada'}
 $docs=Invoke-RestMethod "$($s.url)/nucleo/comunicaciones/$($row.id)/documentos" -WebSession $s.web
 foreach($doc in $docs){$tmp=Join-Path $Root ('evidence\download-'+[guid]::NewGuid().ToString('N')+'.bin');Invoke-WebRequest "$($s.url)/nucleo/comunicaciones/$($row.id)/documentos/$($doc.id)/original" -WebSession $s.web -OutFile $tmp
  if((Get-FileHash -LiteralPath $tmp).Hash.ToLowerInvariant() -ne $doc.original.sha256){throw 'Original descargado difiere'};$count++
 }
 $result+=@{folio=$row.folio;type=$row.tipo;documentVersion=$detail.entrega.comunicacion.version;ackVersion=$detail.recepcion.registro.version;attachments=$docs.Count;state=$row.estado}
}
$filtered=Invoke-RestMethod "$($s.url)/nucleo/smr/comunicaciones?tipo=NNA&limite=1" -WebSession $s.web;if($filtered.total -ne $ExpectedNna -or $filtered.items.Count -ne 1){throw 'Filtro/paginación incorrectos'}
$state=Suspend-SmrConsumer $s
Invoke-SmrPost $s '/nucleo/logout' $null|Out-Null
@{atUtc=[DateTime]::UtcNow.ToString('o');scope='PULL_HTTPS_LOCAL_MYSQL_NTFS_WINDOWS';records=$result;originalsVerified=$count;pendingAck=$state.ackPendientes;consumerPaused=$true;productionE2e=$false}|ConvertTo-Json -Depth 8|Set-Content -LiteralPath "$Root\evidence\runtime-sintetico.json"
Write-Output "PULL/ACK HTTPS local: $ExpectedRecords Comunicaciones, $count originales íntegros, ACK pendientes 0; consumidor pausado."
