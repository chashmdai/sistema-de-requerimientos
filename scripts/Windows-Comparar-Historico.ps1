param()
$ErrorActionPreference='Stop'
& (Join-Path $PSScriptRoot 'Windows-BaseDatos.ps1') -Mode fingerprint -Identity app | Set-Content C:\req\evidencias\historico-u45-final.json
$a=Get-Content C:\req\evidencias\sql-destino-importado.json -Raw|ConvertFrom-Json -AsHashtable
$b=Get-Content C:\req\evidencias\historico-u45-final.json -Raw|ConvertFrom-Json -AsHashtable
$changes=@();[long]$originalRows=0;[long]$currentRows=0
foreach($name in $a['tables'].Keys){$x=$a['tables'][$name];$y=$b['tables'][$name];$originalRows+=$x['count'];$currentRows+=$y['count'];if(-not $y -or $y['count'] -ne $x['count'] -or $y['sha256'] -cne $x['sha256']){$changes+=$name}}
$identical=$changes.Count -eq 0 -and $a['tables'].Count -eq $b['tables'].Count
@{tables=$a['tables'].Count;baselineRows=$originalRows;currentRows=$currentRows;changedTables=$changes;identical=$identical}|ConvertTo-Json|Tee-Object C:\req\evidencias\comparacion-historico-u45-final.json
if(-not $identical){throw 'Historico no coincide; conservar evidencia, no reparar automáticamente'}
& (Join-Path $PSScriptRoot 'Windows-Verificar-Custodia.ps1') | Tee-Object C:\req\evidencias\custodia-u45-final.json
