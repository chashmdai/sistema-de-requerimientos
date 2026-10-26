param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
$available=[uint64](Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory*1024
if($available -lt 4GB){
 @{scope='QA_AV_CAPACIDAD';state='BLOQUEADO_RECURSOS';availablePhysicalBytes=$available;requiredFreeBytes=4GB;plannedJobLimitBytes=1536MB;scannerExecuted=$false;notACompletedTest=$true}|ConvertTo-Json|Tee-Object C:\req\evidencias\capacidad-av-windows-2026-10-26.json
 return
}
$root='C:\req\testdata\isolation-'+[guid]::NewGuid().ToString('N');New-Item -ItemType Directory -Path $root|Out-Null
foreach($n in @('Canario','Aislamiento')){& C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /nologo /platform:x64 /target:exe ('/out:'+(Join-Path $root ($n+'.exe'))) (Join-Path $PSScriptRoot ('windows-isolation\'+$n+'.cs'));if($LASTEXITCODE){throw 'Compilacion de sonda fallida'}}
$results=@()
foreach($mode in @('av-capacity-clean','av-capacity-eicar','av-unavailable')){
 $text=((& (Join-Path $root 'Aislamiento.exe') $root $mode none 90000)-join '').Trim()
 $expected=switch($mode){'av-capacity-clean'{0}'av-capacity-eicar'{1}'av-unavailable'{2}}
 if($LASTEXITCODE -ne 0 -or -not $text.Contains(";exit=$expected;timeout=False;") -or -not $text.EndsWith(';profileDeleted=true')){throw ('Scanner aislado no comprobado: '+$mode)}
 $results+=@{mode=$mode;result=$text}
}
@{scope='QA_AV_CAPACIDAD';state='SCANNER_PROBADO_NO_INTEGRADO';root=$root;results=$results;fullDocumentalRuntime=$false}|ConvertTo-Json -Depth 5|Tee-Object C:\req\evidencias\capacidad-av-windows-2026-10-26.json
