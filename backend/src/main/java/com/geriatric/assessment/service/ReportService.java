package com.geriatric.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geriatric.assessment.config.AppProperties;
import com.geriatric.assessment.dto.ReportDTO;
import com.geriatric.assessment.dto.ScaleResultDTO;
import com.geriatric.assessment.entity.*;
import com.geriatric.assessment.repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评估报告生成服务。
 *
 * <p>评估完成后汇总各量表得分与风险，生成通俗易懂的可视化报告
 * （含基础信息、量表结果、健康生活建议、免责声明），并导出 PDF。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final AssessmentReportRepository reportRepository;
    private final AssessmentTaskRepository taskRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final ScaleRepository scaleRepository;
    private final SysUserRepository userRepository;
    private final ScoringService scoringService;
    private final DialogService dialogService;
    private final AppProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成评估报告并落库，返回报告ID。
     */
    @Transactional
    public Long generate(AssessmentTask task) {
        ReportDTO dto = buildReport(task);
        AssessmentReport report = new AssessmentReport();
        report.setTaskId(task.getId());
        report.setUserId(task.getUserId());
        try {
            report.setContentJson(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            report.setContentJson("{}");
        }
        // 导出PDF
        try {
            String pdfPath = exportPdf(task, dto);
            report.setPdfPath(pdfPath);
        } catch (Exception e) {
            log.warn("PDF导出失败(不影响在线预览): {}", e.getMessage());
        }
        reportRepository.save(report);
        return report.getId();
    }

    /** 构建报告DTO（含免责声明） */
    private ReportDTO buildReport(AssessmentTask task) {
        SysUser user = userRepository.findById(task.getUserId()).orElse(null);
        List<ScaleResultDTO> results = new ArrayList<>();
        List<String> overallSuggestions = new ArrayList<>();

        for (Scale scale : scaleRepository.findByEnabledTrueOrderBySortOrderAsc()) {
            List<AssessmentAnswer> answers = answerRepository.findByTaskIdOrderByIdAsc(task.getId()).stream()
                    .filter(a -> scale.getId().equals(a.getScaleId()))
                    .collect(Collectors.toList());
            if (answers.isEmpty()) {
                continue; // 未填写的量表不入报告（分级评估下低风险人群未做全量表）
            }
            ScoringService.ScaleScore sc = scoringService.score(scale, answers);
            results.add(ScaleResultDTO.builder()
                    .scaleName(scale.getName())
                    .tableNo(scale.getTableNo())
                    .category(scale.getCategory())
                    .score(sc.score)
                    .fullScore(sc.fullScore > 0 ? sc.fullScore : null)
                    .riskLevel(sc.riskLevel)
                    .risk(sc.risk)
                    .riskText(sc.riskText)
                    .suggestions(sc.suggestions)
                    .build());
            overallSuggestions.addAll(sc.suggestions);
        }

        String disclaimer = dialogService.render("DISCLAIMER", Map.of());
        String genderText = user != null && user.getGender() != null && user.getGender() == 1 ? "男" : "女";
        return ReportDTO.builder()
                .userName(user != null ? user.getName() : "")
                .age(user != null ? user.getAge() : null)
                .genderText(genderText)
                .disclaimer(disclaimer)
                .scaleResults(results)
                .overallSuggestions(overallSuggestions.stream().distinct().collect(Collectors.toList()))
                .build();
    }

    /** 读取报告DTO（前端预览） */
    public ReportDTO getByTask(Long taskId) {
        return reportRepository.findFirstByTaskIdOrderByIdDesc(taskId)
                .map(r -> { ReportDTO dto = readReport(r.getContentJson());
                            if (dto != null) { dto.setReportId(r.getId()); dto.setTaskId(taskId); }
                            return dto; })
                .orElse(null);
    }

    public ReportDTO getById(Long reportId) {
        return reportRepository.findById(reportId)
                .map(r -> { ReportDTO dto = readReport(r.getContentJson());
                            if (dto != null) { dto.setReportId(r.getId()); }
                            return dto; })
                .orElse(null);
    }

    private ReportDTO readReport(String json) {
        try {
            return objectMapper.readValue(json, ReportDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** 导出PDF报告（OpenPDF，iText 5 兼容API） */
    private String exportPdf(AssessmentTask task, ReportDTO dto) throws Exception {
        Path dir = Paths.get(props.getReportPdfDir()).toAbsolutePath();
        Files.createDirectories(dir);
        String fileName = "report_" + task.getId() + "_" + System.currentTimeMillis() + ".pdf";
        Path file = dir.resolve(fileName);

        // 中文字体：跨平台多路径尝试，找不到才兜底
        BaseFont bf;
        String[] fontCandidates = {
                "C:/Windows/Fonts/simsun.ttc,0",                                    // Windows 宋体
                "C:/Windows/Fonts/msyh.ttc,0",                                      // Windows 微软雅黑
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",      // Linux Noto CJK
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
                "/System/Library/Fonts/PingFang.ttc,0",                            // macOS 苹方
        };
        bf = null;
        for (String f : fontCandidates) {
            try {
                bf = BaseFont.createFont(f, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                break;
            } catch (Exception ignore) { /* 试下一个 */ }
        }
        if (bf == null) {
            bf = BaseFont.createFont(); // 最后兜底（可能不支持中文）
            log.warn("未找到中文字体，PDF 中文可能显示为方块");
        }
        Font titleFont = new Font(bf, 18, Font.BOLD);
        Font normalFont = new Font(bf, 11);
        Font boldFont = new Font(bf, 13, Font.BOLD);
        Font smallFont = new Font(bf, 9);

        try (OutputStream os = Files.newOutputStream(file)) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, os);
            doc.open();
            doc.add(new Paragraph("老年综合评估报告", titleFont));
            doc.add(new Paragraph("姓名：" + safe(dto.getUserName())
                    + "    年龄：" + (dto.getAge() == null ? "-" : dto.getAge())
                    + "    性别：" + safe(dto.getGenderText()), normalFont));
            doc.add(new Paragraph(" ", normalFont));
            for (ScaleResultDTO r : dto.getScaleResults()) {
                doc.add(new Paragraph("【" + safe(r.getScaleName()) + "】(" + safe(r.getTableNo()) + ")", boldFont));
                doc.add(new Paragraph("  得分：" + r.getScore()
                        + (r.getFullScore() != null ? " / " + r.getFullScore() : "")
                        + "    风险：" + safe(r.getRiskLevel()), normalFont));
                if (r.getRiskText() != null && !r.getRiskText().isBlank()) {
                    doc.add(new Paragraph("  " + safe(r.getRiskText()), normalFont));
                }
                doc.add(new Paragraph(" ", normalFont));
            }
            doc.add(new Paragraph("健康生活建议：", boldFont));
            for (String s : dto.getOverallSuggestions()) {
                doc.add(new Paragraph("  · " + safe(s), normalFont));
            }
            doc.add(new Paragraph(" ", normalFont));
            doc.add(new Paragraph("免责声明：" + safe(dto.getDisclaimer()), smallFont));
            doc.close();
        }
        return file.toString();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
