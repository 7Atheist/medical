package com.geriatric.assessment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义应用配置（读取 application.yml 的 app.* 前缀）。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** AES 加密密钥 */
    private String aesKey;

    /** Python AI 与语音服务地址 */
    private String aiServiceUrl;

    /** 登录会话有效期(小时) */
    private Integer sessionExpireHours = 72;

    /** 报告 PDF 存储目录 */
    private String reportPdfDir = "./reports";

    /** 后台管理令牌（生产通过环境变量 ADMIN_TOKEN 注入，留空则禁止后台访问） */
    private String adminToken = "";
}
