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
import requests

from ..config import settings

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
