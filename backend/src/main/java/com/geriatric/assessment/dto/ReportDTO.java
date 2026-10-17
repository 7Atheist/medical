package com.geriatric.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评估报告DTO（前端预览展示用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    private Long reportId;
    private Long taskId;
    private String userName;
    private Integer age;
    private String genderText;
    private LocalDateTime createdAt;
    /** 免责声明（固定展示） */
    private String disclaimer;
    /** 各量表评估结果 */
    private List<ScaleResultDTO> scaleResults;
    /** 总体健康生活建议 */
    private List<String> overallSuggestions;
}

