package com.geriatric.assessment.scheduler;

import com.geriatric.assessment.entity.AssessmentAnswer;
import com.geriatric.assessment.entity.AssessmentReport;
import com.geriatric.assessment.entity.AssessmentTask;
import com.geriatric.assessment.repository.AssessmentAnswerRepository;
import com.geriatric.assessment.repository.AssessmentReportRepository;
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
 * 数据生命周期清理定时任务。
 *
 * <p>两类自动清理：</p>
 * <ul>
 *   <li>未完成草稿：自最后作答起保留3天（draft_retention_days），超期清除</li>
 *   <li>已完成记录：自完成时间起保留30天（history_retention_days），超期自动删除</li>
 * </ul>
 * 默认每天凌晨 03:00 执行一次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DraftCleanupScheduler {

    private final AssessmentTaskRepository taskRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final AssessmentReportRepository reportRepository;
    private final ParamService paramService;

    /** 每天 03:00 执行 */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanup() {
        cleanupExpiredDrafts();
        cleanupExpiredHistory();
    }

    /** 清理过期未完成草稿 */
    private void cleanupExpiredDrafts() {
        int days = paramService.getInt("draft_retention_days", 3);
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        List<AssessmentTask> expired = taskRepository
                .findByStatusNotAndLastAnswerAtBefore("COMPLETED", deadline);
        if (expired.isEmpty()) {
            return;
        }
        log.info("清理过期草稿 {} 条（保留{}天）", expired.size(), days);
        for (AssessmentTask task : expired) {
            answerRepository.deleteByTaskId(task.getId());
            reportRepository.deleteByTaskId(task.getId());
            taskRepository.delete(task);
        }
    }

    /** 清理过期已完成历史记录（默认30天） */
    private void cleanupExpiredHistory() {
        int days = paramService.getInt("history_retention_days", 30);
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        List<AssessmentTask> expired = taskRepository
                .findByStatusAndCompletedAtBefore("COMPLETED", deadline);
        if (expired.isEmpty()) {
            return;
        }
        log.info("清理过期历史记录 {} 条（保留{}天）", expired.size(), days);
        for (AssessmentTask task : expired) {
            answerRepository.deleteByTaskId(task.getId());
            reportRepository.deleteByTaskId(task.getId());
            taskRepository.delete(task);
        }
    }
}
