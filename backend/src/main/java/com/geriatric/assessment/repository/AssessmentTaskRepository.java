package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.AssessmentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评估任务数据访问层。
 */
public interface AssessmentTaskRepository extends JpaRepository<AssessmentTask, Long> {

    /** 某用户全部任务（历史记录），新→旧 */
    List<AssessmentTask> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 按状态统计 */
    long countByStatus(String status);

    /** 某用户未完成草稿 */
    List<AssessmentTask> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, String status);

    /** 定时清理：未完成且超过保留期的草稿 */
    List<AssessmentTask> findByStatusNotAndLastAnswerAtBefore(String status, LocalDateTime deadline);
}
