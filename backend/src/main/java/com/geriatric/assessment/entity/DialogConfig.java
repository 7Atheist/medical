package com.geriatric.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 对话话术配置实体：关怀式问候、重复播报、追问、疲劳提示等场景话术。
 */
@Getter
@Setter
@Entity
@Table(name = "dialog_config")
public class DialogConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 场景编码：GREETING/REPEAT/FOLLOWUP/FATIGUE/UNCERTAIN/GUIDE/DISCLAIMER/PAUSE/SCREENING_PASS/SCREENING_RISK */
    @Column(nullable = false, length = 32)
    private String scene;

    /** 话术内容，支持 {name}、{question} 等占位符 */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
