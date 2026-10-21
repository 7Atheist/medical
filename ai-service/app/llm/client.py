"""
大模型（LLM）语义归一化客户端：OpenAI 兼容接口。

调用方：app.semantic.engine 的第二层语义归一化。
设计要点：
  1. 强约束系统提示词：仅允许在题目给定选项内选择，禁止编造、禁止跑题闲聊
  2. 低温度 + JSON 结构化输出，抑制幻觉
  3. 结合用户画像（年龄/性别/称谓）调整语气，让问诊更有温度、更贴合老人
"""
import json
import time
import hashlib
import os
import requests

from ..config import settings

# ------------------------------------------------------------------
# 话术包装本地缓存：同一题干+称呼只调一次 LLM，避免重复付费调用
# 缓存文件：ai-service/data/wrap_cache.json
# ------------------------------------------------------------------
_WRAP_CACHE_FILE = os.path.join(os.path.dirname(__file__), "..", "data", "wrap_cache.json")
_WRAP_CACHE = {}

def _load_wrap_cache():
    """启动时加载缓存文件"""
    global _WRAP_CACHE
    try:
        if os.path.exists(_WRAP_CACHE_FILE):
            with open(_WRAP_CACHE_FILE, "r", encoding="utf-8") as f:
                _WRAP_CACHE = json.load(f)
    except Exception:
        _WRAP_CACHE = {}

def _save_wrap_cache():
    """写回缓存文件（轻量，题目量不大）"""
    try:
        os.makedirs(os.path.dirname(_WRAP_CACHE_FILE), exist_ok=True)
        with open(_WRAP_CACHE_FILE, "w", encoding="utf-8") as f:
            json.dump(_WRAP_CACHE, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"[wrap-cache] 写缓存失败: {e}", flush=True)

_load_wrap_cache()

# =====================================================================
# AI 系统提示词（核心约束：严格基于量表，规范语言，有温度）
# =====================================================================
SYSTEM_PROMPT = """你是"自助式老年综合评估对话系统"的健康评估助手，面向社区居家老年人开展综合健康调查。

【最重要的硬性规则】
1. 你只能把老人的回答映射到【当前题目给定的选项集合】中的某个选项值（option_value）。
   题目没给的选项，绝对不许使用；绝不能编造、推断选项。
2. 你无权决定题目跳转、顺序或评估流程，那些由系统状态机控制。你只做"语义理解与映射"。
3. 如果老人回答含糊不清、信息不足，你标记 needs_followup=true，并用温和、简短、耐心的语气追问
   （最多问一句，句子不超过40字，引导老人说出能对应到选项的信息），不要反问其他无关问题。
4. 如果老人完全答非所问、闲聊或无法判断，也标记 needs_followup=true；绝不强行选一个答案。
5. 语气要求：像晚辈和长辈聊天一样亲切、尊重、口语化，避免医学术语，避免生硬问卷感。
   结合用户画像中的年龄、性别与称谓（如"爷爷/奶奶"）调整语气，高龄老人更尊重、语速更慢。

【输出格式】
只输出一个 JSON 对象，不要输出任何其他文字、解释或 Markdown：
{
  "option_value": "<命中的选项值，未命中填 null>",
  "option_text": "<命中的选项文本，未命中填 null>",
  "confidence": <0到1的小数，表示把握程度>,
  "needs_followup": <true/false，是否需追问>,
  "followup_question": "<需追问时的一句温和追问，不需要追问时填 ''>"
}
"""


def build_user_prompt(question_text, options, user_text, profile, history):
    """构造用户侧提示词：把题目、选项、老人输入与画像注入上下文。"""
    opts_text = "\n".join(
        [f"- {o.get('value', '')}: {o.get('label', '')}" for o in options]
    ) if options else "（本题无固定选项，请原样记录老人描述）"

    profile_text = ""
    if profile:
        title = profile.get("title", "")
        name = profile.get("name", "")
        age = profile.get("age", "")
        gender = profile.get("gender", "")
        if title or name:
            profile_text = f"\n【用户画像】称谓:{title or ''} 姓名:{name or ''} 年龄:{age or ''}岁 性别:{gender or ''}"
            profile_text += "（请据此选择亲切的称呼与语气）"

    history_text = ""
    if history:
        parts = []
        for h in history[-4:]:  # 只看最近4轮
            parts.append(f"问:{h.get('question','')} 答:{h.get('answer','')}")
        history_text = "\n【最近对话】\n" + "\n".join(parts)

    return (
        f"【当前题目】{question_text}\n"
        f"【本题允许的选项】\n{opts_text}\n"
        f"{profile_text}\n"
        f"【老人刚才的回答】{user_text}\n"
        f"{history_text}\n"
        f"请把老人的回答映射到上面的某个选项，返回 JSON。"
    )


