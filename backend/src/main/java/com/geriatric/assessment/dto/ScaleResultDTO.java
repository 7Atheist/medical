package com.geriatric.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单量表评估结果DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleResultDTO {
    private String scaleName;
    private String tableNo;
    private String category;
    /** 得分 */
    private Double score;
    /** 满分（null表示无固定满分） */
    private Double fullScore;
    /** 风险等级文本，如 无衰弱 / 衰弱前期 / 衰弱 */
    private String riskLevel;
    /** 风险等级数值 0~N（0=正常） */
    private Integer risk;
    /** 风险说明 */
    private String riskText;
    /** 健康生活建议 */
    private List<String> suggestions;
}
