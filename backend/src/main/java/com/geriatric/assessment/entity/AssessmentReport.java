package com.geriatric.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 评估报告实体：评估完成后生成的可视化报告。
 */
@Getter
@Setter
@Entity
@Table(name = "assessment_report")
public class AssessmentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 报告内容 JSON */
    @Column(name = "content_json", nullable = false, columnDefinition = "json")
    private String contentJson;

    /** PDF 文件路径 */
    @Column(name = "pdf_path", length = 512)
    private String pdfPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
