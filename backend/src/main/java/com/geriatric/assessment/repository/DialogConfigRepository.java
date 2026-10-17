package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.DialogConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 对话话术数据访问层。
 */
public interface DialogConfigRepository extends JpaRepository<DialogConfig, Long> {

    Optional<DialogConfig> findBySceneAndEnabledTrue(String scene);
}