def call_llm(question_text, options, user_text, profile, history):
    """
    调用大模型做语义归一化。
    返回 dict；未配置/失败返回 None（调用方降级处理）。
    """
    if not settings.llm_enabled or not settings.llm_api_key:
        return None
    headers = {
        "Authorization": f"Bearer {settings.llm_api_key}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": settings.llm_model,
        "temperature": settings.llm_temperature,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": build_user_prompt(
                question_text, options, user_text, profile, history)},
        ],
    }
    url = settings.llm_base_url.rstrip("/") + "/chat/completions"
    try:
        resp = requests.post(url, json=payload, headers=headers, timeout=settings.llm_timeout)
        resp.raise_for_status()
        data = resp.json()
        content = data["choices"][0]["message"]["content"]
        # 兼容模型可能返回被 ```json ``` 包裹的内容
        content = content.strip()
        if content.startswith("```"):
            content = content.split("```")[1]
            if content.startswith("json"):
                content = content[4:]
        return json.loads(content)
    except Exception as e:  # noqa: BLE001
        # 记录后返回 None，交给上层降级
        print(f"[llm] 调用失败: {e}", flush=True)
        return None


def llm_available() -> bool:
    """LLM 是否可用（用于 /health 探活）"""
    return settings.llm_enabled and bool(settings.llm_api_key)


# =====================================================================
# 题目生活化包装：把医学题干改写成聊天开场白（方案核心体验）
# =====================================================================
WRAP_SYSTEM_PROMPT = """你是一位陪长辈聊天的小护士，负责把健康问题包装成拉家常的开场白。

【规则】
1. 把医学题干改写成一句亲切、生活化的聊天开场白，像晚辈跟长辈唠嗑。
2. 可以自然引入日常场景：吃饭、睡觉、看电视、散步、天气、天气变化、身体感受。
3. 不要出现"评估""问卷""量表""体检""题目"这类词，不要像在问话。
4. 30字以内，口语化，像聊天开头，不是直接提问。
5. 保持温和、有温度，像"张大爷，最近睡得踏实不？"这种感觉。
6. 不要给出选项，不要引导选答案，只是开启这个话题。
"""


def wrap_question_life(question_text, options, profile=None):
    """
    把医学题干包装成生活化聊天开场白。
    返回 str（包装后的话术）；LLM 不可用或失败时返回 None（调用方降级显示原题）。
    带本地缓存：同一题干+称呼只调一次 LLM。
    """
    if not settings.llm_enabled or not settings.llm_api_key:
        return None
    opts_text = ""
    if options:
        opts_text = "\n本题实际要了解的方面：" + "、".join(
            [f"{o.get('label','')}" for o in options]
        )
    profile_text = ""
    title = ""
    if profile:
        title = profile.get("title", "")
        if title:
            profile_text = f"\n对方称呼：{title}"
    # 缓存 key：题干 + 称呼（不同老人称呼不同，话术开头不同）
    cache_key = hashlib.md5(f"{question_text}|{title}".encode("utf-8")).hexdigest()
    if cache_key in _WRAP_CACHE:
        return _WRAP_CACHE[cache_key]

    user_msg = (
        f"原题干：{question_text}{opts_text}{profile_text}\n"
        f"请改写成一句生活化的聊天开场白（30字内）。"
    )
    headers = {
        "Authorization": f"Bearer {settings.llm_api_key}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": settings.llm_model,
        "temperature": 0.7,  # 包装话术需要一点变化，比归一化稍高
        "messages": [
            {"role": "system", "content": WRAP_SYSTEM_PROMPT},
            {"role": "user", "content": user_msg},
        ],
    }
    url = settings.llm_base_url.rstrip("/") + "/chat/completions"
    try:
        resp = requests.post(url, json=payload, headers=headers, timeout=settings.llm_timeout)
        resp.raise_for_status()
        data = resp.json()
        content = data["choices"][0]["message"]["content"].strip()
        if content:
            _WRAP_CACHE[cache_key] = content
            _save_wrap_cache()
        return content if content else None
    except Exception as e:  # noqa: BLE001
        print(f"[llm-wrap] 包装失败: {e}", flush=True)
        return None
