-- =====================================================================
-- 自助式老年综合评估对话系统 - 数据库初始化脚本
-- 数据库: MySQL 8.0+  (字符集 utf8mb4，支持中文与emoji无关的扩展字符)
-- 用途  : 初始化业务主数据、评估草稿、量表配置、报告、系统参数等全部表结构
-- 说明  : 敏感字段(身份证号、健康答案等)由应用层AES加密后落库，本脚本只建表
-- =====================================================================

-- 创建数据库(如不存在)
CREATE DATABASE IF NOT EXISTS `geriatric_assessment`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE `geriatric_assessment`;

-- =====================================================================
-- 1. 用户表：实名用户（姓名 + 身份证号核验登录，无需账号密码）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(64)     NOT NULL                COMMENT '用户姓名',
  `id_card`     VARCHAR(256)    NOT NULL                COMMENT '身份证号(AES加密存储)',
  `id_card_mask`VARCHAR(32)     NOT NULL                COMMENT '身份证号脱敏展示(如 4401**********1234)',
  `birth_date`  DATE            NOT NULL                COMMENT '出生日期(由身份证号自动解析)',
  `age`         INT             NOT NULL                COMMENT '年龄(自动计算)',
  `gender`      TINYINT         NOT NULL DEFAULT 0      COMMENT '性别 1男 2女(由身份证号第17位解析)',
  `phone`       VARCHAR(64)     DEFAULT NULL            COMMENT '联系电话(AES加密，可选)',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_id_card_hash` (`id_card`)               -- 身份证号密文唯一，防止重复建档
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实名用户表';

-- =====================================================================
-- 2. 评估任务表：一次完整评估的生命周期（初筛→分级→评估→报告/草稿）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `assessment_task` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id`            BIGINT UNSIGNED NOT NULL                COMMENT '所属用户ID',
  `status`             VARCHAR(32)     NOT NULL DEFAULT 'SCREENING'
                                       COMMENT '任务状态 INIT/SCREENING(初筛)/ASSESSING(全量评估)/COMPLETED(完成归档)',
  `input_mode`         VARCHAR(16)     NOT NULL DEFAULT 'MANUAL'
                                       COMMENT '输入模式 MANUAL手动 / VOICE语音',
  `current_scale_id`   BIGINT UNSIGNED DEFAULT NULL            COMMENT '当前正在执行的量表ID',
  `current_question_id`BIGINT UNSIGNED DEFAULT NULL            COMMENT '当前题目ID',
  `followup_count`     INT             NOT NULL DEFAULT 0      COMMENT '当前题已连续追问次数(≤2)',
  `completed_scales`   JSON            DEFAULT NULL            COMMENT '已完成的量表ID数组(JSON)，用于FSM推进',
  `screening_done`     TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '初筛是否完成 0否 1是',
  `screening_risk`     TINYINT         DEFAULT NULL            COMMENT '初筛风险 0低风险 1存在风险(NULL表示未判定)',
  `last_answer_at`     DATETIME        DEFAULT NULL            COMMENT '最后一次作答时间(草稿3天保活依据)',
  `completed_at`       DATETIME        DEFAULT NULL            COMMENT '评估完成时间',
  `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估任务表';

-- =====================================================================
-- 3. 评估答案表：每一轮问答的原始输入、归一化结果、置信度、追问标记
-- =====================================================================
CREATE TABLE IF NOT EXISTS `assessment_answer` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`         BIGINT UNSIGNED NOT NULL                COMMENT '所属任务ID',
  `scale_id`        BIGINT UNSIGNED DEFAULT NULL            COMMENT '量表ID',
  `question_id`     BIGINT UNSIGNED DEFAULT NULL            COMMENT '题目ID',
  `user_text`       TEXT            DEFAULT NULL            COMMENT '老人原始输入(AES加密)',
  `asr_text`        TEXT            DEFAULT NULL            COMMENT '语音识别转写文本(AES加密)',
  `normalized_value`VARCHAR(64)     DEFAULT NULL            COMMENT '归一化后的标准选项值',
  `normalized_text` VARCHAR(128)    DEFAULT NULL            COMMENT '归一化后的标准选项文本',
  `confidence`      DECIMAL(4,3)    DEFAULT NULL            COMMENT '匹配置信度 0~1',
  `uncertain`       TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否标记答案不确定 0否 1是',
  `is_followup`     TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否为追问轮次 0首次 1追问',
  `followup_of`     BIGINT UNSIGNED DEFAULT NULL            COMMENT '追问所属的首次作答ID',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估答案表';

