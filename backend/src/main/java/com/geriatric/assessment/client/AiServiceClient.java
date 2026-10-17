package com.geriatric.assessment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geriatric.assessment.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 与语音服务客户端：调用 Python FastAPI 独立服务。
 *
 * <p>接口：</p>
 * <ul>
 *   <li>POST {ai}/api/ai/normalize —— 语义归一化（词典规则 + LLM），将老人口语映射到标准选项</li>
 *   <li>POST {ai}/api/ai/fsm/next —— 量表状态机推进（Python侧实现，失败时后端本地兜底）</li>
 *   <li>POST {ai}/api/ai/asr/transcribe —— 语音转写（可选，前端默认用浏览器ASR）</li>
 * </ul>
 *
 * <p>设计原则：AI 服务不可用时绝不阻断评估主流程——语义归一化失败时，
 * 前端可降级为静态表单直接勾选（见方案6.6/7.4 降级机制）。</p>
 */
@Slf4j
@Component
public class AiServiceClient {

    private final WebClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiServiceClient(AppProperties props) {
        this.client = WebClient.builder()
                .baseUrl(props.getAiServiceUrl())
                .build();
    }

    /**
     * 语义归一化：把老人输入的文本/语音转写文本映射到当前题目的标准选项。
     *
     * @param questionText  题目题干
     * @param dialogText    关怀话术
     * @param options       选项 [{value,label}]
     * @param userText      老人输入文本
     * @param profile       用户画像
     * @param history       最近对话历史
     * @return 归一化结果；AI服务不可用或异常返回 null（由调用方降级处理）
     */
    public Map<String, Object> normalize(String questionText, String dialogText,
                                         List<Map<String, String>> options, String userText,
                                         Map<String, Object> profile, List<Map<String, String>> history) {
        try {
            Map<String, Object> body = Map.of(
                    "question_text", nullToEmpty(questionText),
                    "dialog_text", nullToEmpty(dialogText),
                    "options", options,
                    "user_text", nullToEmpty(userText),
                    "profile", profile,
                    "history", history
            );
            String json = client.post()
                    .uri("/api/ai/normalize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            JsonNode node = objectMapper.readTree(json);
            if (node.path("code").asInt() != 0) {
                log.warn("AI归一化返回错误: {}", node.path("message").asText());
                return null;
            }
            JsonNode data = node.path("data");
            return objectMapper.convertValue(data, Map.class);
        } catch (Exception e) {
            log.warn("AI归一化服务不可用: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 语音转写（可选服务，前端默认浏览器ASR，此接口供部署自有ASR时使用）。
     */
    public String transcribe(byte[] audio, String format) {
        try {
            String json = client.post()
                    .uri("/api/ai/asr/transcribe")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("audio_base64", java.util.Base64.getEncoder().encodeToString(audio),
                            "format", format))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();
            JsonNode node = objectMapper.readTree(json);
            if (node.path("code").asInt() != 0) {
                return null;
            }
            return node.path("data").path("text").asText(null);
        } catch (Exception e) {
            log.warn("ASR服务不可用: {}", e.getMessage());
            return null;
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
