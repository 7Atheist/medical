# agent.md

本文件为 AI 编程助手提供本仓库的开发指引，作为整个仓库的导航入口，说明项目背景、代码位置、执行命令和修改边界。涉及模块实现时，先阅读对应源码和配置，再决定修改范围。

- 业务后端：`backend/`，Java 业务流程、权限、数据库、计分与报告。
- 患者与管理前端：`frontend/`，Vue 页面、请求封装、浏览器语音与草稿。
- AI 服务：`ai-service/`，FastAPI 接口、语义归一化、话术包装与辅助状态机。
- 数据库与本地启动：`deploy/sql/`、根目录启动和停止脚本。

以上目录是各模块的源码入口，具体实现以对应源码和配置为准。

> 仓库：https://github.com/7Atheist/medical.git  
> 核对日期：2026-10-28；main 提交：`f7519f8f55abb24890b96ffc043f0aabcebd368c`。  
> 本文目前保存于仓库外，所有相对路径与命令均以 medical 仓库根目录为起点。  
> 技术事实基于该 GitHub 提交；开发规范约束后续工作，待完善项单独说明，不能当作已经实现。

# 项目背景

本项目是“自助式老年综合评估对话系统”，让老年人通过点选、文字或语音完成初筛与量表调查，并生成报告、保留历史记录。前端、Java 后端与 Python AI 服务在同一个 Git 仓库中，服务通过 HTTP 通信，MySQL 保存配置和业务数据。

仓库提供 21 套量表、约 99 道题的种子数据。README 声明依据 WS/T 888—2026，医学内容与阈值仍需对照原标准复核，不能仅凭说明视为已经认证。LLM 负责限定选项的理解和话术包装，流程、计分与报告由业务代码和量表配置控制。

当前版本边界：患者以姓名＋身份证号登录，管理员使用独立管理令牌。护士和医生账号、就诊关联、护士复核及医生归档不属于本次 GitHub 提交已实现的功能，不得将本地其他分支的医院 P0 改动混入此版本说明。报告仅作参考，不构成医疗诊断。

## 约定

1. 命令环境：以 Windows PowerShell 为主；命令从 medical 仓库根目录执行，不写死开发者机器上的盘符与工具路径。
2. 编码：源码、SQL 和文档使用 UTF-8；读取中文文件时显式指定 UTF-8。
3. 先思考再编码：先阅读相关源码，说明需求理解、影响范围和验证目标。角色职责、审核流程、医学阈值存在歧义时，先向负责人明确；已明确的常规实现直接推进。
4. 修改前备份：查看 `git status --short`，保留用户已有改动；修改指定文件前建立可恢复备份，不擅自覆盖其他工作。
5. 简单优先：只修改与目标直接相关的代码，不顺手重构、改风格或引入未要求的服务与框架。
6. 目标驱动：按“实现 → 验证 → 修正”完成任务，交付时明确通过项、未运行项及限制。
7. 进程管理：记录自己启动的进程；临时验证结束后停止这些进程。不得按端口误杀其他项目，不得自动启动或停止用户原有的数据库及调试服务。
8. 事实与计划分开：下文的规范不代表现有代码已全部满足；部署能力、测试覆盖和医学准确性只按实际证据描述。

## 技术栈

