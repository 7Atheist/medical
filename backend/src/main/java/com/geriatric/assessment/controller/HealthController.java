package com.geriatric.assessment.controller;

import com.geriatric.assessment.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查接口（Nginx/运维探活用，无需鉴权）。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.ok(Map.of("status", "UP", "service", "assessment-backend"));
    }
}
