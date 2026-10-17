"""
语音播报（TTS）服务。

设计：前端使用浏览器 SpeechSynthesis 中文语音播报（零成本、无需服务），
本服务仅提供配置查询与预留本地/云端 TTS 能力，确保系统可独立运行。
"""
from ..config import settings


def synthesize(text: str) -> dict:
    """合成语音。
    返回 {"audio_base64": ..., "provider": ...}；browser 模式返回空，由前端播报。
    """
    if settings.tts_provider == "browser":
        return {"audio_base64": "", "provider": "browser", "text": text}
    # 预留：本地/云端 TTS（如 edge-tts）可在此扩展
    return {"audio_base64": "", "provider": settings.tts_provider, "text": text, "note": "not implemented"}
