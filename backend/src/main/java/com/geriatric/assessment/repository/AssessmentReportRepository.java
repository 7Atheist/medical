package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.AssessmentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 评估报告数据访问层。
 */
public interface AssessmentReportRepository extends JpaRepository<AssessmentReport, Long> {

    /** 按任务ID查最新报告（可能存在历史重复生成的记录，取最新一条） */
    Optional<AssessmentReport> findFirstByTaskIdOrderByIdDesc(Long taskId);

    List<AssessmentReport> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 按任务ID删除报告（删除任务时级联清理） */
    void deleteByTaskId(Long taskId);
}
