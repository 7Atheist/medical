package com.geriatric.assessment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应：返回会话 token 与用户画像。
 */
@Data
@Builder
public class LoginResponse {

    private String token;
    private UserProfileDTO profile;
}
