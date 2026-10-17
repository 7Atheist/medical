package com.geriatric.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评估答案实体：记录每一轮问答的原始输入与归一化结果。
 *
 * <p>user_text/asr_text 为老人原始输入（AES密文）；normalized_value/normalized_text
 * 为映射到量表标准选项后的结果；confidence 为匹配置信度；uncertain 标记是否
 * 因多次追问仍无法确定而标记"答案不确定"。</p>
 */
@Getter
@Setter
@Entity
@Table(name = "assessment_answer")
public class AssessmentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "scale_id")
    private Long scaleId;

    @Column(name = "question_id")
    private Long questionId;

    /** 老人原始输入（密文） */
    @Column(name = "user_text", columnDefinition = "text")
    private String userText;

    /** 语音识别转写文本（密文） */
    @Column(name = "asr_text", columnDefinition = "text")
    private String asrText;

    /** 归一化后的标准选项值 */
    @Column(name = "normalized_value", length = 64)
    private String normalizedValue;

    /** 归一化后的标准选项文本 */
    @Column(name = "normalized_text", length = 128)
    private String normalizedText;

    /** 匹配置信度 0~1 */
    private BigDecimal confidence;

    /** 是否标记答案不确定 */
    @Column(nullable = false)
    private Boolean uncertain = false;

    /** 是否为追问轮次 */
    @Column(name = "is_followup", nullable = false)
    private Boolean followup = false;

    /** 追问所属的首次作答ID */
    @Column(name = "followup_of")
    private Long followupOf;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
