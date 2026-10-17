# -*- coding: utf-8 -*-
"""
AI 服务核心逻辑冒烟测试（不依赖数据库）：
  1. 语义归一化：词典命中 / LLM未配置时追问 / 开放题
  2. 量表状态机：顺序推进 / 跳转规则
运行：python -m tests.smoke_test
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from app.semantic import engine
from app.fsm import fsm

PASS = 0


def check(name, cond, detail=""):
    global PASS
    if cond:
        PASS += 1
        print(f"[OK]   {name}")
    else:
        print(f"[FAIL] {name} {detail}")
        sys.exit(1)


def test_normalize():
    opts = [{"value": "0", "label": "否"}, {"value": "1", "label": "是"}]
    # 1) 词典命中（后端传入词典）
    r = engine.normalize("近3个月体重是否下降", "", opts, "瘦了五六斤",
                         {"age": 72, "gender": "女", "title": "奶奶"}, [],
                         [{"option_value": "1", "synonyms": "瘦了,下降,轻了", "match_type": "CONTAINS"}])
    check("词典命中 -> option_value=1", r["option_value"] == "1", str(r))
    check("词典命中 -> confidence>=0.9", r["confidence"] >= 0.9, str(r))

    # 2) 词典未命中且LLM未启用 -> 需追问（不强行选）
    r2 = engine.normalize("最近睡得好吗", "", opts, "我就想聊聊天",
                          {"age": 70, "gender": "男", "title": "爷爷"}, [], [])
    check("词典未命中且无LLM -> needs_followup", r2["needs_followup"] is True, str(r2))
    check("词典未命中 -> 不强行给选项", r2["option_value"] is None, str(r2))

    # 3) 开放题（无选项）-> 原样记录
    r3 = engine.normalize("画钟试验", "", [], "我画了个圆和指针", {}, [], [])
    check("开放题 -> 原样记录", r3["option_text"] == "我画了个圆和指针", str(r3))


def test_fsm():
    questions = [
        {"id": 1, "sort_order": 1, "jump_rule": None},
        {"id": 2, "sort_order": 2, "jump_rule": {"0": 4}},   # 选项0跳到题4
        {"id": 3, "sort_order": 3, "jump_rule": None},
        {"id": 4, "sort_order": 4, "jump_rule": None},
    ]
    # 顺序推进
    r1 = fsm.next_step(questions, 1, None)
    check("题1 -> 顺序到题2", r1["next_question_id"] == 2, str(r1))
    # 跳转规则
    r2 = fsm.next_step(questions, 2, "0")
    check("题2选0 -> 跳转到题4", r2["next_question_id"] == 4, str(r2))
    r3 = fsm.next_step(questions, 2, "1")
    check("题2选1 -> 顺序到题3", r3["next_question_id"] == 3, str(r3))
    # 最后一题完成
    r4 = fsm.next_step(questions, 4, None)
    check("最后一题 -> scale_done", r4["scale_done"] is True, str(r4))


if __name__ == "__main__":
    test_normalize()
    test_fsm()
    print(f"\n全部通过：{PASS} 项")
