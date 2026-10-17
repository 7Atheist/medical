package com.geriatric.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求：仅需姓名＋身份证号（无需账号密码）。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "请输入姓名")
    private String name;

    @NotBlank(message = "请输入身份证号")
    private String idCard;
}
