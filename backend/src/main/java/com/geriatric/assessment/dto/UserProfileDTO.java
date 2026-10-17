package com.geriatric.assessment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户画像（由身份证号自动解析，注入AI上下文以个性化问诊）。
 */
@Data
@Builder
public class UserProfileDTO {

    private Long userId;
    private String name;
    /** 脱敏身份证号 */
    private String idCardMask;
    private String birthDate;
    private Integer age;
    /** 1男 2女 */
    private Integer gender;
    /** 性别文本 */
    private String genderText;
    /** 称谓（根据年龄性别生成，如 爷爷/奶奶/大爷/大妈） */
    private String title;
}
