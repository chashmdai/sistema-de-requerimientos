#Requires -Version 7.4
# Los pesos ordenan trabajo; nunca definen el inventario o la obligatoriedad de una clase.
function New-CiTestAllocation {
 param([Parameter(Mandatory)][string[]]$All,[Parameter(Mandatory)][ValidateRange(1,8)][int]$Parts,
       [Parameter(Mandatory)][System.Collections.IDictionary]$Hints,[Parameter(Mandatory)][string]$SourceHash)
 $unique=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
 foreach($name in $All){if($name -notmatch '^[A-Za-z_$][A-Za-z0-9_$.]*$' -or -not $unique.Add($name)){throw 'Inventario duplicado o inválido'}}
 if($All.Count -lt $Parts -or -not $Hints.Count){throw 'Inventario o pesos insuficientes'}
 $maximum=0L
 foreach($entry in $Hints.GetEnumerator()){
  if($entry.Key -notmatch '^[A-Za-z_$][A-Za-z0-9_$.]*$' -or $entry.Value -isnot [ValueType] -or [double]$entry.Value -le 0 -or [double]$entry.Value -ne [Math]::Floor([double]$entry.Value) -or [double]$entry.Value -gt 2147483647){throw 'Peso de duración inválido'}
  $maximum=[Math]::Max($maximum,[long]$entry.Value)
 }
 # Una clase nueva recibe un peso conservador, sin omitirse por no tener medición previa.
 $weights=[Collections.Generic.Dictionary[string,long]]::new([StringComparer]::Ordinal)
 foreach($name in $All){$weights[$name]=if($Hints.Contains($name)){[long]$Hints[$name]}else{$maximum}}
 $ordered=[string[]]$All.Clone()
 $comparer=[Collections.Generic.Comparer[string]]::Create([Comparison[string]]{param($a,$b)
  $comparison=$weights[$b].CompareTo($weights[$a]);if($comparison -ne 0){return $comparison};return [StringComparer]::Ordinal.Compare($a,$b)
 })
 [Array]::Sort($ordered,$comparer)
 $totals=[long[]]::new($Parts);$assigned=[Collections.Generic.Dictionary[string,int]]::new([StringComparer]::Ordinal)
 foreach($name in $ordered){$target=0;for($i=1;$i -lt $Parts;$i++){if($totals[$i] -lt $totals[$target]){$target=$i}};$assigned[$name]=$target+1;$totals[$target]+=$weights[$name]}
 $canonical=[string[]]$All.Clone();[Array]::Sort($canonical,[StringComparer]::Ordinal)
 $fingerprint=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes("SIGER_LPT_DURACION_1`n$SourceHash`n$Parts`n"+($canonical -join "`n")))).ToLowerInvariant()
 return @{algorithm='LPT_DURACION_1';fingerprint=$fingerprint;byRoot=$assigned;estimatedMilliseconds=$totals}
}
function Get-CiTestAllocation {
 param([Parameter(Mandatory)][string[]]$All,[Parameter(Mandatory)][int]$Parts)
 $file=Join-Path $PSScriptRoot 'ci-test-duration-hints.json'
 $data=Get-Content -LiteralPath $file -Raw|ConvertFrom-Json -AsHashtable
 if($data.schema -ne 'SIGER_CI_DURATION_HINTS_1'){throw 'Versión de pesos desconocida'}
 return New-CiTestAllocation -All $All -Parts $Parts -Hints $data.milliseconds -SourceHash (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash
}
