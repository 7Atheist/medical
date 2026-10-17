"""
自助式老年综合评估对话系统 - AI与语音独立服务 (FastAPI)

启动：uvicorn app.main:app --host 0.0.0.0 --port 8000

接口：
  GET  /api/ai/health              探活
  POST /api/ai/normalize           语义归一化（词典规则 + LLM + 追问）
  POST /api/ai/fsm/next            量表状态机推进
  POST /api/ai/asr/transcribe      语音转写（provider 可配置）
  POST /api/ai/tts/synthesize      语音合成（provider 可配置）
"""
from fastapi import FastAPI
from pydantic import BaseModel, Field
from typing import List, Optional, Dict

from .config import settings
from .semantic import engine as semantic_engine
from .fsm import fsm as fsm_engine
from .asr import service as asr_service
from .tts import service as tts_service
from .llm import client as llm_client

app = FastAPI(
    title=settings.app_name,
    version="1.0.0",
    description="自助式老年综合评估对话系统 - AI与语音独立服务",
)


# ---------------- 请求模型 ----------------
class OptionItem(BaseModel):
    value: str
    label: str


class DictItem(BaseModel):
    option_value: str
    synonyms: str = ""
    match_type: str = "CONTAINS"


class NormalizeRequest(BaseModel):
    question_text: str = ""
    dialog_text: str = ""
    options: List[OptionItem] = []
    user_text: str = ""
    profile: Optional[Dict] = None
    history: List[Dict] = []
    dictionaries: List[DictItem] = []


class FsmRequest(BaseModel):
    questions: List[Dict] = []
    current_question_id: Optional[int] = None
    chosen_value: Optional[str] = None


class AsrRequest(BaseModel):
    audio_base64: str = ""
    format: str = "wav"


class TtsRequest(BaseModel):
    text: str = ""


# ---------------- 统一响应 ----------------
def ok(data):
    return {"code": 0, "message": "success", "data": data}


# ---------------- 路由 ----------------
@app.get("/api/ai/health")
def health():
    return ok({
        "status": "up",
        "llm_enabled": llm_client.llm_available(),
        "asr_provider": settings.asr_provider,
        "tts_provider": settings.tts_provider,
    })


@app.post("/api/ai/normalize")
def normalize(req: NormalizeRequest):
    options = [{"value": o.value, "label": o.label} for o in req.options]
    dictionaries = [{"option_value": d.option_value, "synonyms": d.synonyms,
                     "match_type": d.match_type} for d in req.dictionaries]
    result = semantic_engine.normalize(
        question_text=req.question_text,
        dialog_text=req.dialog_text,
        options=options,
        user_text=req.user_text,
        profile=req.profile or {},
        history=req.history or [],
        dictionaries=dictionaries,
    )
    return ok(result)


@app.post("/api/ai/fsm/next")
def fsm_next(req: FsmRequest):
    result = fsm_engine.next_step(req.questions, req.current_question_id, req.chosen_value)
    return ok(result)


@app.post("/api/ai/asr/transcribe")
def asr_transcribe(req: AsrRequest):
    result = asr_service.transcribe(req.audio_base64, req.format)
    return ok(result)


@app.post("/api/ai/tts/synthesize")
def tts_synthesize(req: TtsRequest):
    result = tts_service.synthesize(req.text)
    return ok(result)