| 部分 | 版本或范围 | 核对文件 |
|---|---|---|
| Java | Java 17 编译目标；建议使用 JDK 17 | `backend/pom.xml` |
| Spring Boot | 3.2.5；Spring Data JPA、Web、Validation、WebFlux 随父 POM 管理 | `backend/pom.xml` |
| Maven | 建议 3.9.x；仓库无 Maven Wrapper，未锁定 Maven 版本 | 仓库文件与构建方式 |
| PDF | OpenPDF 1.3.39；Lombok 注解处理器 1.18.32 | `backend/pom.xml` |
| Node.js | Vite 锁定版本要求 `^18.0.0 || >=20.0.0`；无固定 Node/npm 版本 | `frontend/package-lock.json` |
| 前端框架与构建 | 锁文件 Vue 3.5.44、Vite 5.4.21、plugin-vue 5.2.4 | `frontend/package-lock.json` |
| 前端组件与状态 | 锁文件 Element Plus 2.14.7、Pinia 2.3.1、Vue Router 4.6.4 | 同上 |
| 前端请求与缓存 | 锁文件 Axios 1.20.0、localforage 1.10.0 | 同上 |
| Python | README 要求 3.10+；本次离线冒烟使用 Python 3.11 | `README.md` |
| AI 服务依赖 | FastAPI ≥0.111、Uvicorn[standard] ≥0.30、Pydantic ≥2.7、requests ≥2.32 | `ai-service/requirements.txt` |
| MySQL | README 选型 8.0，环境要求 8.x；其他 8.x 版本需验证 SQL 兼容性 | `README.md`、`deploy/sql/` |
| 大模型 | 默认 `deepseek-chat`，通过 requests 调 OpenAI 兼容 HTTP 接口 | `ai-service/app/config.py`、`app/llm/client.py` |

前端声明范围以 `package.json` 为准，复现依赖使用 `npm ci` 和已提交的锁文件。Python 依赖目前只声明下限，没有锁文件；不得声称所有新版本均已兼容。更换依赖版本需同步清单、锁文件和验证结果。


# 项目目录结构

```text
medical/
├── frontend/
│   ├── package.json / package-lock.json
│   ├── vite.config.js              # 端口、@ 别名、/api 与 /ai-api 代理
│   └── src/
│       ├── api/http.js             # 业务 Axios、用户/管理令牌、响应解包
│       ├── api/index.js            # 业务 API 与 AI 包装 API
│       ├── router/index.js         # 页面路由和登录守卫
│       ├── store/user.js           # 用户状态
│       ├── views/                  # 患者页面及 admin/ 管理页面
│       ├── components/             # 公共组件与字号控制
│       ├── utils/                  # speech.js、db.js、fontScale.js
│       └── styles/global.css       # 全局样式
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── resources/application.yml
│       └── java/com/geriatric/assessment/
│           ├── controller/         # HTTP 路由、参数入口
│           ├── dto/                # Java 接口请求/响应结构
│           ├── service/            # 流程、计分、报告等业务逻辑
│           ├── repository/         # JPA 数据访问
│           ├── entity/             # 数据实体
│           ├── config/             # 会话、拦截器、CORS、初始化
│           ├── common/             # Result、BizException、异常处理
│           ├── client/AiServiceClient.java
│           ├── util/               # 身份证解析、AES-GCM
│           └── scheduler/DraftCleanupScheduler.java
├── ai-service/
│   ├── requirements.txt
│   ├── tests/smoke_test.py          # 9 项离线语义/状态机冒烟检查
│   └── app/
│       ├── main.py                 # FastAPI 路由、Pydantic 模型
│       ├── config.py               # 环境变量和默认值
│       ├── llm/client.py           # 提示词、LLM 调用、包装缓存
│       ├── semantic/engine.py      # 规则 → LLM → 追问
│       ├── semantic/dictionary.py
│       ├── fsm/fsm.py              # 辅助状态机
│       ├── asr/ / tts/             # 可选服务提供者
│       └── data/dictionary.json
├── deploy/sql/
│   ├── schema.sql                  # 10 张基础表；不由 JPA 自动建表
│   ├── seed.sql                    # 量表及基础配置
│   ├── seed_questions.sql          # 题目种子
│   └── init_db.ps1                 # 首次初始化
├── start_all.ps1 / start_all.bat
├── stop_all.ps1 / stop_all.bat
├── agent.md
└── README.md
```

Java 文件下文以类名标识时，均在 `backend/src/main/java/com/geriatric/assessment/` 对应子目录内。


## 开发规范

