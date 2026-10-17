package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.Scale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 量表配置数据访问层。
 */
public interface ScaleRepository extends JpaRepository<Scale, Long> {

    /** 启用量表按执行顺序排列 */
    List<Scale> findByEnabledTrueOrderBySortOrderAsc();

    Optional<Scale> findByCode(String code);

    List<Scale> findByNameContaining(String name);
}
