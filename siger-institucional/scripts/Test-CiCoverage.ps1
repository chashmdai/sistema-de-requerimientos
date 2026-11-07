#Requires -Version 7.4
[CmdletBinding()]
param([Parameter(Mandatory)][ValidateSet('Plan','Validate','Aggregate')][string]$Mode,
      [Parameter(Mandatory)][string]$Directory,[int]$Part=0,[ValidateRange(1,8)][int]$Parts=2,[string]$JavaHome=$env:JAVA_HOME,
      [string]$ClassesDirectory='',[string]$ReportsDirectory='')
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$backend=Join-Path $repo 'siger-institucional/backend'
if(-not $ClassesDirectory){$ClassesDirectory=Join-Path $backend 'target/test-classes'}
if(-not $ReportsDirectory){$ReportsDirectory=Join-Path $backend 'target/surefire-reports'}
$head=(& git -C $repo rev-parse HEAD);if($LASTEXITCODE){throw 'HEAD no verificable'}
$run=if($env:GITHUB_RUN_ID){$env:GITHUB_RUN_ID}else{'LOCAL'}
. (Join-Path $PSScriptRoot 'CiTestAllocation.ps1')
$platform=if($IsWindows){'Windows'}elseif($IsLinux){'Linux'}elseif($IsMacOS){'macOS'}else{throw 'Sistema operativo de la evidencia no reconocido'}
function Save-Manifest($value,$path){$value|ConvertTo-Json -Depth 12|Set-Content -LiteralPath $path -Encoding utf8}
function Same-Set($a,$b){$left=@($a|Sort-Object -Unique);$right=@($b|Sort-Object -Unique);return $left.Count -eq $right.Count -and -not(Compare-Object $left $right)}
function Sum-Field($reports,$field){$sum=0;foreach($report in $reports){$sum+=[int]$report[$field]};return $sum}
if($Mode -eq 'Plan'){
 if($Part -lt 1 -or $Part -gt $Parts -or -not $JavaHome){throw 'Partición/Java25 requeridos'}
 $java=Join-Path $JavaHome $(if($IsWindows){'bin/java.exe'}else{'bin/java'})
 $all=@(& $java -Xmx128m -XX:-UsePerfData (Join-Path $PSScriptRoot 'CiTestInventory.java') $ClassesDirectory)
 if($LASTEXITCODE -or -not $all.Count -or @($all|Where-Object {$_ -notmatch '^[A-Za-z_$][A-Za-z0-9_$.]*$'}).Count){throw 'Inventario JUnit compilado inválido'}
 $allocation=Get-CiTestAllocation -All $all -Parts $Parts
 $selected=@($all|Where-Object {$allocation.byRoot[$_] -eq $Part})
 if(-not $selected.Count -or ($selected -join ',').Length -gt 20000){throw 'Partición vacía o demasiado grande; revisar distribución sin omitir clases'}
 [IO.Directory]::CreateDirectory([IO.Path]::GetFullPath($Directory))|Out-Null
 $plan=[ordered]@{schema='SIGER_CI_COVERAGE_3';allocation_algorithm=$allocation.algorithm;allocation_sha256=$allocation.fingerprint;estimated_ms=$allocation.estimatedMilliseconds[$Part-1];head=$head;run=$run;platform=$platform;part=$Part;parts=$Parts;status='PLAN';all=$all;selected=$selected;reports=@();tests=0;failures=0;errors=0;skipped=0}
 Save-Manifest $plan (Join-Path $Directory "coverage-$Part.json");return $plan
}
if($Mode -eq 'Validate'){
 $file=Join-Path $Directory "coverage-$Part.json";$plan=Get-Content -LiteralPath $file -Raw|ConvertFrom-Json -AsHashtable
 if($plan.head -ne $head -or $plan.run -ne $run -or $plan.platform -ne $platform -or $plan.part -ne $Part -or $plan.parts -ne $Parts){throw 'Manifiesto no corresponde al HEAD/ejecución/OS/partición'}
 if(-not $JavaHome){throw 'Java25 requerido para verificar el inventario ejecutado'}
 $java=Join-Path $JavaHome $(if($IsWindows){'bin/java.exe'}else{'bin/java'})
 $compiled=@(& $java -Xmx128m -XX:-UsePerfData (Join-Path $PSScriptRoot 'CiTestInventory.java') $ClassesDirectory)
 if($LASTEXITCODE -or -not(Same-Set $compiled $plan.all)){throw 'El inventario compilado no corresponde al plan'}
 $allocation=Get-CiTestAllocation -All $compiled -Parts $Parts
 if($plan.schema -ne 'SIGER_CI_COVERAGE_3' -or $plan.allocation_algorithm -ne $allocation.algorithm -or $plan.allocation_sha256 -ne $allocation.fingerprint -or -not(Same-Set $plan.selected @($compiled|Where-Object {$allocation.byRoot[$_] -eq $Part}))){throw 'Plan de reparto modificado o de otra versión'}
 $reports=@(Get-ChildItem -LiteralPath $ReportsDirectory -File -Filter 'TEST-*.xml'|ForEach-Object {
  $settings=[Xml.XmlReaderSettings]::new();$settings.DtdProcessing=[Xml.DtdProcessing]::Prohibit;$settings.XmlResolver=$null
  $reader=[Xml.XmlReader]::Create($_.FullName,$settings)
  try{$xml=[Xml.XmlDocument]::new();$xml.XmlResolver=$null;$xml.Load($reader);$suite=$xml.DocumentElement;if($suite.LocalName -ne 'testsuite'){throw 'XML Surefire inválido'};[ordered]@{name=$suite.GetAttribute('name');root=($suite.GetAttribute('name') -split '\$')[0];tests=[int]$suite.GetAttribute('tests');failures=[int]$suite.GetAttribute('failures');errors=[int]$suite.GetAttribute('errors');skipped=[int]$suite.GetAttribute('skipped')}}finally{$reader.Dispose()}
 })
 $plan.reports=$reports;foreach($field in @('tests','failures','errors','skipped')){$plan[$field]=Sum-Field $reports $field}
 $valid=(Same-Set $plan.selected @($reports.root)) -and $plan.tests -gt 0 -and $plan.failures -eq 0 -and $plan.errors -eq 0
 $plan.status=if($valid){'PASSED'}else{'FAILED'};Save-Manifest $plan $file
 if(-not $valid){throw 'Partición incompleta, fuera de selección o con fallos; no acredita cobertura'}
 Write-Output "Partición $Part/${Parts}: $($plan.tests) métodos, $($plan.skipped) omitidos, cero fallos/errores; clases seleccionadas cubiertas."
 return
}
$manifests=@(Get-ChildItem -LiteralPath $Directory -Recurse -File -Filter 'coverage-*.json'|ForEach-Object {Get-Content -LiteralPath $_.FullName -Raw|ConvertFrom-Json -AsHashtable})
if($manifests.Count -ne $Parts){throw 'Faltan manifiestos o hay particiones duplicadas'}
$reference=$manifests[0];$selected=@();$totals=[ordered]@{tests=0;failures=0;errors=0;skipped=0}
foreach($m in $manifests){
 if($m.schema -ne 'SIGER_CI_COVERAGE_3' -or $m.head -ne $head -or $m.run -ne $run -or $m.platform -ne $platform -or $m.parts -ne $Parts -or $m.status -ne 'PASSED' -or -not(Same-Set $m.all $reference.all) -or -not(Same-Set $m.selected @($m.reports.root)) -or $m.reports.Count -ne @($m.reports.name|Sort-Object -Unique).Count){throw 'Cobertura no corresponde a la misma ejecución completa y OS'}
 $allocation=Get-CiTestAllocation -All $m.all -Parts $Parts
 if($m.allocation_algorithm -ne $allocation.algorithm -or $m.allocation_sha256 -ne $allocation.fingerprint){throw 'Pesos o algoritmo no corresponden al reparto de este HEAD'}
 $expected=@($m.all|Where-Object {$allocation.byRoot[$_] -eq $m.part})
 if(-not(Same-Set $expected $m.selected)){throw 'Selección alterada respecto del reparto determinista'}
 $selected+=@($m.selected);foreach($field in @($totals.Keys)){$sum=Sum-Field $m.reports $field;if($sum -ne [int]$m[$field]){throw 'Totales no corresponden a los informes ejecutados'};$totals[$field]+=$sum}
}
if(-not(Same-Set @($manifests.part) @(1..$Parts)) -or $selected.Count -ne @($selected|Sort-Object -Unique).Count -or -not(Same-Set $selected $reference.all) -or $totals.failures -or $totals.errors){throw 'Clases omitidas/duplicadas o fallos: cobertura incompleta'}
$summary="HEAD $head · $($reference.all.Count) clases JUnit, $($totals.tests) métodos, $($totals.skipped) omitidos; cero fallos/errores. Todas las particiones disjuntas y completas."
Write-Output $summary;if($env:GITHUB_STEP_SUMMARY){Add-Content -LiteralPath $env:GITHUB_STEP_SUMMARY -Value $summary}
