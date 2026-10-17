package com.geriatric.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 量表题目实体。
 *
 * <p>question_text 为量表原始题干；dialog_text 为关怀式对话文案（手动/语音模式提问）；
 * options 为选项 JSON 数组：[{"value","label","score"}]；jump_rule 为跳转规则 JSON。</p>
 */
@Getter
@Setter
@Entity
@Table(name = "scale_question")
public class ScaleQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scale_id", nullable = false)
    private Long scaleId;

    /** 题型：CHOICE选择题 / OPEN开放题 */
    @Column(name = "question_type", nullable = false, length = 16)
    private String questionType = "CHOICE";

    /** 量表原始题干 */
    @Column(name = "question_text", nullable = false, columnDefinition = "text")
    private String questionText;

    /** 关怀式对话文案 */
    @Column(name = "dialog_text", columnDefinition = "text")
    private String dialogText;

    /** 选项 JSON */
    @Column(columnDefinition = "json")
    private String options;

    /** 跳转规则 JSON */
    @Column(name = "jump_rule", columnDefinition = "json")
    private String jumpRule;

    /** 题目顺序 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
