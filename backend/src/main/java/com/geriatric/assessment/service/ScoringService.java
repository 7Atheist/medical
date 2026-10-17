package com.geriatric.assessment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geriatric.assessment.entity.AssessmentAnswer;
import com.geriatric.assessment.entity.Scale;
import com.geriatric.assessment.entity.ScaleQuestion;
import com.geriatric.assessment.repository.ScaleQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 量表计分与风险判定服务。
 *
 * <p>依据量表配置中的选项分值（options[].score）对各题归一化答案求和，
 * 再按 risk_rule（JSON阈值区间）判定风险等级并生成风险说明与健康建议。</p>
 *
 * <pre>
 * risk_rule JSON 示例：
 * {"levels":[
 *   {"max":0,"risk":0,"level":"无衰弱","text":"未见明显衰弱表现","suggestions":["保持现有运动习惯"]},
 *   {"min":1,"max":2,"risk":1,"level":"衰弱前期","text":"存在衰弱前期迹象","suggestions":["建议加强营养与力量训练"]},
 *   {"min":3,"risk":2,"level":"衰弱","text":"存在衰弱","suggestions":["建议尽早就医综合评估"]}
 * ]}
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final ScaleQuestionRepository questionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 计分结果。
     */
    public static class ScaleScore {
        public double score = 0;
        public double fullScore = 0;
        public Integer risk = 0;
        public String riskLevel = "正常";
        public String riskText = "";
        public List<String> suggestions = new ArrayList<>();
        /** 有明确归一化答案的题数 / 应答题数 */
        public int answered = 0;
        public int total = 0;
        /** 无法识别的题目数（答案不确定） */
        public int uncertainCount = 0;
    }

    /**
     * 对某量表在指定任务下的答案集合计分并判定风险。
     *
     * @param scale   量表配置
     * @param answers 该任务该量表的全部答案
     * @return 计分结果
     */
    public ScaleScore score(Scale scale, List<AssessmentAnswer> answers) {
        ScaleScore result = new ScaleScore();
        List<ScaleQuestion> questions = questionRepository.findByScaleIdAndEnabledTrueOrderBySortOrderAsc(scale.getId());
        result.total = questions.size();

        // 建立 题目id → 最终答案 映射：优先非追问、取最后一次有效归一化答案
        Map<Long, AssessmentAnswer> primaryByQ = new HashMap<>();
        for (AssessmentAnswer a : answers) {
            if (a.getQuestionId() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(a.getUncertain())) {
                result.uncertainCount++;
            }
            // 归一化值非空才算有效答案
            if (a.getNormalizedValue() != null && !a.getNormalizedValue().isBlank()) {
                primaryByQ.put(a.getQuestionId(), a); // 后面的覆盖前面的（同题多轮取最新）
            }
        }
        result.answered = primaryByQ.size();

        // 汇总得分
        for (ScaleQuestion q : questions) {
            AssessmentAnswer a = primaryByQ.get(q.getId());
            if (a == null) {
                continue;
            }
            result.fullScore += sumFullScore(q);
            Double s = optionScore(q, a.getNormalizedValue());
            if (s != null) {
                result.score += s;
            }
        }

        // 特殊逻辑判定：谵妄(B.21)、GLIM营养不良(B.20)无法用“得分区间”表达，
        // 须按标准中的条目组合逻辑（如 条目1且条目2阳性 且 条目3或4阳性）判定。
        if ("DELIRIUM".equals(scale.getCode()) || "GLIM".equals(scale.getCode())) {
            applyLogicRule(scale, questions, primaryByQ, result);
            return result;
        }

        // 风险判定
        applyRiskRule(scale, result);
        return result;
    }

    /**
     * 应用“条目组合逻辑”型判定（谵妄 / GLIM营养不良）。
     *
     * <p>按题目 sort_order 取各题归一化答案（1=是/阳性），再按国标 B.21 / B.20 的
     * 组合规则计算风险，而非简单得分求和。</p>
     *
     * @param scale     量表配置（取其 code 分支）
     * @param questions 该量表题目（按 sort_order）
     * @param primaryByQ 题目id→最终答案
     * @param result    待填充的计分结果
     */
    private void applyLogicRule(Scale scale, List<ScaleQuestion> questions,
                                Map<Long, AssessmentAnswer> primaryByQ, ScaleScore result) {
        // 收集按题目顺序排列的是/否答案（1=是）
        List<Integer> flags = new ArrayList<>();
        for (ScaleQuestion q : questions) {
            AssessmentAnswer a = primaryByQ.get(q.getId());
            if (a != null && a.getNormalizedValue() != null
                    && ("1".equals(a.getNormalizedValue()) || "0".equals(a.getNormalizedValue()))) {
                flags.add("1".equals(a.getNormalizedValue()) ? 1 : 0);
            }
        }

        if ("DELIRIUM".equals(scale.getCode())) {
            // B.21：条目1(急性起病与波动)且条目2(注意力不集中)阳性，且 条目3(思维混乱)或条目4(意识水平改变)阳性
            if (flags.size() >= 4 && flags.get(0) == 1 && flags.get(1) == 1 && (flags.get(2) == 1 || flags.get(3) == 1)) {
                result.risk = 2;
                result.riskLevel = "谵妄(意识模糊)";
                result.riskText = "符合谵妄诊断标准，建议立即由医务人员进行专业评估";
                result.suggestions.add("建议尽快请医务人员进一步评估与处理");
                result.suggestions.add("评估期间注意防跌倒，避免患者独处");
            } else {
                result.risk = 0;
                result.riskLevel = "未见谵妄";
                result.riskText = "未发现符合谵妄诊断标准的表现";
                result.suggestions.add("如出现意识突然改变或注意力异常，请及时就医");
            }
        } else if ("GLIM".equals(scale.getCode())) {
            // B.20：任一体重减轻/低BMI/肌肉减少（表现型） 且 任一摄入减少/炎症（病因型） → 营养不良
            boolean anyPhenotype = flags.size() >= 3 && (flags.get(0) == 1 || flags.get(1) == 1 || flags.get(2) == 1);
            boolean anyEtiology = flags.size() >= 5 && (flags.get(3) == 1 || flags.get(4) == 1);
            if (anyPhenotype && anyEtiology) {
                // 严重程度：显著体重下降或低BMI → 重度；否则中度
                boolean severe = flags.get(0) == 1 || flags.get(1) == 1;
                result.risk = 2;
                result.riskLevel = severe ? "营养不良(重度)" : "营养不良(中度)";
                result.riskText = "符合GLIM营养不良诊断标准";
                result.suggestions.add("建议尽早就医，由营养专业人员制定营养干预计划");
            } else {
                result.risk = 0;
                result.riskLevel = "未见营养不良";
                result.riskText = "未发现符合GLIM营养不良诊断标准的表现";
                result.suggestions.add("保持均衡饮食，适当增加优质蛋白摄入");
            }
        }
    }

    /** 该题满分：取全部选项中的最大分值（与国标“各题满分之和”口径一致） */
    private double sumFullScore(ScaleQuestion q) {
        double maxScore = 0;
        JsonNode opts = parse(q.getOptions());
        if (opts != null && opts.isArray()) {
            for (JsonNode o : opts) {
                if (o.has("score")) {
                    maxScore = Math.max(maxScore, o.get("score").asDouble());
                }
            }
        }
        return maxScore;
    }

    /** 取某选项分值 */
    private Double optionScore(ScaleQuestion q, String value) {
        JsonNode opts = parse(q.getOptions());
        if (opts == null || !opts.isArray()) {
            return null;
        }
        for (JsonNode o : opts) {
            if (value.equals(o.path("value").asText())) {
                return o.path("score").isMissingNode() ? null : o.get("score").asDouble();
            }
        }
        return null;
    }

    /**
     * 应用 risk_rule 阈值区间判定风险。
     *
     * <p>兼容两种 JSON 结构，兼容历史/新种子数据：
     * <pre>
     *   A. {"levels":[{...},{...}]}    —— 对象包裹形式（后台导入/DataInitializer 旧格式）
     *   B. [{...},{...}]               —— 裸数组形式（seed.sql 新格式）
     * </pre>
     * 两种都会被解析为 levels 列表参与区间判定。</p>
     */
    private void applyRiskRule(Scale scale, ScaleScore result) {
        JsonNode rule = parse(scale.getRiskRule());
        JsonNode levels = null;
        if (rule != null) {
            if (rule.isArray()) {
                levels = rule;                     // 形式 B：裸数组
            } else if (rule.has("levels")) {
                levels = rule.get("levels");       // 形式 A：{"levels":[...]}
            }
        }
        if (levels == null || !levels.isArray()) {
            result.risk = 0;
            result.riskLevel = "未配置";
            result.riskText = "该量表未配置风险判定规则，请后台维护";
            return;
        }
        for (JsonNode lv : levels) {
            double min = lv.path("min").isMissingNode() ? Double.MIN_VALUE : lv.get("min").asDouble();
            double max = lv.path("max").isMissingNode() ? Double.MAX_VALUE : lv.get("max").asDouble();
            if (result.score >= min && result.score <= max) {
                result.risk = lv.path("risk").asInt(0);
                result.riskLevel = lv.path("level").asText("正常");
                result.riskText = lv.path("text").asText("");
                if (lv.has("suggestions") && lv.get("suggestions").isArray()) {
                    lv.get("suggestions").forEach(s -> result.suggestions.add(s.asText()));
                }
                break;
            }
        }
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", json);
            return null;
        }
    }
}
