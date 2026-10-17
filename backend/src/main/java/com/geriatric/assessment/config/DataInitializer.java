package com.geriatric.assessment.config;

import com.geriatric.assessment.entity.Scale;
import com.geriatric.assessment.repository.ScaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 启动数据初始化：为缺少计分/风险规则的量表补充默认规则。
 *
 * <p>规则来源于方案附录A的计分阈值，后台可随时在【量表管理】中覆盖。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ScaleRepository scaleRepository;

    /** 量表编码 → 默认 risk_rule JSON */
    private static final Map<String, String> DEFAULT_RISK_RULES = new LinkedHashMap<>();

    static {
        // FRAIL衰弱：总分0~5
        DEFAULT_RISK_RULES.put("FRAIL",
                "{\"levels\":[" +
                        "{\"max\":0,\"risk\":0,\"level\":\"无衰弱\",\"text\":\"未见明显衰弱表现\",\"suggestions\":[\"继续保持良好的饮食与适度活动\"]}," +
                        "{\"min\":1,\"max\":2,\"risk\":1,\"level\":\"衰弱前期\",\"text\":\"存在衰弱前期迹象，建议关注\",\"suggestions\":[\"建议加强蛋白质营养摄入，逐步增加力量锻炼\",\"可咨询社区健康管理师\"]}," +
                        "{\"min\":3,\"risk\":2,\"level\":\"衰弱\",\"text\":\"存在衰弱表现\",\"suggestions\":[\"建议尽早就医进行全面综合评估\",\"注意跌倒防护，改善居家安全\"]}]}");
        // 阿森斯失眠：总分0~24
        DEFAULT_RISK_RULES.put("ATHENS-INSOMNIA",
                "{\"levels\":[" +
                        "{\"max\":3,\"risk\":0,\"level\":\"无睡眠障碍\",\"text\":\"未发现明显睡眠障碍\",\"suggestions\":[\"保持规律作息，白天适度活动\"]}," +
                        "{\"min\":4,\"max\":6,\"risk\":1,\"level\":\"可能有睡眠障碍\",\"text\":\"可能存在睡眠障碍，建议关注\",\"suggestions\":[\"睡前避免浓茶咖啡，保持卧室安静\",\"如持续存在可咨询医生\"]}," +
                        "{\"min\":7,\"risk\":2,\"level\":\"存在睡眠障碍\",\"text\":\"存在明显睡眠障碍\",\"suggestions\":[\"建议就医评估睡眠问题\",\"避免自行长期服用安眠药\"]}]}");
        // 基本日常生活活动能力：总分0~100
        DEFAULT_RISK_RULES.put("ADL-BASIC",
                "{\"levels\":[" +
                        "{\"max\":100,\"risk\":0,\"level\":\"无依赖\",\"text\":\"日常生活活动能力正常\",\"suggestions\":[\"保持现有自理能力，注意适度锻炼\"]}," +
                        "{\"min\":61,\"max\":99,\"risk\":1,\"level\":\"轻度依赖\",\"text\":\"日常生活活动轻度依赖\",\"suggestions\":[\"可借助辅助器具改善，注意居家安全\"]}," +
                        "{\"min\":41,\"max\":60,\"risk\":2,\"level\":\"中度依赖\",\"text\":\"日常生活活动中度依赖\",\"suggestions\":[\"建议家人加强看护，评估是否需要辅助服务\"]}," +
                        "{\"min\":0,\"max\":40,\"risk\":3,\"level\":\"重度依赖\",\"text\":\"日常生活活动重度依赖\",\"suggestions\":[\"建议及时就医并考虑专业照护\"]}]}");
        // 工具性日常生活活动能力：总分0~8
        DEFAULT_RISK_RULES.put("ADL-INSTRUMENTAL",
                "{\"levels\":[" +
                        "{\"max\":8,\"risk\":0,\"level\":\"正常\",\"text\":\"工具性日常生活活动正常\",\"suggestions\":[\"继续保持\"]}," +
                        "{\"min\":6,\"max\":7,\"risk\":1,\"level\":\"轻度依赖\",\"text\":\"工具性日常生活轻度依赖\",\"suggestions\":[\"家人可适当协助复杂事务\"]}," +
                        "{\"min\":3,\"max\":5,\"risk\":2,\"level\":\"中度依赖\",\"text\":\"工具性日常生活中度依赖\",\"suggestions\":[\"建议加强家人支持，评估社区服务\"]}," +
                        "{\"min\":0,\"max\":2,\"risk\":3,\"level\":\"严重依赖\",\"text\":\"工具性日常生活严重依赖\",\"suggestions\":[\"建议及时就医并考虑专业照护\"]}]}");
    }

    @Override
    public void run(String... args) {
        for (Scale scale : scaleRepository.findAll()) {
            if ((scale.getRiskRule() == null || scale.getRiskRule().isBlank())
                    && DEFAULT_RISK_RULES.containsKey(scale.getCode())) {
                scale.setRiskRule(DEFAULT_RISK_RULES.get(scale.getCode()));
                scaleRepository.save(scale);
                log.info("初始化量表[{}]默认风险规则", scale.getCode());
            }
        }
    }
}
