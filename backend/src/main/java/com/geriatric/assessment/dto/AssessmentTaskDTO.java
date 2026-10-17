package com.geriatric.assessment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评估任务DTO（列表/历史/草稿展示）。
 */
@Data
@Builder
public class AssessmentTaskDTO {

    private Long taskId;
    private String status;
    private String statusText;
    private String inputMode;
    private String inputModeText;
    /** 当前量表名称 */
    private String currentScaleName;
    /** 进度文案，如 "完成 5/13 题" */
    private String progressText;
    private LocalDateTime createdAt;
    private LocalDateTime lastAnswerAt;
    private Integer screeningRisk;
}
