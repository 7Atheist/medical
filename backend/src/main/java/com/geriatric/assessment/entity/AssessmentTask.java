package com.geriatric.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 评估任务实体：一次完整评估的生命周期。
 *
 * <p>状态流转：SCREENING(初筛) → (存在风险) ASSESSING(全量评估) → COMPLETED(完成归档)
 * 或 SCREENING → 低风险 → COMPLETED(简易报告)。</p>
 *
 * <p>草稿断点：current_scale_id / current_question_id / completed_scales 记录
 * 评估进度，followup_count 记录当前题追问轮数；last_answer_at 用于草稿3天保活清理。</p>
 */
@Getter
@Setter
@Entity
@Table(name = "assessment_task")
public class AssessmentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 状态：SCREENING / ASSESSING / COMPLETED */
    @Column(nullable = false, length = 32)
    private String status = "SCREENING";

    /** 输入模式：MANUAL / VOICE */
    @Column(name = "input_mode", nullable = false, length = 16)
    private String inputMode = "MANUAL";

    @Column(name = "current_scale_id")
    private Long currentScaleId;

    @Column(name = "current_question_id")
    private Long currentQuestionId;

    /** 当前题已连续追问次数 */
    @Column(name = "followup_count", nullable = false)
    private Integer followupCount = 0;

    /** 已完成的量表ID数组（JSON，FSM推进依据） */
    @Column(name = "completed_scales", columnDefinition = "json")
    private String completedScales;

    /** 初筛是否完成 */
    @Column(name = "screening_done", nullable = false)
    private Boolean screeningDone = false;

    /** 初筛风险：0低风险 1存在风险 null未判定 */
    @Column(name = "screening_risk")
    private Integer screeningRisk;

    /** 最后一次作答时间（草稿3天保活） */
    @Column(name = "last_answer_at")
    private LocalDateTime lastAnswerAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.completedScales == null) {
            this.completedScales = "[]";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
