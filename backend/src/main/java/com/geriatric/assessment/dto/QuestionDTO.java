package com.geriatric.assessment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 展示给老人的当前题目DTO（含关怀式话术与选项）。
 */
@Data
@Builder
public class QuestionDTO {

    private Long questionId;
    private Long scaleId;
    /** 量表名称 */
    private String scaleName;
    /** 标准表号 */
    private String tableNo;
    /** 题型：CHOICE选择题 / OPEN开放题 */
    private String questionType;
    /** 关怀式提问文案（若为空回退到原始题干） */
    private String questionText;
    /** 该题在量表中的序号（从1开始） */
    private Integer questionIndex;
    /** 量表总题数 */
    private Integer questionTotal;
    /** 选项列表 [{value,label}] */
    private List<OptionDTO> options;
    /** 是否为初筛阶段 */
    private Boolean screening;
}

