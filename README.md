# 自助式老年综合评估对话系统

依据《自助式老年综合评估对话系统项目方案》(V3.0) 实现的全栈 + AI 项目。
面向社区居家老年人，老人凭"姓名＋身份证号"实名登录即可自助完成整套老年综合评估，
每道题均可直接点选选项、也可语音回答（开放题支持手动输入），AI 严格基于 21 套量表（WS/T 888—2026）提问与计分。

> **重要声明**：本系统为独立自助健康调查工具，评估结果仅作个人健康参考，
> 不构成医疗诊断，不替代医生诊疗。量表原文请以 WS/T 888—2026 标准附录B为准，
> 系统内置题目为可配置种子数据，上线前请在后台按标准原文校正。

---

## 一、技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue3 + Vite + Element Plus + Pinia + Vue Router + Axios + IndexedDB(localforage) |
| 业务后端 | Java 17 + Spring Boot 3.2 + Spring Data JPA + MySQL |
| AI/语音/FSM | Python + FastAPI（语义归一化、语音识别/合成、量表状态机） |
| 大模型 | OpenAI 兼容接口（Qwen/vLLM 私有化等），强约束系统提示词，低温度抑制幻觉 |
| 存储 | MySQL（敏感字段 AES-GCM 加密）+ 本地/可选 MinIO 存 PDF |
| 部署 | 本地一键启停脚本（`start_all.bat` / `stop_all.bat`） |

## 二、系统架构

```
┌──────────────────────────────────────────────┐
│               前端 (Vue3)                │
│  登录/首页/统一评估/报告/历史/后台管理   │
│  文字三档放大 · IndexedDB 离线草稿 · 浏览器ASR/TTS │
└───────────────┬──────────────────────────────┘
                │ /api (浏览器直连)
┌───────────────▼──────────────────────────────┐
│        业务后端 (Spring Boot :8080)            │
│  实名登录(姓名+身份证号) · 画像解析 · 分级评估引擎 │
│  量表状态机 · 计分与风险判定 · 报告/PDF · 草稿清理 │
│  后台管理 · AES加密 · 调用AI服务(带降级)          │
└───────┬──────────────────────┬───────────────┘
        │ HTTP                  │ HTTP
┌───────▼──────────┐   ┌───────▼──────────────────┐
│  MySQL 存储       │   │ AI 与语音服务 (FastAPI :8000)│
│  用户/任务/答案/   │   │  语义归一化(词典+LLM) · FSM  │
│  量表/词典/报告    │   │  ASR / TTS (可配置服务商)    │
└──────────────────┘   └──────────────────────────┘
```

## 三、功能清单

1. **实名登录**：姓名＋身份证号，无需账号密码；自动解析出生日期/年龄/性别生成用户画像。
2. **统一综合评估**：不再区分手动/语音模式——每道题同时支持「点选选项 / 语音回答 / 开放题打字」，三者任选其一；可随时点🔊重听题目。
3. **智能分级**：先初筛，低风险直接生成简易报告；存在风险才加载全部 21 套量表。
4. **量表状态机**：题目顺序、量表分支、跳转均由配置驱动，AI 无权决定流程。
5. **三层语义解析**：词典规则 → 限定域 LLM → 温和追问（≤2 轮）→ 标记"答案不确定"。
6. **AI 系统提示词**：强约束仅允许在给定选项内选择，禁止编造、禁止跑题闲聊、语气有温度。
7. **自动计分与风险提示**：按各量表阈值判定风险等级，报告高亮提示 + 健康建议。
8. **评估报告**：在线预览 + PDF 导出，固定展示免责声明。
9. **数据持久化**：后端 MySQL 主存储 + 前端 IndexedDB 兜底；未完成草稿保留 3 天，定时自动清理。
10. **文字放大**：标准/放大(1.6×)/特大(2.4×)三档一键切换，全局生效并记忆。
11. **后台管理**：量表/题目配置、对话话术、语义词典、系统参数、用户、统计、日志。
12. **安全与降级**：身份证号与健康数据 AES 加密；AI/语音故障自动降级为静态表单勾选。

