#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr')
$ErrorActionPreference='Stop'
& "$PSScriptRoot\Stop-Smr.ps1" -Root $Root -Instance recepcion -DrainOnly
Stop-Service -Name SMRRecepcion -ErrorAction Stop
