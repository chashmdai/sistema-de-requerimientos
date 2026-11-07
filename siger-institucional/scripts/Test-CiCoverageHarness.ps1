#Requires -Version 7.4
[CmdletBinding()]
param([Parameter(Mandatory)][string]$JavaHome)
$ErrorActionPreference='Stop'
$directory=Join-Path ([IO.Path]::GetTempPath()) ('siger-ci-inventory-'+[guid]::NewGuid().ToString('N'))
$sources=Join-Path $directory 'src';$classes=Join-Path $directory 'classes';$reports=Join-Path $directory 'reports';$coverage=Join-Path $directory 'coverage'
foreach($path in @($sources,$classes,$reports,$coverage)){[IO.Directory]::CreateDirectory($path)|Out-Null}
$fixtures=@{
 'org/junit/jupiter/api/Test.java'='package org.junit.jupiter.api; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.METHOD,ElementType.ANNOTATION_TYPE}) public @interface Test {}'
 'org/junit/jupiter/params/ParameterizedTest.java'='package org.junit.jupiter.params; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface ParameterizedTest {}'
 'fixtures/DirectTest.java'='package fixtures; public class DirectTest { @org.junit.jupiter.api.Test void direct(){} }'
 'fixtures/Base.java'='package fixtures; abstract class Base { @org.junit.jupiter.api.Test void inherited(){} }'
 'fixtures/InheritedTest.java'='package fixtures; public class InheritedTest extends Base {}'
 'fixtures/Contract.java'='package fixtures; interface Contract { @org.junit.jupiter.api.Test default void contract(){} }'
 'fixtures/ImplementedTest.java'='package fixtures; public class ImplementedTest implements Contract {}'
 'fixtures/NestedTest.java'='package fixtures; public class NestedTest { class Inside { @org.junit.jupiter.api.Test void nested(){} } }'
 'fixtures/MetaTest.java'='package fixtures; import java.lang.annotation.*; @org.junit.jupiter.api.Test @Retention(RetentionPolicy.RUNTIME) @interface Spec {} public class MetaTest { @Spec void meta(){} }'
 'fixtures/ParamTest.java'='package fixtures; public class ParamTest { @org.junit.jupiter.params.ParameterizedTest void param(int value){} }'
}
foreach($entry in $fixtures.GetEnumerator()){$file=Join-Path $sources $entry.Key;[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($file))|Out-Null;[IO.File]::WriteAllText($file,$entry.Value)}
$javac=Join-Path $JavaHome $(if($IsWindows){'bin/javac.exe'}else{'bin/javac'})
& $javac -d $classes @(Get-ChildItem -LiteralPath $sources -Recurse -Filter '*.java'|ForEach-Object FullName)
if($LASTEXITCODE){throw 'Fixtures de inventario no compilados'}
$script=Join-Path $PSScriptRoot 'Test-CiCoverage.ps1';$plans=@();$saved=@{}
foreach($part in 1..2){
 $plan=& $script -Mode Plan -Directory $coverage -Part $part -Parts 2 -JavaHome $JavaHome -ClassesDirectory $classes
 $plans+=,$plan
 foreach($name in $plan.selected){$suite=if($name -eq 'fixtures.NestedTest'){$name+'$Inside'}else{$name};[IO.File]::WriteAllText((Join-Path $reports "TEST-$name.xml"),"<testsuite name='$suite' tests='1' failures='0' errors='0' skipped='0'/>")}
 & $script -Mode Validate -Directory $coverage -Part $part -Parts 2 -ReportsDirectory $reports|Out-Null
 # Each partition gets an independent report directory; never reuse stale reports.
 $reports=Join-Path $directory "reports-$part-next";[IO.Directory]::CreateDirectory($reports)|Out-Null
 $saved[$part]=Get-Content -LiteralPath (Join-Path $coverage "coverage-$part.json") -Raw
}
$expected=@('fixtures.DirectTest','fixtures.ImplementedTest','fixtures.InheritedTest','fixtures.MetaTest','fixtures.NestedTest','fixtures.ParamTest')
if(Compare-Object @($plans[0].all) $expected){throw 'Inventario no reconoce herencia, interfaces, anidación o meta-anotaciones'}
& $script -Mode Aggregate -Directory $coverage -Parts 2|Out-Null
function Restore-Manifests {foreach($part in 1..2){[IO.File]::WriteAllText((Join-Path $coverage "coverage-$part.json"),$saved[$part])}}
function Change-Manifest([int]$part,[scriptblock]$change){$file=Join-Path $coverage "coverage-$part.json";$value=Get-Content -LiteralPath $file -Raw|ConvertFrom-Json -AsHashtable;& $change $value;$value|ConvertTo-Json -Depth 12|Set-Content -LiteralPath $file}
$checks=0
function Denied([string]$name,[scriptblock]$action){$denied=$false;try{& $action|Out-Null}catch{$denied=$true};if(-not $denied){throw "Control aceptó evidencia inválida: $name"};$script:checks++}
foreach($field in @('head','run','schema','status','platform')){
 Restore-Manifests;Change-Manifest 1 {$args[0][$field]='INVALID'}
 Denied $field {& $script -Mode Aggregate -Directory $coverage -Parts 2}
}
Restore-Manifests;Change-Manifest 2 {$args[0].part=1};Denied 'partición duplicada' {& $script -Mode Aggregate -Directory $coverage -Parts 2}
Restore-Manifests;Change-Manifest 1 {$args[0].selected=@()};Denied 'clases omitidas' {& $script -Mode Aggregate -Directory $coverage -Parts 2}
Restore-Manifests;Change-Manifest 1 {$args[0].reports=@()};Denied 'clases sin ejecutar' {& $script -Mode Aggregate -Directory $coverage -Parts 2}
Restore-Manifests;Change-Manifest 1 {$args[0].failures=1};Denied 'fallo de test' {& $script -Mode Aggregate -Directory $coverage -Parts 2}
Restore-Manifests;Change-Manifest 1 {$args[0].all+=@('fixtures.OmittedTest')};Change-Manifest 2 {$args[0].all+=@('fixtures.OmittedTest')};Denied 'inventario ampliado sin ejecutar' {& $script -Mode Aggregate -Directory $coverage -Parts 2}
Restore-Manifests
Denied 'XML faltante' {& $script -Mode Validate -Directory $coverage -Part 1 -Parts 2 -ReportsDirectory $reports}
Write-Output "Inventario compilado: 6 raíces, 2 particiones completas; $checks controles negativos aprobados."
# Comprueba también el reparto actual de CI con tres manifiestos reales del auxiliar.
# La raíz adicional evita una partición vacía en este inventario sintético reducido.
$extra=Join-Path $sources 'fixtures/ExtraTest.java'
[IO.File]::WriteAllText($extra,'package fixtures; public class ExtraTest { @org.junit.jupiter.api.Test void extra(){} }')
& $javac -cp $classes -d $classes $extra
if($LASTEXITCODE){throw 'Fixture adicional no compilado'}
$coverage3=Join-Path $directory 'coverage-three'
foreach($part in 1..3){
 $plan=& $script -Mode Plan -Directory $coverage3 -Part $part -Parts 3 -JavaHome $JavaHome -ClassesDirectory $classes
 if(Compare-Object @($plan.all) @($expected+'fixtures.ExtraTest')){throw 'Inventario de tres particiones incompleto'}
 $reports3=Join-Path $directory "reports-three-$part";[IO.Directory]::CreateDirectory($reports3)|Out-Null
 foreach($name in $plan.selected){$suite=if($name -eq 'fixtures.NestedTest'){$name+'$Inside'}else{$name};[IO.File]::WriteAllText((Join-Path $reports3 "TEST-$name.xml"),"<testsuite name='$suite' tests='1' failures='0' errors='0' skipped='0'/>")}
 & $script -Mode Validate -Directory $coverage3 -Part $part -Parts 3 -ReportsDirectory $reports3|Out-Null
}
& $script -Mode Aggregate -Directory $coverage3 -Parts 3|Out-Null
Denied 'tres manifiestos con cantidad declarada incorrecta' {& $script -Mode Aggregate -Directory $coverage3 -Parts 2}
$withheld=Join-Path $directory 'withheld-third.json';$third=Join-Path $coverage3 'coverage-3.json'
Move-Item -LiteralPath $third -Destination $withheld
try{Denied 'tercer manifiesto ausente' {& $script -Mode Aggregate -Directory $coverage3 -Parts 3}}
finally{Move-Item -LiteralPath $withheld -Destination $third}
& $script -Mode Aggregate -Directory $coverage3 -Parts 3|Out-Null
Write-Output "Inventario adicional: 7 raíces, 3 particiones completas; $checks controles negativos acumulados aprobados. Fixtures de metadatos conservados: $directory"
