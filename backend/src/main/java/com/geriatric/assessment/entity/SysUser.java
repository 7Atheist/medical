package com.geriatric.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实名用户实体。
 *
 * <p>以"姓名＋身份证号"核验登录，无账号密码。身份证号以 AES 密文存储，
 * 另存脱敏串用于界面展示。出生日期、年龄、性别由身份证号自动解析。</p>
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 姓名 */
    @Column(nullable = false, length = 64)
    private String name;

    /** 身份证号密文 */
    @Column(name = "id_card", nullable = false, length = 256)
    private String idCard;

    /** 身份证号脱敏串 */
    @Column(name = "id_card_mask", nullable = false, length = 32)
    private String idCardMask;

    /** 出生日期 */
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    /** 年龄 */
    @Column(nullable = false)
    private Integer age;

    /** 性别：1男 2女 */
    @Column(nullable = false)
    private Integer gender;

    /** 联系电话（可选，密文） */
    private String phone;

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
