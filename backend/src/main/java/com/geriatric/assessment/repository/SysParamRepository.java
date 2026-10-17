package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.SysParam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 系统参数数据访问层。
 */
public interface SysParamRepository extends JpaRepository<SysParam, Long> {

    Optional<SysParam> findByParamKey(String paramKey);
}
