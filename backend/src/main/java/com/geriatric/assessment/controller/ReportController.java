package com.geriatric.assessment.controller;

import com.geriatric.assessment.common.Result;
import com.geriatric.assessment.config.AuthInterceptor;
import com.geriatric.assessment.dto.ReportDTO;
import com.geriatric.assessment.entity.AssessmentReport;
import com.geriatric.assessment.repository.AssessmentReportRepository;
import com.geriatric.assessment.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 评估报告接口：在线预览与 PDF 导出。
 */
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final AssessmentReportRepository reportRepository;

    private Long uid(HttpServletRequest req) {
        Object v = req.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return v == null ? null : (Long) v;
    }

    /** 按任务取报告 */
    @GetMapping("/task/{taskId}")
    public Result<ReportDTO> byTask(HttpServletRequest req, @PathVariable Long taskId) {
        // 校验归属
        AssessmentReport r = reportRepository.findFirstByTaskIdOrderByIdDesc(taskId)
                .orElseThrow(() -> new com.geriatric.assessment.common.BizException(404, "报告不存在"));
        if (!r.getUserId().equals(uid(req))) {
            throw new com.geriatric.assessment.common.BizException(403, "无权访问");
        }
        return Result.ok(reportService.getByTask(taskId));
    }

    /** 历史报告列表（返回简表） */
    @GetMapping("/list")
    public Result<List<AssessmentReport>> list(HttpServletRequest req) {
        return Result.ok(reportRepository.findByUserIdOrderByCreatedAtDesc(uid(req)));
    }

    /**
     * 下载PDF报告。
     */
    @GetMapping("/{reportId}/pdf")
    public ResponseEntity<?> pdf(HttpServletRequest req, @PathVariable Long reportId) {
        AssessmentReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new com.geriatric.assessment.common.BizException(404, "报告不存在"));
        if (!r.getUserId().equals(uid(req))) {
            throw new com.geriatric.assessment.common.BizException(403, "无权访问");
        }
        if (r.getPdfPath() == null || r.getPdfPath().isBlank()) {
            throw new com.geriatric.assessment.common.BizException(404, "PDF尚未生成");
        }
        try {
            Path p = Paths.get(r.getPdfPath());
            if (!Files.exists(p)) {
                throw new com.geriatric.assessment.common.BizException(404, "PDF文件不存在");
            }
            String filename = URLEncoder.encode("老年综合评估报告_" + reportId + ".pdf", StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new FileSystemResource(p.toFile()));
        } catch (Exception e) {
            throw new com.geriatric.assessment.common.BizException(500, "PDF下载失败");
        }
    }
}
