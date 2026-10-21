package com.geriatric.assessment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geriatric.assessment.client.AiServiceClient;
import com.geriatric.assessment.common.BizException;
import com.geriatric.assessment.config.AppProperties;
import com.geriatric.assessment.dto.*;
import com.geriatric.assessment.entity.*;
import com.geriatric.assessment.repository.*;
import com.geriatric.assessment.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评估核心服务：统筹整个自助评估流程。
 *
 * <p>职责（对应方案"六、双模式评估输入模块详细方案"与"4.2 智能分级评估引擎"）：</p>
 * <ul>
 *   <li>创建评估任务、断点续聊（草稿恢复）</li>
 *   <li>读取当前题目（关怀式话术）</li>
 *   <li>提交答案并做三层语义解析：词典规则匹配 → AI语义归一化 → 追问收敛（≤2轮）</li>
 *   <li>量表状态机推进（题目顺序、量表分支、分级评估），严格由配置驱动，不放大模型决定跳转</li>
 *   <li>初筛风险判定：低风险提前结束生成简易报告，存在风险则加载全量21套量表</li>
 *   <li>评估完成触发报告生成</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentTaskRepository taskRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final AssessmentReportRepository reportRepository;
    private final ScaleRepository scaleRepository;
    private final ScaleQuestionRepository questionRepository;
    private final SemanticDictionaryRepository dictionaryRepository;
    private final SysUserRepository userRepository;
    private final ScoringService scoringService;
    private final AiServiceClient aiClient;
    private final ParamService paramService;
    private final DialogService dialogService;
    private final ReportService reportService;
    private final AppProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ------------------------------------------------------------------
    // 1. 任务创建与断点续聊
    // ------------------------------------------------------------------

    /**
     * 创建新评估任务。
     *
     * @param userId    用户ID
     * @param inputMode 输入模式 MANUAL/VOICE
     */
    @Transactional
    public Long createTask(Long userId, String inputMode) {
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        AssessmentTask task = new AssessmentTask();
        task.setUserId(userId);
        task.setInputMode("VOICE".equalsIgnoreCase(inputMode) ? "VOICE" : "MANUAL");
        task.setStatus("SCREENING");
        task.setCompletedScales("[]");

        // 定位第一个初筛量表
        Scale first = firstScreeningScale(task);
        if (first != null) {
            task.setCurrentScaleId(first.getId());
            task.setCurrentQuestionId(firstQuestionId(first.getId()));
        }
        taskRepository.save(task);
        return task.getId();
    }

    /** 查询某用户未完成草稿（供前端一键续评） */
    public List<AssessmentTaskDTO> listDrafts(Long userId) {
        return taskRepository.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, "COMPLETED")
                .stream().map(this::toTaskDTO).collect(Collectors.toList());
    }

    /** 查询某用户历史评估记录 */
    public List<AssessmentTaskDTO> listHistory(Long userId) {
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toTaskDTO).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // 2. 读取当前题目
    // ------------------------------------------------------------------

    /**
     * 获取当前题目。任务已完成返回 null（前端跳转报告）。
     */
    @Transactional(readOnly = true)
    public QuestionDTO getCurrentQuestion(Long taskId, Long userId) {
        AssessmentTask task = mustOwn(taskId, userId);
        ensureStarted(task);
        return buildQuestionDTO(task);
    }

    // ------------------------------------------------------------------
    // 3. 删除评估记录（用户主动删除 / 定时清理）
    // ------------------------------------------------------------------

    /**
     * 删除某条评估记录（级联删除答案、报告、任务本身）。
     * 仅允许任务归属者删除。
     *
     * @param taskId 任务ID
     * @param userId 当前登录用户ID（校验归属）
     */
    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        AssessmentTask task = mustOwn(taskId, userId);
        // 级联删：答案 → 报告 → 任务
        answerRepository.deleteByTaskId(taskId);
        reportRepository.deleteByTaskId(taskId);
        taskRepository.delete(task);
        log.info("用户{}删除评估任务{}", userId, taskId);
    }

    // ------------------------------------------------------------------
    // 4. 提交答案（核心）
    // ------------------------------------------------------------------

    /**
     * 提交答案并推进评估流程。
     */
    @Transactional
    public AnswerResponse submitAnswer(Long taskId, Long userId, SubmitAnswerRequest req) {
        AssessmentTask task = mustOwn(taskId, userId);
        ensureNotCompleted(task);
        ensureStarted(task);

        // 老人表达疲惫：友好提示暂停，草稿自动保存
        if (Boolean.TRUE.equals(req.getPause())) {
            return AnswerResponse.builder()
                    .type("PAUSE")
                    .message(dialogService.render("PAUSE", Map.of("name", userName(userId))))
                    .build();
        }

        ScaleQuestion q = questionRepository.findById(req.getQuestionId())
                .orElseThrow(() -> new BizException(400, "题目不存在"));
        if (!q.getId().equals(task.getCurrentQuestionId())) {
            // 容错：题目已过期（并发/刷新），回到当前题
            throw new BizException(400, "题目状态已更新，请刷新后重试");
        }

        // ---- 解析答案（三层语义）----
        ResolveResult resolved = resolveAnswer(task, q, req);

        // ---- 需要追问 ----
        int maxFollowup = paramService.getInt("max_followup_count", 2);
        if (resolved.needFollowup && task.getFollowupCount() < maxFollowup) {
            // 记录本轮原始输入，暂不落归一化结果
            saveAnswer(task, q, req, resolved, false);
            task.setFollowupCount(task.getFollowupCount() + 1);
            task.setLastAnswerAt(LocalDateTime.now());
            taskRepository.save(task);
            String prompt = resolved.followupPrompt;
            if (prompt == null || prompt.isBlank()) {
                prompt = dialogService.render("FOLLOWUP", Map.of("question", safeDialog(q)));
            }
            return AnswerResponse.builder()
                    .type("FOLLOWUP")
                    .message(prompt)
                    .question(buildQuestionDTO(task))
                    .build();
        }

        // ---- AI/语音服务不可用：降级为静态表单（仅当必须归一化时）----
        if (resolved.staticFallback) {
            return AnswerResponse.builder()
                    .type("STATIC_FALLBACK")
                    .message("语音/文字识别服务暂时不可用，请直接点击下方选项作答")
                    .question(buildQuestionDTO(task))
                    .build();
        }

        // ---- 超过最大追问仍模糊 → 标记答案不确定 ----
        boolean uncertain = resolved.needFollowup; // 走到这里说明已到追问上限或本身不确定
        if (uncertain) {
            resolved.optionValue = null;
            resolved.optionText = null;
            resolved.confidence = 0.0;
        }
        saveAnswer(task, q, req, resolved, uncertain);
        task.setLastAnswerAt(LocalDateTime.now());

        // ---- 推进（量表状态机）----
        return advance(task);
    }

    // ------------------------------------------------------------------
    // 4. 三层语义解析
    // ------------------------------------------------------------------

    private static class ResolveResult {
        String optionValue;
        String optionText;
        double confidence = 0;
        boolean needFollowup = false;
        String followupPrompt;
        boolean staticFallback = false; // AI服务不可用且词典未命中，需降级静态表单
    }

    /**
     * 解析老人输入 → 标准选项。
     *
     * <p>层1 词典规则匹配（稳定可控，优先）→
     * 层2 限定域LLM语义归一化（强约束提示词，仅允许选当前题目预设选项）→
     * 层3 追问收敛（由本方法标记 needFollowup，外层控制追问次数）。</p>
     */
    private ResolveResult resolveAnswer(AssessmentTask task, ScaleQuestion q, SubmitAnswerRequest req) {
        ResolveResult r = new ResolveResult();

        // 开放题：直接记录原文，无需归一化
        if ("OPEN".equals(q.getQuestionType())) {
            r.optionValue = null;
            r.optionText = null;
            r.confidence = 1.0;
            return r;
        }

        // 直接勾选选项：最高可信，无需AI
        if ("OPTION".equalsIgnoreCase(req.getAnswerSource()) && req.getOptionValue() != null) {
            OptionMatched m = matchOptionByValue(q, req.getOptionValue());
            if (m != null) {
                r.optionValue = m.value;
                r.optionText = m.label;
                r.confidence = 1.0;
                return r;
            }
            // 选项值不合法，当作需要澄清
            r.needFollowup = true;
            return r;
        }

        // 文本/语音转写文本：先词典，再LLM
        String text = (req.getText() == null ? "" : req.getText().trim());
        if (text.isEmpty()) {
            r.needFollowup = true;
            return r;
        }

        // 层1：词典规则匹配（语音来源跳过词典，强制走AI分析，保证口语化回答的准确率）
        boolean isVoice = "VOICE".equalsIgnoreCase(req.getAnswerSource());
        OptionMatched dictMatch = null;
        if (!isVoice) {
            dictMatch = matchByDictionary(q, text);
        }
        if (dictMatch != null) {
            r.optionValue = dictMatch.value;
            r.optionText = dictMatch.label;
            r.confidence = dictMatch.confidence;
            return r;
        }

        // 层2：调用AI语义归一化
        Map<String, Object> ai = aiClient.normalize(
                q.getQuestionText(), q.getDialogText(),
                optionsToMaps(optionsOf(q)), text,
                profileOf(task.getUserId()),
                recentHistory(task)
        );

        if (ai != null) {
            String v = (String) ai.get("option_value");
            boolean needF = Boolean.TRUE.equals(ai.get("needs_followup"));
            if (v != null && !v.isBlank()) {
                r.optionValue = v;
                OptionMatched m = matchOptionByValue(q, v);
                r.optionText = m == null ? (String) ai.get("option_text") : m.label;
                r.confidence = ai.get("confidence") instanceof Number
                        ? ((Number) ai.get("confidence")).doubleValue() : 0.8;
                double threshold = paramService.getDouble("confidence_threshold", 0.6);
                if (r.confidence < threshold) {
                    r.needFollowup = true;
                    r.followupPrompt = (String) ai.get("followup_question");
                }
                return r;
            }
            if (needF) {
                r.needFollowup = true;
                r.followupPrompt = (String) ai.get("followup_question");
                return r;
            }
            // 无法判定
            r.needFollowup = true;
            return r;
        }

        // AI服务不可用：降级静态表单（老人可直接勾选选项完成评估）
        r.staticFallback = true;
        return r;
    }

    /** 按选项值直接匹配（直接勾选场景） */
    private OptionMatched matchOptionByValue(ScaleQuestion q, String value) {
        JsonNode opts = parse(q.getOptions());
        if (opts == null || !opts.isArray()) {
            return null;
        }
        for (JsonNode o : opts) {
            if (value.equals(o.path("value").asText())) {
                return new OptionMatched(o.path("value").asText(), o.path("label").asText(), 1.0);
            }
        }
        return null;
    }

    /** 层1：词典规则匹配 */
    private OptionMatched matchByDictionary(ScaleQuestion q, String text) {
        List<SemanticDictionary> dicts = dictionaryRepository
                .findByQuestionIdOrQuestionIdIsNullOrderBySortOrderAsc(q.getId());
        if (dicts.isEmpty()) {
            return null;
        }
        // 先匹配题目级词典，再匹配全局词典
        for (SemanticDictionary d : dicts) {
            boolean hit = matchOne(d, text);
            if (hit) {
                // 反查选项文本
                JsonNode opts = parse(q.getOptions());
                String label = null;
                if (opts != null && opts.isArray()) {
                    for (JsonNode o : opts) {
                        if (d.getOptionValue().equals(o.path("value").asText())) {
                            label = o.path("label").asText();
                            break;
                        }
                    }
                }
                return new OptionMatched(d.getOptionValue(), label, 0.95);
            }
        }
        return null;
    }

    private boolean matchOne(SemanticDictionary d, String text) {
        String type = d.getMatchType() == null ? "CONTAINS" : d.getMatchType();
        for (String syn : d.getSynonyms().split(",")) {
            syn = syn.trim();
            if (syn.isEmpty()) {
                continue;
            }
            if ("EQUALS".equalsIgnoreCase(type)) {
                if (text.equals(syn)) {
                    return true;
                }
            } else if ("REGEX".equalsIgnoreCase(type)) {
                if (text.matches(syn)) {
                    return true;
                }
            } else { // CONTAINS
                if (text.contains(syn)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class OptionMatched {
        String value;
        String label;
        double confidence;

        OptionMatched(String value, String label, double confidence) {
            this.value = value;
            this.label = label;
            this.confidence = confidence;
        }
    }

    /** 保存一条答案（敏感字段AES加密） */
    private void saveAnswer(AssessmentTask task, ScaleQuestion q, SubmitAnswerRequest req,
                            ResolveResult r, boolean uncertain) {
        AssessmentAnswer a = new AssessmentAnswer();
        a.setTaskId(task.getId());
        a.setScaleId(q.getScaleId());
        a.setQuestionId(q.getId());
        if (req.getText() != null && !req.getText().isBlank()) {
            a.setUserText(AesUtil.encrypt(req.getText(), props.getAesKey()));
        }
        if ("VOICE".equalsIgnoreCase(req.getAnswerSource()) && req.getText() != null) {
            a.setAsrText(AesUtil.encrypt(req.getText(), props.getAesKey()));
        }
        a.setNormalizedValue(r.optionValue);
        a.setNormalizedText(r.optionText);
        a.setConfidence(BigDecimal.valueOf(r.confidence));
        a.setUncertain(uncertain);
        a.setFollowup(task.getFollowupCount() > 0);
        answerRepository.save(a);
    }

    // ------------------------------------------------------------------
    // 5. 量表状态机推进
    // ------------------------------------------------------------------

    /**
     * 推进评估：当前量表内下一题，或量表完成后的分支决策。
     * 全程由量表配置（题目顺序/初筛标记/风险阈值）驱动，AI无权决定跳转。
     */
    private AnswerResponse advance(AssessmentTask task) {
        // 判定当前量表是否完成
        List<ScaleQuestion> scaleQuestions = questionRepository
                .findByScaleIdAndEnabledTrueOrderBySortOrderAsc(task.getCurrentScaleId());
        boolean scaleDone = isScaleComplete(task, task.getCurrentScaleId(), scaleQuestions.size());

        if (!scaleDone) {
            // 同一量表内下一题
            ScaleQuestion next = nextQuestionInScale(scaleQuestions, task.getCurrentQuestionId());
            task.setCurrentQuestionId(next.getId());
            task.setFollowupCount(0);
            taskRepository.save(task);
            return AnswerResponse.builder()
                    .type("QUESTION")
                    .message(successMessage(task.getUserId()))
                    .question(buildQuestionDTO(task))
                    .build();
        }

        // 量表完成 → 记录完成
        List<Long> completed = completedScaleIds(task);
        if (!completed.contains(task.getCurrentScaleId())) {
            completed.add(task.getCurrentScaleId());
            task.setCompletedScales(toJson(completed));
        }
        Scale finishedScale = scaleRepository.findById(task.getCurrentScaleId()).orElse(null);

        // ---- 初筛阶段决策 ----
        if (!Boolean.TRUE.equals(task.getScreeningDone())) {
            Scale nextScreening = nextScreeningScale(task);
            if (nextScreening != null) {
                moveToScale(task, nextScreening);
                taskRepository.save(task);
                return questionResponse(task, "初筛进展顺利，我们继续几个简单问题");
            }
            // 初筛全部完成 → 判定风险
            task.setScreeningDone(true);
            int risk = computeScreeningRisk(task, completed);
            task.setScreeningRisk(risk);
            if (risk >= 1) {
                // 存在风险 → 进入全量评估
                task.setStatus("ASSESSING");
                Scale nextNonScreen = nextNonScreeningScale(task);
                if (nextNonScreen != null) {
                    moveToScale(task, nextNonScreen);
                    taskRepository.save(task);
                    String msg = dialogService.render("SCREENING_RISK", Map.of("name", userName(task.getUserId())));
                    return AnswerResponse.builder()
                            .type("QUESTION")
                            .message(msg)
                            .screeningDone(true)
                            .screeningRisk(risk)
                            .question(buildQuestionDTO(task))
                            .build();
                }
                // 没有非初筛量表，直接完成
                return finish(task);
            }
            // 低风险 → 直接生成简易报告结束（保留原设计：初筛通过不做全量评估）
            return finish(task);
        }

        // ---- 全量评估阶段 ----
        Scale nextNonScreen = nextNonScreeningScale(task);
        if (nextNonScreen != null) {
            moveToScale(task, nextNonScreen);
            taskRepository.save(task);
            return questionResponse(task, "好的，我们继续下一部分");
        }
        // 全部完成
        return finish(task);
    }

    /** 全部量表完成：归档并生成报告 */
    private AnswerResponse finish(AssessmentTask task) {
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        task.setCurrentQuestionId(null);
        taskRepository.save(task);
        Long reportId = reportService.generate(task);
        String msg = dialogService.render(
                Boolean.TRUE.equals(task.getScreeningDone()) && task.getScreeningRisk() != null
                        && task.getScreeningRisk() == 0 ? "SCREENING_PASS" : "GREETING",
                userRepository.findById(task.getUserId())
                        .map(u -> (Map<String, String>) Map.of("name", safe(u.getName()), "title", titleOf(u)))
                        .orElse(Map.of("name", userName(task.getUserId()))));
        return AnswerResponse.builder()
                .type("DONE")
                .message("评估已完成，报告已生成")
                .reportId(reportId)
                .screeningDone(task.getScreeningDone())
                .screeningRisk(task.getScreeningRisk())
                .build();
    }

    /** 初筛风险判定：任一初筛量表风险等级 ≥ 阈值 → 存在风险(1) */
    private int computeScreeningRisk(AssessmentTask task, List<Long> completedScales) {
        int trigger = paramService.getInt("screening_trigger_risk", 1);
        for (Scale s : scaleRepository.findByEnabledTrueOrderBySortOrderAsc()) {
            if (!Boolean.TRUE.equals(s.getScreening())) {
                continue;
            }
            if (!completedScales.contains(s.getId())) {
                continue;
            }
            List<AssessmentAnswer> answers = answersOfScale(task, s.getId());
            ScoringService.ScaleScore sc = scoringService.score(s, answers);
            if (sc.risk >= trigger) {
                return 1;
            }
        }
        return 0;
    }

    private void moveToScale(AssessmentTask task, Scale scale) {
        task.setCurrentScaleId(scale.getId());
        task.setCurrentQuestionId(firstQuestionId(scale.getId()));
        task.setFollowupCount(0);
    }

    /** 当前量表内，位于指定题之后的下一题 */
    private ScaleQuestion nextQuestionInScale(List<ScaleQuestion> questions, Long currentId) {
        int idx = -1;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getId().equals(currentId)) {
                idx = i;
                break;
            }
        }
        if (idx >= 0 && idx + 1 < questions.size()) {
            return questions.get(idx + 1);
        }
        // 找不到则取第一题（防御）
        return questions.get(0);
    }

    /** 量表是否完成：该任务中该量表已有答案的去重题数 >= 应答题数 */
    private boolean isScaleComplete(AssessmentTask task, Long scaleId, int totalQuestions) {
        if (totalQuestions == 0) {
            return true;
        }
        long answeredQ = answerRepository.findByTaskIdOrderByIdAsc(task.getId()).stream()
                .filter(a -> scaleId.equals(a.getScaleId()) && a.getQuestionId() != null)
                .map(AssessmentAnswer::getQuestionId).distinct().count();
        return answeredQ >= totalQuestions;
    }

    private List<AssessmentAnswer> answersOfScale(AssessmentTask task, Long scaleId) {
        return answerRepository.findByTaskIdOrderByIdAsc(task.getId()).stream()
                .filter(a -> scaleId.equals(a.getScaleId()))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // 6. 量表/题目定位辅助
    // ------------------------------------------------------------------

    private Scale firstScreeningScale(AssessmentTask task) {
        Scale s = nextScreeningScale(task);
        return s != null ? s : nextNonScreeningScale(task);
    }

    /** 下一个未完成的初筛量表（跳过无题目的量表，避免流程悬空） */
    private Scale nextScreeningScale(AssessmentTask task) {
        List<Long> completed = completedScaleIds(task);
        for (Scale s : scaleRepository.findByEnabledTrueOrderBySortOrderAsc()) {
            if (Boolean.TRUE.equals(s.getScreening()) && !completed.contains(s.getId())
                    && hasQuestions(s.getId())) {
                return s;
            }
        }
        return null;
    }

    /** 下一个未完成的非初筛量表（跳过无题目的量表） */
    private Scale nextNonScreeningScale(AssessmentTask task) {
        List<Long> completed = completedScaleIds(task);
        for (Scale s : scaleRepository.findByEnabledTrueOrderBySortOrderAsc()) {
            if (!Boolean.TRUE.equals(s.getScreening()) && !completed.contains(s.getId())
                    && hasQuestions(s.getId())) {
                return s;
            }
        }
        return null;
    }

    /** 量表是否配置了至少一道启用的题目 */
    private boolean hasQuestions(Long scaleId) {
        return questionRepository.findByScaleIdAndEnabledTrueOrderBySortOrderAsc(scaleId).size() > 0;
    }

    private Long firstQuestionId(Long scaleId) {
        List<ScaleQuestion> qs = questionRepository.findByScaleIdAndEnabledTrueOrderBySortOrderAsc(scaleId);
        return qs.isEmpty() ? null : qs.get(0).getId();
    }

    private List<Long> completedScaleIds(AssessmentTask task) {
        List<Long> list = new ArrayList<>();
        JsonNode arr = parse(task.getCompletedScales());
        if (arr != null && arr.isArray()) {
            arr.forEach(n -> list.add(n.asLong()));
        }
        return list;
    }

    // ------------------------------------------------------------------
    // 7. DTO 构建
    // ------------------------------------------------------------------

    /** 构建当前题目DTO */
    private QuestionDTO buildQuestionDTO(AssessmentTask task) {
        if (task.getCurrentScaleId() == null || task.getCurrentQuestionId() == null) {
            return null;
        }
        Scale scale = scaleRepository.findById(task.getCurrentScaleId()).orElse(null);
        ScaleQuestion q = questionRepository.findById(task.getCurrentQuestionId()).orElse(null);
        if (scale == null || q == null) {
            return null;
        }
        List<ScaleQuestion> qs = questionRepository.findByScaleIdAndEnabledTrueOrderBySortOrderAsc(scale.getId());
        int index = 0;
        for (int i = 0; i < qs.size(); i++) {
            if (qs.get(i).getId().equals(q.getId())) {
                index = i + 1;
                break;
            }
        }
        String text = (q.getDialogText() != null && !q.getDialogText().isBlank())
                ? q.getDialogText() : q.getQuestionText();
        text = renderText(text, task.getUserId());
        return QuestionDTO.builder()
                .questionId(q.getId())
                .scaleId(scale.getId())
                .scaleName(scale.getName())
                .tableNo(scale.getTableNo())
                .questionType(q.getQuestionType())
                .questionText(text)
                .questionIndex(index)
                .questionTotal(qs.size())
                .options(optionsOf(q))
                .screening(Boolean.TRUE.equals(scale.getScreening()))
                .build();
    }

    private List<OptionDTO> optionsOf(ScaleQuestion q) {
        List<OptionDTO> list = new ArrayList<>();
        JsonNode opts = parse(q.getOptions());
        if (opts != null && opts.isArray()) {
            for (JsonNode o : opts) {
                list.add(OptionDTO.builder()
                        .value(o.path("value").asText())
                        .label(o.path("label").asText())
                        .build());
            }
        }
        return list;
    }

    /** 选项转为 {value,label} Map 列表（供AI服务调用） */
    private List<Map<String, String>> optionsToMaps(List<OptionDTO> opts) {
        List<Map<String, String>> list = new ArrayList<>();
        for (OptionDTO o : opts) {
            Map<String, String> m = new HashMap<>();
            m.put("value", o.getValue());
            m.put("label", o.getLabel());
            list.add(m);
        }
        return list;
    }

    private AnswerResponse questionResponse(AssessmentTask task, String message) {
        return AnswerResponse.builder()
                .type("QUESTION")
                .message(message)
                .question(buildQuestionDTO(task))
                .build();
    }

    /**
     * 生成答题成功的问候话术。
     *
     * <p>以当前用户姓名/称谓填充 GREETING 话术中的 {name}/{title} 占位符，
     * 避免出现字面量“{name}”。</p>
     *
     * @param userId 当前评估用户ID（用于取姓名与称谓）
     * @return 渲染后的问候文案
     */
    private String successMessage(Long userId) {
        return userRepository.findById(userId)
                .map(u -> dialogService.render("GREETING",
                        Map.of("name", safe(u.getName()), "title", titleOf(u))))
                .orElse(dialogService.render("GREETING", Map.of()));
    }

    private AssessmentTaskDTO toTaskDTO(AssessmentTask task) {
        String scaleName = task.getCurrentScaleId() == null ? "" :
                scaleRepository.findById(task.getCurrentScaleId()).map(Scale::getName).orElse("");
        boolean done = "COMPLETED".equals(task.getStatus());
        return AssessmentTaskDTO.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .statusText(done ? "已完成" : "进行中")
                .inputMode(task.getInputMode())
                .inputModeText("VOICE".equals(task.getInputMode()) ? "语音问答" : "手动输入")
                .currentScaleName(scaleName)
                .screeningRisk(task.getScreeningRisk())
                .createdAt(task.getCreatedAt())
                .lastAnswerAt(task.getLastAnswerAt())
                .build();
    }

    // ------------------------------------------------------------------
    // 辅助
    // ------------------------------------------------------------------

    private AssessmentTask mustOwn(Long taskId, Long userId) {
        AssessmentTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(404, "评估任务不存在"));
        if (!task.getUserId().equals(userId)) {
            throw new BizException(403, "无权访问该评估任务");
        }
        return task;
    }

    private void ensureNotCompleted(AssessmentTask task) {
        if ("COMPLETED".equals(task.getStatus())) {
            throw new BizException(400, "该评估已完成");
        }
    }

    private void ensureStarted(AssessmentTask task) {
        if (task.getCurrentScaleId() == null || task.getCurrentQuestionId() == null) {
            // 防御性初始化
            Scale first = firstScreeningScale(task);
            if (first != null) {
                task.setCurrentScaleId(first.getId());
                task.setCurrentQuestionId(firstQuestionId(first.getId()));
                taskRepository.save(task);
            }
        }
    }

    private String safeDialog(ScaleQuestion q) {
        return (q.getDialogText() != null && !q.getDialogText().isBlank())
                ? q.getDialogText() : q.getQuestionText();
    }

    private String userName(Long userId) {
        return userRepository.findById(userId).map(SysUser::getName).orElse("");
    }

    /**
     * 用用户画像替换话术/题干中的占位符：{name} 姓名、{title} 称谓(如 爷爷)、{age} 年龄。
     */
    private String renderText(String text, Long userId) {
        if (text == null || text.indexOf('{') < 0) {
            return text;
        }
        return userRepository.findById(userId).map(u -> text
                .replace("{name}", safe(u.getName()))
                .replace("{age}", u.getAge() == null ? "" : String.valueOf(u.getAge()))
                .replace("{title}", titleOf(u))).orElse(text);
    }

    private String titleOf(SysUser u) {
        boolean male = u.getGender() != null && u.getGender() == 1;
        Integer age = u.getAge();
        if (male) {
            return age != null && age >= 80 ? "爷爷" : "大爷";
        }
        return age != null && age >= 80 ? "奶奶" : "大娘";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    /** 构造用户画像（注入AI上下文） */
    private Map<String, Object> profileOf(Long userId) {
        return userRepository.findById(userId).map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", u.getName());
            m.put("age", u.getAge());
            m.put("gender", u.getGender() != null && u.getGender() == 1 ? "男" : "女");
            m.put("birth_date", String.valueOf(u.getBirthDate()));
            return m;
        }).orElse(Map.of());
    }

    /** 最近对话历史（供LLM理解上下文） */
    private List<Map<String, String>> recentHistory(AssessmentTask task) {
        List<Map<String, String>> hist = new ArrayList<>();
        for (AssessmentAnswer a : answerRepository.findByTaskIdOrderByIdAsc(task.getId())) {
            if (a.getQuestionId() == null) {
                continue;
            }
            Map<String, String> m = new HashMap<>();
            questionRepository.findById(a.getQuestionId()).ifPresent(q ->
                    m.put("question", q.getQuestionText()));
            m.put("answer", a.getNormalizedText() == null ? "（未识别）" : a.getNormalizedText());
            hist.add(m);
        }
        return hist;
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(List<Long> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }
}
