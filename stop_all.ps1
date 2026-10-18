# =====================================================================
# One-click stop for 老年综合评估系统 (Windows)
# Stops: Backend 8080 / AI 8000 / Frontend 5173
# Usage:  powershell -ExecutionPolicy Bypass -File stop_all.ps1
# Or just double-click stop_all.bat
# =====================================================================
$ErrorActionPreference = 'Continue'
function PortPid([int]$p){
  $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
  if($c){ return $c[0].OwningProcess } else { return 0 }
}
$stopped = $false
foreach($p in @(8080,8000,5173)){
  $pp = PortPid $p
  if($pp){
    Stop-Process -Id $pp -Force -ErrorAction SilentlyContinue
    Write-Output ("Stopped pid " + $pp + " on port " + $p)
    $stopped = $true
  } else {
    Write-Output ("Port " + $p + " not running")
  }
}
Start-Sleep -Seconds 2
Write-Output "--------------------------------------------"
foreach($p in @(8080,8000,5173)){
  if((PortPid $p)){ Write-Output ("port " + $p + " STILL listening") }
  else { Write-Output ("port " + $p + " closed") }
}
if(-not $stopped){ Write-Output "Nothing was running. All services already stopped." }
Write-Output "Done."
