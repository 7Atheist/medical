package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.AssessmentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 评估答案数据访问层。
 */
public interface AssessmentAnswerRepository extends JpaRepository<AssessmentAnswer, Long> {

    /** 某任务的全部答案 */
    List<AssessmentAnswer> findByTaskIdOrderByIdAsc(Long taskId);

    /** 某任务某题的答案 */
    List<AssessmentAnswer> findByTaskIdAndQuestionId(Long taskId, Long questionId);

    /** 按任务ID删除全部答案（删除任务时级联清理） */
    void deleteByTaskId(Long taskId);
}
