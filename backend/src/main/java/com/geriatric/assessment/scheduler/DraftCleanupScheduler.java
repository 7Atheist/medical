package com.geriatric.assessment.scheduler;

import com.geriatric.assessment.entity.AssessmentAnswer;
import com.geriatric.assessment.entity.AssessmentTask;
import com.geriatric.assessment.repository.AssessmentAnswerRepository;
import com.geriatric.assessment.repository.AssessmentTaskRepository;
import com.geriatric.assessment.service.ParamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 草稿生命周期清理定时任务。
 *
 * <p>方案5.2 要求：未完成草稿自最后一次作答起保留3天，超期自动清除且不可恢复。
 * 默认每天凌晨清理一次；保留周期由系统参数 draft_retention_days 控制。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DraftCleanupScheduler {

    private final AssessmentTaskRepository taskRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final ParamService paramService;

    /** 每天 03:00 执行 */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredDrafts() {
        int days = paramService.getInt("draft_retention_days", 3);
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        List<AssessmentTask> expired = taskRepository
                .findByStatusNotAndLastAnswerAtBefore("COMPLETED", deadline);
        if (expired.isEmpty()) {
            return;
        }
        log.info("清理过期草稿 {} 条（保留{}天）", expired.size(), days);
        for (AssessmentTask task : expired) {
            // 先删答案，再删任务
            List<AssessmentAnswer> answers = answerRepository.findByTaskIdOrderByIdAsc(task.getId());
            answerRepository.deleteAll(answers);
            taskRepository.delete(task);
        }
    }
}
