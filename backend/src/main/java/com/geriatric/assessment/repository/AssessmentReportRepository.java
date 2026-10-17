package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.AssessmentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 评估报告数据访问层。
 */
public interface AssessmentReportRepository extends JpaRepository<AssessmentReport, Long> {

    Optional<AssessmentReport> findByTaskId(Long taskId);

    List<AssessmentReport> findByUserIdOrderByCreatedAtDesc(Long userId);
}