-- =====================================================================
-- 4. 量表表：21套量表的元信息、计分与风险判定规则（后台可导入维护）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `scale` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '量表ID',
  `code`          VARCHAR(32)     NOT NULL                COMMENT '量表编码，如 FRAIL',
  `name`          VARCHAR(128)    NOT NULL                COMMENT '量表名称',
  `table_no`      VARCHAR(16)     DEFAULT NULL            COMMENT '标准表号，如 表B.10',
  `category`      VARCHAR(64)     NOT NULL                COMMENT '评估类别，如 躯体功能/精神心理/社会与环境/老年综合征',
  `is_screening`  TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否为初筛量表 0否 1是',
  `sort_order`    INT             NOT NULL DEFAULT 0      COMMENT '执行顺序',
  `enabled`       TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '是否启用',
  `scoring_rule`  JSON            DEFAULT NULL            COMMENT '计分规则JSON(含各题选项分值、汇总方式)',
  `risk_rule`     JSON            DEFAULT NULL            COMMENT '风险判定规则JSON(阈值→风险等级/提示文案)',
  `description`   TEXT            DEFAULT NULL            COMMENT '评估内容与评判要点说明',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量表配置表';

-- =====================================================================
-- 5. 量表题目表：题目题干、关怀式提问文案、选项、跳转规则
-- =====================================================================
CREATE TABLE IF NOT EXISTS `scale_question` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `scale_id`      BIGINT UNSIGNED NOT NULL                COMMENT '所属量表ID',
  `question_type` VARCHAR(16)     NOT NULL DEFAULT 'CHOICE'
                                          COMMENT '题型 CHOICE选择题 / OPEN开放题',
  `question_text` TEXT            NOT NULL                COMMENT '量表原始题干',
  `dialog_text`   TEXT            DEFAULT NULL            COMMENT '有温度关怀式提问文案(对话话术)',
  `options`       JSON            DEFAULT NULL            COMMENT '选项JSON数组:[{value,label,score}]',
  `jump_rule`     JSON            DEFAULT NULL            COMMENT '跳转规则JSON:选项值→下一题/结束',
  `sort_order`    INT             NOT NULL DEFAULT 0      COMMENT '题目顺序',
  `enabled`       TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '是否启用',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_scale` (`scale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量表题目表';

-- =====================================================================
-- 6. 语义映射词典表：口语化表述 → 量表标准选项（第一层规则匹配）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `semantic_dictionary` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `question_id`   BIGINT UNSIGNED DEFAULT NULL            COMMENT '题目ID(NULL表示全局通用词典)',
  `option_value`  VARCHAR(64)     NOT NULL                COMMENT '映射到的标准选项值',
  `synonyms`      VARCHAR(512)    NOT NULL                COMMENT '口语同义词/关键词，逗号分隔',
  `match_type`    VARCHAR(16)     NOT NULL DEFAULT 'CONTAINS'
                                        COMMENT '匹配方式 CONTAINS包含 / EQUALS完全等于 / REGEX正则',
  `sort_order`    INT             NOT NULL DEFAULT 0      COMMENT '优先级(越小越先匹配)',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语义映射词典表';

-- =====================================================================
-- 7. 对话话术配置表：关怀式话术、控话提示、疲劳提示、播报话术
-- =====================================================================
CREATE TABLE IF NOT EXISTS `dialog_config` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene`       VARCHAR(32)     NOT NULL                COMMENT '场景编码: GREETING问候/REPEAT重复/FOLLOWUP追问/FATIGUE疲劳/UNCERTAIN不确定/GUIDE控话/DISCLAIMER免责',
  `content`     TEXT            NOT NULL                COMMENT '话术内容(支持{name}等占位符)',
  `sort_order`  INT             NOT NULL DEFAULT 0      COMMENT '顺序',
  `enabled`     TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '是否启用',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话话术配置表';

-- =====================================================================
-- 8. 系统参数表：追问最大次数、置信度阈值、草稿保留周期等
-- =====================================================================
CREATE TABLE IF NOT EXISTS `sys_param` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `param_key`   VARCHAR(64)     NOT NULL                COMMENT '参数键',
  `param_value` VARCHAR(512)    NOT NULL                COMMENT '参数值',
  `description` VARCHAR(255)    DEFAULT NULL            COMMENT '参数说明',
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`param_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

-- =====================================================================
-- 9. 评估报告表：评估完成后生成的可视化报告与PDF文件
-- =====================================================================
CREATE TABLE IF NOT EXISTS `assessment_report` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`     BIGINT UNSIGNED NOT NULL                COMMENT '关联任务ID',
  `user_id`     BIGINT UNSIGNED NOT NULL                COMMENT '所属用户ID',
  `content_json`JSON            NOT NULL                COMMENT '报告内容JSON(各量表得分、风险、建议)',
  `pdf_path`    VARCHAR(512)    DEFAULT NULL            COMMENT 'PDF文件路径(MinIO或本地)',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估报告表';

-- =====================================================================
-- 10. 系统日志表：后台管理查看的操作日志
-- =====================================================================
CREATE TABLE IF NOT EXISTS `sys_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT UNSIGNED DEFAULT NULL            COMMENT '操作用户ID',
  `action`      VARCHAR(128)    NOT NULL                COMMENT '操作动作',
  `detail`      TEXT            DEFAULT NULL            COMMENT '操作详情',
  `ip`          VARCHAR(64)     DEFAULT NULL            COMMENT '来源IP',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';
