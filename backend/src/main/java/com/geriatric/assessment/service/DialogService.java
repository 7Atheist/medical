package com.geriatric.assessment.service;

import com.geriatric.assessment.entity.DialogConfig;
import com.geriatric.assessment.repository.DialogConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 对话话术服务：根据场景取话术并填充 {name}、{question} 等占位符。
 */
@Service
@RequiredArgsConstructor
public class DialogService {

    private final DialogConfigRepository repository;

    /** 取某场景话术，并替换占位符 */
    public String render(String scene, Map<String, String> vars) {
        String template = repository.findBySceneAndEnabledTrue(scene)
                .map(DialogConfig::getContent)
                .orElse("");
        if (template.isEmpty()) {
            return "";
        }
        String out = template;
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        return out;
    }
}
