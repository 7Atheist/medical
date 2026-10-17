"""
自助式老年综合评估对话系统 - AI与语音独立服务 配置

技术栈：Python + FastAPI
职责  ：语义归一化（规则词典 + 大模型强约束映射）、量表状态机、ASR/TTS 接口
"""
import os
from dataclasses import dataclass, field
from typing import List


@dataclass
class Settings:
    # ---------- 服务 ----------
    app_name: str = "geriatric-ai-service"
    host: str = os.getenv("AI_HOST", "0.0.0.0")
    port: int = int(os.getenv("AI_PORT", "8000"))

    # ---------- 大模型（语义归一化第二层） ----------
    # 支持任何 OpenAI 兼容接口：DeepSeek / vLLM 私有化部署 / 阿里云百炼 等
    # 默认对接 DeepSeek（https://api.deepseek.com，OpenAI 兼容）
    llm_enabled: bool = os.getenv("LLM_ENABLED", "true").lower() in ("true", "1")
    llm_base_url: str = os.getenv("LLM_BASE_URL", "https://api.deepseek.com")
    llm_model: str = os.getenv("LLM_MODEL", "deepseek-chat")
    # 密钥读取顺序：LLM_API_KEY / DEEP_SEEK_API_KEY（推荐，用户系统变量） / DEEPSEEK_API_KEY（旧名兼容）
    llm_api_key: str = (os.getenv("LLM_API_KEY")
                        or os.getenv("DEEP_SEEK_API_KEY")
                        or os.getenv("DEEPSEEK_API_KEY")
                        or "")
    llm_timeout: float = float(os.getenv("LLM_TIMEOUT", "60"))  # 大模型响应可能较慢，放宽到60s
    llm_temperature: float = float(os.getenv("LLM_TEMPERATURE", "0.1"))  # 低温度抑制幻觉

    # ---------- 语义归一化 ----------
    # 词典匹配置信度（命中即视为高置信）
    dict_confidence: float = 0.95
    # LLM 未达到该置信度时触发追问
    confidence_threshold: float = float(os.getenv("CONFIDENCE_THRESHOLD", "0.60"))
    # 最大追问次数（与后端参数保持一致，后端为权威）
    max_followup: int = int(os.getenv("MAX_FOLLOWUP", "2"))

    # ---------- 语音 ----------
    # ASR：local=本地Whisper（需额外安装）/ cloud=云服务商 / browser=前端浏览器ASR（无需本服务）
    asr_provider: str = os.getenv("ASR_PROVIDER", "browser")
    asr_cloud_url: str = os.getenv("ASR_CLOUD_URL", "")
    asr_cloud_token: str = os.getenv("ASR_CLOUD_TOKEN", "")
    # TTS：browser=前端浏览器TTS（无需本服务）
    tts_provider: str = os.getenv("TTS_PROVIDER", "browser")

    # ---------- 内置兜底词典文件 ----------
    builtin_dict_path: str = os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "data", "dictionary.json")

    # 允许的选项值范围（防止越界，前端也会校验）
    allowed_option_range: List[int] = field(default_factory=lambda: [i for i in range(101)])


settings = Settings()
