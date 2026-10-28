# 三方接口契约文档（API.md）

> **本文件是三板块并行开发的唯一接口依据。** 后端、前端、AI 服务三方以此为准，任何接口字段变更必须同步更新本文件。
> **接口 owner：后端（板块②）**，字段定义以 `backend/.../dto/` 为准。

## 一、总体架构与端口

| 板块 | 技术 | 端口 | 根路径 |
|---|---|---|---|
| ① 前端 | Vue3 + Vite | 5173 | `/` |
| ② 业务后端 | Spring Boot | 8080 | `/api` |
| ③ AI 服务 | FastAPI | 8000 | `/api/ai`（前端经 `/ai-api` 代理访问） |

**统一响应格式**（后端与 AI 服务一致）：
```json
{ "code": 0, "message": "success", "data": { ... } }
```
`code=0` 表示成功；非 0 表示失败，`message` 为错误提示。

**鉴权方式**：除 `/auth/login`、`/api/ai/health` 外，所有请求头需携带 `Authorization: Bearer <token>`。
前端在 `http.js` 拦截器自动附带；后端由 `config/AuthInterceptor` 校验。

---

## 二、后端接口（板块② 提供，板块① 消费）

### 2.1 认证
| 方法 | 路径 | 请求体 | 返回 data |
|---|---|---|---|
| POST | `/api/auth/login` | `{name, idCard}` | `{token, profile:{title,name,age,gender}}` |

### 2.2 评估流程
| 方法 | 路径 | 请求体 | 返回 data |
|---|---|---|---|
| POST | `/api/assess/create` | `{inputMode: "MANUAL"/"VOICE"}` | `taskId`(Long) |
| GET | `/api/assess/drafts` | - | `[{taskId,status,updatedAt,...}]` 未完成草稿 |
| GET | `/api/assess/history` | - | `[{...}]` 已完成历史 |
| DELETE | `/api/assess/{taskId}` | - | 删除记录（级联删答案+报告） |
| GET | `/api/assess/{taskId}/question` | - | `QuestionDTO` |
| POST | `/api/assess/{taskId}/answer` | `SubmitAnswerRequest` | `AnswerResponse` |
| POST | `/api/assess/{taskId}/audio` | multipart: `questionId`,`file` | `AnswerResponse`（自有ASR时用） |

**QuestionDTO** 结构：
```json
{
  "questionId": 46, "scaleId": 1, "scaleName": "FRAIL衰弱量表", "tableNo": "表B.1",
  "questionType": "CHOICE", "questionText": "关怀式提问文案（空则用原题干）",
  "questionIndex": 1, "questionTotal": 5,
  "options": [{"value":"0","label":"否","score":0}],
  "screening": true
}
```

**SubmitAnswerRequest** 结构：
```json
{ "questionId": 46, "answerSource": "OPTION", "optionValue": "1", "text": "", "pause": false }
```
- `answerSource`：`OPTION`(点选,无需AI) / `TEXT`(手输,需AI) / `VOICE`(语音,需AI)

**AnswerResponse** 结构：
```json
{
  "type": "QUESTION|FOLLOWUP|DONE",
  "message": "提示文案",
  "question": "<QuestionDTO>",   // FOLLOWUP=追问问题, QUESTION=下一题
  "reportId": 18,               // type=DONE 时
  "screeningDone": false,
  "screeningRisk": 1,           // 0低风险 1存在风险
  "uncertain": false            // 多次追问仍无法确定
}
```

### 2.3 报告
| 方法 | 路径 | 说明 | 返回 data |
|---|---|---|---|
| GET | `/api/report/task/{taskId}` | 按任务取报告 | `ReportDTO` |
| GET | `/api/report/list` | 历史报告列表 | `[{...}]` |
| GET | `/api/report/{reportId}/pdf` | 下载PDF（需 `?token=`） | PDF 文件流 |

### 2.4 后台管理（板块② 提供，需 `ADMIN_TOKEN`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/stats` | 统计 |
| GET | `/api/admin/users?name=` | 用户列表 |
| GET/POST | `/api/admin/scales` | 量表列表/保存 |
| GET/POST | `/api/admin/scales/{id}/questions`、`/api/admin/questions` | 题目 |
| GET/POST | `/api/admin/dictionary` | 语义词典 |
| GET/POST | `/api/admin/dialog` | 对话话术 |
| GET/PUT | `/api/admin/params/{key}` | 系统参数 |
| GET | `/api/admin/logs` | 日志 |

---

## 三、AI 服务接口（板块③ 提供，板块② 消费）

后端通过 `client/AiServiceClient.java`（`AI_SERVICE_URL`，默认 `http://localhost:8000`）调用。

### 3.1 探活
| 方法 | 路径 | 返回 data |
|---|---|---|
| GET | `/api/ai/health` | `{status:"up", llm_enabled, asr_provider, tts_provider}` |

### 3.2 语义归一化（核心，板块② 调用来理解老人回答）
| 方法 | 路径 |
|---|---|
| POST | `/api/ai/normalize` |

**请求体**：
```json
{
  "question_text": "原题干",
  "dialog_text": "关怀话术",
  "options": [{"value":"0","label":"否"}],
  "user_text": "老人回答（语音转写或手输）",
  "profile": {"title":"爷爷","name":"张大爷","age":76,"gender":1},
  "history": [{"question":"...","answer":"..."}],
  "dictionaries": [{"option_value":"1","synonyms":"能,行","match_type":"CONTAINS"}]
}
```
**返回 data**：
```json
{
  "option_value": "1",          // 命中的选项值，未命中 null
  "option_text": "是",
  "confidence": 0.85,           // 0~1
  "needs_followup": false,      // true 需追问
  "followup_question": ""       // needs_followup=true 时的一句追问
}
```

### 3.3 话术包装（把题干改成聊天开场白）
| 方法 | 路径 | 请求体 | 返回 data |
|---|---|---|---|
| POST | `/api/ai/wrap` | `{question_text, options, profile}` | `{original, wrapped, llm_used}` |

### 3.4 量表状态机
| 方法 | 路径 | 请求体 |
|---|---|---|
| POST | `/api/ai/fsm/next` | `{questions, current_question_id, chosen_value}` |

### 3.5 语音（自有 ASR/TTS 时用，浏览器方案无需）
| 方法 | 路径 | 请求体 | 说明 |
|---|---|---|---|
| POST | `/api/ai/asr/transcribe` | `{audio_base64, format}` | 语音转写 |
| POST | `/api/ai/tts/synthesize` | `{text}` | 语音合成 |

---

## 四、前端代理（板块① 负责，无需后端/AI 改动）

`frontend/vite.config.js`：
- `/api` → `http://localhost:8080`（后端）
- `/ai-api` → `http://localhost:8000`，转发时去 `/ai-api` 前缀（AI 服务）

前端调用 AI 用独立 axios 实例 `aiHttp`（超时 5 秒，超时降级用原题），见 `frontend/src/api/index.js`。

---

## 五、数据库 schema 归属

- **数据库属于板块②**（`backend` + `deploy/sql/`）。
- 板块③ AI 服务**只读** `assessment_answer` 的归一化字段（`normalized_value/normalized_text/confidence`），这些字段由后端在调用 `/api/ai/normalize` 后回填。
- **任何表结构变更必须由板块② owner 统一改 `deploy/sql/schema.sql`**，并同步通知另两板块。

## 六、变更流程

1. 任何一方想改接口字段 → 先提 issue 到接口 owner（板块②）。
2. owner 更新本文件 + 对应代码 → 通知另两方。
3. 另两方按新契约改，禁止各自偷偷改字段名。
