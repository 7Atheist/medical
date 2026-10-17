package com.geriatric.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 自助式老年综合评估对话系统 - 业务后端启动类
 *
 * <p>技术栈：Java 17 + Spring Boot 3 + Spring Data JPA + MySQL</p>
 *
 * <p>职责：实名登录、评估任务管理、量表计分与风险判定、评估报告生成、
 * 后台管理、草稿生命周期清理，并作为网关调用 Python FastAPI 的
 * AI 语义归一化 / FSM / ASR / TTS 独立服务。</p>
 */
@SpringBootApplication
@EnableScheduling
public class GeriatricAssessmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeriatricAssessmentApplication.class, args);
    }
}
