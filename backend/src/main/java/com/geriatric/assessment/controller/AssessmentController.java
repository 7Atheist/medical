package com.geriatric.assessment.controller;

import com.geriatric.assessment.client.AiServiceClient;
import com.geriatric.assessment.common.BizException;
import com.geriatric.assessment.common.Result;
import com.geriatric.assessment.config.AuthInterceptor;
import com.geriatric.assessment.dto.AnswerResponse;
import com.geriatric.assessment.dto.AssessmentTaskDTO;
import com.geriatric.assessment.dto.QuestionDTO;
import com.geriatric.assessment.dto.SubmitAnswerRequest;
import com.geriatric.assessment.service.AssessmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 评估流程接口：任务创建、草稿/历史查询、取题、提交答案、语音转写。
 */
@RestController
@RequestMapping("/assess")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AiServiceClient aiClient;

    /** 从请求上下文取当前登录用户ID（由 AuthInterceptor 注入） */
    private Long uid(HttpServletRequest req) {
        Object v = req.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return v == null ? null : (Long) v;
    }

    /**
     * 创建评估任务。
     *
     * @param body {inputMode: MANUAL/VOICE}
     */
    @PostMapping("/create")
    public Result<Long> create(HttpServletRequest req, @RequestBody(required = false) Map<String, String> body) {
        String mode = body == null ? "MANUAL" : body.getOrDefault("inputMode", "MANUAL");
        return Result.ok(assessmentService.createTask(uid(req), mode));
    }

    /** 未完成草稿列表（断点续聊） */
    @GetMapping("/drafts")
    public Result<List<AssessmentTaskDTO>> drafts(HttpServletRequest req) {
        return Result.ok(assessmentService.listDrafts(uid(req)));
    }

    /** 历史评估记录 */
    @GetMapping("/history")
    public Result<List<AssessmentTaskDTO>> history(HttpServletRequest req) {
        return Result.ok(assessmentService.listHistory(uid(req)));
    }

    /** 删除一条评估记录（级联删除答案、报告） */
    @DeleteMapping("/{taskId}")
    public Result<Void> delete(HttpServletRequest req, @PathVariable Long taskId) {
        assessmentService.deleteTask(taskId, uid(req));
        return Result.ok(null);
    }

    /** 获取当前题目 */
    @GetMapping("/{taskId}/question")
    public Result<QuestionDTO> question(HttpServletRequest req, @PathVariable Long taskId) {
        return Result.ok(assessmentService.getCurrentQuestion(taskId, uid(req)));
    }

    /** 提交答案 */
    @PostMapping("/{taskId}/answer")
    public Result<AnswerResponse> answer(HttpServletRequest req, @PathVariable Long taskId,
                                         @Valid @RequestBody SubmitAnswerRequest body) {
        return Result.ok(assessmentService.submitAnswer(taskId, uid(req), body));
    }

    /**
     * 语音作答：上传音频 → 调用ASR转写 → 走统一答案提交。
     * 说明：前端默认使用浏览器语音识别，此接口供部署自有ASR服务时使用。
     */
    @PostMapping("/{taskId}/audio")
    public Result<AnswerResponse> audioAnswer(HttpServletRequest req, @PathVariable Long taskId,
                                              @RequestParam("questionId") Long questionId,
                                              @RequestParam("file") MultipartFile file) {
        try {
            String text = aiClient.transcribe(file.getBytes(),
                    file.getOriginalFilename() != null && file.getOriginalFilename().endsWith(".wav") ? "wav" : "mp3");
            if (text == null || text.isBlank()) {
                throw new BizException(500, "语音识别失败，请重试或改用手动输入");
            }
            SubmitAnswerRequest body = new SubmitAnswerRequest();
            body.setQuestionId(questionId);
            body.setAnswerSource("VOICE");
            body.setText(text);
            return Result.ok(assessmentService.submitAnswer(taskId, uid(req), body));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "语音处理失败");
        }
    }
}
