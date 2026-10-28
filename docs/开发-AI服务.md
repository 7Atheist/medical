# 板块③ AI 服务开发说明（Python FastAPI）

> 你负责 AI 与语音服务，**同时承担部署脚本与文档维护**（平衡工作量）。
> 技术栈 Python 3.10+ + FastAPI + DeepSeek API。
> 接口契约见 [API.md](./API.md)。

## 你的职责范围
- `ai-service/app/` 全部 AI 逻辑
- LLM 调用（DeepSeek）：语义归一化、话术包装
- 语义词典规则匹配（第一层）
- 量表状态机（FSM）
- 语音识别/合成（浏览器方案为主，服务端可选）
- **额外承担**：`deploy/` 数据库脚本 + 一键启动脚本 + `README.md`/`agent.md` 文档维护

## 你的主要文件
```
ai-service/app/
├─ main.py               # ★ FastAPI 服务入口，所有路由
├─ config.py             # 环境变量配置（DeepSeek key 等）
├─ llm/client.py         # ★ LLM 客户端：SYSTEM_PROMPT / WRAP_SYSTEM_PROMPT / 话术缓存
├─ semantic/
│  ├─ engine.py          # ★ 语义归一化引擎（词典→LLM→追问）
│  └─ dictionary.py      # 词典规则匹配
├─ fsm/fsm.py            # 量表状态机
├─ asr/service.py        # 语音识别（可选服务商）
├─ tts/service.py        # 语音合成
└─ data/wrap_cache.json  # 话术包装缓存（运行时生成，可删）
```

## 启动与验证
```powershell
cd ai-service
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000   # :8000

# 健康检查
Invoke-RestMethod http://localhost:8000/api/ai/health
# 应返回 {"code":0,...,"data":{"status":"up","llm_enabled":true,...}}
```

## 关键约定
1. **系统提示词强约束**：AI 只能在给定选项内选、不决定流程、含糊就追问、禁止编造医学结论（见 `agent.md`）。
2. **提示词改动**：必须先改 `agent.md` 再改 `client.py`，然后跑回归用例：
   - 老人答"我还行吧" → `needs_followup=true` + 追问
   - 老人答"是，经常累" → 映射到"是"
   - 老人聊家常 → 温和拉回，不强行给分
3. **归一化温度 0.1**（确定性），**话术包装温度 0.7**（略有变化）。
4. **话术缓存**：同一题干+称呼只调一次 LLM，存 `data/wrap_cache.json`，避免重复付费。
5. **降级兜底**：DeepSeek 不可用 → 返回 None，后端自动降级词典匹配；超时 5 秒。
6. **不存敏感数据**：不传身份证号，只传称谓；不持久化对话日志。
7. **DeepSeek key** 从系统环境变量 `DEEP_SEEK_API_KEY` 读取，不写进代码。

## 额外承担（平衡工作量）
- `deploy/sql/`：数据库脚本（schema/seed/seed_questions），与后端共同维护
- `start_all.bat` / `stop_all.bat`：一键启停脚本
- `README.md`：项目手册
- `agent.md`：AI 系统提示词规范

## 接口 owner 沟通
- AI 服务接口 `/api/ai/*` 由你定义，后端通过 `AiServiceClient` 调用
- 想改语义归一化返回字段 → 找接口 owner（板块②）确认并更新 [API.md](./API.md)
