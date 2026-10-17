"""
三层语义解析引擎（对应方案 6.3）。

层1  规则词典匹配：优先、稳定、零成本，命中即返回高置信选项。
层2  限定域 LLM 语义归一化：词典未命中时启用，强约束提示词限定量表选项。
层3  追问收敛：匹配置信度不足或无法判定时，生成温和追问（次数由后端控制）。

所有选项值必须来自题目给定的 options 集合，禁止越界/编造。
"""
import re

from ..config import settings
from ..llm import client as llm_client
from . import dictionary as dict_rules


class NormalizeResult:
    def __init__(self):
        self.option_value = None
        self.option_text = None
        self.confidence = 0.0
        self.needs_followup = False
        self.followup_question = ""
        self.llm_used = False

    def to_dict(self):
        return {
            "option_value": self.option_value,
            "option_text": self.option_text,
            "confidence": round(self.confidence, 3),
            "needs_followup": self.needs_followup,
            "followup_question": self.followup_question,
            "llm_used": self.llm_used,
        }


def _option_value_set(options):
    """返回合法选项值集合，用于越界校验"""
    return {str(o.get("value")) for o in options}


def _option_label(options, value):
    for o in options:
        if str(o.get("value")) == str(value):
            return o.get("label", "")
    return None


# ---------------------------------------------------------------
# 层1：规则词典匹配
# ---------------------------------------------------------------
def _rule_match(question_text, options, user_text, dictionaries):
    """
    dictionaries: 后端传入的题目级+全局词典条目
      [{option_value, synonyms(逗号分隔), match_type}]
    命中返回 (value, label)；未命中返回 (None, None)
    """
    text = (user_text or "").strip()

    # 先匹配后端传入词典
    if dictionaries:
        for d in dictionaries:
            syns = [s.strip() for s in str(d.get("synonyms", "")).split(",") if s.strip()]
            mtype = d.get("match_type", "CONTAINS")
            for syn in syns:
                hit = False
                if mtype == "EQUALS":
                    hit = text == syn
                elif mtype == "REGEX":
                    try:
                        hit = re.search(syn, text) is not None
                    except Exception:  # noqa: BLE001
                        hit = False
                else:  # CONTAINS
                    hit = syn in text
                if hit:
                    return d.get("option_value"), _option_label(options, d.get("option_value"))
        return None, None

    # 兜底：内置通用词典（仅适用于 是/否 与 频度/程度 类）
    option_values = _option_value_set(options)
    if option_values in ({"0", "1"}, {"1", "0"}):
        if dict_rules.matches_keyword(text, dict_rules.GENERAL_YES):
            return "1", _option_label(options, "1")
        if dict_rules.matches_keyword(text, dict_rules.GENERAL_NO):
            return "0", _option_label(options, "0")
    return None, None


# ---------------------------------------------------------------
# 层2：LLM 语义归一化
# ---------------------------------------------------------------
def _llm_match(question_text, options, user_text, profile, history):
    if not llm_client.llm_available():
        return None
    raw = llm_client.call_llm(question_text, options, user_text, profile, history)
    if not raw:
        return None
    return raw


# ---------------------------------------------------------------
# 主入口：三层解析
# ---------------------------------------------------------------
def normalize(question_text, dialog_text, options, user_text, profile, history, dictionaries):
    """
    归一化入口。
    options: [{value,label}]
    dictionaries: 后端传入的词典条目（可空）
    """
    result = NormalizeResult()
    user_text = (user_text or "").strip()

    # 开放题（无选项）：无需归一化，由后端直接记录
    if not options:
        result.confidence = 1.0
        result.option_text = user_text
        return result.to_dict()

    # ---------- 层1：规则词典 ----------
    value, label = _rule_match(question_text, options, user_text, dictionaries)
    if value is not None:
        result.option_value = str(value)
        result.option_text = label or _option_label(options, value)
        result.confidence = settings.dict_confidence
        return result.to_dict()

    # ---------- 层2：LLM ----------
    raw = _llm_match(question_text, options, user_text, profile, history)
    if raw:
        result.llm_used = True
        v = raw.get("option_value")
        conf = float(raw.get("confidence", 0.0))
        if v is not None and str(v) in _option_value_set(options):
            # 命中合法选项
            result.option_value = str(v)
            result.option_text = _option_label(options, v) or raw.get("option_text")
            result.confidence = conf
            if conf >= settings.confidence_threshold:
                return result.to_dict()
            # 置信度不足 → 追问
            result.needs_followup = True
            result.followup_question = raw.get("followup_question") or _default_followup(dialog_text)
            return result.to_dict()
        # LLM 判定需追问或无法判定
        result.needs_followup = True
        result.followup_question = raw.get("followup_question") or _default_followup(dialog_text)
        return result.to_dict()

    # ---------- 层3：LLM 不可用且词典未命中 → 需追问（后端可降级静态表单） ----------
    result.needs_followup = True
    result.followup_question = _default_followup(dialog_text)
    return result.to_dict()


def _default_followup(dialog_text):
    return "我有点没听明白，您可以再说得清楚一些吗？比如直接告诉我是或者不是。" if dialog_text else \
        "能再跟您确认一下吗？您刚才说的，我更想听您直接回答是或不是。"