1. 任务顺序：分析需求与源码 → 明确目标和验证方法 → 备份及检查工作区 → 必要时建立任务分支 → 实现 → 适用检查 → 审查差异 → 交付。推送和合并按用户授权执行。
2. 计划与文档：当前仓库的开发说明位于根目录 README.md 和 agent.md。常规修改同步对应现有文档；跨模块任务需要新增计划时，可在任务范围内新建 docs/plan，明确其为新增目录，不在目录树中假装已存在。
3. 计划格式：需要计划文档时，按大步骤与子步骤拆分，用 `- [ ]` 标记待办，完成后改为 `- [x]`，每项应有可验证结果。
4. Java 分层：Controller 接收参数与返回响应，业务逻辑在 Service，持久化使用 Repository；业务校验抛 `BizException`，由 `GlobalExceptionHandler` 转换为 `Result<T>`。禁止新增裸 RuntimeException 代替业务错误。
5. 前端联动：业务 API 优先使用 `frontend/src/api/` 封装；修改字段时同步页面和 Java DTO，禁止只修改一端。
6. AI 边界：LLM 不生成分值、风险阈值、下一题 ID 或医学诊断；模型输出必须由业务校验，不能直接当作确定答案计分。
7. 数据库变更：保持 `ddl-auto=none`，结构变化提供显式 SQL 与影响说明；禁止通过 JPA 自动建表覆盖迁移，禁止为通过测试清库或删除量表配置。
8. 验证强度：功能和缺陷修改补充适用的业务检查；权限、流程、计分等关键逻辑应有可复现测试。纯文档调整只做文档校验，不启动应用。

### 构建与启动命令

以下为 PowerShell 命令。先进入自己的 medical 仓库根目录，确认 `backend/pom.xml`、`frontend/package.json` 存在。

#### 工具检查与首次安装

```powershell
java -version
mvn -version
node --version
npm --version
python --version
mysql --version
```

安装并构建，逐条执行，任何步骤非零退出即停止排查：

```powershell
npm --prefix .\frontend ci
npm --prefix .\frontend run build
mvn -f .\backend\pom.xml clean package
python -m venv .\ai-service\.venv
& .\ai-service\.venv\Scripts\python.exe -m pip install -r .\ai-service\requirements.txt
```

预期生成 `frontend/dist/`、`backend/target/assessment-backend.jar`。仓库当前未见 Java 测试类，打包成功不能代表业务回归已通过。

#### 配置与数据库前提

- MySQL 必须已启动并监听本地 3306；远端 `start_all.ps1` 只检查，不负责启动 MySQL。
- 后端默认数据库名 `geriatric_assessment`，`spring.jpa.hibernate.ddl-auto=none`。
- 初始化会写数据库；只对确认过的开发库执行，已有数据先备份，不对医院数据直接导入种子。

首次空库初始化，从根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\sql\init_db.ps1
```

脚本会询问 root 密码，按顺序执行 `schema.sql` → `seed.sql` → `seed_questions.sql`。检查每一步输出与退出状态。Windows PowerShell 中文编码异常时停止，改用 UTF-8 文件读取和支持 utf8mb4 的 MySQL 客户端导入。

| 配置项 | 作用与当前行为 |
|---|---|
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | 后端数据库连接；一键脚本按本地 3306/root 初始化，并覆盖后端 DB_URL，自定义连接使用手动启动 |
| `AES_KEY` | 后端敏感字段加密配置；试用前替换开发默认值并妥善保存，已有数据不可直接换钥 |
| `ADMIN_TOKEN` | 管理员共享令牌；为空时还会查询 `sys_param.admin_token`，两处均无配置才拒绝 |
| `AI_SERVICE_URL` | 后端访问 AI 服务的地址，默认 `http://localhost:8000` |
| `REPORT_PDF_DIR` | PDF 目录，默认相对后端工作目录的 `./reports` |
| `LLM_ENABLED` | 默认 true；离线验证设为 false |
| `LLM_BASE_URL`、`LLM_MODEL` | 默认 `https://api.deepseek.com`、`deepseek-chat` |
| LLM 密钥读取优先级 | `LLM_API_KEY` → `DEEP_SEEK_API_KEY` → `DEEPSEEK_API_KEY` |
| `LLM_TIMEOUT`、`LLM_TEMPERATURE` | 默认 60 秒、0.1 |
| `CONFIDENCE_THRESHOLD`、`MAX_FOLLOWUP` | AI 默认 0.60、2；与后端 sys_param 的对应参数保持一致 |

