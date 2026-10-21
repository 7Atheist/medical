# 自助式老年综合评估对话系统

依据 **WS/T 888—2026《医疗机构老年综合评估技术操作标准》** 实现的全栈 + AI 项目。
面向社区居家老年人，凭「姓名＋身份证号」实名登录即可自助完成整套老年综合评估。
聊天式交互，支持点选选项 / 语音回答 / 文字输入，AI 负责口语化理解与话术包装。

> **重要声明**：本系统为自助健康调查工具，结果仅作个人健康参考，不构成医疗诊断，不替代医生诊疗。

---

## 一、项目构想与总需求

### 1.1 背景
我国 60 岁以上人口已超 2.97 亿，老年综合评估（CGA）是老年医学核心手段，但传统 CGA 需要医护人员面对面、耗时 1-2 小时，难以在社区和居家场景普及。

### 1.2 目标
让老人**像聊天一样**完成 21 套量表（约 99 题），不需要医护在场，不需要理解医学术语，通过语音/点选即可完成自助评估。

### 1.3 核心需求
| 需求 | 说明 |
|---|---|
| 实名登录 | 姓名+身份证号，无需账号密码，自动解析年龄性别 |
| 聊天式交互 | 聊天气泡流，不像问卷像聊天 |
| 多模态输入 | 点选 / 语音 / 文字，三选一 |
| AI 话术包装 | 医学题干改写成生活化开场白（"最近睡得踏实不？"） |
| AI 语义理解 | 口语化回答交 AI 映射到标准选项，不猜不编 |
| 智能分级 | 初筛低风险出简易报告，有风险进入全量 21 量表 |
| 自动计分 | 按国标阈值判定风险等级，报告高亮提示 |
| 历史记录 | 每次评估持久化，支持删除和自动清理 |
| 老人友好 | 三档字号放大、大按钮、高对比、移动端适配 |

---

## 二、技术栈选型

### 2.1 技术栈总览

| 层 | 技术 | 选型理由 |
|---|---|---|
| 前端 | Vue3 + Vite + Element Plus | 国内生态成熟、Element Plus 组件库适合中后台、Vite 启动快 |
| 前端状态 | Pinia + Vue Router | Pinia 轻量比 Vuex 简单、Router 官方路由 |
| 前端请求 | Axios + IndexedDB(localforage) | Axios 拦截器方便加 token、IndexedDB 存离线草稿 |
| 业务后端 | Java 17 + Spring Boot 3.2 | 企业级稳定、JPA 减少 SQL、Spring 生态完整 |
| 数据访问 | Spring Data JPA + Hibernate | 自动建表、Repository 接口即写即用 |
| 数据库 | MySQL 8.0 | 关系型数据适合量表配置、事务可靠、社区熟悉 |
| AI 服务 | Python 3.10+ + FastAPI | FastAPI 异步高性能、Python AI 生态最好（requests/openai 兼容） |
| 大模型 | DeepSeek（`deepseek-chat`） | 国内可访问、中文效果好、价格低、OpenAI 兼容接口 |
| 语音 | 浏览器 Web Speech API | 免费、无需额外服务、老人手机浏览器原生支持 |
| PDF | iText / 浏览器打印 | 跨平台中文字体支持 |
| 部署 | Windows 一键脚本 | 项目在 Windows 开发，bat/ps1 双击即用 |

### 2.2 为什么前后端分离成 Java + Python 两个服务

- **Java 后端**负责业务流程：用户、任务、计分、报告、权限——这些需要事务和稳定，Spring 最稳
- **Python AI 服务**负责 LLM 调用和语义理解——Python 调 OpenAI 兼容接口最方便
- 两者通过 HTTP 通信，Python 服务挂了 Java 自动降级为词典匹配，不影响核心流程

### 2.3 为什么用 DeepSeek 而不是 GPT
- 国内直连无需代理
- 中文理解能力强（老人聊天口语化）
- 价格约为 GPT-4 的 1/100
- OpenAI 兼容接口，换模型只改 base_url

---

## 三、系统架构

```
┌─────────────────────────────────────────────┐
│         前端 Vue3 (:5173)                   │
│  登录 / 聊天式评估 / 报告 / 历史 / 后台     │
│  三档字号 · 浏览器ASR/TTS · 离线草稿        │
└──────────────┬──────────────────────────────┘
               │ /api 直连
┌──────────────▼──────────────────────────────┐
│    业务后端 Spring Boot (:8080)             │
│  实名登录 · 画像解析 · 量表状态机            │
│  计分风险 · 报告PDF · 历史清理 · AES加密     │
└───────┬──────────────────────┬──────────────┘
        │                    │
┌───────▼──────┐   ┌────────▼───────────────┐
│  MySQL 8    │   │  AI 服务 FastAPI (:8000) │
│  10张表      │   │  语义归一化 · 话术包装   │
│  21套量表    │   │  DeepSeek LLM 调用      │
│  99道题      │   │  词典规则匹配           │
└──────────────┘   └─────────────────────────┘
```

---

## 四、数据库设计

数据库 `geriatric_assessment`，共 **10 张表**，分三类：

### 4.1 固定配置表（不删不改）

| 表名 | 说明 | 关键字段 |
|---|---|---|
| `scale` | 21 套量表元数据 | code(FRAIL/ADL/GDS...)、name、category、is_screening、scoring_rule(JSON)、risk_rule(JSON) |
| `scale_question` | 99 道题目 | scale_id、question_text(原题干)、dialog_text(话术)、options(JSON: value/label/score)、jump_rule |
| `semantic_dictionary` | 口语同义词典 | question_id、option_value、synonyms(逗号分隔)、match_type |
| `dialog_config` | 对话话术配置 | scene(GREETING/FOLLOWUP/DISCLAIMER)、content、sort_order |
| `sys_param` | 系统参数 | max_followup_count、confidence_threshold、history_retention_days |

