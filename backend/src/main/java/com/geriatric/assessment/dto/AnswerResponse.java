package com.geriatric.assessment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 提交答案后的响应。
 *
 * <p>type：QUESTION-进入下一题 / FOLLOWUP-需要追问 / DONE-评估完成</p>
 */
@Data
@Builder
public class AnswerResponse {

    /** QUESTION / FOLLOWUP / DONE */
    private String type;
    /** 友好提示文案 */
    private String message;
    /** 下一题（FOLLOWUP时为追问问题，QUESTION时为下一题） */
    private QuestionDTO question;
    /** 评估完成时报告ID */
    private Long reportId;
    /** 是否刚完成初筛 */
    private Boolean screeningDone;
    /** 初筛风险：0低风险 1存在风险 */
    private Integer screeningRisk;
    /** 是否因多次追问仍无法确定而标记答案不确定 */
    private Boolean uncertain;
}
