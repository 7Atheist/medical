package com.geriatric.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 量表配置实体：21套量表的元信息与计分/风险规则。
 *
 * <p>scoring_rule / risk_rule 以 JSON 存储，后台可导入维护：
 * <pre>
 *   risk_rule 示例（FRAIL）：
 *   {"levels":[{"max":0,"level":"无衰弱","risk":0,"text":"...","suggestion":"..."},
 *              {"min":1,"max":2,"level":"衰弱前期","risk":1,...},
 *              {"min":3,"level":"衰弱","risk":2,...}]}
 * </pre>
 * is_screening=1 的量表参与初筛，先于其他量表执行。</p>
 */
@Getter
@Setter
@Entity
@Table(name = "scale")
public class Scale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 量表编码，如 FRAIL */
    @Column(nullable = false, length = 32, unique = true)
    private String code;

    /** 量表名称 */
    @Column(nullable = false, length = 128)
    private String name;

    /** 标准表号，如 表B.10 */
    @Column(name = "table_no", length = 16)
    private String tableNo;

    /** 评估类别 */
    @Column(nullable = false, length = 64)
    private String category;

    /** 是否初筛量表 */
    @Column(name = "is_screening", nullable = false)
    private Boolean screening = false;

    /** 执行顺序 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 计分规则 JSON */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "scoring_rule", columnDefinition = "json")
    private String scoringRule;

    /** 风险判定规则 JSON */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "risk_rule", columnDefinition = "json")
    private String riskRule;

    /** 评估内容说明 */
    @Column(columnDefinition = "text")
    private String description;

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
