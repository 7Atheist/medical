package com.geriatric.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交答案请求。
 *
 * <p>answerSource 说明：
 * OPTION - 老人手动直接勾选选项（无需AI归一化，最稳定）
 * TEXT   - 手动输入文本（需AI语义归一化）
 * VOICE  - 语音作答（语音转写文本需AI语义归一化）
 * </p>
 */
@Data
public class SubmitAnswerRequest {

    @NotNull(message = "缺少题目ID")
    private Long questionId;

    /** 答案来源：OPTION / TEXT / VOICE */
    private String answerSource = "OPTION";

    /** OPTION 时：老人勾选的标准选项值 */
    private String optionValue;

    /** TEXT/VOICE 时：原始输入文本（VOICE时为语音转写文本） */
    private String text;

    /** 老人表示疲惫/暂停 */
    private Boolean pause = false;
}
