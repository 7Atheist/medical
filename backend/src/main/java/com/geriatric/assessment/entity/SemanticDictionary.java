package com.geriatric.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 语义映射词典实体：口语化表述 → 量表标准选项。
 *
 * <p>作为三层语义解析的第一层（规则词典匹配），稳定可控、无需调用大模型。
 * question_id 为空表示全局通用词典。</p>
 */
@Getter
@Setter
@Entity
@Table(name = "semantic_dictionary")
public class SemanticDictionary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 题目ID（NULL=全局通用） */
    @Column(name = "question_id")
    private Long questionId;

    /** 映射到的标准选项值 */
    @Column(name = "option_value", nullable = false, length = 64)
    private String optionValue;

    /** 口语同义词，逗号分隔 */
    @Column(nullable = false, length = 512)
    private String synonyms;

    /** 匹配方式：CONTAINS包含 / EQUALS完全等于 / REGEX正则 */
    @Column(name = "match_type", nullable = false, length = 16)
    private String matchType = "CONTAINS";

    /** 优先级（越小越先匹配） */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
