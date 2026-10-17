package com.geriatric.assessment.controller;

import com.geriatric.assessment.common.Result;
import com.geriatric.assessment.dto.LoginRequest;
import com.geriatric.assessment.dto.LoginResponse;
import com.geriatric.assessment.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：以"姓名＋身份证号"实名登录（无账号密码）。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录。成功后返回 token 与自动解析的用户画像。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }
}
