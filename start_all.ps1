# =====================================================================
# One-click start for 老年综合评估系统 (Windows)
# Starts (if not already running): Backend 8080 / AI 8000 / Frontend 5173
# Usage:  powershell -ExecutionPolicy Bypass -File start_all.ps1
# Or just double-click start_all.bat
# =====================================================================
$ErrorActionPreference = 'Continue'
# 自动定位项目根目录（脚本所在目录），兼容任意克隆路径
$root     = $PSScriptRoot
# JDK 可执行文件：优先 JAVA_HOME，其次 PATH 中的 java（通用，不写死本机路径）
$java = $null
if ($env:JAVA_HOME) { $cand = Join-Path $env:JAVA_HOME "bin\java.exe"; if (Test-Path $cand) { $java = $cand } }
if (-not $java) { $gc = Get-Command java -ErrorAction SilentlyContinue; if ($gc) { $java = $gc.Source } }
$jar      = Join-Path $root "backend\target\assessment-backend.jar"
$python   = "python"
$mysqlP   = 3306
$bkP      = 8080
$aiP      = 8000
$frP      = 5173

function PortPid([int]$p){
  $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
  if($c){ return $c[0].OwningProcess } else { return 0 }
}

Write-Output "== Check services =="

# ---- 1. MySQL (database) ----
if((PortPid $mysqlP) -eq 0){
  Write-Output "[!] MySQL is NOT running on port $mysqlP"
  Write-Output "    Please start MySQL first (port 3306)."
} else {
  Write-Output "[OK] MySQL  running (pid $(PortPid $mysqlP))"
}

# ---- 2. Backend (Spring Boot, :8080) ----
if((PortPid $bkP) -eq 0){
  if(-not (Test-Path $jar)){ Write-Output "[!] jar not found: $jar"; }
  elseif(-not $java){ Write-Output "[!] 未找到 JDK 的 java，请安装 JDK17 并设置 JAVA_HOME 后重试"; }
  else {
    # 数据库密码：优先系统环境变量 DB_PASSWORD；未设置时运行时提示输入（避免明文入库/泄露）
    if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = Read-Host "请输入 MySQL root 密码（或先 setx DB_PASSWORD 设置）" }
    $env:DB_URL = "jdbc:mysql://localhost:3306/geriatric_assessment?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
    Start-Process -FilePath $java -ArgumentList @("-jar",$jar) `
      -WorkingDirectory (Join-Path $root "backend") `
      -RedirectStandardOutput (Join-Path $root "backend\app.out.log") `
      -RedirectStandardError  (Join-Path $root "backend\app.err.log") `
      -WindowStyle Hidden
    Write-Output "[..] Backend starting (jar) ..."
  }
} else {
  Write-Output "[OK] Backend running (pid $(PortPid $bkP))"
}

# ---- 3. AI & voice service (FastAPI, :8000) ----
if((PortPid $aiP) -eq 0){
  Start-Process -FilePath $python -ArgumentList @("-m","uvicorn","app.main:app","--host","0.0.0.0","--port","8000") `
    -WorkingDirectory (Join-Path $root "ai-service") `
    -RedirectStandardOutput (Join-Path $root "ai-service\uv.out.log") `
    -RedirectStandardError  (Join-Path $root "ai-service\uv.err.log") `
    -WindowStyle Hidden
  Write-Output "[..] AI service starting ..."
} else {
  Write-Output "[OK] AI service running (pid $(PortPid $aiP))"
}

# ---- 4. Frontend (Vite dev, :5173) ----
if((PortPid $frP) -eq 0){
  Start-Process -FilePath "cmd.exe" -ArgumentList @("/c","npm run dev") `
    -WorkingDirectory (Join-Path $root "frontend") `
    -RedirectStandardOutput (Join-Path $root "frontend\vite.out.log") `
    -RedirectStandardError  (Join-Path $root "frontend\vite.err.log") `
    -WindowStyle Hidden
  Write-Output "[..] Frontend starting ..."
} else {
  Write-Output "[OK] Frontend running (pid $(PortPid $frP))"
}

# ---- health check (wait up to 120s; AI service starts slowly) ----
Write-Output ""
Write-Output "== Waiting for services (max 120s) =="
$bk=$false; $ai=$false; $fr=$false
$deadline = (Get-Date).AddSeconds(120)
while((Get-Date) -lt $deadline){
  try { $r=Invoke-WebRequest -Uri "http://localhost:$bkP/api/health" -TimeoutSec 6 -UseBasicParsing; if($r.StatusCode -eq 200){$bk=$true} }catch{}
  try { $r=Invoke-WebRequest -Uri "http://localhost:$aiP/api/ai/health" -TimeoutSec 6 -UseBasicParsing; if($r.StatusCode -eq 200){$ai=$true} }catch{}
  try { $r=Invoke-WebRequest -Uri "http://localhost:$frP" -TimeoutSec 6 -UseBasicParsing; if($r.StatusCode -eq 200){$fr=$true} }catch{}
  if($bk -and $ai -and $fr){ break }
  Start-Sleep -Seconds 3
}

Write-Output "=================================================="
Write-Output ("Backend  http://localhost:$bkP/api/health  -> " + $(if($bk){"UP"}else{"DOWN"}))
Write-Output ("AI       http://localhost:$aiP/api/ai/health -> " + $(if($ai){"UP"}else{"DOWN"}))
Write-Output ("Frontend http://localhost:$frP              -> " + $(if($fr){"UP"}else{"DOWN"}))
Write-Output "=================================================="
if($bk -and $ai -and $fr){
  Write-Output "All services are up. Open http://localhost:5173 in your browser."
} else {
  Write-Output "Some services are still DOWN. Check the log files:"
  Write-Output "  backend\app.err.log / ai-service\uv.err.log / frontend\vite.err.log"
}
