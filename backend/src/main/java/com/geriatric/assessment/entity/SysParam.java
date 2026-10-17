package com.geriatric.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统参数实体：追问最大次数、置信度阈值、草稿保留周期、AI服务配置等。
 */
@Getter
@Setter
@Entity
@Table(name = "sys_param")
public class SysParam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "param_key", nullable = false, length = 64, unique = true)
    private String paramKey;

    @Column(name = "param_value", nullable = false, length = 512)
    private String paramValue;

    private String description;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
