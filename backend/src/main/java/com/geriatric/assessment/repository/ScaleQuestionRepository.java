package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.ScaleQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 量表题目数据访问层。
 */
public interface ScaleQuestionRepository extends JpaRepository<ScaleQuestion, Long> {

    /** 某量表的启用题目按顺序 */
    List<ScaleQuestion> findByScaleIdAndEnabledTrueOrderBySortOrderAsc(Long scaleId);
}