通过运行环境注入密钥。仓库未配置自动读取 `.env`，仅创建该文件不会让配置生效。不得将真实密钥写入本文、源码或示例。

#### 一条命令启动全部应用

完成依赖安装、后端打包、数据库和环境变量准备后，使用已激活的 Python 虚拟环境执行：

```powershell
. .\ai-service\.venv\Scripts\Activate.ps1
powershell -ExecutionPolicy Bypass -File .\start_all.ps1
```

真正启动命令为第二行；第一行确保脚本使用安装过依赖的 Python。若本机策略禁止激活，使用下述手动启动方式。

脚本检查数据库和 scale 表，在缺表时尝试导入基础数据，随后启动后端 8080、AI 8000、前端 5173。它不会自动重编译已有 JAR，也不等于执行数据库迁移；仅有 scale 表并不证明其余表和种子完整。

#### 分服务手动启动

数据库已准备好、配置已注入后，在三个独立终端分别从根目录执行：

```powershell
# 终端 1：后端；保留 backend 作为工作目录，确保相对 PDF 路径一致
Set-Location .\backend
java -jar .\target\assessment-backend.jar
```

```powershell
# 终端 2：AI 服务
Set-Location .\ai-service
& .\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

```powershell
# 终端 3：前端
npm --prefix .\frontend run dev
```

访问 `http://localhost:5173`。一键脚本会将 AI 绑定到 0.0.0.0；医院试用前需另行核对网络暴露与访问控制。

#### 停止服务

手动终端使用 Ctrl+C。仓库也提供：

```powershell
powershell -ExecutionPolicy Bypass -File .\stop_all.ps1
```

该脚本按端口结束 8080、8000、5173 的监听进程，执行前须确认属于本项目；它不停止 MySQL。不要将文档校验当作启动或停止服务的授权。

### 验证命令与成功标准

#### 无需启动服务的检查

从根目录执行：

```powershell
# 前端：校验 Vue 与打包依赖，预期退出码 0 并生成 dist
npm --prefix .\frontend run build

# 后端：编译和执行已有测试；当前 GitHub 版本没有 Java 测试类
mvn -f .\backend\pom.xml test

# AI：禁用外部 LLM，不需要数据库、网络密钥或常驻服务
$previousLlmEnabled = $env:LLM_ENABLED
try {
    $env:LLM_ENABLED = 'false'
    & .\ai-service\.venv\Scripts\python.exe .\ai-service\tests\smoke_test.py
    if ($LASTEXITCODE -ne 0) { throw 'AI 冒烟测试失败' }
}
finally {
    $env:LLM_ENABLED = $previousLlmEnabled
}
```

AI 预期输出 9 项通过并退出 0。其覆盖词典命中、无法理解时追问、开放题和状态机顺序/跳转；不覆盖真实模型回复、权限、计分阈值、PDF 或全流程。

当前前端仅有 `dev/build/preview` 脚本，没有 test/lint 脚本，不得编造 `npm test` 或 `npm run lint`。

