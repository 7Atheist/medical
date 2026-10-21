# 自助式老年综合评估对话系统

依据 WS/T 888—2026《医疗机构老年综合评估技术操作标准》实现的全栈 + AI 项目。
面向社区居家老年人，凭「姓名＋身份证号」实名登录即可自助完成整套老年综合评估。
聊天式交互，支持点选选项 / 语音回答 / 文字输入，AI 负责口语化理解与话术包装。

> **重要声明**：本系统为自助健康调查工具，结果仅作个人健康参考，不构成医疗诊断，不替代医生诊疗。

---

## 一、技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue3 + Vite + Element Plus + Pinia + Vue Router + Axios + IndexedDB |
| 业务后端 | Java 17 + Spring Boot 3.2 + Spring Data JPA + MySQL 8 |
| AI/语音服务 | Python 3.10+ + FastAPI + DeepSeek API（OpenAI 兼容） |
| 大模型 | DeepSeek（`deepseek-chat`，低温度 + JSON 结构化输出） |
| 存储 | MySQL（敏感字段 AES 加密） |
| 部署 | Windows 一键启停脚本（`start_all.bat` / `stop_all.bat`） |

## 二、系统架构

```
┌─────────────────────────────────────────────┐
│         前端 Vue3 (:5173)                   │
│  登录 / 聊天式评估 / 报告 / 历史 / 后台     │
│  三档字号 · 浏览器ASR/TTS · 离线草稿        │
└──────────────┬──────────────────────────────┘
               │ /api 直连
┌──────────────▼──────────────────────────────┐
│    业务后端 Spring Boot (:8080)             │
│  实名登录 · 画像解析 · 量表状态机           │
│  计分风险 · 报告PDF · 历史清理 · AES加密   │
└───────┬──────────────────────┬──────────────┘
        │                    │
┌───────▼──────┐   ┌────────▼───────────────┐
│  MySQL 8     │   │  AI 服务 FastAPI (:8000) │
│  21套量表    │   │  语义归一化 · 话术包装   │
│  99道题      │   │  DeepSeek LLM 调用      │
└──────────────┘   └─────────────────────────┘
```

## 三、功能清单

1. **实名登录**：姓名＋身份证号，自动解析年龄/性别/称谓
2. **聊天式评估**：聊天气泡流，护士头像提问、选项点选即提交
3. **生活化话术包装**：AI 把医学题干改成聊天开场白（"最近睡得踏实不？"）
4. **统一输入**：每题同时支持点选选项 / 语音回答 / 文字输入
5. **语音识别**：点击开始录音、再点停止，识别文本交 AI 分析
6. **AI 语义归一化**：口语化回答交 DeepSeek 分析映射到标准选项，不猜不编
7. **智能分级**：初筛低风险出简易报告；有风险进入全量 21 量表
8. **三层解析**：词典规则 → LLM 语义归一化 → 温和追问（≤2轮）
9. **自动计分**：按国标阈值判定风险等级，报告高亮提示
10. **评估报告**：在线预览 + PDF 导出（跨平台中文字体）
11. **历史记录**：MySQL 持久化，手动删除 + 30天自动清理
12. **三档字号**：标准/放大(1.6×)/特大(2.4×)，老人友好
13. **后台管理**：量表/题目/词典/话术/参数/用户/统计/日志
14. **安全降级**：AI 故障自动降级词典匹配；AES 加密敏感数据
15. **移动端适配**：小屏自动调整间距字号

## 四、目录结构

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
│   ├── schema.sql
│   ├── seed.sql           # 21套量表元数据
│   └── seed_questions.sql # 99道题目
├── start_all.bat          # 一键启动（询问MySQL密码+自动建表）
├── stop_all.bat           # 一键停止
├── start_all.ps1
├── stop_all.ps1
├── agent.md               # AI 系统提示词文档
└── README.md
```

## 五、快速开始

### 首次部署

```powershell
# 1. 环境
#    JDK 17 + Python 3.10+ + Node.js 18+ + MySQL 8.x

# 2. 安装依赖
cd frontend && npm install
cd ..\backend && mvn clean package -DskipTests
cd ..\ai-service && pip install -r requirements.txt

# 3. 设置 DeepSeek API Key（系统环境变量）
[Environment]::SetEnvironmentVariable("DEEP_SEEK_API_KEY","sk-你的key","User")

# 4. 一键启动（自动询问MySQL密码、自动建库建表）
#    双击 start_all.bat
```

### 之后每次启动

双击 `start_all.bat`，自动：
1. 检查 MySQL 是否运行
2. 询问 MySQL root 密码
3. 检查数据库是否存在，不存在自动创建
4. 检查量表表是否存在，不存在自动导入 21 套量表 + 99 道题
5. 启动后端(8080) / AI服务(8000) / 前端(5173)
6. 健康检查并打印访问地址

停止：双击 `stop_all.bat`。

### 访问

浏览器打开 http://localhost:5173

测试账号：
- 张大爷 / `440102195001011231`
- 李奶奶 / `440102195001011258`

## 六、配置说明

| 环境变量 | 说明 |
|---|---|
| `DB_PASSWORD` | MySQL root 密码（启动时询问） |
| `DEEP_SEEK_API_KEY` | DeepSeek API 密钥（**必填**，系统环境变量） |
| `ADMIN_TOKEN` | 后台管理令牌（不设则后台拒绝访问） |
| `AES_KEY` | 敏感字段加密密钥（生产必改） |

## 七、重要提示

- **量表权威性**：21 套量表严格按 WS/T 888—2026 附录 B 录入
- **AI 提示词**：见 [agent.md](./agent.md)，强约束仅在给定选项内选择
- **数据删除边界**：用户测试结果可删；量表定义/题目/词典等固定数据不动
- **密钥安全**：API Key 只放系统环境变量，不提交 Git