## 四、目录结构

```
medical/
├── frontend/                 # Vue3 前端
│   └── src/{views,components,api,store,utils,router,styles}
├── backend/                  # Spring Boot 业务后端
│   └── src/main/java/com/geriatric/assessment/{controller,service,repository,entity,dto,config,util,client,scheduler}
├── ai-service/               # Python FastAPI AI/语音/FSM 服务
│   └── app/{semantic,llm,fsm,asr,tts}
├── deploy/
│   └── sql/                  # 建表 + 种子数据（本地数据库初始化）
└── README.md
```

## 五、快速开始（Windows 一键启动）

双击根目录 `start_all.bat` 即可一次性拉起三个服务（已运行的会自动跳过）：
- 业务后端（Spring Boot）：http://localhost:8080/api/health
- AI/语音服务（FastAPI）：http://localhost:8000/api/ai/health
- 前端（Vue3）：http://localhost:5173

需要先确认 MySQL(3306) 已启动、且数据库已初始化（首次见 `deploy/sql/` 与 `README` 的数据库初始化说明）。
停止全部服务双击 `stop_all.bat`。

## 六、本地开发（Windows）

### 0. 一键启动 / 一键停止（推荐）

双击 `start_all.bat` 即可一次性拉起后端(8080)、AI服务(8000)、前端(5173)（已运行的会自动跳过），
并自动做健康检查、打印访问地址；双击 `stop_all.bat` 一键全部关闭。
（脚本内已含数据库连接与 jar 路径，无需手动配环境变量。）

```powershell
# 或用命令行
powershell -ExecutionPolicy Bypass -File start_all.ps1
powershell -ExecutionPolicy Bypass -File stop_all.ps1
```

### 1. 初始化数据库

```powershell
cd deploy\sql
$mysql="mysql"   # 若 mysql 不在 PATH，改为你本机的完整路径
& $mysql -uroot -p你的密码 < schema.sql
& $mysql -uroot -p你的密码 geriatric_assessment < seed.sql
& $mysql -uroot -p你的密码 geriatric_assessment < seed_questions.sql
```

### 2. 启动 AI 与语音服务

```powershell
cd F:\Study\medical\ai-service
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### 3. 启动业务后端

```powershell
cd F:\Study\medical\backend
# 通过环境变量覆盖数据库密码（application.yml 默认 root/123456）
$env:DB_PASSWORD="你的密码"
mvn spring-boot:run
```

### 4. 启动前端

```powershell
cd F:\Study\medical\frontend
npm install
npm run dev   # http://localhost:5173 （已代理 /api 到 8080）
```

## 七、配置说明

| 环境变量 | 默认 | 说明 |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/geriatric_assessment?useUnicode=true&characterEncoding=UTF-8...` | 数据库连接 |
| `DB_USERNAME` / `DB_PASSWORD` | root / 123456 | 数据库账号密码 |
| `AES_KEY` | 内置占位 | 敏感字段加密密钥，**生产必须改为强随机串** |
| `AI_SERVICE_URL` | `http://localhost:8000` | AI/语音服务地址 |
| `LLM_ENABLED` | true | 是否启用大模型语义归一化（不填 key 时自动降级为词典） |
| `LLM_BASE_URL` / `LLM_MODEL` | `https://api.deepseek.com` / `deepseek-chat` | OpenAI 兼容大模型配置（默认对接 **DeepSeek**） |
| `LLM_API_KEY` 或 `DEEP_SEEK_API_KEY` | 空 | DeepSeek API 密钥（二选一；推荐用系统变量 `DEEP_SEEK_API_KEY`） |
| `ASR_PROVIDER` / `TTS_PROVIDER` | browser | 语音识别/合成方案（默认浏览器免费实现） |

### 接入 DeepSeek（语义归一化第二层）