### 4.2 用户与评估过程表

| 表名 | 说明 | 关键字段 |
|---|---|---|
| `sys_user` | 实名用户 | name、id_card(AES加密)、id_card_mask(脱敏)、birth_date、age、gender |
| `assessment_task` | 评估任务 | user_id、status(SCREENING/ASSESSING/COMPLETED)、current_scale_id、current_question_id、followup_count、completed_scales(JSON)、screening_risk |
| `assessment_answer` | 每轮答案 | task_id、question_id、user_text(原始输入)、asr_text(语音转写)、normalized_value(AI映射)、confidence、uncertain、is_followup |

### 4.3 报告与日志表（可清理）

| 表名 | 说明 | 关键字段 |
|---|---|---|
| `assessment_report` | 评估报告 | task_id、user_id、content_json(各量表得分)、pdf_path |
| `sys_log` | 操作日志 | user_id、action、detail、ip |

### 4.4 数据流向

```
老人登录 → sys_user 自动建档
         → assessment_task 创建任务
         → scale_question 逐题取题
         → assessment_answer 记录每轮答案（原始+AI归一化）
         → 计分引擎读 scale.scoring_rule + risk_rule
         → assessment_report 生成报告
         → 30天后自动删 task/answer/report
```

---

## 五、功能清单

1. **实名登录**：姓名＋身份证号，自动解析年龄/性别/称谓
2. **聊天式评估**：聊天气泡流，护士头像提问、选项点选即提交
3. **生活化话术包装**：AI 把医学题干改成聊天开场白（"最近睡得踏实不？"）
4. **统一输入**：每题同时支持点选选项 / 语音回答 / 文字输入
5. **语音识别**：点击开始录音、再点停止，识别文本交 AI 分析
6. **AI 语义归一化**：口语化回答交 DeepSeek 分析映射到标准选项
7. **智能分级**：初筛低风险出简易报告；有风险进入全量 21 量表
8. **三层解析**：词典规则 → LLM 语义归一化 → 温和追问（≤2轮）
9. **自动计分**：按国标阈值判定风险等级，报告高亮提示
10. **评估报告**：在线预览 + PDF 导出
11. **历史记录**：MySQL 持久化，手动删除 + 30天自动清理
12. **三档字号**：标准/放大(1.6×)/特大(2.4×)，老人友好
13. **后台管理**：量表/题目/词典/话术/参数/用户/统计/日志
14. **安全降级**：AI 故障自动降级词典匹配；AES 加密敏感数据
15. **移动端适配**：小屏自动调整间距字号

---

## 六、目录结构

```
medical/
├── frontend/              # Vue3 前端
│   └── src/{views,components,api,store,utils,router}
├── backend/               # Spring Boot 后端
│   └── src/main/java/com/geriatric/assessment/
│       {controller,service,repository,entity,dto,config,util,client,scheduler}
├── ai-service/            # Python FastAPI AI 服务
│   └── app/{llm,semantic,config,main.py}
├── deploy/sql/            # 建表 + 种子数据
│   ├── schema.sql         # 10张表结构
│   ├── seed.sql           # 21套量表元数据
│   └── seed_questions.sql # 99道题目
├── start_all.bat          # 一键启动（询问MySQL密码+自动建表）
├── stop_all.bat           # 一键停止
├── start_all.ps1
├── stop_all.ps1
├── agent.md               # AI 系统提示词规范文档
└── README.md
```

---

## 七、快速开始

### 7.1 环境要求
- JDK 17
- Python 3.10+
- Node.js 18+
- MySQL 8.x

### 7.2 首次部署

```powershell
# 1. 安装依赖
cd frontend && npm install
cd ..\backend && mvn clean package -DskipTests
cd ..\ai-service && pip install -r requirements.txt

# 2. 设置 DeepSeek API Key（系统环境变量）
[Environment]::SetEnvironmentVariable("DEEP_SEEK_API_KEY","sk-你的key","User")

# 3. 一键启动（自动询问MySQL密码、自动建库建表）
#    双击 start_all.bat
```

### 7.3 之后每次启动
双击 `start_all.bat`，自动：
1. 检查 MySQL 是否运行
2. 询问 MySQL root 密码
3. 检查数据库是否存在，不存在自动创建
4. 检查量表表是否存在，不存在自动导入 21 套量表 + 99 道题
5. 启动后端(8080) / AI服务(8000) / 前端(5173)
6. 健康检查并打印访问地址

停止：双击 `stop_all.bat`。

### 7.4 访问
浏览器打开 http://localhost:5173

测试账号：
- 张大爷 / `440102195001011231`
- 李奶奶 / `440102195001011258`

---

## 八、配置说明

| 环境变量 | 说明 |
|---|---|
| `DB_PASSWORD` | MySQL root 密码（启动时询问） |
| `DEEP_SEEK_API_KEY` | DeepSeek API 密钥（**必填**，系统环境变量） |
| `ADMIN_TOKEN` | 后台管理令牌（不设则后台拒绝访问） |
| `AES_KEY` | 敏感字段加密密钥（生产必改） |

---

## 九、重要提示

- **量表权威性**：21 套量表严格按 WS/T 888—2026 附录 B 录入
- **AI 提示词**：见 [agent.md](./agent.md)，强约束仅在给定选项内选择
- **数据删除边界**：用户测试结果（task/answer/report）可删；量表定义/题目/词典等固定配置不动
- **密钥安全**：API Key 只放系统环境变量，不提交 Git
- **报告免责**：所有报告页面固定展示"本结果不构成医疗诊断"
