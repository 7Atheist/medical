package com.geriatric.assessment.service;

import com.geriatric.assessment.entity.*;
import com.geriatric.assessment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理服务：量表配置、对话话术、语义词典、系统参数、用户与日志统计。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final SysUserRepository userRepository;
    private final AssessmentTaskRepository taskRepository;
    private final ScaleRepository scaleRepository;
    private final ScaleQuestionRepository questionRepository;
    private final SemanticDictionaryRepository dictionaryRepository;
    private final DialogConfigRepository dialogRepository;
    private final SysParamRepository paramRepository;
    private final SysLogRepository logRepository;

    /** 数据总览统计 */
    public Map<String, Object> stats() {
        Map<String, Object> m = new HashMap<>();
        m.put("userCount", userRepository.count());
        m.put("taskCount", taskRepository.count());
        m.put("scaleCount", scaleRepository.count());
        m.put("completedTaskCount", taskRepository.countByStatus("COMPLETED"));
        m.put("ageDist", userRepository.countByAge());
        return m;
    }

    // ---------------- 用户 ----------------
    public List<SysUser> users(String name) {
        if (name == null || name.isBlank()) {
            return userRepository.findAll();
        }
        return userRepository.findByNameContaining(name);
    }

    // ---------------- 量表 ----------------
    public List<Scale> scales() {
        return scaleRepository.findAll();
    }

    @Transactional
    public Scale saveScale(Scale scale) {
        if (scale.getId() != null) {
            scaleRepository.findById(scale.getId()).ifPresent(existing -> {
                scale.setCreatedAt(existing.getCreatedAt());
            });
        }
        return scaleRepository.save(scale);
    }

    public List<ScaleQuestion> questions(Long scaleId) {
        return questionRepository.findByScaleIdAndEnabledTrueOrderBySortOrderAsc(scaleId);
    }

    @Transactional
    public ScaleQuestion saveQuestion(ScaleQuestion q) {
        return questionRepository.save(q);
    }

    // ---------------- 语义词典 ----------------
    public List<SemanticDictionary> dictionary() {
        return dictionaryRepository.findAll();
    }

    @Transactional
    public SemanticDictionary saveDictionary(SemanticDictionary d) {
        return dictionaryRepository.save(d);
    }

    // ---------------- 对话话术 ----------------
    public List<DialogConfig> dialogs() {
        return dialogRepository.findAll();
    }

    @Transactional
    public DialogConfig saveDialog(DialogConfig d) {
        return dialogRepository.save(d);
    }

    // ---------------- 系统参数 ----------------
    public List<SysParam> params() {
        return paramRepository.findAll();
    }

    // ---------------- 日志 ----------------
    public List<SysLog> logs() {
        return logRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public void recordLog(Long userId, String action, String detail, String ip) {
        SysLog log = new SysLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetail(detail);
        log.setIp(ip);
        logRepository.save(log);
    }
}