#### 服务已经运行时的联通检查

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8000/api/ai/health
(Invoke-WebRequest http://localhost:5173 -UseBasicParsing).StatusCode
```

要求健康接口返回成功、前端 HTTP 200。AI 健康字段 `llm_enabled` 只表明已启用且存在密钥，不能证明供应商连接、额度或推理正常。

#### 按修改范围补充业务验证

| 修改范围 | 最低验证目标 |
|---|---|
| 权限、任务、报告 | 本人可访问；其他用户不可访问、修改或下载；无效令牌被拒绝 |
| 答案归一化 | 点选合法值；越界选项；“我还行吧”需澄清；跑题不强行赋分；低置信度追问；追问上限后不确定 |
| 量表与计分 | 最低分、最高分、风险边界、缺失答案及跳题场景；核对原标准来源 |
| 服务故障 | LLM 禁用/超时/格式错误、AI 服务中断、ASR 不支持时可继续点选 |
| 提示词与包装 | 时间范围、否定词、能力/食欲等判断对象不变；原题兜底可用 |
| UI 与语音 | 登录 → 创建任务 → 作答 → 报告/历史；三档字号；不支持语音时保留文字和点选 |

交付时写明命令、结果、未验证内容和阻塞原因。不得把编译成功描述为医院试用验收通过。


## Git 规范

以下为后续协作约定，不表示仓库已经建立对应分支或 CI 策略。本次远端检查确认存在 main，未发现 dev 分支；创建开发分支时应以实际仓库状态为准。

- 修改前运行 `git status --short` 和 `git diff`，识别用户尚未提交的工作，禁止未经授权执行 reset --hard、clean 或覆盖改动。
- 需要独立开发分支时，从确认过的当前基线创建；先检查现有分支和用户要求，不自动创建 dev 或切换基线。
- 建议提交信息使用 `类型(模块): 中文摘要`，例如 `docs(agent): 完善仓库开发指引`、`fix(backend-assess): 校验答案选项范围`。模块按 backend、frontend、ai-service、deploy、agent 等实际位置填写。
- 每次提交只包含本任务文件，说明修改目的和适用验证结果；禁止将用户无关改动一并提交。
- 提交前执行 `git diff --check`，禁止提交真实密钥、患者数据、日志、话术缓存和构建产物。
- 未授权时不自行推送、合并或删除分支；不得将未通过检查或存在冲突的工作描述为稳定版本。

## Where to Go Next

当前没有模块级指引文件，按任务先读取下列实际入口：

| 工作类型 | 先查看的位置 |
|---|---|
| 业务接口与参数 | `backend/src/main/java/com/geriatric/assessment/controller/`、`dto/`（同一 Java 基础包下） |
| 任务推进与计分 | `backend/src/main/java/com/geriatric/assessment/service/AssessmentService.java`、`ScoringService.java`（同目录） |
| 认证与管理员权限 | 同一 Java 基础包下的 `config/AuthInterceptor.java`、`AdminAuthInterceptor.java`、`SessionManager.java`、`WebConfig.java` |
| 前端页面与状态 | `frontend/src/views/`、`frontend/src/store/user.js`、`frontend/src/router/index.js` |
| 前后端与 AI 联调 | `frontend/src/api/`、`frontend/vite.config.js`、Java 基础包下的 `client/AiServiceClient.java` |
| AI 接口与提示词 | `ai-service/app/main.py`、`ai-service/app/llm/client.py`、`ai-service/app/semantic/engine.py` |
| 现有 AI 测试 | `ai-service/tests/smoke_test.py` |
| 安装与配置 | `README.md`、`backend/src/main/resources/application.yml`、`ai-service/app/config.py` |
| 数据库与启动 | `deploy/sql/`、`start_all.ps1`、`stop_all.ps1` |

表格中出现的 Java 子路径，均相对于 `backend/src/main/java/com/geriatric/assessment/`，不是仓库根目录的独立文件夹。

## Cross-Cutting Conventions

以下约定适用于整个仓库，具体模块实现以对应源码为准。

- 文档语言：使用简体中文，保留代码中的真实字段名、文件名与命令。
- 同步更新：修改架构、接口、配置、提示词或运行方式时，同步 README.md 与本文涉及的说明，避免文档和代码互相矛盾。
- 权限与归属：任务、答案、报告的查询、修改、删除和下载必须在后端校验当前用户及资源归属，禁止只依赖前端守卫或客户端 userId。
- 模型输出校验：选项值必须属于当前题目的 options；置信度必须是 0～1 的有效数值；无法确认或 needs_followup=true 时不得强行赋分。
- 隐私与密钥：禁止在代码、日志、缓存、文档和 Git 中新增真实密钥、完整身份证号、姓名及健康回答明文；外部模型调用按最小必要原则控制字段，禁止直接发送完整用户对象。
- 测试位置：现有 Python 检查在 ai-service/tests；新增 Java 测试使用 backend/src/test/java。当前前端没有 test/lint 脚本，不得引用不存在的 frontend/tests 或测试入口。
- 量表依据：修改选项分值、跳题、初筛分流、风险阈值时注明标准来源，并验证最低分、最高分、风险边界、缺失答案与不确定答案。
- 交付说明：写明修改行为、执行过的检查与未验证内容；编译通过不能替代完整业务验收。

### 接口契约与前后端联动

当前仓库未见独立维护的统一 API 规范文件。查接口时以这些源码为准：

| 契约内容 | 定义位置 |
|---|---|
| Java 路由 | `backend/src/main/java/com/geriatric/assessment/controller/` |
| Java 请求/响应字段 | 同包路径下 `dto/`，特别是 SubmitAnswerRequest、QuestionDTO、AnswerResponse、ReportDTO |
| 统一业务响应 | `common/Result.java`、`common/GlobalExceptionHandler.java` |
| 前端请求封装与令牌 | `frontend/src/api/index.js`、`frontend/src/api/http.js` |
| 后端调用 AI 的字段与超时 | `backend/src/main/java/com/geriatric/assessment/client/AiServiceClient.java` |
| Python 模型与路由 | `ai-service/app/main.py`；运行后可查 `http://localhost:8000/docs` 和 `/openapi.json` |

#### 路径与代理

- 后端 context-path 为 `/api`；Controller 里写 `/assess`，实际 URL 是 `/api/assess/...`。前端 http 已设置 baseURL=/api，禁止再重复添加前缀。
- Vite 将 `/api` 转发到 8080；`/ai-api` 转发到 8000 并移除 `/ai-api` 前缀。
- 包装请求链路：`/ai-api/api/ai/wrap` → `http://localhost:8000/api/ai/wrap`。
- Vite 代理只适用于开发服务器；生产静态部署要另配对应反向代理。
- Java 的 CORS 配置在 WebConfig，不得为解决代理路径错误随意放宽所有环境的安全设置。

#### 认证与响应

- 患者：`POST /api/auth/login`；业务请求携带 `Authorization: Bearer <token>`。token 是 SessionManager 内存会话，默认滑动过期 72 小时，重启后失效，当前不是 JWT。
- 管理员：`/api/admin/` 下的全部接口 使用独立管理令牌；前端分别存储 `token` 和 `adminToken`，禁止混用。
- PDF 下载目前通过查询参数 token 兼容浏览器下载，入口在 `reportPdfUrl`；不要将这种方式扩展为普通 API 的默认做法。
- Java 普通 JSON 与 AI 成功响应使用 `{ code, message, data }`，code=0 才是业务成功；HTTP 200 不保证业务成功。FastAPI 参数校验可能返回 HTTP 422/detail，不能假设所有错误均有该包装。

#### 答案字段映射

| 前端 → Java（camelCase） | Java → Python（snake_case） |
|---|---|
| `questionId` | `question_text`、`dialog_text`、`options` |
| `answerSource: OPTION / TEXT / VOICE` | 文本统一进入 `user_text` |
| `optionValue` / `text` / `pause` | `profile`、`history`；Python 另支持 `dictionaries` |

Java 返回的 `AnswerResponse.type` 为 `QUESTION / FOLLOWUP / DONE`。修改时检查 Assessment.vue 对响应的处理。Python 返回 `option_value`、`option_text`、`confidence`、`needs_followup`、`followup_question`，引擎另添加 `llm_used`。


### 产品内 AI 提示词规范

本节保留原文的角色、限定选项、追问、话术包装与安全边界，并消除示例歧义。实际提示词常量位于 `ai-service/app/llm/client.py` 的 `SYSTEM_PROMPT` 与 `WRAP_SYSTEM_PROMPT`；下列规范是修改与验收目标，不表示提示词及校验代码已全部同步。

#### 角色、称呼与语言

- 健康调查助手采用耐心、温和、尊重的语气，不冒充真实医护身份，不做诊断、不开药、不提供个体治疗方案。
- 优先使用传入且经脱敏的 `profile.title`，缺失时称“您”，不猜姓名或年龄。
- 面向患者的 AI 话术避免生硬的“请选择”“请作答”，避免不必要的医学术语。
- “评估、问卷、量表、体检、题目、选项、诊断、治疗、处方”等限制仅用于润色聊天话术；不用于约束 API 字段、代码、技术文档、正式报告和必要的知情说明。
- 可以用温和问句，保持一句一个意思。禁止为口语化而改变原题时间窗口、频率、否定关系和判断对象。

| 原题意 | 可接受的话术示例 |
|---|---|
| 最近两周是否情绪低落 | 您最近两周有没有觉得心情低落？ |
| 能否自行进食 | 您吃饭能自己完成，还是需要人帮忙？ |
| 最近四周是否疲劳 | 您最近四周有没有觉得累、没精神？ |

不能将“能否自行进食”改成“饭香不香”，也不能将“两周”改为不明确的“最近”。

#### 语义归一化契约

- 有选项时只允许映射到给定 options 的 value，不能补充选项。
- 回答含糊、跑题或缺乏判断条件时，输出 `needs_followup=true`；一次只问一句、不超过 40 字，追问内容应适合本题，不能把所有题都追问成“是或不是”。
- confidence 是 0～1 的 JSON 数值；默认低于 0.6 必须进入澄清，阈值最终服从统一配置。
- 未命中使用 JSON `null`，禁止字符串 `"null"`。布尔值使用 true/false。
- 只输出一个 JSON 对象，不输出解释或 Markdown。引擎可在模型结果外补充 `llm_used`。
- 追问轮数由后端 `max_followup_count` 控制，默认 2；达到上限仍无法判断则保留不确定状态，不编造确定答案。

假定选项为 `0=否、1=是`，明确回答命中示例：

```json
{
  "option_value": "1",
  "option_text": "是",
  "confidence": 0.95,
  "needs_followup": false,
  "followup_question": ""
}
```

含糊回答示例：

```json
{
  "option_value": null,
  "option_text": null,
  "confidence": 0.2,
  "needs_followup": true,
  "followup_question": "您说还行，是能自己吃饭，还是需要人帮忙呢？"
}
```

开放题没有固定 options，当前 Python 引擎直接记录描述，不按固定选项题强行归类；不得由 LLM 编造量表分值。

#### 话术包装与缓存

- 包装输出一句简短的生活化话术，目标 30 字以内；以保留医学含义为优先条件，不满足则使用原题。
- 不提前替患者选择答案，不暗示预期结果，不添加疾病结论。
- 修改提示词后同步本文，并验证时间窗口、能力与食欲等容易混淆的概念。
- 当前包装 temperature=0.7；归一化使用 LLM_TEMPERATURE，默认 0.1。低温度不保证绝对确定性。
- 实际缓存路径：`ai-service/app/data/wrap_cache.json`。当前键为题干＋称呼的哈希，未包含选项、模型和提示词版本。相关内容改变时需更新缓存设计或仅清理该运行缓存，禁止误删业务数据。
- 缓存只应用于通用话术，禁止新增患者姓名、身份证号或健康回答。

#### 当前语音与故障降级链路

```text
OPTION：后端校验标准选项 → 保存答案 → 后端推进
TEXT：后端词典 → AI 服务（Python 自身也先规则匹配，再 LLM）→ 澄清/确定
VOICE：浏览器 ASR 转文字 → 跳过 Java 词典 → AI 服务（仍可能先命中 Python 规则）
OPEN：记录描述，不进行固定选项映射
```

“调用 AI 服务”不等于“必然调用外部 LLM”，不能用它证明语音理解准确率。浏览器 ASR/TTS 的可用性取决于浏览器、权限和服务提供者；本项目不保证离线可用。

| 故障点 | 当前代码行为及检查位置 |
|---|---|
| 前端包装等待超时 | aiHttp 超时 5 秒，Assessment.vue 兜底显示原题 |
| Python 调外部 LLM 超时/失败 | 默认 60 秒；client 返回 None，语义引擎追问，包装接口返回原题 |
| Java 等待语义服务超时 | AiServiceClient 为 15 秒，返回 null，由后端引导静态点选 |
| Python 已有规则命中 | 无需外部 LLM；规则未命中且无 LLM 时追问 |
| ASR 不可用 | 保留点选与文字输入；后端可选转写调用超时 20 秒 |

以上是不同层次的超时，禁止统一写成“AI 超时 5 秒”。前端停止等待不保证 Python 已取消外部请求。


### 当前实现的易错点与待完善项

这些是源码核对发现的差距，记录它们是为了避免后续错误假设；更新文档不能视为已修复代码。

1. 隐私白名单：AI 调用只带姓名与称谓等非敏感字段，不带身份证。 AssessmentService.profileOf 现在只传 name/age/gender/title（已移除 birth_date）；前端 Assessment.vue 通过 safeProfile() 白名单后传给 AI wrap 接口（不含 idCardMask、birthDate）；client.build_user_prompt 仅拼接 title/name/age/gender。后端会保存答案，部分字段 AES 加密。仍需复核：历史回答 recentHistory 是否包含敏感信息、第三方处理边界与用户告知。
2. 答案校验仍有缺口。 Python 词典命中分支未统一校验 options 成员；LLM 分支对高置信度结果可能忽略 needs_followup。Java 对非空 AI 选项未强制拒绝越界，某些情况会给缺失置信度默认值。修改归一化时必须补足边界，不能仅增强提示词。
3. 称谓策略不统一。 原文的“70 岁以上”不符合当前后端 `titleOf`（80 岁阈值）；后端 profileOf 未传 title。应统一配置并贯通字段，本文不把旧文档阈值当作实际逻辑。
4. 医学阈值需复核。 规则来源包括 SQL 和 DataInitializer 的默认风险规则；ScoringService 按配置区间匹配。修改前同时检查初始化与计分路径，对重叠区间、缺失回答及初筛分流做边界验证。
5. 自动清理会删除业务记录。 DraftCleanupScheduler 默认每天 03:00 清理，草稿默认 3 天、已完成记录默认 30 天。医院试用前需明确留存要求；禁止把“有加密”视为可直接采用演示留存策略。
6. 当前权限形态有限。 管理员共享令牌、内存用户会话、直接暴露的 AI 接口均需结合医院部署要求评估，不能声称已经具备完整角色权限或审计闭环。
7. 初始化不等于迁移。 启动脚本以 scale 表是否存在决定初始化，不能自动修复缺表、升级旧结构或保证种子完整。已有库的变更必须提供显式 SQL。
8. 提示词与示例尚有冲突。 当前 client.py 的 JSON 模板仍有字符串 null 歧义，包装规则仍写“不是直接提问”却给问句示例；修改提示词时按上方产品内 AI 提示词规范同步修正并回归。


## 生产部署与访问边界

本仓库提供 Windows 本地启动脚本，当前提交没有 Docker Compose、宝塔 Nginx 配置、生产镜像发布流程或成熟的医院部署方案。后续部署以实际新增文件和验证结果为准。

- 网络入口：开发端口为前端 5173、后端 8080、AI 8000、MySQL 3306；这些是本地开发默认值，不是生产网络拓扑。部署时由统一入口代理业务与 AI 路径，并按实际环境配置 HTTPS 和访问控制。
- 代理约定：必须同时处理 /api 与 /ai-api 的转发与前缀移除，Vite 开发代理不会随 dist 自动部署。禁止因接口不通随意放开 CORS 或公开数据库。
- AI 暴露：一键脚本将 AI 绑定 0.0.0.0；AI 路由未见独立鉴权。试用前需通过实际部署配置限制可访问范围，不得宣称已具备完整访问保护。
- 配置保护：数据库密码、管理令牌、AES_KEY 和 LLM 密钥通过运行环境注入，不使用演示默认值承担真实业务；仓库未自动读取 .env，生产配置也不得提交 Git。
- 数据与换钥：迁移前备份数据库与报告文件；已有加密数据不能直接更换 AES_KEY 而不安排解密迁移。不得把真实数据或备份放在公开 Web 目录。
- 留存与清理：默认草稿 3 天、完成记录 30 天及每日 03:00 清理规则需由医院明确留存口径后再采用，不能直接套用演示策略。
- 认证与业务责任：当前为内存患者会话和管理员共享令牌。医院试用需要的岗位账号、患者/就诊关联、复核、归档及审计必须单独实现与验证，不由本文声明替代。
- 医学声明：保留报告“不构成医疗诊断”的说明；医院负责人确认使用范围、医护责任与量表口径后，再确定试用边界。