密钥从环境变量读取，支持 `LLM_API_KEY` 或 `DEEP_SEEK_API_KEY`（二者取一，优先后者）。默认模型 `deepseek-chat`（V3）：

```powershell
# Windows：注册为持久用户环境变量（推荐，重启仍生效）
[Environment]::SetEnvironmentVariable("LLM_ENABLED","true","User")
[Environment]::SetEnvironmentVariable("DEEP_SEEK_API_KEY","sk-你的DeepSeekKey","User")

# 当前会话临时生效
$env:LLM_ENABLED="true"
$env:DEEP_SEEK_API_KEY="sk-你的DeepSeekKey"
```

```bash
# Linux
export LLM_ENABLED=true
export DEEP_SEEK_API_KEY=sk-你的DeepSeekKey
```

- 默认模型 `deepseek-chat`（V3），结构化 JSON 映射更稳定；如用 `deepseek-reasoner`（R1）需自行校验输出格式。
- 不填 key 时系统自动降级为纯规则词典归一化，不影响整体可用。
- key 只放系统环境变量，不要提交到源码仓库；改完环境变量后需重启 AI 服务。

系统参数（后台【系统参数】可改）：`max_followup_count=2`、`confidence_threshold=0.60`、
`draft_retention_days=3`、`screening_trigger_risk=1`、`admin_token=admin@2026`（**上线务必修改**）。

## 八、重要提示

- **量表权威性（已按国标重写）**：系统 21 套量表已严格按 **WS/T 888—2026《医疗机构老年综合评估技术操作标准》** 附录 B.1–B.21 重写种子数据——
  21 套量表元数据（含计分规则与风险阈值）、99 道题（题干/选项/分值/关怀式话术）均已按标准原文录入，
  判读阈值（如 ADL≤40 重度依赖、SPPB 10-12 良、NRS2002 3-7 风险、MNA-SF≤11 风险、Braden 高分低危、GLIM/谵妄按条目组合逻辑等）与标准一致。
  其中部分“医护操作/计时/行为观察”类量表（SPPB、耳语、画钟、便秘、行为观察、洼田、GLIM）已改造为老人可自助回答的选择题形式，若需严格按临床操作执行，请在后台为医护端补充人工测评入口。
- **大模型接入**：默认已对接 **DeepSeek**（`https://api.deepseek.com` / `deepseek-chat`），
  设置系统环境变量 `DEEP_SEEK_API_KEY`（或 `LLM_API_KEY`）即可启用；不填 key 时自动降级为词典规则归一化（稳定、零幻觉风险）。
  接入后务必使用仓库内强约束系统提示词并保持低温度。
- **后台令牌**：默认 `admin@2026`，上线前务必在【系统参数】修改。
- **加密密钥**：`AES_KEY` 上线前务必改为强随机密钥并妥善保管，丢失将无法解密历史数据。

## 九、本版修复记录

- 修复登录后首条问候出现字面量 `{name}`（现按性别/年龄渲染为“张大爷/李奶奶”等称呼）。
- 修复文字三档放大无效（缩放 class 原加在 `#app` 导致 `body` 读不到 CSS 变量，已改为加在 `<html>` 根元素并联动 Element Plus 字号变量；实测 16px→25.6px→38.4px）。
- 修复“部分量表未配置风险判定规则”（风险规则 JSON 格式不兼容导致的判定缺失，已兼容 `{"levels":[...]}` 与裸数组两种格式；现 21 套量表全部正常判读）。
- 修复初筛误判：高危（如 FRAIL≥3）不再被当作低风险跳过全量评估。
- 修正部分量表满分口径（如阿森斯失眠量表满分 24 而非 48），与国标一致。
- **统一评估交互**：取消“手动输入 / 语音一问一答”两种模式的拆分，首页只保留一个“开始评估”入口；每道题同时提供选项点选、语音回答与开放题文字输入，三种方式任选，避免语音模式下无法点选提交的问题。
