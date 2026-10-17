package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.SysLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 系统日志数据访问层。
 */
public interface SysLogRepository extends JpaRepository<SysLog, Long> {
}
