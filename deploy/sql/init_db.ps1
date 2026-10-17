# =====================================================================
# 本地数据库初始化脚本 (Windows)
# 用法: powershell -File init_db.ps1
# 会提示输入 MySQL root 密码，然后执行 建表 + 种子数据
# =====================================================================
param([string]$MysqlBin = "F:\JAVA\MYSQL\mysql-8.0.40-winx64\bin\mysql.exe")

$mysql = $MysqlBin
$pwd = Read-Host -Prompt "请输入 MySQL root 密码"
$base = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "==> 建库建表 schema.sql"
& $mysql "-uroot" "-p$pwd" < (Join-Path $base "schema.sql")
if ($LASTEXITCODE -ne 0) { Write-Host "schema.sql 执行失败"; exit 1 }

Write-Host "==> 写入 21 套量表元数据 seed.sql"
& $mysql "-uroot" "-p$pwd" "geriatric_assessment" < (Join-Path $base "seed.sql")
if ($LASTEXITCODE -ne 0) { Write-Host "seed.sql 执行失败"; exit 1 }

Write-Host "==> 写入题目种子 seed_questions.sql"
& $mysql "-uroot" "-p$pwd" "geriatric_assessment" < (Join-Path $base "seed_questions.sql")
if ($LASTEXITCODE -ne 0) { Write-Host "seed_questions.sql 执行失败"; exit 1 }

Write-Host "==> 数据库初始化完成。"
