"""
量表状态机（FSM）：控制评估流程的题目顺序与量表分支跳转。

对应方案 6.4：跳转逻辑由量表配置驱动，状态机决定，不由大模型决定，
保证不漏题、不错跳题目。AI 只负责理解老人输入，无权修改评估流程。

说明：业务后端内置同规则的高可用状态机作为权威推进引擎（AI服务不可用时
仍可评估），本模块为该规则的 Python 独立实现，供复用与校验。
"""
import json
from typing import List, Optional


class FSMState:
    """一次推进的输入状态"""

    def __init__(self, questions: List[dict], current_question_id: Optional[int],
                 current_scale_id: Optional[int]):
        """
        questions: 当前量表的启用题目列表 [{id, sort_order, jump_rule?}]
        """
        self.questions = sorted(questions, key=lambda q: q.get("sort_order", 0))
        self.current_question_id = current_question_id
        self.current_scale_id = current_scale_id

    def index_of(self, question_id):
        for i, q in enumerate(self.questions):
            if q.get("id") == question_id:
                return i
        return -1

    def next_question(self, current_id: Optional[int], chosen_value: Optional[str]):
        """
        依据跳转规则或顺序返回下一题。
        返回 (next_question_id or None, scale_done: bool)
        """
        idx = self.index_of(current_id)
        if idx < 0:
            # 找不到当前题 → 回到第一题
            if self.questions:
                return self.questions[0].get("id"), False
            return None, True

        # 优先应用跳转规则
        q = self.questions[idx]
        jump = q.get("jump_rule")
        if jump and chosen_value is not None:
            next_id = self._jump_target(jump, chosen_value)
            if next_id is not None:
                return next_id, False

        # 默认顺序推进
        if idx + 1 < len(self.questions):
            return self.questions[idx + 1].get("id"), False
        return None, True  # 本量表最后一题完成

    @staticmethod
    def _jump_target(jump, chosen_value):
        if isinstance(jump, str):
            try:
                jump = json.loads(jump)
            except Exception:  # noqa: BLE001
                return None
        if not isinstance(jump, dict):
            return None
        # 形如 {"0":"5","1":"8"}：选项值 → 下一题id
        if chosen_value in jump:
            return jump[chosen_value]
        # 形如 {"default":"9"}
        return jump.get("default")


def next_step(questions, current_question_id, chosen_value):
    """
    纯函数：给定当前量表的题目配置与当前题，返回下一题。
    供 FastAPI 端点与单元测试复用。
    """
    fsm = FSMState(questions, current_question_id, None)
    next_id, scale_done = fsm.next_question(current_question_id, chosen_value)
    return {
        "next_question_id": next_id,
        "scale_done": scale_done,
    }
