"""
语音识别（ASR）服务。

设计：
  - browser ：前端使用浏览器 Web Speech API 识别（零成本、免费、中文支持好，推荐默认）
  - cloud   ：对接云服务商（阿里云/腾讯云等），通过 ASR_CLOUD_URL/ASR_CLOUD_TOKEN 配置
  - local   ：预留本地 Whisper 等（需自行安装模型）

所有 provider 在未配置或失败时返回 None，由调用方降级为"先回显文本让老人确认"或
静态表单直接选择，符合方案"语音识别失败时自动降级为文字选择题"的要求。
"""
import base64
import time

from ..config import settings


def transcribe(audio_base64: str, format_hint: str = "wav") -> dict:
    """
    转写音频为文本。
    audio_base64: 音频二进制(base64)
    format_hint : wav / mp3 / webm 等
    返回 {"text": str}；失败返回 {"text": ""}。
    """
    provider = settings.asr_provider
    if provider == "cloud":
        return _cloud_asr(audio_base64, format_hint)
    if provider == "local":
        return _local_asr(audio_base64, format_hint)
    # browser / 默认：本服务不做识别，交给前端浏览器 ASR
    return {"text": "", "note": "browser asr"}


def _cloud_asr(audio_base64: str, fmt: str) -> dict:
    """对接云服务商的示例骨架：请按所选云厂商 API 填写。"""
    if not settings.asr_cloud_url or not settings.asr_cloud_token:
        return {"text": "", "note": "cloud asr 未配置"}
    try:
        import requests
        resp = requests.post(
            settings.asr_cloud_url,
            json={"audio_base64": audio_base64, "format": fmt},
            headers={"Authorization": f"Bearer {settings.asr_cloud_token}"},
            timeout=20,
        )
        resp.raise_for_status()
        data = resp.json()
        return {"text": data.get("text") or data.get("result") or ""}
    except Exception as e:  # noqa: BLE001
        return {"text": "", "note": str(e)}


def _local_asr(audio_base64: str, fmt: str) -> dict:
    """预留本地 Whisper：可按需接入 faster-whisper / openai-whisper。"""
    return {"text": "", "note": "local asr 未安装模型"}
