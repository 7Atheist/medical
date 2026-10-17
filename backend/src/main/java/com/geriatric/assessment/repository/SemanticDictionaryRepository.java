package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.SemanticDictionary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 语义映射词典数据访问层。
 */
public interface SemanticDictionaryRepository extends JpaRepository<SemanticDictionary, Long> {

    /** 指定题目的词典 + 全局通用词典 */
    List<SemanticDictionary> findByQuestionIdOrQuestionIdIsNullOrderBySortOrderAsc(Long questionId);
}
