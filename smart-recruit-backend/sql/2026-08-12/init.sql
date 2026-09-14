-- ============================================================================
-- SmartRecruit Platform - 数据库初始化脚本（全新环境一键初始化）
-- 生成时间: 2026-08-12 17:46:25
-- 内容: 1) 创建 7 个业务库  2) 创建全部表  3) 写入系统基础数据
-- 基础数据: 系统配置/部门/权限/角色/用户(管理员、苏三)/题库种子（已过滤逻辑删除数据）
-- 业务数据（候选人、简历、Offer 等）不属于初始化数据，未包含。
-- ============================================================================

-- ============================================================================
-- 1. 创建数据库
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `smart_recruit_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_recruit_recruitment` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_recruit_interview` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_recruit_offer` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_recruit_talent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_recruit_referral` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_recruit_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================================
-- 2. 建表
-- ============================================================================

USE `smart_recruit_system`;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `config_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键，如 email_suffix',
  `config_value` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置值',
  `description` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

CREATE TABLE `sys_department` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父部门ID，0=顶级部门',
  `dept_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部门名称',
  `dept_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部门编码，全局唯一',
  `leader_id` bigint DEFAULT NULL COMMENT '负责人ID，关联sys_user.id',
  `headcount` int NOT NULL DEFAULT '0' COMMENT '编制人数',
  `staff_count` int NOT NULL DEFAULT '0' COMMENT '当前在岗人数',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '层级：1=一级部门，2=二级，3=三级...',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号，小值在前',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0=停用，1=启用',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '部门描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

CREATE TABLE `sys_notification` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `recipient_id` bigint NOT NULL COMMENT '接收人用户ID',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '通知类型：0=SYSTEM,1=INTERVIEW,2=OFFER,3=ONBOARDING,4=REFERRAL,5=TALENT',
  `title` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知内容',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '已读状态：0=未读，1=已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `action_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '点击跳转URL',
  `business_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联业务类型',
  `business_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_notif_unread` (`recipient_id`,`is_read`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

CREATE TABLE `sys_operation_log` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `user_id` bigint DEFAULT NULL COMMENT '操作人用户ID',
  `username` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人用户名',
  `module` tinyint NOT NULL DEFAULT '0' COMMENT '操作模块：0=SYSTEM,1=JOB,2=CANDIDATE,3=INTERVIEW,4=OFFER,5=ONBOARD,6=TALENT,7=REFERRAL,8=AI',
  `action` tinyint NOT NULL DEFAULT '0' COMMENT '操作动作：0=CREATE,1=UPDATE,2=DELETE,3=EXPORT,4=IMPORT',
  `target_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标资源类型，如rec_job_position',
  `target_id` bigint DEFAULT NULL COMMENT '目标资源ID',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作描述',
  `request_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HTTP方法',
  `request_uri` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URI',
  `request_params` json DEFAULT NULL COMMENT '请求参数（敏感字段已脱敏）',
  `response_status` int DEFAULT NULL COMMENT 'HTTP响应状态码',
  `client_ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'User-Agent',
  `duration_ms` bigint DEFAULT NULL COMMENT '请求耗时（毫秒）',
  `error_msg` text COLLATE utf8mb4_unicode_ci COMMENT '异常信息',
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '全链路追踪ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_oplog_module_action` (`module`,`action`,`create_time`),
  KEY `idx_oplog_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';

CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父权限ID，0=顶层',
  `perm_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称，如"用户列表"',
  `perm_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码，如recruitment:job:view',
  `perm_type` tinyint NOT NULL DEFAULT '1' COMMENT '类型：1=菜单，2=按钮，3=API',
  `module` tinyint NOT NULL DEFAULT '0' COMMENT '所属模块：0=RECRUIT,1=JOB,2=CANDIDATE,3=INTERVIEW,4=TALENT,5=ANALYTICS,6=OFFER,7=ONBOARD,8=REFERRAL,9=SYSTEM,10=AI',
  `path` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '前端路由路径',
  `component` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '前端组件路径',
  `icon` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标class',
  `api_method` tinyint DEFAULT NULL COMMENT 'HTTP方法：0=GET,1=POST,2=PUT,3=DELETE',
  `api_path` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API接口路径',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0=停用，1=启用',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '可见性：0=隐藏，1=可见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`),
  KEY `idx_module` (`module`),
  KEY `idx_perm_parent_sort` (`parent_id`,`sort_order`),
  KEY `idx_perm_type_status` (`perm_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE `sys_role` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `role_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称，如"超级管理员"',
  `role_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码，如ROLE_SUPER_ADMIN',
  `description` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0=停用，1=启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_role_sort` (`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `role_id` bigint NOT NULL COMMENT '角色ID，关联sys_role.id',
  `permission_id` bigint NOT NULL COMMENT '权限ID，关联sys_permission.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

CREATE TABLE `sys_user` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `username` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名，登录账号，全局唯一',
  `password` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码，BCrypt加密，强度12',
  `real_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '真实姓名',
  `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电子邮箱',
  `mobile` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号码',
  `avatar` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint NOT NULL DEFAULT '0' COMMENT '性别：0=未知，1=男，2=女',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID，关联sys_department.id',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=正常，2=冻结，3=待审批',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近登录时间',
  `last_login_ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最近登录IP',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  `referral_code` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '注册时使用的内推码',
  `position` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职位名称',
  `job_level` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职级（如P5/P6/M1等）',
  `age` int DEFAULT NULL COMMENT '年龄',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_mobile` (`mobile`),
  KEY `idx_email` (`email`),
  KEY `idx_user_status_deleted` (`status`,`deleted`),
  KEY `idx_user_dept_status` (`department_id`,`status`),
  KEY `idx_sys_user_referral_code` (`referral_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联sys_user.id',
  `role_id` bigint NOT NULL COMMENT '角色ID，关联sys_role.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';


USE `smart_recruit_recruitment`;
CREATE TABLE `analytics_candidate_daily` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `stage` tinyint NOT NULL COMMENT '候选人当前阶段（rec_candidate.status）',
  `source` tinyint NOT NULL COMMENT '来源渠道（rec_candidate.source）',
  `candidate_count` int NOT NULL DEFAULT '0' COMMENT '当日新增候选人数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`stat_date`,`stage`,`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人日汇总（定时任务统计）';

CREATE TABLE `careers_job_position` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rec_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SOCIAL' COMMENT '招聘类型: SOCIAL/CAMPUS/HOT',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位名称',
  `dept` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部门/团队',
  `location` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作地点',
  `exp` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '经验要求',
  `salary` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '薪资范围',
  `category` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '分类: tech/product/market/data/operation',
  `tags` json DEFAULT NULL COMMENT '标签数组 [{text, cls}]',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1=已发布, 0=草稿',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=正常 1=已删除',
  `responsibilities` json DEFAULT NULL COMMENT '岗位职责: ["职责1", "职责2"]',
  `requirements` json DEFAULT NULL COMMENT '任职要求: ["要求1", "要求2"]',
  `bonus` json DEFAULT NULL COMMENT '加分项:   ["加分1", "加分2"]',
  PRIMARY KEY (`id`),
  KEY `idx_rec_type_status` (`rec_type`,`status`,`sort_order`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招聘官网职位表';

CREATE TABLE `rec_activity_feed` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '活动类型：0=APPLY,1=SCREEN,2=INTERVIEW,3=OFFER,4=HIRE,5=REFERRAL,6=SYSTEM',
  `title` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动态标题',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '动态描述',
  `actor_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `actor_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人姓名',
  `related_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联业务类型',
  `related_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_related` (`related_type`,`related_id`),
  KEY `idx_activity_type_time` (`type`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态消息表';

CREATE TABLE `rec_ai_screening_result` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `resume_id` bigint NOT NULL COMMENT '简历ID，关联rec_resume.id',
  `overall_score` decimal(5,2) DEFAULT NULL COMMENT '综合评分（0-100）',
  `dimensions` json DEFAULT NULL COMMENT '维度评分列表',
  `matched_keywords` json DEFAULT NULL COMMENT '匹配的关键词列表',
  `missing_keywords` json DEFAULT NULL COMMENT '缺失的关键词列表',
  `suggestion` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '建议：STRONG_HIRE/HIRE/CONSIDER/REJECT',
  `summary_text` text COLLATE utf8mb4_unicode_ci COMMENT 'AI分析摘要',
  `strengths` json DEFAULT NULL COMMENT '优势列表',
  `weaknesses` json DEFAULT NULL COMMENT '劣势列表',
  `source` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HEURISTIC' COMMENT '来源：LLM/HEURISTIC',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI简历筛选结果表';

CREATE TABLE `rec_application` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID，关联rec_candidate.id',
  `job_position_id` bigint NOT NULL COMMENT '职位ID，关联rec_job_position.id',
  `resume_id` bigint DEFAULT NULL COMMENT '投递使用的简历ID，关联rec_resume.id',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=PENDING,1=SCREENING,2=SCREEN_PASSED,3=SCREEN_FAILED,4=INTERVIEWING,5=INTERVIEW_PASSED,6=INTERVIEW_FAILED,7=OFFER_PENDING,8=OFFER_ACCEPTED,9=OFFER_DECLINED,10=HIRED,11=REJECTED,12=WITHDRAWN',
  `current_stage` tinyint NOT NULL DEFAULT '0' COMMENT '当前阶段：0=RESUME_SCREEN,1=INTERVIEW,2=OFFER,3=ONBOARDING',
  `match_score` decimal(5,2) DEFAULT NULL COMMENT 'AI匹配分数（0-100）',
  `match_detail` json DEFAULT NULL COMMENT 'AI匹配详情（5维JSON）',
  `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_candidate_job` (`candidate_id`,`job_position_id`),
  KEY `idx_current_stage` (`current_stage`),
  KEY `idx_apply_time` (`apply_time`),
  KEY `idx_match_score` (`match_score`),
  KEY `idx_app_status_stage` (`status`,`current_stage`),
  KEY `idx_app_job_status` (`job_position_id`,`status`),
  KEY `idx_app_candidate_time` (`candidate_id`,`apply_time`),
  KEY `idx_app_candidate_job` (`candidate_id`,`job_position_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递记录表';

CREATE TABLE `rec_candidate` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `user_id` bigint DEFAULT NULL COMMENT '系统用户 ID (sys_user.id)',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '候选人姓名',
  `email` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '电子邮箱，全局唯一',
  `mobile` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号码',
  `gender` tinyint NOT NULL DEFAULT '0' COMMENT '性别：0=未知，1=男，2=女',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `id_card` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '身份证号（AES-256加密存储）',
  `avatar_color` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像背景色（前端显示）',
  `avatar_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像图片URL（从简历PDF中提取）',
  `education_level` tinyint DEFAULT NULL COMMENT '学历：0=HIGH_SCHOOL,1=ASSOCIATE,2=BACHELOR,3=MASTER,4=PHD',
  `school` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '毕业院校',
  `major` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业',
  `years_of_experience` int NOT NULL DEFAULT '0' COMMENT '工作年限',
  `current_company` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前任职公司',
  `current_position` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前职位',
  `current_salary` int DEFAULT NULL COMMENT '当前月薪（CNY）',
  `expected_salary_min` int DEFAULT NULL COMMENT '最低期望月薪',
  `expected_salary_max` int DEFAULT NULL COMMENT '最高期望月薪',
  `city` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所在城市',
  `skills` json DEFAULT NULL COMMENT '技能标签（JSON数组），如["Java","Spring","MySQL"]',
  `ai_tags` json DEFAULT NULL COMMENT 'AI智能标签（JSON数组），如["后端技术","互联网行业","有管理经验"]',
  `source` tinyint NOT NULL DEFAULT '0' COMMENT '来源：0=DIRECT,1=REFERRAL,2=WEBSITE,3=LINKEDIN,4=BOSS,5=LAGOU,6=LIEPIN,7=OTHER',
  `source_detail` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源详情',
  `referrer_id` bigint DEFAULT NULL COMMENT '内推人ID，关联sys_user.id',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=NEW,1=SCREENING,2=SCREEN_PASSED,3=INTERVIEWING,4=OFFERED,5=HIRED,6=REJECTED,7=WITHDRAWN',
  `ai_match_score` decimal(5,2) DEFAULT NULL COMMENT 'AI综合匹配度（0-100）',
  `last_active_time` datetime DEFAULT NULL COMMENT '最近活跃时间',
  `tags` json DEFAULT NULL COMMENT '自定义标签（JSON数组）',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_name` (`name`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_source` (`source`),
  KEY `idx_referrer_id` (`referrer_id`),
  KEY `idx_city` (`city`),
  KEY `idx_years_of_experience` (`years_of_experience`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_candidate_status_source` (`status`,`source`),
  KEY `idx_candidate_deleted_status` (`deleted`,`status`),
  KEY `idx_rec_candidate_user_id` (`user_id`),
  KEY `idx_candidate_skills` ((cast(`skills` as char(256) array))),
  FULLTEXT KEY `ft_candidate_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人表';

CREATE TABLE `rec_candidate_stage_history` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID',
  `application_id` bigint NOT NULL COMMENT '投递记录ID',
  `from_stage` tinyint DEFAULT NULL COMMENT '原阶段：0=RESUME_SCREEN,1=INTERVIEW,2=OFFER,3=ONBOARDING',
  `to_stage` tinyint NOT NULL DEFAULT '0' COMMENT '新阶段：0=RESUME_SCREEN,1=INTERVIEW,2=OFFER,3=ONBOARDING',
  `from_status` tinyint DEFAULT NULL COMMENT '原状态：0=PENDING,1=SCREENING,2=SCREEN_PASSED,3=SCREEN_FAILED,4=INTERVIEWING,5=INTERVIEW_PASSED,6=INTERVIEW_FAILED,7=OFFER_PENDING,8=OFFER_ACCEPTED,9=OFFER_DECLINED,10=HIRED,11=REJECTED,12=WITHDRAWN',
  `to_status` tinyint NOT NULL DEFAULT '0' COMMENT '新状态：0=PENDING,1=SCREENING,2=SCREEN_PASSED,3=SCREEN_FAILED,4=INTERVIEWING,5=INTERVIEW_PASSED,6=INTERVIEW_FAILED,7=OFFER_PENDING,8=OFFER_ACCEPTED,9=OFFER_DECLINED,10=HIRED,11=REJECTED,12=WITHDRAWN',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人姓名',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '流转时间',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_stage_history_cand_app` (`candidate_id`,`application_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人阶段流转历史表';

CREATE TABLE `rec_communication_log` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '沟通类型：0=AI_SCREEN,1=PHONE_CALL,2=EMAIL,3=INTERVIEW,4=NOTE,5=REFERRAL,6=OFFER_COMM,7=OTHER',
  `title` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '沟通标题',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '沟通内容',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人姓名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '沟通时间',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_type` (`type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_comm_candidate_time` (`candidate_id`,`create_time`),
  KEY `idx_comm_type_time` (`type`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='沟通记录表';

CREATE TABLE `rec_job_channel` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `job_position_id` bigint NOT NULL COMMENT '职位ID，关联rec_job_position.id',
  `channel_code` tinyint NOT NULL DEFAULT '0' COMMENT '渠道编码：0=BOSS,1=LAGOU,2=LIEPIN,3=WEBSITE,4=LINKEDIN,5=OTHER',
  `channel_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '渠道名称：BOSS直聘/拉勾/猎聘/官网/LinkedIn/其他',
  `is_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0=关闭，1=启用',
  `external_job_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外部平台职位ID',
  `external_job_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外部平台职位链接',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `close_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_channel` (`job_position_id`,`channel_code`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_job_channel_enabled` (`job_position_id`,`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位-渠道发布记录表';

CREATE TABLE `rec_job_position` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位标题，如"高级Java开发工程师"',
  `department_id` bigint NOT NULL COMMENT '所属部门ID，关联sys_department.id',
  `position_level` tinyint DEFAULT NULL COMMENT '职级：0=P5,1=P6,2=P7,3=P8,4=P9,5=T2,6=T3,7=T4,8=T5,9=T6,10=T7,11=T8,12=T9',
  `position_type` tinyint NOT NULL DEFAULT '0' COMMENT '职位类型：0=FULL_TIME,1=PART_TIME,2=INTERN,3=CONTRACT',
  `experience_level` tinyint NOT NULL DEFAULT '2' COMMENT '经验要求：0=ENTRY,1=JUNIOR,2=MID,3=SENIOR,4=LEAD,5=EXECUTIVE',
  `education_level` tinyint DEFAULT NULL COMMENT '学历要求：0=HIGH_SCHOOL,1=ASSOCIATE,2=BACHELOR,3=MASTER,4=PHD',
  `min_salary` int DEFAULT NULL COMMENT '最低月薪（CNY）',
  `max_salary` int DEFAULT NULL COMMENT '最高月薪（CNY）',
  `hc_total` int NOT NULL DEFAULT '1' COMMENT 'HC总数',
  `hc_filled` int NOT NULL DEFAULT '0' COMMENT '已录取人数',
  `work_location` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作城市，如"北京"',
  `job_description` text COLLATE utf8mb4_unicode_ci COMMENT '职位描述（Markdown）',
  `responsibilities` json DEFAULT NULL COMMENT '岗位职责（JSON数组，AI生成或手动输入）',
  `requirements` json DEFAULT NULL COMMENT '任职要求（JSON数组）',
  `plus_points` json DEFAULT NULL COMMENT '加分项（JSON数组）',
  `skills` json DEFAULT NULL COMMENT '技能标签（JSON数组），如["Java","Spring","MySQL"]',
  `benefits` json DEFAULT NULL COMMENT '福利待遇（JSON数组）',
  `channels` json DEFAULT NULL COMMENT '发布渠道配置：{"boss":true,"lagou":true,"liepin":false,"website":true}',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=DRAFT,1=PUBLISHED,2=PAUSED,3=CLOSED',
  `is_urgent` tinyint NOT NULL DEFAULT '0' COMMENT '是否急聘：0=否，1=是',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `close_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `application_count` int NOT NULL DEFAULT '0' COMMENT '申请人数（缓存同步）',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量（缓存同步）',
  `responsible_id` bigint DEFAULT NULL COMMENT '招聘负责人ID，关联sys_user.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  `education_required` tinyint DEFAULT NULL COMMENT '学历要求: 0=高中,1=大专,2=本科,3=硕士,4=博士',
  `age_min` int DEFAULT NULL COMMENT '最低年龄要求',
  `age_max` int DEFAULT NULL COMMENT '最高年龄要求',
  PRIMARY KEY (`id`),
  KEY `idx_work_location` (`work_location`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_responsible_id` (`responsible_id`),
  KEY `idx_job_dept_status` (`department_id`,`status`),
  KEY `idx_job_urgent_status` (`is_urgent`,`status`),
  KEY `idx_job_status_publish` (`status`,`publish_time`),
  KEY `idx_job_type_location` (`position_type`,`work_location`),
  FULLTEXT KEY `ft_job_title_skills` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位表';

CREATE TABLE `rec_resume` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID，关联rec_candidate.id',
  `job_position_id` bigint DEFAULT NULL COMMENT '关联职位ID',
  `file_name` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `file_path` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件存储路径（MinIO/OSS）',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小（字节）',
  `file_type` tinyint DEFAULT NULL COMMENT '文件类型：0=PDF,1=DOCX,2=JPG,3=PNG',
  `parsed_content` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'AI解析纯文本内容',
  `parsed_json` json DEFAULT NULL COMMENT 'AI解析结构化数据（技能/教育/工作经历）',
  `parse_status` tinyint NOT NULL DEFAULT '0' COMMENT '解析状态：0=PENDING,1=PARSING,2=SUCCESS,3=FAILED',
  `parse_error` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '解析失败原因详情（异常类型+消息+堆栈）',
  `screening_status` tinyint NOT NULL DEFAULT '0' COMMENT '筛选状态：0=待处理,1=已通过,2=已淘汰',
  `auto_screen` tinyint NOT NULL DEFAULT '1' COMMENT '上传后是否自动AI筛选：1=开启,0=关闭',
  `parse_score` decimal(5,2) DEFAULT NULL COMMENT '解析质量评分（0-100）',
  `ai_match_score` decimal(5,2) DEFAULT NULL COMMENT 'AI综合匹配度（0-100）',
  `degree_score` decimal(5,2) DEFAULT NULL COMMENT '学历维度分',
  `skill_score` decimal(5,2) DEFAULT NULL COMMENT '技能维度分',
  `experience_score` decimal(5,2) DEFAULT NULL COMMENT '经验维度分',
  `behavior_score` decimal(5,2) DEFAULT NULL COMMENT '行为维度分',
  `semantic_score` decimal(5,2) DEFAULT NULL COMMENT '语义维度分',
  `match_detail` json DEFAULT NULL COMMENT 'AI匹配详情（5维JSON）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认简历：0=否，1=是',
  `source_channel` tinyint DEFAULT NULL COMMENT '来源渠道：0=DIRECT,1=REFERRAL,2=WEBSITE,3=LINKEDIN,4=BOSS,5=LAGOU,6=LIEPIN,7=OTHER',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  PRIMARY KEY (`id`),
  KEY `idx_job_position_id` (`job_position_id`),
  KEY `idx_parse_status` (`parse_status`),
  KEY `idx_ai_match_score` (`ai_match_score`),
  KEY `idx_is_default` (`candidate_id`,`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历表';

CREATE TABLE `rec_workbench_task` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '任务归属人ID',
  `title` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务标题',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务描述',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '任务类型：0=筛选简历,1=安排面试,2=审批Offer,3=办理入职,4=反馈评审,5=其他',
  `priority` tinyint NOT NULL DEFAULT '1' COMMENT '优先级：0=高,1=中,2=低',
  `related_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联业务类型：CANDIDATE,INTERVIEW,OFFER,ONBOARDING',
  `related_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `candidate_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联候选人姓名',
  `due_date` datetime DEFAULT NULL COMMENT '截止日期',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=待办,1=已完成,2=已忽略',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_user_priority` (`user_id`,`priority`),
  KEY `idx_related` (`related_type`,`related_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作台待办任务表';


USE `smart_recruit_interview`;
CREATE TABLE `analytics_interview_daily` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '当日创建面试数',
  `passed_count` int NOT NULL DEFAULT '0' COMMENT '当日创建且结果为通过数',
  `cancelled_count` int NOT NULL DEFAULT '0' COMMENT '当日创建且已取消数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试日汇总（定时任务统计）';

CREATE TABLE `rec_interview` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `application_id` bigint NOT NULL COMMENT '投递记录ID，关联rec_application.id',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID，关联rec_candidate.id',
  `candidate_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '候选人姓名（冗余字段，避免跨库查询）',
  `job_position_id` bigint NOT NULL COMMENT '职位ID，关联rec_job_position.id',
  `job_title` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '职位名称（冗余字段，避免跨库查询）',
  `round` int NOT NULL DEFAULT '1' COMMENT '面试轮次：1/2/3...',
  `is_final_round` tinyint NOT NULL DEFAULT '0' COMMENT '是否为终面：0=否, 1=是。终面完成后不再安排后续面试',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '面试类型：0=PHONE,1=VIDEO,2=ONSITE,3=AI,4=TECHNICAL,5=HR,6=EXECUTIVE',
  `subject` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '面试主题',
  `scheduled_time` datetime NOT NULL COMMENT '计划面试时间',
  `duration_minutes` int NOT NULL DEFAULT '60' COMMENT '预计时长（分钟）',
  `location` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '面试地点或视频会议链接',
  `interviewer_ids` json NOT NULL COMMENT '面试官ID数组，如[101,102,103]',
  `interviewer_names` json DEFAULT NULL COMMENT '面试官姓名数组',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=SCHEDULED,1=IN_PROGRESS,2=COMPLETED,3=CANCELLED,4=RESCHEDULED,5=NO_SHOW',
  `result` tinyint DEFAULT NULL COMMENT '面试结果：0=PASS,1=FAIL,2=HOLD',
  `feedback` text COLLATE utf8mb4_unicode_ci COMMENT '面试官反馈文本',
  `score` decimal(5,2) DEFAULT NULL COMMENT '综合评分（0-100）',
  `ai_transcript` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'AI语音转写全文',
  `ai_question_status` tinyint NOT NULL DEFAULT '0' COMMENT 'AI出题状态：0=模板/未生成,1=生成中,2=生成成功,3=生成失败',
  `ai_analysis` json DEFAULT NULL COMMENT 'AI分析结果（情感/语言/逻辑等）',
  `evaluation` json DEFAULT NULL COMMENT '结构化评价（5维雷达图JSON）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  `evaluation_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'AI评估状态: PROCESSING=评估中, COMPLETED=已完成, FAILED=失败',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_scheduled_time` (`scheduled_time`),
  KEY `idx_result` (`result`),
  KEY `idx_interview_status_time` (`status`,`scheduled_time`),
  KEY `idx_interview_candidate_round` (`candidate_id`,`round`),
  KEY `idx_interview_job_round` (`job_position_id`,`round`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试记录表';

CREATE TABLE `rec_interview_assessment` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `interview_id` bigint NOT NULL COMMENT '面试ID，关联rec_interview.id',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID',
  `technology_depth` decimal(3,1) DEFAULT NULL COMMENT '技术深度评分（1.0-5.0）',
  `communication` decimal(3,1) DEFAULT NULL COMMENT '沟通表达评分',
  `problem_solving` decimal(3,1) DEFAULT NULL COMMENT '问题解决评分',
  `learning_ability` decimal(3,1) DEFAULT NULL COMMENT '学习能力评分',
  `teamwork` decimal(3,1) DEFAULT NULL COMMENT '团队协作评分',
  `overall_score` decimal(5,2) DEFAULT NULL COMMENT '综合评分',
  `overall_comment` text COLLATE utf8mb4_unicode_ci COMMENT '综合评价文本',
  `strengths` json DEFAULT NULL COMMENT '优势标签（JSON数组）',
  `weaknesses` json DEFAULT NULL COMMENT '不足标签（JSON数组）',
  `key_moments` json DEFAULT NULL COMMENT '关键节点时间线（JSON数组）',
  `suggestion` tinyint DEFAULT NULL COMMENT '建议：0=STRONG_HIRE,1=HIRE,2=HOLD,3=REJECT,4=RETEST',
  `ai_generated` tinyint NOT NULL DEFAULT '0' COMMENT '是否AI生成：0=人工，1=AI',
  `assessor_id` bigint DEFAULT NULL COMMENT '评估人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评估时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_overall_score` (`overall_score`),
  KEY `idx_suggestion` (`suggestion`),
  KEY `idx_assess_candidate_score` (`candidate_id`,`overall_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试评估记录表';

CREATE TABLE `rec_interview_feedback` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `interview_id` bigint NOT NULL COMMENT '面试ID，关联rec_interview.id',
  `interviewer_id` bigint NOT NULL COMMENT '面试官用户ID，关联sys_user.id',
  `technology_depth` decimal(3,1) NOT NULL COMMENT '技术深度评分（1.0-5.0）',
  `communication` decimal(3,1) NOT NULL COMMENT '沟通表达评分',
  `problem_solving` decimal(3,1) NOT NULL COMMENT '问题解决评分',
  `learning_ability` decimal(3,1) NOT NULL COMMENT '学习能力评分',
  `teamwork` decimal(3,1) NOT NULL COMMENT '团队协作评分',
  `overall_rating` decimal(3,1) NOT NULL COMMENT '综合评价分（1.0-5.0）',
  `dimensions` json DEFAULT NULL COMMENT '六维度评分（JSON）',
  `overall_comment` text COLLATE utf8mb4_unicode_ci COMMENT '综合评价',
  `strengths` text COLLATE utf8mb4_unicode_ci COMMENT '候选人优势',
  `weaknesses` text COLLATE utf8mb4_unicode_ci COMMENT '候选人不足',
  `suggestions` text COLLATE utf8mb4_unicode_ci COMMENT '面试官建议',
  `hire_recommendation` tinyint NOT NULL DEFAULT '0' COMMENT '录用建议：0=STRONG_HIRE,1=HIRE,2=HOLD,3=REJECT',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_interviewer_id` (`interviewer_id`),
  KEY `idx_hire_recommendation` (`hire_recommendation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试反馈表';

CREATE TABLE `rec_interview_question` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `position_type` tinyint NOT NULL DEFAULT '0' COMMENT '岗位类型：0=FRONTEND,1=BACKEND,2=AI,3=PM,4=DEVOPS,5=DATA_ENGINEER,6=FULLSTACK,7=MOBILE',
  `category` tinyint NOT NULL DEFAULT '0' COMMENT '题目类别：0=TECHNOLOGY,1=PROJECT,2=BEHAVIORAL,3=MANAGEMENT',
  `difficulty` tinyint NOT NULL DEFAULT '1' COMMENT '难度：0=EASY,1=MEDIUM,2=HARD',
  `question_text` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目内容',
  `reference_answer` text COLLATE utf8mb4_unicode_ci COMMENT '参考答案（可选）',
  `is_ai_generated` tinyint NOT NULL DEFAULT '0' COMMENT '是否AI生成：0=人工，1=AI',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0=停用，1=启用',
  `use_count` int NOT NULL DEFAULT '0' COMMENT '使用次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_qst_use_count` (`use_count`),
  KEY `idx_qst_pos_cat_diff` (`position_type`,`category`,`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题库表';

CREATE TABLE `rec_online_assessment` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `interview_id` bigint DEFAULT NULL COMMENT '关联面试ID',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID',
  `candidate_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '候选人姓名（冗余）',
  `job_title` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '应聘职位（冗余）',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '测评类型：0=编程测评,1=性格测评,2=智商逻辑测评',
  `type_label` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '测评类型标签',
  `sent_time` datetime DEFAULT NULL COMMENT '发送时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=未发送,1=等待完成,2=已完成',
  `score` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '测评成绩',
  `access_token` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '测评访问令牌（JWT，有效期7天）',
  `candidate_email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人邮箱（发送测评时从候选人或手动输入获取）',
  `questions_json` text COLLATE utf8mb4_unicode_ci COMMENT 'AI生成的测评题目JSON（AssessmentQuestionItem数组，含correctAnswer供后端评分）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=未删除,1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='在线测评管理表';

CREATE TABLE `rec_question_bank` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `bank_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '套题名称，如：Java后端-技术基础面',
  `department_id` bigint NOT NULL COMMENT '部门ID，关联sys_department.id',
  `department_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部门名称',
  `job_position_id` bigint DEFAULT NULL COMMENT '职位ID，关联rec_job_position.id',
  `job_title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位名称',
  `question_type` tinyint NOT NULL DEFAULT '3' COMMENT '套题类型：0=技术面,1=项目面,2=行为/HR面,3=综合面',
  `difficulty` tinyint NOT NULL DEFAULT '2' COMMENT '难度：1=简单,2=中等,3=困难',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '套题说明',
  `question_count` int NOT NULL DEFAULT '0' COMMENT '题目数量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0=草稿,1=启用,2=停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常,1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_qb_dept` (`department_id`),
  KEY `idx_qb_job` (`job_position_id`),
  KEY `idx_qb_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题库-套题表';

CREATE TABLE `rec_question_bank_item` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `bank_id` bigint NOT NULL COMMENT '套题ID，关联rec_question_bank.id',
  `question_type` tinyint NOT NULL DEFAULT '0' COMMENT '题型：0=单选,1=多选,2=问答',
  `question` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目内容',
  `options` json DEFAULT NULL COMMENT '选项（JSON数组），问答题为NULL',
  `answer` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '答案（单选/多选为选项标识；问答为参考要点）',
  `explanation` text COLLATE utf8mb4_unicode_ci COMMENT '题目解读/解析',
  `difficulty` tinyint NOT NULL DEFAULT '2' COMMENT '难度：1=简单,2=中等,3=困难',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常,1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_qbi_bank` (`bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题库-题目表';


USE `smart_recruit_offer`;
CREATE TABLE `analytics_offer_daily` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `sent_count` int NOT NULL DEFAULT '0' COMMENT '当日发送 Offer 数',
  `accepted_count` int NOT NULL DEFAULT '0' COMMENT '当日发送且被接受数',
  `declined_count` int NOT NULL DEFAULT '0' COMMENT '当日发送且被拒绝数',
  `pending_count` int NOT NULL DEFAULT '0' COMMENT '当日发送且待回复数',
  `onboard_count` int NOT NULL DEFAULT '0' COMMENT '当日完成入职人数',
  `confirm_total_days` bigint NOT NULL DEFAULT '0' COMMENT '当日发送的 Offer 确认周期天数合计',
  `confirm_count` int NOT NULL DEFAULT '0' COMMENT '当日发送且有确认周期的 Offer 数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer/入职日汇总（定时任务统计）';

CREATE TABLE `ofr_contract` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `contract_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '合同编号，如 CT-2026-0001',
  `offer_id` bigint DEFAULT NULL COMMENT '关联 Offer ID',
  `candidate_id` bigint DEFAULT NULL COMMENT '候选人ID',
  `candidate_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '候选人姓名',
  `candidate_email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人邮箱',
  `job_title` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职位名称',
  `department_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '部门名称',
  `offer_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联 Offer 编号',
  `total_package` decimal(12,2) DEFAULT NULL COMMENT '年薪总包（元）',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '合同正文（模板渲染后的文本）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=草稿,1=待审批,2=已审批,3=已发送,4=已签署,5=已拒绝,6=已归档',
  `sign_token` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人签署 Token（发送时生成）',
  `signed_by_hr` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HR 签署人姓名',
  `signed_by_candidate` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人签署姓名',
  `candidate_signature` text COLLATE utf8mb4_unicode_ci COMMENT '候选人电子签章（手写签名 PNG data URL）',
  `candidate_id_card` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人身份证号码',
  `candidate_phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人联系电话',
  `candidate_address` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人通讯地址',
  `sign_time` datetime DEFAULT NULL COMMENT '签署时间',
  `reject_reason` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拒绝原因',
  `void_reason` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作废原因',
  `valid_from` date DEFAULT NULL COMMENT '合同生效日期',
  `valid_until` date DEFAULT NULL COMMENT '合同截止日期',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常,1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_offer_id` (`offer_id`),
  KEY `idx_status` (`status`),
  KEY `idx_candidate` (`candidate_name`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同表';

CREATE TABLE `ofr_contract_sign_record` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `signer_type` tinyint NOT NULL DEFAULT '0' COMMENT '签署方：0=HR,1=候选人',
  `signer_id` bigint DEFAULT NULL COMMENT '签署人用户ID（HR）',
  `signer_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '签署人姓名',
  `action` tinyint NOT NULL DEFAULT '1' COMMENT '动作：1=签署,2=拒绝',
  `sign_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签署时间',
  `sign_ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签署 IP',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注/拒绝原因',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同签署记录表';

CREATE TABLE `rec_approval_flow_config` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `flow_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程名称，如"技术研发部Offer审批流程"',
  `department_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '适用部门名称，关联rec_offer.department_name',
  `is_active` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0=停用,1=启用',
  `max_levels` int NOT NULL DEFAULT '2' COMMENT '审批层级总数',
  `nodes` json NOT NULL COMMENT '审批节点配置JSON数组：[{level,nodeName,approvers}]',
  `description` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_department` (`department_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer审批流程配置表';

CREATE TABLE `rec_offer` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `application_id` bigint DEFAULT NULL COMMENT '投递记录ID（NULL=未关联申请）',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID，关联rec_candidate.id',
  `candidate_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人姓名',
  `candidate_email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人邮箱',
  `job_position_id` bigint NOT NULL COMMENT '职位ID，关联rec_job_position.id',
  `department_id` bigint DEFAULT NULL COMMENT '部门ID（冗余字段，由 department_name 替代）',
  `offer_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Offer编号：OFF-YYYYMMDD-NNNN',
  `position_title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '录用职位名称',
  `department_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '录用部门名称',
  `level` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '录用职级（P4-P10, M1-M5）',
  `salary_structure` json DEFAULT NULL COMMENT '薪酬结构（JSON：base, bonus, allowance, stock）',
  `work_location` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '工作地点',
  `salary_base` decimal(12,2) DEFAULT NULL COMMENT '月基本工资（旧字段，已由 salary_structure JSON 替代）',
  `bonus_months` int NOT NULL DEFAULT '0' COMMENT '年终奖月数',
  `stock_shares` int NOT NULL DEFAULT '0' COMMENT '股票数量（股/年）',
  `total_package_yearly` decimal(12,2) DEFAULT NULL COMMENT '年度总包（旧字段，由 salary_structure JSON 计算）',
  `benefits` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '福利描述',
  `probation_months` int NOT NULL DEFAULT '3' COMMENT '试用期月数',
  `probation_salary_ratio` decimal(3,2) NOT NULL DEFAULT '0.80' COMMENT '试用期薪资比例',
  `expected_onboard_date` date DEFAULT NULL COMMENT '预计入职日期',
  `valid_until` date NOT NULL COMMENT 'Offer有效期截止日',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=DRAFT,1=PENDING,2=APPROVED,3=SENT,4=ACCEPTED,5=REJECTED,6=NEGOTIATING,7=EXPIRED',
  `ai_accept_probability` decimal(5,2) DEFAULT NULL COMMENT 'AI接受度预测（0-100）',
  `ai_risk_level` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'AI接受度风险级别：LOW/MEDIUM/HIGH',
  `ai_suggestion` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'AI接受度预测建议',
  `ai_predicted_at` datetime DEFAULT NULL COMMENT 'AI预测时间',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `confirm_token` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人确认Token',
  `respond_time` datetime DEFAULT NULL COMMENT '候选人回复时间',
  `decline_reason` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拒绝原因',
  `attachment_path` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '已签署Offer附件路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offer_no` (`offer_no`),
  UNIQUE KEY `uk_application_id` (`application_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_job_position_id` (`job_position_id`),
  KEY `idx_valid_until` (`valid_until`),
  KEY `idx_expected_onboard_date` (`expected_onboard_date`),
  KEY `idx_offer_status_valid` (`status`,`valid_until`),
  KEY `idx_offer_dept_status` (`department_id`,`status`),
  KEY `idx_confirm_token` (`confirm_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer表';

CREATE TABLE `rec_offer_approval` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `offer_id` bigint NOT NULL COMMENT 'Offer ID，关联rec_offer.id',
  `approver_id` bigint NOT NULL COMMENT '审批人用户ID，关联sys_user.id',
  `approver_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审批人姓名',
  `approver_role` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人角色名称，如"HR经理"',
  `approval_level` int NOT NULL DEFAULT '1' COMMENT '审批级别：1=HR经理，2=部门总监，3=VP，4=招聘总监',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=PENDING,1=APPROVED,2=REJECTED',
  `comment` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批意见',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_status` (`status`),
  KEY `idx_approval_offer_level` (`offer_id`,`approval_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer审批记录表';

CREATE TABLE `rec_onboarding` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `offer_id` bigint NOT NULL COMMENT 'Offer ID，关联rec_offer.id',
  `candidate_id` bigint NOT NULL COMMENT '候选人（新员工）ID，关联rec_candidate.id',
  `department_id` bigint NOT NULL COMMENT '部门ID，关联sys_department.id',
  `department_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '部门名称（从Offer反范式化）',
  `employee_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '员工工号：EMP-YYYY-NNNN',
  `employee_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '员工姓名',
  `position_title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位名称',
  `level` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职级（如P7、M1）',
  `expected_onboard_date` date NOT NULL COMMENT '预计入职日期',
  `actual_onboard_date` date DEFAULT NULL COMMENT '实际入职日期',
  `current_step` tinyint NOT NULL DEFAULT '1' COMMENT '当前入职步骤：1=资料收集,2=设备发放,3=账号开通,4=欢迎页,5=导师分配,6=入职培训',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=PENDING,1=ACTIVE,2=DONE,3=AT_RISK',
  `employee_status` tinyint NOT NULL DEFAULT '0' COMMENT '员工状态：0=待入职,1=试用期,2=正式,3=已离职',
  `risk_score` decimal(5,2) DEFAULT NULL COMMENT '留存风险评分（0-100）',
  `risk_level` tinyint DEFAULT NULL COMMENT '风险等级：0=LOW,1=MEDIUM,2=HIGH',
  `retention_score6_m` int DEFAULT NULL COMMENT '6个月留任概率',
  `retention_score12_m` int DEFAULT NULL COMMENT '12个月留任概率',
  `document_status` json DEFAULT NULL COMMENT '6项资料状态：{"idCard":"verified","diploma":"pending",...}',
  `equipment_status` json DEFAULT NULL COMMENT '5项设备状态：{"laptop":"delivered","monitor":"pending",...}',
  `mentor_id` bigint DEFAULT NULL COMMENT '导师ID，关联sys_user.id',
  `mentor_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '导师姓名',
  `buddy_id` bigint DEFAULT NULL COMMENT '伙伴用户ID，关联sys_user.id',
  `welcome_sent` tinyint NOT NULL DEFAULT '0' COMMENT '欢迎页是否已发送：0=未发送，1=已发送',
  `training_progress` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '入职培训进度（0-100）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  `account_username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '系统账号用户名',
  `account_email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '系统账号邮箱',
  `account_created_at` datetime DEFAULT NULL COMMENT '账号创建时间',
  `retention_risk_factors` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '风险因素（;;分隔）',
  `retention_interventions` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '干预建议（;;分隔）',
  `retention_predicted_at` datetime DEFAULT NULL COMMENT '留任预测时间',
  `retention_input_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入职5步数据SHA-256指纹',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offer_id` (`offer_id`),
  UNIQUE KEY `uk_employee_no` (`employee_no`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_onboarding_status_date` (`status`,`expected_onboard_date`),
  KEY `idx_onboarding_dept_status` (`department_id`,`status`),
  KEY `idx_onboard_date_status` (`expected_onboard_date`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入职管理表';

CREATE TABLE `rec_onboarding_document` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `onboarding_id` bigint NOT NULL COMMENT '入职记录ID，关联rec_onboarding.id',
  `doc_type` tinyint NOT NULL DEFAULT '0' COMMENT '文档类型：0=ID_CARD,1=DIPLOMA,2=RESIGNATION,3=PHYSICAL,4=BANK_CARD,5=PHOTO',
  `doc_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资料名称：身份证/学历证书/离职证明/体检报告/银行卡/入职照片',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=MISSING,1=PENDING,2=VERIFIED',
  `file_path` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件存储路径',
  `verified_by` bigint DEFAULT NULL COMMENT '核验人ID',
  `verified_time` datetime DEFAULT NULL COMMENT '核验时间',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_onboarding_doc` (`onboarding_id`,`doc_type`),
  KEY `idx_status` (`status`),
  KEY `idx_onboard_doc_status` (`onboarding_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入职资料明细表';

CREATE TABLE `rec_onboarding_equipment` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `onboarding_id` bigint NOT NULL COMMENT '入职记录ID，关联rec_onboarding.id',
  `equipment_type` tinyint NOT NULL DEFAULT '0' COMMENT '设备类型：0=LAPTOP,1=MONITOR,2=PHONE,3=BADGE,4=DESK',
  `equipment_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备名称：笔记本电脑/显示器/工作手机/门禁卡/工位',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=PENDING,1=ASSIGNED,2=SHIPPED,3=DELIVERED',
  `asset_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资产编号',
  `assigned_by` bigint DEFAULT NULL COMMENT '分配人ID',
  `assigned_time` datetime DEFAULT NULL COMMENT '分配时间',
  `delivered_time` datetime DEFAULT NULL COMMENT '交付时间',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_onboarding_equip` (`onboarding_id`,`equipment_type`),
  KEY `idx_status` (`status`),
  KEY `idx_onboard_equip_status` (`onboarding_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入职设备明细表';


USE `smart_recruit_talent`;
CREATE TABLE `analytics_candidate_daily` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `stage` tinyint NOT NULL COMMENT '候选人阶段（rec_candidate.status）',
  `source` tinyint NOT NULL COMMENT '来源渠道（rec_candidate.source）',
  `candidate_count` int NOT NULL DEFAULT '0' COMMENT '当日新增候选人数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`stat_date`,`stage`,`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人日汇总快照（talent 本地）';

CREATE TABLE `analytics_interview_daily` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '当日创建面试数',
  `passed_count` int NOT NULL DEFAULT '0' COMMENT '当日创建且结果为通过数',
  `cancelled_count` int NOT NULL DEFAULT '0' COMMENT '当日创建且已取消数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试日汇总快照（talent 本地）';

CREATE TABLE `analytics_offer_daily` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `sent_count` int NOT NULL DEFAULT '0' COMMENT '当日发送 Offer 数',
  `accepted_count` int NOT NULL DEFAULT '0' COMMENT '当日发送且被接受数',
  `declined_count` int NOT NULL DEFAULT '0' COMMENT '当日发送且被拒绝数',
  `pending_count` int NOT NULL DEFAULT '0' COMMENT '当日发送且待回复数',
  `onboard_count` int NOT NULL DEFAULT '0' COMMENT '当日完成入职人数',
  `confirm_total_days` bigint NOT NULL DEFAULT '0' COMMENT '当日发送的 Offer 确认周期天数合计',
  `confirm_count` int NOT NULL DEFAULT '0' COMMENT '当日发送且有确认周期的 Offer 数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer/入职日汇总快照（talent 本地）';

CREATE TABLE `rec_talent_campaign` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `campaign_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `template_type` tinyint NOT NULL DEFAULT '0' COMMENT '模板类型：0=MONTHLY_GREETING,1=JOB_RECOMMEND,2=EVENT_INVITE,3=REACTIVATION,4=CUSTOM',
  `target_audience` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标人群描述',
  `target_count` int NOT NULL DEFAULT '0' COMMENT '目标人数',
  `message_content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `send_method` tinyint NOT NULL DEFAULT '0' COMMENT '发送方式：0=EMAIL,1=SMS,2=PUSH,3=WECHAT',
  `scheduled_time` datetime DEFAULT NULL COMMENT '计划发送时间',
  `sent_time` datetime DEFAULT NULL COMMENT '实际发送时间',
  `reach_count` int NOT NULL DEFAULT '0' COMMENT '触达人数',
  `response_count` int NOT NULL DEFAULT '0' COMMENT '响应人数',
  `response_rate` decimal(5,2) DEFAULT NULL COMMENT '响应率（%）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=DRAFT,1=SCHEDULED,2=SENDING,3=COMPLETED,4=CANCELLED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_scheduled_time` (`scheduled_time`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_campaign_status_sched` (`status`,`scheduled_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人才激活营销活动表';

CREATE TABLE `rec_talent_pool` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID，关联rec_candidate.id',
  `pool_type` tinyint NOT NULL DEFAULT '0' COMMENT '人才库类型：0=GENERAL,1=TECHNICAL,2=MANAGEMENT,3=INTERN,4=EXECUTIVE',
  `tags` json DEFAULT NULL COMMENT '自定义标签（JSON数组）',
  `ai_tags` json DEFAULT NULL COMMENT 'AI智能标签（JSON数组），如["后端技术","互联网行业"]',
  `skill_level` tinyint DEFAULT NULL COMMENT '技能等级：0=JUNIOR,1=MID,2=SENIOR,3=EXPERT,4=LEAD',
  `availability` tinyint NOT NULL DEFAULT '1' COMMENT '求职状态：0=ACTIVE,1=PASSIVE,2=NOT_AVAILABLE',
  `expected_position` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '期望职位',
  `expected_location` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '期望工作城市',
  `expected_salary_min` int DEFAULT NULL COMMENT '最低期望月薪',
  `expected_salary_max` int DEFAULT NULL COMMENT '最高期望月薪',
  `last_contact_time` datetime DEFAULT NULL COMMENT '最近联系时间',
  `last_active_time` datetime DEFAULT NULL COMMENT '最近活跃时间',
  `match_score` decimal(5,2) DEFAULT NULL COMMENT 'AI推荐匹配度（0-100）',
  `matched_position_id` bigint DEFAULT NULL COMMENT '匹配推荐的职位ID',
  `note` text COLLATE utf8mb4_unicode_ci COMMENT '内部备注',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0=已移出，1=在库',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '入库人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入库人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_candidate_pool` (`candidate_id`,`pool_type`),
  KEY `idx_availability` (`availability`),
  KEY `idx_skill_level` (`skill_level`),
  KEY `idx_match_score` (`match_score`),
  KEY `idx_last_contact_time` (`last_contact_time`),
  KEY `idx_talent_pool_type_avail` (`pool_type`,`availability`),
  KEY `idx_talent_candidate_status` (`candidate_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人才库表';


USE `smart_recruit_referral`;
CREATE TABLE `ref_bonus_record` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `ref_record_id` bigint NOT NULL COMMENT '内推记录ID，关联ref_record.id',
  `referrer_id` bigint NOT NULL COMMENT '内推人ID',
  `amount` decimal(12,2) NOT NULL COMMENT '发放金额',
  `stage` tinyint NOT NULL DEFAULT '0' COMMENT '发放阶段：0=ONBOARD,1=INITIAL,2=ONBOARD_PROBATION,3=PROBATION_PASS,4=FULL',
  `stage_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '阶段名称：入职发放/首次发放/试用期发放/转正发放/全额结清',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=PENDING,1=PAID,2=CANCELLED',
  `paid_time` datetime DEFAULT NULL COMMENT '发放时间',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_stage` (`stage`),
  KEY `idx_bonus_ref_status` (`ref_record_id`,`status`),
  KEY `idx_bonus_referrer_status` (`referrer_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内推奖金发放记录表';

CREATE TABLE `ref_leaderboard` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `program_id` bigint NOT NULL COMMENT '内推项目ID',
  `period_type` tinyint NOT NULL DEFAULT '0' COMMENT '周期类型：0=MONTHLY,1=QUARTERLY,2=YEARLY',
  `period_value` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '周期值：2026-07/2026-Q3/2026',
  `referrer_id` bigint NOT NULL COMMENT '内推人ID',
  `referrer_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内推人姓名',
  `referrer_dept_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内推人部门',
  `rank` int NOT NULL COMMENT '排名',
  `referral_count` int NOT NULL DEFAULT '0' COMMENT '内推总数',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功入职数',
  `bonus_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '累计奖金',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
  PRIMARY KEY (`id`),
  KEY `idx_referrer_id` (`referrer_id`),
  KEY `idx_rank` (`rank`),
  KEY `idx_leaderboard_program_rank` (`program_id`,`period_type`,`period_value`,`rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内推排行榜快照表';

CREATE TABLE `ref_program` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目名称，如"2026 Q3 技术内推激励计划"',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '项目描述/政策说明',
  `bonus_amount` decimal(12,2) NOT NULL COMMENT '基础内推奖金（CNY）',
  `bonus_structure` json DEFAULT NULL COMMENT '奖金发放结构：{"入职发放":50,"转正发放":50}',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期（NULL=持续进行）',
  `eligible_dept_ids` json DEFAULT NULL COMMENT '适用范围（部门ID数组，NULL=全部）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0=停用，1=启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人用户名',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人用户名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_date` (`start_date`),
  KEY `idx_end_date` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内推项目表';

CREATE TABLE `ref_program_job` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `program_id` bigint NOT NULL COMMENT '内推项目ID，关联ref_program.id',
  `job_position_id` bigint NOT NULL COMMENT '职位ID，关联rec_job_position.id',
  `is_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用内推：0=关闭，1=启用',
  `bonus_amount` decimal(12,2) DEFAULT NULL COMMENT '该职位内推奖金（NULL=使用项目默认奖金）',
  `tag` tinyint DEFAULT NULL COMMENT '标签：0=URGENT,1=HIGH_BONUS,2=TECH,3=INTERN',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `job_title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职位标题快照',
  `min_salary` int DEFAULT NULL COMMENT '最低薪资快照',
  `max_salary` int DEFAULT NULL COMMENT '最高薪资快照',
  `head_count` int DEFAULT NULL COMMENT '招聘人数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_program_job` (`program_id`,`job_position_id`),
  KEY `idx_job_position_id` (`job_position_id`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_program_job_enabled` (`program_id`,`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内推项目-职位关联表';

CREATE TABLE `ref_record` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `program_id` bigint NOT NULL COMMENT '内推项目ID，关联ref_program.id',
  `program_job_id` bigint NOT NULL COMMENT '内推职位关联ID，关联ref_program_job.id',
  `referrer_id` bigint NOT NULL COMMENT '内推人用户ID，关联sys_user.id',
  `candidate_id` bigint NOT NULL COMMENT '被推荐候选人ID，关联rec_candidate.id',
  `job_position_id` bigint NOT NULL COMMENT '推荐职位ID，关联rec_job_position.id',
  `relationship` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内推人与候选人关系：同事/朋友/校友/前同事/其他',
  `referral_note` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '推荐理由',
  `resume_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简历文件URL（RustFS 存储路径）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=PENDING,1=CONTACTED,2=INTERVIEWING,3=HIRED,4=REJECTED,5=CANCELLED',
  `bonus_status` tinyint DEFAULT NULL COMMENT '奖金状态：0=PENDING,1=PARTIAL_PAID,2=FULL_PAID',
  `bonus_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '应发奖金总额',
  `bonus_paid` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '已发放金额',
  `hired_time` datetime DEFAULT NULL COMMENT '被推荐人入职时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `share_token` varchar(12) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联的分享token',
  `referral_code` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联的内推码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_referrer_candidate_job` (`referrer_id`,`candidate_id`,`job_position_id`),
  KEY `idx_program_id` (`program_id`),
  KEY `idx_job_position_id` (`job_position_id`),
  KEY `idx_status` (`status`),
  KEY `idx_bonus_status` (`bonus_status`),
  KEY `idx_ref_status_referrer` (`referrer_id`,`status`),
  KEY `idx_ref_candidate_program` (`candidate_id`,`program_id`),
  KEY `idx_ref_record_share_token` (`share_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内推记录表';

CREATE TABLE `ref_share_token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `token` varchar(32) NOT NULL COMMENT '分享令牌',
  `program_id` bigint NOT NULL COMMENT '内推计划ID',
  `referrer_name` varchar(100) DEFAULT NULL COMMENT '分享人姓名',
  `source` varchar(100) DEFAULT NULL COMMENT '分享来源渠道',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `referral_code` varchar(8) DEFAULT NULL COMMENT '内推码，6-8位大写字母+数字',
  `referrer_id` bigint DEFAULT NULL COMMENT '生成此分享的员工用户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  UNIQUE KEY `uk_ref_share_token_referral_code` (`referral_code`),
  KEY `idx_program_id` (`program_id`),
  KEY `idx_ref_share_token_referrer_id` (`referrer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内推分享链接令牌表';


USE `smart_recruit_ai`;
CREATE TABLE `ai_agent_info` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `agent_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '智能体唯一标识（kebab-case，如 resume-parser）',
  `agent_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '智能体类名，如 ResumeParserAgent',
  `display_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '前端展示名称，如 简历解析',
  `model` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '绑定模型',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职责描述',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '层级：0=编排层,1=执行层,2=复盘层',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=RUNNING,1=IDLE,2=PAUSED,3=ERROR',
  `config` json DEFAULT NULL COMMENT '运行配置（JSON）',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：1=启用,0=停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_id` (`agent_id`),
  KEY `idx_type_status` (`type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI智能体注册表';

CREATE TABLE `ai_agent_metric` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `agent_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Agent名称',
  `metric_time` datetime NOT NULL COMMENT '指标采集时间',
  `calls_per_hour` decimal(10,2) DEFAULT NULL COMMENT '每小时调用次数',
  `avg_latency_ms` decimal(10,2) DEFAULT NULL COMMENT '平均延迟（毫秒）',
  `accuracy_pct` decimal(5,2) DEFAULT NULL COMMENT '准确率（%）',
  `health_pct` decimal(5,2) DEFAULT NULL COMMENT '健康度（%）',
  `tokens_24h` bigint DEFAULT NULL COMMENT '24小时Token消耗',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功次数',
  `fail_count` int NOT NULL DEFAULT '0' COMMENT '失败次数',
  `total_calls` int NOT NULL DEFAULT '0' COMMENT '累计调用次数',
  `model_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用模型名称',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_metric_time` (`agent_name`,`metric_time`),
  KEY `idx_metric_agent_health` (`agent_name`,`accuracy_pct`),
  KEY `idx_metric_time_range` (`metric_time`,`health_pct`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent运行指标表';

CREATE TABLE `ai_agent_task` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `agent_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Agent名称：ResumeParser/MatchScorer/SmartScreener/JdGenerator/InterviewQuestion/InterviewAssessor/TalentRecommender/OfferPredict/RetentionPredict',
  `task_type` tinyint NOT NULL DEFAULT '0' COMMENT '任务类型：0=RESUME_PARSE,1=SCREEN,2=EVALUATE,3=PREDICT,4=RECOMMEND,5=QUESTION_GEN,6=JD_GEN',
  `priority` tinyint NOT NULL DEFAULT '1' COMMENT '优先级：0=LOW,1=MEDIUM,2=HIGH',
  `input_data` json DEFAULT NULL COMMENT '输入数据',
  `output_data` json DEFAULT NULL COMMENT '输出数据',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=QUEUED,1=RUNNING,2=COMPLETED,3=FAILED,4=RETRYING',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `max_retries` int NOT NULL DEFAULT '3' COMMENT '最大重试次数',
  `progress` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '执行进度（0-100）',
  `started_at` datetime DEFAULT NULL COMMENT '开始执行时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `error_stack` text COLLATE utf8mb4_unicode_ci COMMENT '异常堆栈',
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '全链路追踪ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_status` (`agent_name`,`status`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_task_type_status` (`task_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent任务表';

CREATE TABLE `ai_event_log` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `agent_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Agent名称',
  `task_id` bigint DEFAULT NULL COMMENT '关联任务ID',
  `event_type` tinyint NOT NULL DEFAULT '0' COMMENT '事件类型：0=INFO,1=WARNING,2=ERROR,3=SUCCESS',
  `event_source` tinyint DEFAULT NULL COMMENT '事件来源：0=ORCHESTRATOR,1=PARSER,2=SCREENER,3=GENERATOR,4=ASSESSOR,5=PREDICTOR',
  `title` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件标题',
  `message` text COLLATE utf8mb4_unicode_ci COMMENT '事件详细信息',
  `data` json DEFAULT NULL COMMENT '事件附加数据',
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '全链路追踪ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_event_agent_time` (`agent_name`,`event_type`,`created_at`),
  KEY `idx_event_task_type` (`task_id`,`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent事件日志表';

CREATE TABLE `ai_interview_log` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `interview_id` bigint NOT NULL COMMENT '面试ID，关联rec_interview.id',
  `engine` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AI引擎',
  `model` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '具体模型版本',
  `assessment_type` tinyint NOT NULL DEFAULT '0' COMMENT '评估类型：0=SPEECH,1=CONTENT,2=VOICE,3=OVERALL',
  `input_tokens` int NOT NULL DEFAULT '0' COMMENT '输入Token数',
  `output_tokens` int NOT NULL DEFAULT '0' COMMENT '输出Token数',
  `total_tokens` int NOT NULL DEFAULT '0' COMMENT '总Token数',
  `duration_ms` bigint NOT NULL DEFAULT '0' COMMENT '处理耗时（毫秒）',
  `cost_usd` decimal(10,6) NOT NULL DEFAULT '0.000000' COMMENT 'API调用成本（USD）',
  `result_json` json DEFAULT NULL COMMENT '评估结果',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=SUCCESS,1=FAILED',
  `error_msg` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  PRIMARY KEY (`id`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_engine` (`engine`),
  KEY `idx_assessment_type` (`assessment_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_log_engine_type` (`engine`,`assessment_type`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试评估调用日志表';

CREATE TABLE `ai_resume_parse_log` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `resume_id` bigint NOT NULL COMMENT '简历ID，关联rec_resume.id',
  `engine` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AI引擎：Claude-4/GPT-4o/Qwen-Max/DeepSeek-R1',
  `model` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '具体模型版本',
  `input_tokens` int NOT NULL DEFAULT '0' COMMENT '输入Token数',
  `output_tokens` int NOT NULL DEFAULT '0' COMMENT '输出Token数',
  `total_tokens` int NOT NULL DEFAULT '0' COMMENT '总Token数',
  `duration_ms` bigint NOT NULL DEFAULT '0' COMMENT '处理耗时（毫秒）',
  `cost_usd` decimal(10,6) NOT NULL DEFAULT '0.000000' COMMENT 'API调用成本（USD）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=SUCCESS,1=FAILED',
  `error_msg` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_engine` (`engine`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_log_engine_time` (`engine`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历解析调用日志表';


-- ============================================================================
-- 3. 基础数据
-- ============================================================================
USE `smart_recruit_system`;


INSERT INTO `sys_department` (`id`, `parent_id`, `dept_name`, `dept_code`, `leader_id`, `headcount`, `staff_count`, `level`, `sort_order`, `status`, `description`, `create_time`, `update_time`, `create_user_id`, `create_by`, `update_user_id`, `update_by`, `deleted`) VALUES (100001,0,'公司总部','HQ',NULL,0,0,1,1,1,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(100002,100001,'技术研发部','TECH',2,0,0,1,1,1,'','2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(100003,100001,'产品部','PRODUCT',NULL,0,0,1,2,1,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(100004,100001,'人力资源部','HR',NULL,0,0,1,3,1,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(100005,100001,'财务部','FINANCE',NULL,0,0,1,4,1,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(100006,100001,'市场部','MARKET',NULL,0,0,1,5,1,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(100007,100001,'销售部','SALES',NULL,0,0,1,6,1,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(100008,100002,'研发组','ENG',NULL,0,0,1,1,1,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(100009,100002,'测试组','QA',NULL,0,0,1,2,1,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(100010,100002,'运维组','DEVOPS',NULL,0,0,1,3,1,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0);




INSERT INTO `sys_permission` (`id`, `parent_id`, `perm_name`, `perm_code`, `perm_type`, `module`, `path`, `component`, `icon`, `api_method`, `api_path`, `sort_order`, `status`, `visible`, `create_time`, `update_time`, `create_user_id`, `create_by`, `update_user_id`, `update_by`, `deleted`) VALUES (400001,0,'工作台','dashboard',1,0,'/dashboard','','Dashboard',NULL,'',1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400002,0,'招聘管理','recruitment',1,0,'/recruitment',NULL,'Briefcase',NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400003,0,'人才库','talent',1,4,'/talent',NULL,'User',NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400004,0,'内推管理','referral',1,8,'/referral',NULL,'Share',NULL,NULL,4,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400005,0,'入职管理','onboarding',1,7,'/onboarding',NULL,'Sunny',NULL,NULL,5,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400006,0,'AI引擎','ai',1,10,'/ai',NULL,'Cpu',NULL,NULL,6,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400007,0,'数据分析','analytics',1,5,'/analytics',NULL,'DataAnalysis',NULL,NULL,7,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400008,0,'系统管理','system',1,9,'/system',NULL,'Setting',NULL,NULL,8,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400009,0,'招聘官网','careers',1,11,'/careers',NULL,'House',NULL,NULL,9,1,1,'2026-08-02 20:10:29','2026-08-02 20:10:29',NULL,'system',NULL,NULL,0),(400101,400002,'职位管理','recruitment:job',1,0,'/recruitment/job',NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400102,400002,'简历筛选','recruitment:resume',1,0,'/recruitment/resume',NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400103,400002,'面试管理','recruitment:interview',1,3,'/recruitment/interview',NULL,NULL,NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400104,400002,'Offer管理','recruitment:offer',1,6,'/recruitment/offer',NULL,NULL,NULL,NULL,4,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400105,400002,'合同管理','recruitment:contract',1,0,'/contracts',NULL,NULL,NULL,NULL,5,1,1,'2026-08-09 13:23:32','2026-08-09 13:23:32',NULL,'system',NULL,NULL,0),(400801,400008,'用户管理','system:user',1,9,'/system/user',NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400802,400008,'角色管理','system:role',1,9,'/system/role',NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400803,400008,'权限管理','system:permission',1,9,'/system/permission',NULL,NULL,NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400804,400008,'部门管理','system:dept',1,9,'/system/dept',NULL,NULL,NULL,NULL,4,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400805,400008,'操作日志','system:log',1,9,'/system/log',NULL,NULL,NULL,NULL,5,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(400806,400008,'系统设置','system:settings',1,9,'/system/settings',NULL,NULL,NULL,NULL,6,1,1,'2026-07-26 12:18:24','2026-07-26 12:18:24',NULL,'system',NULL,NULL,0),(400901,400009,'官网配置','careers:config',1,11,'/careers/config',NULL,NULL,NULL,NULL,1,1,1,'2026-08-02 20:10:29','2026-08-02 20:10:29',NULL,'system',NULL,NULL,0),(400902,400009,'热门职位','careers:hot-jobs',1,11,'/careers/hot-jobs',NULL,NULL,NULL,NULL,2,1,1,'2026-08-02 20:47:08','2026-08-02 20:47:08',NULL,'system',NULL,NULL,0),(400903,400009,'社会招聘','careers:social',1,11,'/careers/social',NULL,NULL,NULL,NULL,3,1,1,'2026-08-02 20:47:08','2026-08-02 20:47:08',NULL,'system',NULL,NULL,0),(400904,400009,'校园招聘','careers:campus',1,11,'/careers/campus',NULL,NULL,NULL,NULL,4,1,1,'2026-08-02 20:47:08','2026-08-02 20:47:08',NULL,'system',NULL,NULL,0),(401101,400101,'创建职位','job:create',2,0,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401102,400101,'编辑职位','job:edit',2,0,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401103,400101,'删除职位','job:delete',2,0,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401104,400101,'发布职位','job:publish',2,0,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401105,400101,'关闭职位','job:close',2,0,NULL,NULL,NULL,NULL,NULL,5,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401106,400101,'查看职位','job:view',2,0,NULL,NULL,NULL,NULL,NULL,6,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401201,400102,'查看候选人','candidate:view',2,2,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401202,400102,'编辑候选人','candidate:edit',2,2,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401203,400102,'导入简历','resume:import',2,2,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401204,400102,'解析简历','resume:parse',2,2,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401205,400102,'筛选简历','resume:screen',2,2,NULL,NULL,NULL,NULL,NULL,5,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401301,400103,'安排面试','interview:schedule',2,3,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401302,400103,'取消面试','interview:cancel',2,3,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401303,400103,'提交反馈','interview:feedback',2,3,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401304,400103,'查看反馈','interview:view_fb',2,3,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401401,400104,'创建Offer','offer:create',2,6,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401402,400104,'审批Offer','offer:approve',2,6,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401403,400104,'发送Offer','offer:send',2,6,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401404,400104,'查看Offer','offer:view',2,6,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401501,400801,'查看用户','user:view',2,9,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401502,400801,'创建用户','user:create',2,9,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401503,400801,'编辑用户','user:edit',2,9,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401504,400801,'删除用户','user:delete',2,9,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401505,400802,'查看角色','role:view',2,9,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401506,400802,'创建角色','role:create',2,9,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401507,400802,'编辑角色','role:edit',2,9,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401508,400802,'删除角色','role:delete',2,9,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401509,400803,'查看权限','perm:view',2,9,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401510,400803,'编辑权限','perm:edit',2,9,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401511,400804,'查看部门','dept:view',2,9,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401512,400804,'创建部门','dept:create',2,9,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401513,400804,'编辑部门','dept:edit',2,9,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401514,400804,'删除部门','dept:delete',2,9,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401515,400008,'查看通知','notification:view',2,9,NULL,NULL,NULL,NULL,NULL,6,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401516,400008,'管理通知','notification:edit',2,9,NULL,NULL,NULL,NULL,NULL,7,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401517,400102,'上传简历','resume:upload',2,2,NULL,NULL,NULL,NULL,NULL,6,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401518,400102,'查看简历','resume:view',2,2,NULL,NULL,NULL,NULL,NULL,7,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401519,400103,'查看面试','interview:view',2,3,NULL,NULL,NULL,NULL,NULL,5,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401520,400103,'创建面试','interview:create',2,3,NULL,NULL,NULL,NULL,NULL,6,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401521,400103,'编辑面试','interview:edit',2,3,NULL,NULL,NULL,NULL,NULL,7,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401522,400104,'编辑Offer','offer:edit',2,6,NULL,NULL,NULL,NULL,NULL,5,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401523,400001,'查看工作台','dashboard:view',2,0,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401524,400007,'查看分析','analytics:view',2,5,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401525,400007,'导出分析','analytics:export',2,5,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401526,400003,'查看人才','talent:view',2,4,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401527,400003,'编辑人才','talent:edit',2,4,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401528,400004,'查看内推','referral:view',2,8,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401529,400004,'创建内推','referral:create',2,8,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401530,400004,'编辑内推','referral:edit',2,8,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401531,400005,'查看入职','onboarding:view',2,7,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401532,400005,'编辑入职','onboarding:edit',2,7,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401533,400006,'查看Agent','agent:view',2,10,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401534,400006,'管理Agent','agent:manage',2,10,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(401535,400806,'编辑系统设置','system:settings:edit',2,9,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-07-26 12:18:24','2026-07-26 12:18:24',NULL,'system',NULL,NULL,0),(401536,400901,'编辑官网配置','careers:config:edit',2,11,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-08-02 20:10:29','2026-08-02 20:10:29',NULL,'system',NULL,NULL,0),(401537,400902,'编辑热门职位','careers:hot-jobs:edit',2,11,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-08-02 20:47:08','2026-08-02 20:47:08',NULL,'system',NULL,NULL,0),(401538,400903,'编辑社会招聘','careers:social:edit',2,11,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-08-02 20:47:08','2026-08-02 20:47:08',NULL,'system',NULL,NULL,0),(401539,400904,'编辑校园招聘','careers:campus:edit',2,11,NULL,NULL,NULL,NULL,NULL,1,1,1,'2026-08-02 20:47:08','2026-08-02 20:47:08',NULL,'system',NULL,NULL,0),(401540,400903,'查看合同','contract:view',2,11,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-08-02 21:29:54','2026-08-09 13:23:32',NULL,'system',NULL,NULL,0),(401541,400904,'管理合同','contract:edit',2,11,NULL,NULL,NULL,NULL,NULL,2,1,1,'2026-08-02 21:29:54','2026-08-09 13:23:32',NULL,'system',NULL,NULL,0),(401601,400101,'审批职位','job:approve',2,0,NULL,NULL,NULL,NULL,NULL,7,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401602,400102,'创建简历','resume:create',2,2,NULL,NULL,NULL,NULL,NULL,8,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401603,400102,'编辑简历','resume:edit',2,2,NULL,NULL,NULL,NULL,NULL,9,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401604,400102,'删除简历','resume:delete',2,2,NULL,NULL,NULL,NULL,NULL,10,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401605,400102,'审批简历','resume:approve',2,2,NULL,NULL,NULL,NULL,NULL,11,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401606,400102,'创建候选人','candidate:create',2,2,NULL,NULL,NULL,NULL,NULL,12,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401607,400102,'删除候选人','candidate:delete',2,2,NULL,NULL,NULL,NULL,NULL,13,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401608,400102,'审批候选人','candidate:approve',2,2,NULL,NULL,NULL,NULL,NULL,14,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401609,400103,'删除面试','interview:delete',2,3,NULL,NULL,NULL,NULL,NULL,8,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401610,400103,'审批面试','interview:approve',2,3,NULL,NULL,NULL,NULL,NULL,9,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401611,400104,'删除Offer','offer:delete',2,6,NULL,NULL,NULL,NULL,NULL,6,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401612,400003,'创建人才','talent:create',2,4,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401613,400003,'删除人才','talent:delete',2,4,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401614,400003,'审批人才','talent:approve',2,4,NULL,NULL,NULL,NULL,NULL,5,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401615,400004,'删除内推','referral:delete',2,8,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401616,400004,'审批内推','referral:approve',2,8,NULL,NULL,NULL,NULL,NULL,5,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401617,400005,'创建入职','onboarding:create',2,7,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401618,400005,'删除入职','onboarding:delete',2,7,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401619,400005,'审批入职','onboarding:approve',2,7,NULL,NULL,NULL,NULL,NULL,5,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401620,400007,'创建分析','analytics:create',2,5,NULL,NULL,NULL,NULL,NULL,3,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401621,400007,'编辑分析','analytics:edit',2,5,NULL,NULL,NULL,NULL,NULL,4,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401622,400007,'删除分析','analytics:delete',2,5,NULL,NULL,NULL,NULL,NULL,5,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401623,400007,'审批分析','analytics:approve',2,5,NULL,NULL,NULL,NULL,NULL,6,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401624,400008,'系统查看','system:view',2,9,NULL,NULL,NULL,NULL,NULL,8,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401625,400008,'系统创建','system:create',2,9,NULL,NULL,NULL,NULL,NULL,9,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401626,400008,'系统编辑','system:edit',2,9,NULL,NULL,NULL,NULL,NULL,10,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401627,400008,'系统删除','system:delete',2,9,NULL,NULL,NULL,NULL,NULL,11,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0),(401628,400008,'系统审批','system:approve',2,9,NULL,NULL,NULL,NULL,NULL,12,1,1,'2026-07-11 09:32:02','2026-07-11 09:32:02',NULL,'system',NULL,NULL,0);




INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `sort_order`, `status`, `create_time`, `update_time`, `create_user_id`, `create_by`, `update_user_id`, `update_by`, `deleted`) VALUES (300001,'超级管理员','ROLE_SUPER_ADMIN','平台超级管理员，拥有所有权限',1,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(300002,'HR管理员','ROLE_HR_ADMIN','人力资源部门管理员',2,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(300003,'招聘专员','ROLE_RECRUITER','招聘专员，负责候选人管理',3,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(300004,'招聘经理','ROLE_HIRING_MGR','招聘经理，负责面试评估和Offer审批',4,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(300005,'面试官','ROLE_INTERVIEWER','面试官，负责面试评估和反馈',5,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(300006,'普通员工','ROLE_EMPLOYEE','普通员工，拥有内推权限',6,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(300007,'只读用户','ROLE_READONLY','只读访问权限',7,1,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0),(300008,'求职者','ROLE_CANDIDATE','求职者，通过手机号快捷登录注册的候选人用户',8,1,'2026-08-01 20:58:26','2026-08-01 20:58:26',NULL,'system',NULL,NULL,0);




INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `create_time`) VALUES (0,300001,400009,'2026-08-05 19:35:03'),(600001,300001,400001,'2026-07-10 19:50:25'),(600002,300001,400002,'2026-07-10 19:50:25'),(600003,300001,400003,'2026-07-10 19:50:25'),(600004,300001,400004,'2026-07-10 19:50:25'),(600005,300001,400005,'2026-07-10 19:50:25'),(600006,300001,400006,'2026-07-10 19:50:25'),(600007,300001,400007,'2026-07-10 19:50:25'),(600008,300001,400008,'2026-07-10 19:50:25'),(600009,300001,400101,'2026-07-10 19:50:25'),(600010,300001,400102,'2026-07-10 19:50:25'),(600011,300001,400103,'2026-07-10 19:50:25'),(600012,300001,400104,'2026-07-10 19:50:25'),(600013,300001,400801,'2026-07-10 19:50:25'),(600014,300001,400802,'2026-07-10 19:50:25'),(600015,300001,400803,'2026-07-10 19:50:25'),(600016,300001,400804,'2026-07-10 19:50:25'),(600017,300001,400805,'2026-07-10 19:50:25'),(600018,300001,401101,'2026-07-10 19:50:25'),(600019,300001,401102,'2026-07-10 19:50:25'),(600020,300001,401103,'2026-07-10 19:50:25'),(600021,300001,401104,'2026-07-10 19:50:25'),(600022,300001,401105,'2026-07-10 19:50:25'),(600023,300001,401106,'2026-07-10 19:50:25'),(600024,300001,401201,'2026-07-10 19:50:25'),(600025,300001,401202,'2026-07-10 19:50:25'),(600026,300001,401203,'2026-07-10 19:50:25'),(600027,300001,401204,'2026-07-10 19:50:25'),(600028,300001,401205,'2026-07-10 19:50:25'),(600029,300001,401301,'2026-07-10 19:50:25'),(600030,300001,401302,'2026-07-10 19:50:25'),(600031,300001,401303,'2026-07-10 19:50:25'),(600032,300001,401304,'2026-07-10 19:50:25'),(600033,300001,401401,'2026-07-10 19:50:25'),(600034,300001,401402,'2026-07-10 19:50:25'),(600035,300001,401403,'2026-07-10 19:50:25'),(600036,300001,401404,'2026-07-10 19:50:25'),(600037,300001,401501,'2026-07-10 19:50:25'),(600038,300001,401502,'2026-07-10 19:50:25'),(600039,300001,401503,'2026-07-10 19:50:25'),(600040,300001,401504,'2026-07-10 19:50:25'),(600041,300001,401505,'2026-07-10 19:50:25'),(600042,300001,401506,'2026-07-10 19:50:25'),(600043,300001,401507,'2026-07-10 19:50:25'),(600044,300001,401508,'2026-07-10 19:50:25'),(600045,300001,401509,'2026-07-10 19:50:25'),(600046,300001,401510,'2026-07-10 19:50:25'),(600047,300001,401511,'2026-07-10 19:50:25'),(600048,300001,401512,'2026-07-10 19:50:25'),(600049,300001,401513,'2026-07-10 19:50:25'),(600050,300001,401514,'2026-07-10 19:50:25'),(600051,300001,401515,'2026-07-10 19:50:25'),(600052,300001,401516,'2026-07-10 19:50:25'),(600053,300001,401517,'2026-07-10 19:50:25'),(600054,300001,401518,'2026-07-10 19:50:25'),(600055,300001,401519,'2026-07-10 19:50:25'),(600056,300001,401520,'2026-07-10 19:50:25'),(600057,300001,401521,'2026-07-10 19:50:25'),(600058,300001,401522,'2026-07-10 19:50:25'),(600059,300001,401523,'2026-07-10 19:50:25'),(600060,300001,401524,'2026-07-10 19:50:25'),(600061,300001,401525,'2026-07-10 19:50:25'),(600062,300001,401526,'2026-07-10 19:50:25'),(600063,300001,401527,'2026-07-10 19:50:25'),(600064,300001,401528,'2026-07-10 19:50:25'),(600065,300001,401529,'2026-07-10 19:50:25'),(600066,300001,401530,'2026-07-10 19:50:25'),(600067,300001,401531,'2026-07-10 19:50:25'),(600068,300001,401532,'2026-07-10 19:50:25'),(600069,300001,401533,'2026-07-10 19:50:25'),(600070,300001,401534,'2026-07-10 19:50:25'),(620001,300003,400001,'2026-07-10 19:50:25'),(620002,300003,400002,'2026-07-10 19:50:25'),(620003,300003,400101,'2026-07-10 19:50:25'),(620004,300003,400102,'2026-07-10 19:50:25'),(620005,300003,400103,'2026-07-10 19:50:25'),(620006,300003,400104,'2026-07-10 19:50:25'),(620007,300003,400003,'2026-07-10 19:50:25'),(620008,300003,400004,'2026-07-10 19:50:25'),(620009,300003,401101,'2026-07-10 19:50:25'),(620010,300003,401102,'2026-07-10 19:50:25'),(620011,300003,401104,'2026-07-10 19:50:25'),(620012,300003,401106,'2026-07-10 19:50:25'),(620013,300003,401201,'2026-07-10 19:50:25'),(620014,300003,401202,'2026-07-10 19:50:25'),(620015,300003,401203,'2026-07-10 19:50:25'),(620016,300003,401204,'2026-07-10 19:50:25'),(620017,300003,401205,'2026-07-10 19:50:25'),(620018,300003,401301,'2026-07-10 19:50:25'),(620019,300003,401401,'2026-07-10 19:50:25'),(620020,300003,401403,'2026-07-10 19:50:25'),(620021,300003,401404,'2026-07-10 19:50:25'),(630001,300004,400001,'2026-07-10 19:50:25'),(630002,300004,400002,'2026-07-10 19:50:25'),(630003,300004,400101,'2026-07-10 19:50:25'),(630004,300004,400102,'2026-07-10 19:50:25'),(630005,300004,400103,'2026-07-10 19:50:25'),(630006,300004,400104,'2026-07-10 19:50:25'),(630007,300004,401106,'2026-07-10 19:50:25'),(630008,300004,401201,'2026-07-10 19:50:25'),(630009,300004,401303,'2026-07-10 19:50:25'),(630010,300004,401304,'2026-07-10 19:50:25'),(630011,300004,401402,'2026-07-10 19:50:25'),(630012,300004,401404,'2026-07-10 19:50:25'),(640001,300005,400001,'2026-07-10 19:50:25'),(640002,300005,400103,'2026-07-10 19:50:25'),(640003,300005,401303,'2026-07-10 19:50:25'),(640004,300005,401304,'2026-07-10 19:50:25'),(650001,300006,400001,'2026-07-10 19:50:25'),(650002,300006,400004,'2026-07-10 19:50:25'),(660001,300007,400001,'2026-07-10 19:50:25'),(660002,300007,400002,'2026-07-10 19:50:25'),(660003,300007,400003,'2026-07-10 19:50:25'),(660004,300007,400004,'2026-07-10 19:50:25'),(660005,300007,400005,'2026-07-10 19:50:25'),(660007,300007,400007,'2026-07-10 19:50:25'),(660008,300007,400101,'2026-07-10 19:50:25'),(660009,300007,400102,'2026-07-10 19:50:25'),(660010,300007,400103,'2026-07-10 19:50:25'),(660011,300007,400104,'2026-07-10 19:50:25'),(670001,300001,400806,'2026-07-26 12:18:24'),(670002,300001,401535,'2026-07-26 12:18:24'),(670003,300001,400901,'2026-08-02 20:10:29'),(670004,300001,401536,'2026-08-02 20:10:29'),(670005,300001,400902,'2026-08-02 20:47:08'),(670006,300001,400903,'2026-08-02 20:47:08'),(670007,300001,400904,'2026-08-02 20:47:08'),(670008,300001,401537,'2026-08-02 20:47:08'),(670009,300001,401538,'2026-08-02 20:47:08'),(670010,300001,401539,'2026-08-02 20:47:08'),(670011,300001,401540,'2026-08-02 21:29:54'),(670012,300001,401541,'2026-08-02 21:29:54'),(680001,300002,400009,'2026-08-06 19:39:44'),(680002,300002,400901,'2026-08-06 19:39:44'),(680003,300002,400902,'2026-08-06 19:39:44'),(680004,300002,400903,'2026-08-06 19:39:44'),(680005,300002,400904,'2026-08-06 19:39:44'),(680006,300002,401536,'2026-08-06 19:39:44'),(680010,300004,400003,'2026-08-06 19:39:44'),(680011,300004,400005,'2026-08-06 19:39:44'),(680012,300004,400007,'2026-08-06 19:39:44'),(680013,300004,400009,'2026-08-06 19:39:44'),(680014,300004,400901,'2026-08-06 19:39:44'),(680015,300004,400902,'2026-08-06 19:39:44'),(680016,300004,400903,'2026-08-06 19:39:44'),(680017,300004,400904,'2026-08-06 19:39:44'),(680018,300004,401536,'2026-08-06 19:39:44'),(680021,300004,401202,'2026-08-06 19:39:44'),(680022,300004,401203,'2026-08-06 19:39:44'),(680023,300004,401204,'2026-08-06 19:39:44'),(680024,300004,401205,'2026-08-06 19:39:44'),(680025,300004,401301,'2026-08-06 19:39:44'),(680026,300004,401302,'2026-08-06 19:39:44'),(700001,300001,400105,'2026-08-09 13:23:32'),(700004,300002,400105,'2026-08-09 13:23:32'),(700005,300002,401540,'2026-08-09 13:23:32'),(700006,300002,401541,'2026-08-09 13:23:32'),(700007,300004,400105,'2026-08-09 13:23:32'),(700008,300004,401540,'2026-08-09 13:23:32'),(2075746191052517379,300001,401601,'2026-07-11 09:32:11'),(2075746191052517380,300001,401602,'2026-07-11 09:32:11'),(2075746191052517381,300001,401603,'2026-07-11 09:32:11'),(2075746191052517382,300001,401604,'2026-07-11 09:32:11'),(2075746191052517383,300001,401605,'2026-07-11 09:32:11'),(2075746191052517384,300001,401606,'2026-07-11 09:32:11'),(2075746191052517385,300001,401607,'2026-07-11 09:32:11'),(2075746191052517386,300001,401608,'2026-07-11 09:32:11'),(2075746191052517387,300001,401609,'2026-07-11 09:32:11'),(2075746191052517388,300001,401610,'2026-07-11 09:32:11'),(2075746191052517389,300001,401611,'2026-07-11 09:32:11'),(2075746191052517390,300001,401612,'2026-07-11 09:32:11'),(2075746191052517391,300001,401613,'2026-07-11 09:32:11'),(2075746191052517392,300001,401614,'2026-07-11 09:32:11'),(2075746191052517393,300001,401615,'2026-07-11 09:32:11'),(2075746191052517394,300001,401616,'2026-07-11 09:32:11'),(2075746191052517395,300001,401617,'2026-07-11 09:32:11'),(2075746191052517396,300001,401618,'2026-07-11 09:32:11'),(2075746191052517397,300001,401619,'2026-07-11 09:32:11'),(2075746191052517398,300001,401620,'2026-07-11 09:32:11'),(2075746191052517399,300001,401621,'2026-07-11 09:32:11'),(2075746191052517400,300001,401622,'2026-07-11 09:32:11'),(2075746191052517401,300001,401623,'2026-07-11 09:32:11'),(2075746191052517402,300001,401624,'2026-07-11 09:32:11'),(2075746191052517403,300001,401625,'2026-07-11 09:32:11'),(2075746191052517404,300001,401626,'2026-07-11 09:32:11'),(2075746191052517405,300001,401627,'2026-07-11 09:32:11'),(2075746191052517406,300001,401628,'2026-07-11 09:32:11'),(2075860606171201537,300002,401101,'2026-07-11 16:31:51'),(2075860608431931393,300002,401102,'2026-07-11 16:31:51'),(2075860608457097217,300002,401103,'2026-07-11 16:31:51'),(2075860608486457345,300002,401106,'2026-07-11 16:31:51'),(2075860608503234562,300002,401601,'2026-07-11 16:31:51'),(2075860608532594689,300002,401104,'2026-07-11 16:31:51'),(2075860608540983298,300002,401105,'2026-07-11 16:31:51'),(2075860608566149122,300002,401518,'2026-07-11 16:31:51'),(2075860608612286465,300002,401602,'2026-07-11 16:31:51'),(2075860608637452289,300002,401603,'2026-07-11 16:31:51'),(2075860608666812418,300002,401604,'2026-07-11 16:31:51'),(2075860608687783938,300002,401605,'2026-07-11 16:31:51'),(2075860608725532674,300002,401203,'2026-07-11 16:31:51'),(2075860608738115586,300002,401204,'2026-07-11 16:31:51'),(2075860608754892802,300002,401205,'2026-07-11 16:31:51'),(2075860608763281409,300002,401517,'2026-07-11 16:31:51'),(2075860608775864322,300002,401201,'2026-07-11 16:31:51'),(2075860608775864323,300008,400001,'2026-08-01 20:59:29'),(2075860608775864324,300008,400002,'2026-08-01 20:59:29');




INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `email`, `mobile`, `avatar`, `gender`, `department_id`, `status`, `last_login_time`, `last_login_ip`, `remark`, `create_time`, `update_time`, `create_user_id`, `create_by`, `update_user_id`, `update_by`, `deleted`, `referral_code`, `position`, `job_level`, `age`) VALUES (1001,'zhangwei','$2b$12$sjxTwz6Ql7/I7nBYvX3v9Or2YHSRMzuv58TkIkmUlIt356u69zK9.','张伟','12lisu@163.com',NULL,NULL,0,100004,1,'2026-08-07 15:32:22','127.0.0.1',NULL,'2026-07-25 14:07:22','2026-07-25 14:07:22',NULL,'system',NULL,NULL,0,NULL,NULL,NULL,NULL),(1005,'chenming','$2b$12$sjxTwz6Ql7/I7nBYvX3v9Or2YHSRMzuv58TkIkmUlIt356u69zK9.','陈明','12lisu@163.com',NULL,NULL,0,100002,1,'2026-08-07 15:50:03','127.0.0.1',NULL,'2026-07-25 14:07:22','2026-07-25 14:07:22',NULL,'system',NULL,NULL,0,NULL,NULL,NULL,NULL),(1101,'dengchao','$2b$12$sjxTwz6Ql7/I7nBYvX3v9Or2YHSRMzuv58TkIkmUlIt356u69zK9.','邓超','12lisu@163.com',NULL,NULL,0,100002,1,'2026-08-07 22:27:00','127.0.0.1',NULL,'2026-07-25 14:07:22','2026-07-25 14:07:22',NULL,'system',NULL,NULL,0,NULL,NULL,NULL,NULL),(1102,'guofeng','$2b$12$sjxTwz6Ql7/I7nBYvX3v9Or2YHSRMzuv58TkIkmUlIt356u69zK9.','郭峰','11lisu@163.com','18100256887',NULL,1,100002,1,NULL,NULL,NULL,'2026-07-25 14:07:22','2026-07-25 14:07:22',NULL,'system',NULL,NULL,0,NULL,'高级Java开发工程师（微服务方向）','P6',29),(200001,'admin','$2b$12$D4iOjKH421dA9ywv9Mwl.O7X4L6KD8SQzYfEXxdhEtl4mdjihTn5m','管理员','admin@smartrecruit.com','13800000001','http://117.72.88.11:9091/smart-recruit-dev/avatars/200001_51b1b327-332b-4d05-b842-b63b66b415eb.jpg',0,100001,1,'2026-07-10 19:51:42','127.0.0.1',NULL,'2026-07-10 19:50:25','2026-07-18 20:18:47',NULL,'system',NULL,NULL,0,NULL,NULL,NULL,NULL),(200002,'hr_zhang','$2a$12$L5Hf/BJrhsuHJxZ.wDlwv.W5c4h.1JdWDSEl5BnjjRa7/1vMSOMpu','张伟','zhangwei@smartrecruit.com','13800000002',NULL,0,100004,1,NULL,NULL,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0,NULL,NULL,NULL,NULL),(200003,'hr_li','$2b$12$8CeYeeWWxlQDT8dQJSfn3edXnrRsz0R/i12biZgkO6D5t.faeHxYa','李娜','lina@smartrecruit.com','13800000003',NULL,0,100004,0,NULL,NULL,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0,NULL,NULL,NULL,NULL),(200004,'hr_wang','$2b$12$8CeYeeWWxlQDT8dQJSfn3edXnrRsz0R/i12biZgkO6D5t.faeHxYa','王芳','wangfang@smartrecruit.com','13800000004',NULL,0,100004,0,NULL,NULL,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0,NULL,NULL,NULL,NULL),(200008,'pm_zhou','$2b$12$8CeYeeWWxlQDT8dQJSfn3edXnrRsz0R/i12biZgkO6D5t.faeHxYa','周杰','zhoujie@smartrecruit.com','13800000008',NULL,0,100003,1,NULL,NULL,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0,NULL,NULL,NULL,NULL),(200009,'hm_sun','$2b$12$8CeYeeWWxlQDT8dQJSfn3edXnrRsz0R/i12biZgkO6D5t.faeHxYa','孙明','sunming@smartrecruit.com','13800000009',NULL,0,100002,1,NULL,NULL,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0,NULL,NULL,NULL,NULL),(200010,'hm_wu','$2b$12$8CeYeeWWxlQDT8dQJSfn3edXnrRsz0R/i12biZgkO6D5t.faeHxYa','吴静','wujing@smartrecruit.com','13800000010',NULL,0,100003,1,NULL,NULL,NULL,'2026-07-10 19:50:25','2026-07-10 19:50:25',NULL,'system',NULL,NULL,0,NULL,NULL,NULL,NULL),(2075564483359485954,'susan','$2a$12$/eTOEw1s5j2DRrNSY2dqt.EKJMY9nPb7.q2K23pxqp/YvMXKP4HDO','苏三','susan@163.com','18200256778','http://117.72.88.11:9091/smart-recruit-dev/avatars/2075564483359485954_78536d23-7502-4db4-801a-67e241f60a30.jpeg',0,100002,1,'2026-08-12 14:10:10','192.168.1.29',NULL,'2026-07-10 20:55:10','2026-07-10 20:55:10',NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL),(2083536561752743938,'u_18300256887','$2a$12$OklcqCQchMQyE/ZUTaEbTuUqoe6kc8CUQ4mrs8IOB93nKBhnFZjQy','用户6887',NULL,'18300256887',NULL,0,NULL,0,'2026-08-02 14:35:53','127.0.0.1',NULL,'2026-08-01 20:53:21','2026-08-01 20:53:21',NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL),(2083807125314707457,'u_18400256887','$2a$12$WvGnpt08y0RKbHn/m.b0Hec6eaK2bIRHNKrwXxfuwlafhEzSUYlWy','用户6887',NULL,'18400256887',NULL,0,NULL,1,'2026-08-04 09:20:22','127.0.0.1',NULL,'2026-08-02 14:48:29','2026-08-02 14:48:29',NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL),(2083835812835729409,'u_18500256666','$2a$12$8bAtpLBHsLhoz6.n0p80YerEnsNhKcSX/BvbWDg9C0gLSs4QvK4e.','用户6666',NULL,'18500256666',NULL,0,NULL,1,'2026-08-02 16:42:28','127.0.0.1',NULL,'2026-08-02 16:42:28','2026-08-02 16:42:28',NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL),(2083848964981796866,'u_18600256887','$2a$12$g76wQlcOSVRqwKfBa9D3yej8T918EbvNoF09/oks178h7Fh7leXtC','用户6887',NULL,'18600256887',NULL,0,NULL,1,'2026-08-02 17:34:44','127.0.0.1',NULL,'2026-08-02 17:34:44','2026-08-02 17:34:44',NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL),(2083880025577107458,'u_18200256889','$2a$12$ROQ.yA3s.nrvyYGXysg./uzKb.KTbv1fnYTvmxHsz1OVNZ27FhaXy','用户6889',NULL,'18200256889',NULL,0,NULL,1,'2026-08-02 19:45:28','127.0.0.1',NULL,'2026-08-02 19:38:09','2026-08-02 19:38:09',NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL);




INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_time`) VALUES (0,2075564483359485954,300001,'2026-07-26 16:49:23'),(500001,200001,300001,'2026-07-10 19:50:25'),(500002,200002,300002,'2026-07-10 19:50:25'),(500003,200003,300003,'2026-07-10 19:50:25'),(500004,200004,300003,'2026-07-10 19:50:25'),(500009,200009,300004,'2026-07-10 19:50:25'),(500010,200010,300004,'2026-07-10 19:50:25'),(500101,1101,300004,'2026-07-25 14:07:22'),(500102,1101,300006,'2026-07-25 14:07:22'),(500105,1005,300004,'2026-07-25 14:07:22'),(500106,1005,300006,'2026-07-25 14:07:22'),(500107,1001,300002,'2026-07-25 14:07:22'),(2075557394767654913,200008,300006,'2026-07-10 20:27:00'),(2075564484324175873,2075564483359485954,300005,'2026-07-10 20:55:10'),(2083528567367380993,1102,300004,'2026-08-01 20:21:35'),(2083536562151202818,2083536561752743938,300008,'2026-08-01 20:53:21'),(2083807125390204929,2083807125314707457,300006,'2026-08-02 14:48:29'),(2083835812969947138,2083835812835729409,300008,'2026-08-02 16:42:28'),(2083848965367672833,2083848964981796866,300008,'2026-08-02 17:34:44'),(2083880025799405569,2083880025577107458,300008,'2026-08-02 19:38:09');




INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `description`, `create_time`, `update_time`, `create_by`, `create_user_id`, `update_by`, `update_user_id`) VALUES (1,'email_suffix','@company.com','系统邮箱后缀，用户邮箱由【用户名 + 此后缀】组成','2026-07-26 12:18:24','2026-07-26 12:18:24','system',NULL,'susan@163.com',NULL),(2,'system_name','SmartRecruit','系统名称，显示在页面标题、邮件标题等处','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,'susan@163.com',NULL),(3,'copyright_text','© 2026 SmartRecruit. All rights reserved.','页面底部版权信息','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,'susan@163.com',NULL),(4,'email_sender_name','SmartRecruit','系统邮件发件人显示名称','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,'susan@163.com',NULL),(5,'email_sender_address','12lisu@163.com','系统邮件发件人邮箱地址','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,'susan@163.com',NULL),(6,'password_min_length','7','密码最小长度','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,'susan@163.com',2075564483359485954),(7,'password_max_length','64','密码最大长度','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,'susan@163.com',2075564483359485954),(8,'password_require_special','1','是否要求密码包含特殊字符：0=否, 1=是','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,'susan@163.com',2075564483359485954),(9,'login_max_attempts','5','登录失败最大尝试次数，超过后账号临时锁定','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,'susan@163.com',2075564483359485954),(10,'session_timeout_minutes','120','会话超时时间（分钟），无操作后自动登出','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,'susan@163.com',2075564483359485954),(11,'upload_max_size_mb','10','文件上传大小限制（MB）','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,NULL,NULL),(12,'upload_allowed_extensions','.pdf,.jpg,.jpeg,.png,.gif,.doc,.docx,.xls,.xlsx','允许上传的文件类型，逗号分隔','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,NULL,NULL),(13,'onboarding_welcome_template','亲爱的 {{employeeName}}：\n\n欢迎加入 {{departmentName}} 部门！\n您的职位：{{jobTitle}}（{{level}}）\n入职日期：{{onboardDate}}\n\n我们为您准备了完善的入职培训计划，您的导师和伙伴将协助您快速融入团队。\n\n期待与您共同成长！\n\n—— 人力资源部','入职欢迎消息模板，支持变量：{{employeeName}}, {{departmentName}}, {{jobTitle}}, {{level}}, {{onboardDate}}','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,NULL,NULL),(14,'onboarding_training_modules','公司文化与制度,信息安全培训,岗位技能培训,合规培训,团队介绍','入职培训模块列表，逗号分隔','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,NULL,NULL),(15,'onboarding_default_password','123456','新员工入职账号默认密码','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,NULL,NULL),(16,'ai_risk_threshold_low','0.25','AI留任预测-低风险阈值','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,NULL,NULL),(17,'ai_risk_threshold_medium','0.50','AI留任预测-中风险阈值','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,NULL,NULL),(18,'ai_risk_threshold_high','0.75','AI留任预测-高风险阈值','2026-07-26 12:28:28','2026-07-26 12:28:28','system',NULL,NULL,NULL),(19,'careers_hero_badge','2026 社会招聘 & 校园招聘同步开启','Hero 徽章文字','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(20,'careers_hero_title_main','20','AI 简历筛选-学历维度权重（%），五项权重合计应为 100','2026-08-02 20:10:29','2026-08-09 13:09:08','system',NULL,NULL,NULL),(21,'careers_hero_title_gradient','35','AI 简历筛选-技能维度权重（%），五项权重合计应为 100','2026-08-02 20:10:29','2026-08-09 13:09:08','system',NULL,NULL,NULL),(22,'careers_hero_desc','20','AI 简历筛选-经验维度权重（%），五项权重合计应为 100','2026-08-02 20:10:29','2026-08-09 13:09:08','system',NULL,NULL,NULL),(23,'careers_hero_cta_primary_label','20','AI 简历筛选-行为维度权重（%），五项权重合计应为 100','2026-08-02 20:10:29','2026-08-09 13:09:08','system',NULL,NULL,NULL),(24,'careers_hero_cta_outline_label','15','AI 简历筛选-语义维度权重（%），五项权重合计应为 100','2026-08-02 20:10:29','2026-08-09 13:09:08','system',NULL,NULL,NULL),(25,'careers_hero_stats','[{\"number\":1200,\"suffix\":\"\",\"label\":\"全球员工\"},{\"number\":18,\"suffix\":\"\",\"label\":\"全球办公城市\"},{\"number\":500,\"suffix\":\"万+\",\"label\":\"服务企业客户\"},{\"number\":96,\"suffix\":\"%\",\"label\":\"员工推荐率\"}]','Hero 统计数据（JSON数组，每项含 number/suffix/label）','2026-08-02 20:10:29','2026-08-11 10:38:00','system',NULL,NULL,NULL),(26,'careers_culture_title','我们的文化','企业文化板块标题','2026-08-02 20:10:29','2026-08-11 11:04:37','system',NULL,NULL,NULL),(27,'careers_culture_subtitle','六大核心价值观驱动我们不断前进','企业文化板块副标题','2026-08-02 20:10:29','2026-08-11 11:04:37','system',NULL,NULL,NULL),(28,'careers_culture_cards','[{\"title\":\"技术驱动创新\",\"description\":\"深度投入 AI / LLM 前沿技术领域，每年技术投入占比营收 25%+，让工程师站在技术浪潮之巅。\",\"iconBg\":\"icon-purple\"},{\"title\":\"高速成长通道\",\"description\":\"双阶梯晋升体系（管理 + 专家），每半年一次晋升窗口，优秀人才不受年限破格提拔。\",\"iconBg\":\"icon-rose\"},{\"title\":\"开放包容文化\",\"description\":\"扁平化组织，坦诚清晰沟通，CEO 定期全员 AMA。多元背景人才汇聚，尊重每一种声音。\",\"iconBg\":\"icon-amber\"},{\"title\":\"数据驱动决策\",\"description\":\"A/B 实验文化深入骨髓，从产品功能到内部流程，一切以数据说话，拒绝拍脑袋。\",\"iconBg\":\"icon-emerald\"},{\"title\":\"顶级工具与资源\",\"description\":\"MacBook Pro + 4K 显示器标配，正版 IDE / AI 工具全报销。提供丰富技术会议与培训资源。\",\"iconBg\":\"icon-cyan\"},{\"title\":\"创业精神永续\",\"description\":\"保持 Day 1 心态，鼓励内部创业与孵化项目。优秀内部项目可获得种子投资与独立运营机会。\",\"iconBg\":\"icon-indigo\"}]','企业文化卡片（JSON数组，每项含 title/description/iconBg）','2026-08-02 20:10:29','2026-08-11 10:53:17','system',NULL,NULL,NULL),(29,'careers_benefits_title','薪酬福利','福利板块标题','2026-08-02 20:10:29','2026-08-11 11:04:37','system',NULL,NULL,NULL),(30,'careers_benefits_subtitle','我们提供行业领先的薪酬与福利体系，关爱每一位伙伴','福利板块副标题','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(31,'careers_benefits_items','[{\"title\":\"具有竞争力的薪酬\",\"description\":\"16 薪起 + 年度调薪，优秀人才享签字费与签约奖金，薪资水平对标一线大厂。\",\"colorClass\":\"c1\"},{\"title\":\"全方位健康保障\",\"description\":\"六险一金（含补充商业保险），年度高端体检，家属体检折扣，EAP 心理援助计划。\",\"colorClass\":\"c2\"},{\"title\":\"弹性工作与假期\",\"description\":\"弹性上下班 + 混合办公（每周可选 2 天远程），12 天带薪年假起，带薪病假不限额。\",\"colorClass\":\"c3\"},{\"title\":\"成长学习基金\",\"description\":\"年度 8,000 元学习基金，覆盖课程 / 书籍 / 会议。内部技术分享 + 外部专家讲座常态化。\",\"colorClass\":\"c4\"},{\"title\":\"股权激励计划\",\"description\":\"核心岗位授予期权 / RSU，与公司共享成长红利。每年新增授予以持续激励。\",\"colorClass\":\"c5\"},{\"title\":\"安居乐业支持\",\"description\":\"安家补贴 + 租房补贴，首次入职异地搬迁全额报销。购房免息借款助力安家。\",\"colorClass\":\"c6\"},{\"title\":\"专利与论文激励\",\"description\":\"专利申请奖 10,000 元起，顶级会议论文奖金 20,000 元。鼓励技术创新与学术贡献。\",\"colorClass\":\"c7\"},{\"title\":\"全球轮岗机会\",\"description\":\"北京 / 上海 / 深圳 / 杭州等城市自由轮岗，未来开放海外办公室短期交换机会。\",\"colorClass\":\"c8\"}]','薪酬福利条目（JSON数组，每项含 title/description/colorClass）','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(32,'careers_testimonials_title','员工心声','员工心声板块标题','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(33,'careers_testimonials_subtitle','听听我们的伙伴怎么说','员工心声板块副标题','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(34,'careers_testimonials','[{\"quote\":\"加入 SmartRecruit 三年，从一名普通工程师成长为 AI 算法负责人。这里不仅有顶尖的技术氛围，更重要的是给了我充分的自主权和试错空间。每年两次的晋升窗口让成长路径非常清晰。\",\"name\":\"张明远\",\"initials\":\"张\",\"role\":\"AI 算法专家 · 2023 年加入\",\"avatarClass\":\"ta1\"},{\"quote\":\"作为两个孩子的妈妈，我最看重的就是工作与生活的平衡。SmartRecruit 的弹性工作制和混合办公政策让我既能全心投入工作，又不错过孩子的成长。公司对女性员工的关怀非常到位。\",\"name\":\"林晓萌\",\"initials\":\"林\",\"role\":\"产品总监 · 2022 年加入\",\"avatarClass\":\"ta2\"},{\"quote\":\"在上一家公司做了五年螺丝钉，来到 SmartRecruit 最大的感受是「被看见」。你的想法会被认真对待，做出的成果会被认可。扁平化的组织让新人也有机会直接跟 VP 级别的前辈交流。\",\"name\":\"陈浩然\",\"initials\":\"陈\",\"role\":\"高级后端工程师 · 2024 年加入\",\"avatarClass\":\"ta3\"}]','员工心声（JSON数组，每项含 quote/name/initials/role/avatarClass）','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(35,'careers_faq_title','常见问题','FAQ板块标题','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(36,'careers_faq_subtitle','关于招聘的常见疑问，这里都有答案','FAQ板块副标题','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(37,'careers_faq_items','[{\"question\":\"招聘流程是怎样的？\",\"answer\":\"简历投递 → 简历筛选（1-3 个工作日）→ 技术面试（2-3 轮，通常含编程考察与系统设计）→ HR 面试（文化匹配与职业规划）→ Offer 沟通 → 正式入职。整体流程通常 2-3 周内完成。\"},{\"question\":\"是否支持远程办公？\",\"answer\":\"我们采用混合办公模式，每周可选 2 天远程办公，核心协作时间（如重要会议、团队站会）需到岗参与。部分岗位（如标注、客服）支持全远程。\"},{\"question\":\"技术面试主要考察什么？\",\"answer\":\"我们不考八股文，更关注计算机基础（数据结构、算法复杂度）、工程能力（代码质量、系统设计、调试思路）以及领域深度。面试过程是双向交流，也欢迎你反向考察团队。\"},{\"question\":\"应届生有培训体系吗？\",\"answer\":\"有的。\\\"星火计划\\\"是我们为新人和应届生设计的系统培养体系，包括：① 1 对 1 Mentor 带教；② 6 个月轮岗了解公司核心业务；③ 定期的技术沙龙与软技能培训；④ 毕业答辩 + 定岗定级。\"},{\"question\":\"可以同时投递多个岗位吗？\",\"answer\":\"最多同时投递 3 个岗位。我们建议根据自身兴趣与能力精准投递，HR 也会在筛选阶段结合你的背景推荐最合适的岗位方向。\"}]','FAQ条目（JSON数组，每项含 question/answer）','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(38,'careers_gallery_title','我们的团队','团队风采板块标题','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(39,'careers_gallery_subtitle','记录每一个精彩瞬间，一起创造美好回忆','团队风采板块副标题','2026-08-02 20:10:29','2026-08-02 20:10:29','system',NULL,NULL,NULL),(40,'careers_gallery_items','[{\"caption\":\"黑客马拉松 2026\",\"subcaption\":\"48 小时极限编程挑战\",\"gradientClass\":\"g1\",\"image\":\"https://picsum.photos/id/60/680/520\"},{\"caption\":\"团队 Offsite 团建\",\"subcaption\":\"在山水间凝聚团队力量\",\"gradientClass\":\"g2\",\"image\":\"https://picsum.photos/id/1/680/520\"},{\"caption\":\"开放式办公环境\",\"subcaption\":\"激发创造力的灵感空间\",\"gradientClass\":\"g3\",\"image\":\"https://picsum.photos/id/10/680/520\"},{\"caption\":\"AI 技术分享会\",\"subcaption\":\"与大咖共话技术前沿\",\"gradientClass\":\"g4\",\"image\":\"https://picsum.photos/id/20/680/520\"},{\"caption\":\"年会盛典\",\"subcaption\":\"年度高光时刻\",\"gradientClass\":\"g5\",\"image\":\"https://picsum.photos/id/26/680/520\"},{\"caption\":\"新员工训练营\",\"subcaption\":\"星火计划 · 融入之旅\",\"gradientClass\":\"g1\",\"image\":\"https://picsum.photos/id/30/680/520\"}]','团队风采条目（JSON数组，每项含 caption/subcaption/gradientClass/image）','2026-08-02 20:10:29','2026-08-03 13:50:29','system',NULL,NULL,NULL),(41,'careers_social_hero_badge','社会招聘','社会招聘 Hero 徽章文字','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(42,'careers_social_hero_title','寻找经验丰富的专业人才','社会招聘 Hero 主标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(43,'careers_social_hero_desc','加入 SmartRecruit 核心团队，与行业顶尖人才一起，\n用 AI 重塑招聘的未来。我们提供具有竞争力的薪酬和开放的成长空间。','社会招聘 Hero 描述文字','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(44,'careers_social_hero_stats','[{\"number\":\"30+\",\"label\":\"在招职位\"},{\"number\":\"5\",\"label\":\"办公城市\"},{\"number\":\"16 薪\",\"label\":\"薪酬保障\"}]','社会招聘 Hero 统计数据（JSON数组，每项含 number/label）','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(45,'careers_social_title','社会招聘职位','社会招聘职位板块标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(46,'careers_social_subtitle','我们正在寻找经验丰富的专业人才，加入核心团队，共同打造下一代智能招聘平台。','社会招聘职位板块副标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(47,'careers_social_empty_text','暂无匹配的职位，请尝试其他筛选条件','社会招聘空状态文字','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(48,'careers_social_filters','[\"全部职位\",\"技术研发\",\"产品 & 设计\",\"市场 & 销售\",\"数据 & AI\",\"运营 & 职能\",\"管理\"]','社会招聘筛选分类（JSON字符串数组）','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(49,'careers_social_jobs','[{\"id\":101,\"title\":\"技术总监（AI 平台）\",\"dept\":\"技术研发部·AI平台组\",\"location\":\"北京\",\"exp\":\"10-15年\",\"salary\":\"80-120K·16薪\",\"category\":\"tech\",\"date\":\"2026-07-25\",\"tags\":[{\"text\":\"热招\",\"cls\":\"hot\"},{\"text\":\"管理岗\",\"cls\":\"tech-tag\"},{\"text\":\"AI\",\"cls\":\"tech-tag\"}]},{\"id\":102,\"title\":\"资深后端架构师（Java）\",\"dept\":\"技术研发部·核心平台组\",\"location\":\"北京/上海\",\"exp\":\"8-12年\",\"salary\":\"60-90K·16薪\",\"category\":\"tech\",\"date\":\"2026-07-24\",\"tags\":[{\"text\":\"热招\",\"cls\":\"hot\"},{\"text\":\"Java\",\"cls\":\"tech-tag\"},{\"text\":\"架构\",\"cls\":\"tech-tag\"}]},{\"id\":103,\"title\":\"高级产品总监（SaaS 平台）\",\"dept\":\"产品部·招聘产品线\",\"location\":\"北京\",\"exp\":\"10-15年\",\"salary\":\"70-100K·16薪\",\"category\":\"product\",\"date\":\"2026-07-23\",\"tags\":[{\"text\":\"B端\",\"cls\":\"tech-tag\"},{\"text\":\"SaaS\",\"cls\":\"tech-tag\"},{\"text\":\"管理岗\",\"cls\":\"tech-tag\"}]},{\"id\":104,\"title\":\"资深 AI 研究员（LLM）\",\"dept\":\"AI 研究院\",\"location\":\"北京/上海\",\"exp\":\"5-12年\",\"salary\":\"60-100K·16薪\",\"category\":\"data\",\"date\":\"2026-07-22\",\"tags\":[{\"text\":\"LLM\",\"cls\":\"tech-tag\"},{\"text\":\"NLP\",\"cls\":\"tech-tag\"},{\"text\":\"博士优先\",\"cls\":\"new\"}]},{\"id\":105,\"title\":\"大客户销售总监\",\"dept\":\"销售部·大客户团队\",\"location\":\"北京/上海/深圳\",\"exp\":\"8-15年\",\"salary\":\"50-80K·16薪\",\"category\":\"market\",\"date\":\"2026-07-21\",\"tags\":[{\"text\":\"热招\",\"cls\":\"hot\"},{\"text\":\"管理岗\",\"cls\":\"tech-tag\"}]},{\"id\":106,\"title\":\"资深安全架构师\",\"dept\":\"基础架构部·安全团队\",\"location\":\"北京\",\"exp\":\"8-12年\",\"salary\":\"55-85K·16薪\",\"category\":\"tech\",\"date\":\"2026-07-20\",\"tags\":[{\"text\":\"安全\",\"cls\":\"tech-tag\"},{\"text\":\"架构\",\"cls\":\"tech-tag\"}]},{\"id\":107,\"title\":\"高级财务总监\",\"dept\":\"财务部\",\"location\":\"北京\",\"exp\":\"10-15年\",\"salary\":\"60-90K·16薪\",\"category\":\"operation\",\"date\":\"2026-07-19\",\"tags\":[{\"text\":\"管理岗\",\"cls\":\"tech-tag\"},{\"text\":\"CPA\",\"cls\":\"tech-tag\"}]},{\"id\":108,\"title\":\"资深 DevOps 工程师\",\"dept\":\"基础架构部·工程效能组\",\"location\":\"杭州\",\"exp\":\"5-10年\",\"salary\":\"45-70K·16薪\",\"category\":\"tech\",\"date\":\"2026-07-18\",\"tags\":[{\"text\":\"K8s\",\"cls\":\"tech-tag\"},{\"text\":\"CI/CD\",\"cls\":\"tech-tag\"}]},{\"id\":109,\"title\":\"高级数据平台架构师\",\"dept\":\"数据与AI平台部\",\"location\":\"北京/杭州\",\"exp\":\"8-12年\",\"salary\":\"55-85K·16薪\",\"category\":\"data\",\"date\":\"2026-07-17\",\"tags\":[{\"text\":\"大数据\",\"cls\":\"tech-tag\"},{\"text\":\"Flink\",\"cls\":\"tech-tag\"},{\"text\":\"架构\",\"cls\":\"tech-tag\"}]},{\"id\":110,\"title\":\"HR 副总裁\",\"dept\":\"人力资源部\",\"location\":\"北京\",\"exp\":\"10-20年\",\"salary\":\"80-120K·16薪\",\"category\":\"operation\",\"date\":\"2026-07-16\",\"tags\":[{\"text\":\"管理岗\",\"cls\":\"tech-tag\"},{\"text\":\"HR\",\"cls\":\"tech-tag\"}]}]','社会招聘职位列表（JSON数组）','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(50,'careers_campus_hero_badge','校园招聘','校园招聘 Hero 徽章文字','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(51,'careers_campus_hero_title','开启你的职业生涯','校园招聘 Hero 主标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(52,'careers_campus_hero_desc','面向应届毕业生及在校实习生，我们提供完善的培训体系与快速成长路径。\n在这里，你将与行业顶尖人才并肩工作，在真实项目中历练成长。','校园招聘 Hero 描述文字','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(53,'careers_campus_hero_cta','查看校招职位','校园招聘 Hero CTA 按钮文字','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(54,'careers_campus_hero_stats','[{\"number\":\"50+\",\"label\":\"校招职位\"},{\"number\":\"20+\",\"label\":\"目标院校\"},{\"number\":\"6 个月\",\"label\":\"培养计划\"}]','校园招聘 Hero 统计数据（JSON数组，每项含 number/label）','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(55,'careers_campus_process_title','招聘流程','校园招聘流程板块标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(56,'careers_campus_process_subtitle','清晰透明的流程，让你每一步都心中有数','校园招聘流程板块副标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(57,'careers_campus_process_steps','[{\"title\":\"网申投递\",\"description\":\"在线提交简历与作品集\"},{\"title\":\"简历筛选\",\"description\":\"HR 团队 3-5 个工作日内反馈\"},{\"title\":\"线上笔试\",\"description\":\"技术岗 90 分钟在线编程测试\"},{\"title\":\"技术面试\",\"description\":\"2-3 轮视频/线下面试\"},{\"title\":\"HR 面试\",\"description\":\"沟通价值观与发展意向\"},{\"title\":\"发放 Offer\",\"description\":\"1 周内确认并发出聘用通知\"}]','校园招聘流程步骤（JSON数组，每项含 title/description）','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(58,'careers_campus_title','校招职位','校招职位板块标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(59,'careers_campus_subtitle','面向 2026/2027 届毕业生及在校实习生','校招职位板块副标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(60,'careers_campus_empty_text','暂无匹配的校招职位，请尝试其他筛选条件','校招职位空状态文字','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(61,'careers_campus_filters','[\"全部职位\",\"技术研发\",\"产品 & 设计\",\"市场 & 营销\",\"数据 & AI\",\"运营 & 职能\",\"实习\"]','校园招聘筛选分类（JSON字符串数组）','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(62,'careers_campus_jobs','[{\"id\":201,\"title\":\"前端开发工程师（2026 届）\",\"dept\":\"技术研发部·前端团队\",\"location\":\"上海\",\"exp\":\"应届生\",\"salary\":\"20-35K·16薪\",\"category\":\"tech\",\"date\":\"2026-07-30\",\"tags\":[{\"text\":\"校招\",\"cls\":\"new\"},{\"text\":\"React\",\"cls\":\"tech-tag\"},{\"text\":\"Vue\",\"cls\":\"tech-tag\"}]},{\"id\":202,\"title\":\"后端开发工程师（2026 届）\",\"dept\":\"技术研发部·核心平台组\",\"location\":\"北京/杭州\",\"exp\":\"应届生\",\"salary\":\"22-38K·16薪\",\"category\":\"tech\",\"date\":\"2026-07-30\",\"tags\":[{\"text\":\"校招\",\"cls\":\"new\"},{\"text\":\"Java\",\"cls\":\"tech-tag\"},{\"text\":\"Go\",\"cls\":\"tech-tag\"}]},{\"id\":203,\"title\":\"算法工程师（2026 届）\",\"dept\":\"AI 研究院\",\"location\":\"北京\",\"exp\":\"应届生\",\"salary\":\"28-45K·16薪\",\"category\":\"data\",\"date\":\"2026-07-29\",\"tags\":[{\"text\":\"校招\",\"cls\":\"new\"},{\"text\":\"NLP\",\"cls\":\"tech-tag\"},{\"text\":\"硕士优先\",\"cls\":\"tech-tag\"}]},{\"id\":204,\"title\":\"产品经理（2026 届）\",\"dept\":\"产品部\",\"location\":\"北京/深圳\",\"exp\":\"应届生\",\"salary\":\"18-30K·16薪\",\"category\":\"product\",\"date\":\"2026-07-28\",\"tags\":[{\"text\":\"校招\",\"cls\":\"new\"},{\"text\":\"B端\",\"cls\":\"tech-tag\"}]},{\"id\":205,\"title\":\"后端开发实习生\",\"dept\":\"技术研发部\",\"location\":\"北京/上海/杭州\",\"exp\":\"在校生\",\"salary\":\"8-12K\",\"category\":\"tech\",\"date\":\"2026-07-27\",\"tags\":[{\"text\":\"实习\",\"cls\":\"new\"},{\"text\":\"可转正\",\"cls\":\"hot\"},{\"text\":\"Java\",\"cls\":\"tech-tag\"}]},{\"id\":206,\"title\":\"前端开发实习生\",\"dept\":\"技术研发部·前端团队\",\"location\":\"上海\",\"exp\":\"在校生\",\"salary\":\"8-12K\",\"category\":\"tech\",\"date\":\"2026-07-26\",\"tags\":[{\"text\":\"实习\",\"cls\":\"new\"},{\"text\":\"React\",\"cls\":\"tech-tag\"},{\"text\":\"可转正\",\"cls\":\"hot\"}]},{\"id\":207,\"title\":\"数据分析师（2026 届）\",\"dept\":\"数据与AI平台部\",\"location\":\"北京/杭州\",\"exp\":\"应届生\",\"salary\":\"18-30K·16薪\",\"category\":\"data\",\"date\":\"2026-07-25\",\"tags\":[{\"text\":\"校招\",\"cls\":\"new\"},{\"text\":\"SQL\",\"cls\":\"tech-tag\"},{\"text\":\"Python\",\"cls\":\"tech-tag\"}]},{\"id\":208,\"title\":\"人力资源管培生（2026 届）\",\"dept\":\"人力资源部\",\"location\":\"北京\",\"exp\":\"应届生\",\"salary\":\"15-25K·16薪\",\"category\":\"operation\",\"date\":\"2026-07-24\",\"tags\":[{\"text\":\"校招\",\"cls\":\"new\"},{\"text\":\"管培生\",\"cls\":\"tech-tag\"}]},{\"id\":209,\"title\":\"AI 算法实习生\",\"dept\":\"AI 研究院\",\"location\":\"北京\",\"exp\":\"在校生\",\"salary\":\"10-15K\",\"category\":\"data\",\"date\":\"2026-07-23\",\"tags\":[{\"text\":\"实习\",\"cls\":\"new\"},{\"text\":\"LLM\",\"cls\":\"tech-tag\"},{\"text\":\"硕博优先\",\"cls\":\"tech-tag\"}]},{\"id\":210,\"title\":\"UI/UX 设计师（2026 届）\",\"dept\":\"设计部·体验设计团队\",\"location\":\"北京/深圳\",\"exp\":\"应届生\",\"salary\":\"18-28K·16薪\",\"category\":\"product\",\"date\":\"2026-07-22\",\"tags\":[{\"text\":\"校招\",\"cls\":\"new\"},{\"text\":\"Figma\",\"cls\":\"tech-tag\"}]}]','校园招聘职位列表（JSON数组）','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(63,'careers_campus_talks_title','宣讲会行程','宣讲会板块标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(64,'careers_campus_talks_subtitle','期待在校园与你相遇','宣讲会板块副标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(65,'careers_campus_talks','[{\"month\":9,\"day\":15,\"school\":\"清华大学\",\"venue\":\"职业发展中心·天一厅\",\"timeSlot\":\"14:00-16:00\",\"status\":\"upcoming\"},{\"month\":9,\"day\":18,\"school\":\"北京大学\",\"venue\":\"英杰交流中心·月光厅\",\"timeSlot\":\"14:00-16:00\",\"status\":\"upcoming\"},{\"month\":9,\"day\":22,\"school\":\"浙江大学\",\"venue\":\"玉泉校区·永谦活动中心\",\"timeSlot\":\"18:30-20:30\",\"status\":\"upcoming\"},{\"month\":9,\"day\":25,\"school\":\"上海交通大学\",\"venue\":\"闵行校区·铁生馆\",\"timeSlot\":\"14:00-16:00\",\"status\":\"upcoming\"}]','校园宣讲会行程（JSON数组，每项含 month/day/school/venue/timeSlot/status）','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(66,'careers_hot_title','热招职位','热门职位板块标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(67,'careers_hot_subtitle','多个团队正在寻找优秀的你，投递简历开启你的 SmartRecruit 之旅。','热门职位板块副标题','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(68,'careers_hot_empty_text','暂无匹配的职位，请尝试其他筛选条件','热门职位空状态文字','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(69,'careers_hot_filters','[\"全部职位\",\"技术研发\",\"产品 & 设计\",\"市场 & 销售\",\"数据 & AI\",\"运营 & 职能\"]','热门职位筛选分类（JSON字符串数组）','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(70,'careers_hot_jobs','[{\"id\":1,\"title\":\"资深后端开发工程师（Java）\",\"dept\":\"技术研发部 · 核心平台组\",\"location\":\"北京\",\"exp\":\"5-10年\",\"salary\":\"40-70K·16薪\",\"category\":\"tech\",\"date\":\"2026-07-28\",\"tags\":[{\"text\":\"热招\",\"cls\":\"hot\"},{\"text\":\"Java\",\"cls\":\"tech-tag\"},{\"text\":\"Spring Boot\",\"cls\":\"tech-tag\"}]},{\"id\":2,\"title\":\"AI 算法工程师（NLP/CV）\",\"dept\":\"AI 研究院 · 算法团队\",\"location\":\"北京 / 上海\",\"exp\":\"3-8年\",\"salary\":\"50-90K·16薪\",\"category\":\"data\",\"date\":\"2026-07-26\",\"tags\":[{\"text\":\"热招\",\"cls\":\"hot\"},{\"text\":\"NLP\",\"cls\":\"tech-tag\"},{\"text\":\"LLM\",\"cls\":\"tech-tag\"}]},{\"id\":3,\"title\":\"高级前端开发工程师（React/Vue）\",\"dept\":\"技术研发部 · 前端团队\",\"location\":\"上海\",\"exp\":\"3-7年\",\"salary\":\"35-60K·16薪\",\"category\":\"tech\",\"date\":\"2026-07-25\",\"tags\":[{\"text\":\"React\",\"cls\":\"tech-tag\"},{\"text\":\"Vue\",\"cls\":\"tech-tag\"}]},{\"id\":4,\"title\":\"高级产品经理（SaaS 方向）\",\"dept\":\"产品部 · 招聘产品线\",\"location\":\"北京\",\"exp\":\"5-8年\",\"salary\":\"35-55K·16薪\",\"category\":\"product\",\"date\":\"2026-07-24\",\"tags\":[{\"text\":\"B端\",\"cls\":\"tech-tag\"},{\"text\":\"SaaS\",\"cls\":\"tech-tag\"}]},{\"id\":5,\"title\":\"资深 UI/UX 设计师\",\"dept\":\"设计部 · 体验设计团队\",\"location\":\"北京 / 深圳\",\"exp\":\"3-6年\",\"salary\":\"30-50K·16薪\",\"category\":\"product\",\"date\":\"2026-07-22\",\"tags\":[{\"text\":\"B端设计\",\"cls\":\"tech-tag\"}]},{\"id\":6,\"title\":\"大客户销售经理（HR Tech）\",\"dept\":\"销售部 · 大客户团队\",\"location\":\"北京 / 上海 / 深圳\",\"exp\":\"5-10年\",\"salary\":\"25-45K·16薪\",\"category\":\"market\",\"date\":\"2026-07-20\",\"tags\":[{\"text\":\"热招\",\"cls\":\"hot\"}]},{\"id\":7,\"title\":\"数据平台开发工程师\",\"dept\":\"数据与 AI 平台部\",\"location\":\"杭州\",\"exp\":\"3-7年\",\"salary\":\"35-60K·16薪\",\"category\":\"data\",\"date\":\"2026-07-18\",\"tags\":[{\"text\":\"大数据\",\"cls\":\"tech-tag\"},{\"text\":\"Flink\",\"cls\":\"tech-tag\"}]},{\"id\":8,\"title\":\"安全合规工程师\",\"dept\":\"基础架构部 · 安全团队\",\"location\":\"北京\",\"exp\":\"5-8年\",\"salary\":\"40-65K·16薪\",\"category\":\"tech\",\"date\":\"2026-07-16\",\"tags\":[{\"text\":\"安全\",\"cls\":\"tech-tag\"},{\"text\":\"合规\",\"cls\":\"tech-tag\"}]},{\"id\":9,\"title\":\"校园招聘 HR\",\"dept\":\"人力资源部 · 招聘团队\",\"location\":\"北京\",\"exp\":\"2-5年\",\"salary\":\"18-30K·16薪\",\"category\":\"operation\",\"date\":\"2026-07-15\",\"tags\":[{\"text\":\"校招\",\"cls\":\"new\"},{\"text\":\"雇主品牌\",\"cls\":\"tech-tag\"}]}]','热门职位列表（JSON数组）','2026-08-02 20:47:08','2026-08-02 20:47:08','system',NULL,NULL,NULL),(2086085202124206081,'watermark_enabled','1','是否开启页面水印（显示当前账号、姓名与时间，防截图泄露）：0=否, 1=是','2026-08-08 21:40:44','2026-08-08 21:40:44','susan@163.com',NULL,'susan@163.com',2075564483359485954),(2086387259922063362,'company_name','SmartRecruit 科技有限公司','公司名称（合同甲方名称）','2026-08-09 17:41:01','2026-08-09 17:41:01','susan@163.com',NULL,'susan@163.com',NULL),(2086387259984977921,'company_credit_code','91310000MA1FL4C9X7','统一社会信用代码（合同甲方信息）','2026-08-09 17:41:01','2026-08-11 10:56:19','susan@163.com',NULL,'susan@163.com',NULL),(2086387260031115265,'company_address','四川省成都市高新区128号','公司住所（合同甲方信息）','2026-08-09 17:41:01','2026-08-11 10:56:19','susan@163.com',NULL,'susan@163.com',NULL),(2086387260052086785,'company_legal_representative','无名','法定代表人（合同甲方信息）','2026-08-09 17:41:01','2026-08-11 10:56:19','susan@163.com',NULL,'susan@163.com',NULL),(2086387260052086786,'careers_stats_items','[{\"number\":1200,\"suffix\":\"\",\"label\":\"员工\"},{\"number\":18,\"suffix\":\"\",\"label\":\"城市\"},{\"number\":500,\"suffix\":\"万+\",\"label\":\"企业客户\"},{\"number\":96,\"suffix\":\"%\",\"label\":\"推荐率\"}]','首页统计数据（JSON数组，每项含 number/suffix/label）','2026-08-11 10:31:31','2026-08-11 10:31:31','system',NULL,NULL,NULL),(2086387260052086787,'careers_cta_title','准备好加入我们了吗？','CTA 标题','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086788,'careers_cta_desc','和一群优秀的人，做一些有意义的事。你的下一段职业生涯，从这里开始。','CTA 描述','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086789,'careers_cta_btn_label','查看热招职位','CTA 按钮文字','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086790,'careers_cta_btn_link','/jobs','CTA 按钮链接','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086791,'careers_footer_desc','AI 驱动的智能招聘平台，致力于用技术让招聘更精准、更高效、更人性化。服务超过 500 万家企业客户，累计处理 50 亿+ 简历。','页脚品牌描述','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086792,'careers_footer_copyright','© 2026 SmartRecruit. All rights reserved.','页脚版权信息','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086793,'careers_footer_contact_email','hr@smartrecruit.com','页脚联系邮箱','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086794,'careers_footer_contact_cities','[{\"label\":\"北京 · 海淀区\",\"icon\":\"location\"},{\"label\":\"上海 · 浦东新区\",\"icon\":\"location\"},{\"label\":\"深圳 · 南山区\",\"icon\":\"location\"},{\"label\":\"杭州 · 余杭区\",\"icon\":\"location\"}]','页脚办公城市（JSON数组，每项含 label/icon）','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086795,'careers_footer_category_title','职位类别','页脚分类栏目标题','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086796,'careers_footer_category_links','[{\"label\":\"技术研发\",\"href\":\"/jobs?cat=tech\"},{\"label\":\"产品与设计\",\"href\":\"/jobs?cat=product\"},{\"label\":\"数据与 AI\",\"href\":\"/jobs?cat=data\"},{\"label\":\"市场与销售\",\"href\":\"/jobs?cat=market\"},{\"label\":\"运营与职能\",\"href\":\"/jobs?cat=ops\"}]','页脚分类链接（JSON数组，每项含 label/href）','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086797,'careers_footer_about_title','关于我们','页脚关于我们标题','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086798,'careers_footer_about_links','[{\"label\":\"企业文化\",\"href\":\"#\"},{\"label\":\"薪酬福利\",\"href\":\"#\"},{\"label\":\"工作生活\",\"href\":\"#\"},{\"label\":\"常见问题\",\"href\":\"#\"},{\"label\":\"联系我们\",\"href\":\"#\"}]','页脚关于我们链接（JSON数组，每项含 label/href）','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086799,'careers_nav_logo_text','SmartRecruit','导航栏 Logo 文字','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086800,'careers_nav_cta_label','投递简历','导航栏 CTA 按钮文字','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL),(2086387260052086801,'careers_nav_links','[{\"type\":\"hash\",\"key\":\"culture\",\"label\":\"企业文化\",\"hash\":\"culture\"},{\"type\":\"route\",\"key\":\"social\",\"label\":\"社会招聘\",\"route\":\"/social-recruitment\"},{\"type\":\"route\",\"key\":\"campus\",\"label\":\"校园招聘\",\"route\":\"/campus-recruitment\"},{\"type\":\"hash\",\"key\":\"benefits\",\"label\":\"薪酬福利\",\"hash\":\"benefits\"},{\"type\":\"hash\",\"key\":\"life\",\"label\":\"工作生活\",\"hash\":\"life\"},{\"type\":\"hash\",\"key\":\"faq\",\"label\":\"常见问题\",\"hash\":\"faq\"}]','导航栏链接（JSON数组，每项含 type/key/label/hash?/route?）','2026-08-11 10:47:40','2026-08-11 10:47:40','system',NULL,NULL,NULL);



USE `smart_recruit_interview`;


INSERT INTO `rec_question_bank` (`id`, `bank_name`, `department_id`, `department_name`, `job_position_id`, `job_title`, `question_type`, `difficulty`, `description`, `question_count`, `status`, `create_time`, `update_time`, `create_by`, `create_user_id`, `update_by`, `update_user_id`, `deleted`) VALUES (950001,'Java后端-技术基础面',100002,'技术研发部',500001,'高级Java开发工程师（微服务方向）',0,2,'考察 Java 并发、JVM、数据库与缓存的硬核基础。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(950002,'Java后端-微服务架构面',100002,'技术研发部',500001,'高级Java开发工程师（微服务方向）',1,3,'考察微服务拆分、分布式事务、服务治理的实战深度。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(950003,'高级前端-技术面',100002,'技术研发部',500002,'高级前端开发工程师',0,2,'考察 JavaScript 原理、框架、性能与工程化。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(950004,'AI算法-技术面',100002,'技术研发部',500005,'AI算法工程师',0,3,'考察机器学习、评估指标与深度学习基础。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(950005,'DevOps-技术面',100002,'技术研发部',500003,'DevOps工程师',0,2,'考察 CI/CD、Kubernetes、监控与 IaC 实践。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(950006,'数据工程师-技术面',100002,'技术研发部',500012,'数据工程师',0,2,'考察数仓建模、实时计算与数据质量。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(950007,'高级产品经理-专业面',100003,'产品部',500006,'高级产品经理（招聘SaaS方向）',0,2,'考察需求分析、优先级、数据分析与 PRD 能力。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(950008,'高级产品经理-行为面',100003,'产品部',500006,'高级产品经理（招聘SaaS方向）',2,1,'考察跨部门沟通、冲突解决与用户洞察。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(950009,'HRBP-专业面',100004,'人力资源部',500008,'HRBP（技术团队方向）',0,2,'考察 HRBP 业务伙伴定位、人才盘点与组织诊断。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(950010,'市场运营-专业面',100006,'市场部',500010,'市场运营经理',0,2,'考察 B 端获客、内容营销与 ROI 意识。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(950011,'财务分析-专业面',100005,'财务部',500015,'财务分析经理',0,2,'考察财务指标、预算管理与经营分析。',4,1,'2026-08-10 20:35:41','2026-08-10 20:35:41','hr_li',NULL,'hr_li',NULL,0),(2086814704496267266,'高级Java开发工程师（微服务方向）-AI生成题1',100002,'技术研发部',NULL,'高级Java开发工程师（微服务方向）',3,2,'由 AI 智能出题生成，加入面试题库沉淀复用。',9,1,'2026-08-10 21:59:31','2026-08-10 21:59:31','susan',NULL,'susan',NULL,0),(2087442575967256577,'Java架构师-AI生成题',100002,'技术研发部',NULL,'Java架构师',3,2,'由 AI 智能出题生成，加入面试题库沉淀复用。',9,1,'2026-08-12 15:34:28','2026-08-12 15:34:28','susan',2075564483359485954,'susan',2075564483359485954,0),(2087452651646062593,'测试工程师',100002,'技术研发部',NULL,'测试开发工程师（自动化测试方向）',0,2,'',7,1,'2026-08-12 16:14:30','2026-08-12 16:14:30','susan',2075564483359485954,'susan',2075564483359485954,0);




INSERT INTO `rec_question_bank_item` (`id`, `bank_id`, `question_type`, `question`, `options`, `answer`, `explanation`, `difficulty`, `sort_order`, `create_time`, `update_time`, `create_user_id`, `create_by`, `update_user_id`, `update_by`, `deleted`) VALUES (950101,950001,0,'在 Java 并发编程中，关于 volatile 关键字的说法，正确的是？','[\"A. volatile 能保证操作的原子性\", \"B. volatile 保证可见性和有序性，但不保证原子性\", \"C. volatile 可以完全替代 synchronized 保证线程安全\", \"D. volatile 变量无需任何同步即可安全自增\"]','B','volatile 保证变量修改对其他线程可见，并禁止指令重排（有序性），但 i++ 这类复合操作不具备原子性，无法替代 synchronized。',2,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950102,950001,0,'JVM 运行时数据区中，哪个区域不会发生 OutOfMemoryError？','[\"A. 堆\", \"B. 方法区/元空间\", \"C. 程序计数器\", \"D. 虚拟机栈\"]','C','程序计数器是唯一不会发生 OOM 的区域，它是线程私有的字节码行号指示器，空间占用极小。',1,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950103,950001,2,'请解释 MySQL InnoDB 索引为什么使用 B+ 树而不是 B 树或哈希索引。',NULL,'参考要点：B+树非叶子节点不存储数据、单节点可容纳更多键、磁盘 IO 次数少；叶子节点通过链表有序连接，天然支持范围查询；哈希索引只支持等值查询且无法排序。','考察对数据库原理的理解，重点看候选人能否从磁盘 IO、范围查询、聚簇索引三个角度展开说明。',3,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950104,950001,1,'下列哪些属于 Redis 的持久化方式？','[\"A. RDB 快照\", \"B. AOF 追加日志\", \"C. 混合持久化\", \"D. WAL 预写日志\"]','A,B,C','Redis 支持 RDB 快照、AOF 追加日志以及 4.0 之后的混合持久化；WAL 是 MySQL/PostgreSQL 等数据库的机制。',1,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950105,950002,2,'请描述一次你主导的微服务拆分过程，包括拆分依据与通信选型。',NULL,'参考要点：按业务域/限界上下文拆分（DDD）；通信选型说明 REST 与 gRPC 的取舍；注册中心（Nacos/Eureka）与配置中心选型；异步场景使用 MQ。','重点考察拆分边界的判断力、技术选型理由以及真实落地经验，避免只背概念。',3,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950106,950002,0,'分布式事务中，Saga 模式的核心思想是？','[\"A. 两阶段提交（2PC）\", \"B. 将长事务拆分为多个本地事务并支持补偿\", \"C. 依赖数据库锁保证强一致\", \"D. 只读事务\"]','B','Saga 将长事务拆成多个本地事务，失败时执行反向补偿操作，保证最终一致性，适用于跨服务长事务；2PC 是同步强一致方案。',3,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950107,950002,0,'服务熔断（Circuit Breaker）的主要目的是？','[\"A. 提升系统并发量\", \"B. 防止故障扩散（雪崩效应）\", \"C. 加快请求响应速度\", \"D. 简化服务配置\"]','B','熔断在依赖故障时快速失败并降级，保护自身与下游服务，防止雪崩效应。',2,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950108,950002,1,'以下哪些属于服务治理（服务网格）关注的能力？','[\"A. 服务发现\", \"B. 限流熔断\", \"C. 负载均衡\", \"D. 链路追踪\"]','A,B,C,D','服务治理涵盖注册发现、负载均衡、限流熔断、链路追踪、灰度发布等能力。',2,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950109,950003,0,'关于 JavaScript 事件循环（Event Loop）的描述，正确的是？','[\"A. 宏任务优先于微任务执行\", \"B. 每轮先执行一个宏任务，再清空微任务队列\", \"C. Promise.then 属于宏任务\", \"D. setTimeout 属于微任务\"]','B','每轮事件循环先执行一个宏任务，然后清空微任务队列；Promise.then 是微任务，setTimeout 是宏任务。',2,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950110,950003,2,'Vue 3 的响应式原理是什么？与 Vue 2 有何区别？',NULL,'参考要点：Vue3 基于 Proxy 代理实现响应式，可拦截新增/删除属性；Vue2 基于 Object.defineProperty，新增属性需 Vue.set 处理；Vue3 性能更好、支持更多数据结构，并配合依赖收集与 effect 调度。','考察框架原理深度，能说清 Proxy 与 defineProperty 的差异即可，进阶可谈依赖收集与更新调度。',2,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950111,950003,0,'以下哪个指标最能反映页面首屏渲染体验？','[\"A. FCP（首次内容绘制）\", \"B. TTI（可交互时间）\", \"C. LCP（最大内容绘制）\", \"D. CLS（布局偏移）\"]','C','LCP 衡量最大内容元素的渲染时间，是 Core Web Vitals 的核心指标；FCP 偏早、TTI 偏交互、CLS 衡量布局稳定性。',2,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950112,950003,1,'下列哪些属于前端工程化实践？','[\"A. 组件库与设计系统建设\", \"B. CI/CD 发布流水线\", \"C. 代码规范与 ESLint\", \"D. 单元测试与自动化测试\"]','A,B,C,D','前端工程化涵盖组件化、构建、质量（Lint/测试）、发布（CI/CD）等全链路。',1,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950113,950004,0,'模型过拟合的典型表现是？','[\"A. 训练集与测试集准确率都很高\", \"B. 训练集准确率高、测试集准确率低\", \"C. 训练集准确率低\", \"D. 模型无法收敛\"]','B','过拟合即模型记住了训练集噪声、泛化能力差，典型表现是训练集表现好而测试集表现差；应对方法包括正则化、数据增强、Dropout、早停与交叉验证。',1,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950114,950004,2,'请说明 AUC 的含义，以及它相比准确率的优势。',NULL,'参考要点：AUC 是 ROC 曲线下面积，衡量模型对正负样本的排序能力；对类别不平衡不敏感，而准确率在正负样本严重不均衡时会产生误导。','考察评估指标的理解，能结合具体业务（如异常检测、CTR 预估）说明更佳。',2,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950115,950004,0,'Transformer 中 Self-Attention 的主要作用是？','[\"A. 提取局部窗口特征\", \"B. 建模序列中任意位置间的依赖关系\", \"C. 加速训练收敛\", \"D. 降低模型参数量\"]','B','Self-Attention 通过 Query/Key/Value 计算序列任意位置间的全局依赖，解决了 RNN 长距离依赖与难以并行的问题。',2,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950116,950004,1,'推荐系统中常见的召回策略包括？','[\"A. 协同过滤召回\", \"B. 向量召回（双塔模型）\", \"C. 热度/规则召回\", \"D. 精排打分模型\"]','A,B,C','召回阶段生成候选集（协同过滤、向量召回、热度/规则），精排是召回之后对候选做精细化打分，不属于召回策略。',2,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950117,950005,0,'以下哪个不是 Kubernetes 的核心组件？','[\"A. kube-apiserver\", \"B. kubelet\", \"C. Docker Swarm\", \"D. etcd\"]','C','K8s 核心组件包括 API Server、Scheduler、Controller Manager、kubelet 与 etcd；Docker Swarm 是另一个容器编排方案。',1,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950118,950005,2,'如何设计一套 CI/CD 流水线？请以你熟悉的工具为例说明。',NULL,'参考要点：代码提交 → 静态检查/单测 → 构建镜像 → 部署测试环境 → 自动化测试 → 灰度发布到生产；可使用 Jenkins、GitLab CI、ArgoCD 等工具，重点关注质量门禁与发布安全。','考察流水线设计能力与工具实践，重点关注质量门禁、回滚机制与发布安全。',2,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950119,950005,0,'Prometheus 中用于查询监控指标的语言是？','[\"A. SQL\", \"B. PromQL\", \"C. LogQL\", \"D. GraphQL\"]','B','PromQL 是 Prometheus 的查询语言；LogQL 是 Loki 的日志查询语言。',1,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950120,950005,1,'基础设施即代码（IaC）的常用工具包括？','[\"A. Terraform\", \"B. Ansible\", \"C. AWS CloudFormation\", \"D. Docker\"]','A,B,C','Terraform、Ansible、CloudFormation 均属 IaC 工具；Docker 是容器运行时，不属于 IaC。',2,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950121,950006,0,'数据仓库建模中，Kimball 维度建模的核心是？','[\"A. 第三范式（3NF）\", \"B. 事实表 + 维度表组成的星型模型\", \"C. 全量宽表\", \"D. 雪花模型\"]','B','Kimball 倡导维度建模，以事实表 + 维度表构成星型模型，查询友好、易于理解。',2,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950122,950006,2,'请说明实时数仓与离线数仓的差异及适用场景。',NULL,'参考要点：离线数仓以 Hive/Spark 为主、T+1 调度，适合深度分析与报表；实时数仓以 Flink/Kafka 为主、毫秒级延迟，适合实时监控、风控与实时大屏。','考察对流批一体思路的理解，能说清链路选型与适用场景即可。',2,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950123,950006,0,'Flink 实现端到端精确一次（Exactly-Once）语义依赖的关键机制是？','[\"A. 分布式快照（Checkpoint）\", \"B. 客户端重试\", \"C. 消息简单去重\", \"D. 单线程顺序消费\"]','A','Flink 通过 Chandy-Lamport 分布式快照配合两阶段提交实现端到端精确一次语义。',3,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950124,950006,1,'常见的数据质量维度包括哪些？','[\"A. 完整性\", \"B. 一致性\", \"C. 及时性\", \"D. 准确性\"]','A,B,C,D','数据质量六维度通常包括完整性、唯一性、一致性、准确性、及时性与有效性。',1,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950125,950007,0,'需求优先级排序时，以下哪种方法最常用且更客观？','[\"A. RICE 评分法\", \"B. 老板拍板\", \"C. 先来后到\", \"D. 按心情排序\"]','A','RICE（Reach-Impact-Confidence-Effort）用影响面、影响度、置信度与成本量化打分排序，比主观拍板更客观。',1,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950126,950007,2,'如何评估一个功能上线后的效果？请给出你的分析框架。',NULL,'参考要点：先明确核心指标（转化率、留存、使用时长）；设计 AB 实验与对照组；跟踪北极星指标与漏斗转化；结合用户反馈持续迭代。','考察数据驱动能力，重点看指标体系设计、AB 实验思路与复盘闭环。',2,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950127,950007,0,'B 端产品与 C 端产品最核心的差异是？','[\"A. 界面是否美观\", \"B. 决策链长、强调业务价值与 ROI\", \"C. 用户规模大小\", \"D. 开发周期长短\"]','B','B 端产品使用者与决策者分离、决策链长，价值主张围绕降本增效、效率与合规，ROI 导向更明显。',1,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950128,950007,1,'一份高质量的 PRD 应包含哪些要素？','[\"A. 背景与业务目标\", \"B. 用户场景与流程图\", \"C. 功能需求、边界与异常流\", \"D. 数据埋点与验收标准\"]','A,B,C,D','高质量 PRD 需覆盖背景目标、用户故事、功能细节、异常流、数据埋点与验收标准，保证研发可执行。',2,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950129,950008,2,'请分享一次你与研发团队就需求实现产生分歧的经历，你是如何处理的？',NULL,'参考要点：先理解对方的技术顾虑与成本，用数据与用户价值对齐目标，必要时给出折中方案并明确验收标准（建议用 STAR 法则回答）。','考察跨部门沟通与冲突解决能力，重点看是否以目标为导向而非情绪化。',1,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950130,950008,2,'你通常如何获取用户需求？请举例说明。',NULL,'参考要点：用户访谈、问卷、数据分析、客服反馈与竞品分析结合；访谈要追问\"为什么\"，数据要结合场景解读，避免只听片面声音。','考察用户研究基本功，能讲出具体方法与落地案例更佳。',2,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950131,950008,0,'项目上线前发现严重缺陷，你的最优做法是？','[\"A. 强行上线后加班修复\", \"B. 评估影响面，必要时延期并同步相关方\", \"C. 隐瞒问题按期上线\", \"D. 把责任推给测试团队\"]','B','发布决策要评估风险与收益，及时透明沟通，必要时延期保障质量，避免把缺陷带到生产。',1,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950132,950008,1,'以下哪些属于优秀产品经理的软技能？','[\"A. 同理心\", \"B. 沟通协调\", \"C. 抗压能力\", \"D. 逻辑思辨\"]','A,B,C,D','产品经理既要硬技能（数据分析、PRD），也要同理心、推动力、抗压与逻辑思辨等软技能。',1,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950133,950009,0,'HRBP 最核心的价值定位是？','[\"A. 处理考勤与入离职手续\", \"B. 作为业务伙伴，支撑业务目标的人才与组织战略\", \"C. 管理员工档案\", \"D. 核算薪酬\"]','B','HRBP 深入业务，围绕组织与人才支撑业务战略落地，是业务与 HR 体系之间的桥梁。',1,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950134,950009,2,'技术团队核心员工离职率上升，你会如何分析并干预？',NULL,'参考要点：先做离职访谈与数据归因（薪酬、发展空间、管理方式等），再针对性改善（晋升通道、激励、管理者辅导），并持续跟踪效果。','考察问题诊断与干预闭环能力，体现数据思维与组织敏感度。',2,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950135,950009,0,'判断候选人是否适合团队文化，面试中最应关注的是？','[\"A. 学历背景\", \"B. 价值观匹配与协作方式\", \"C. 薪资期望高低\", \"D. 表达口才\"]','B','文化匹配关注价值观、协作风格与动机，可结合行为面试法（STAR）评估。',2,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950136,950009,1,'HRBP 通常参与哪些工作模块？','[\"A. 组织诊断\", \"B. 人才盘点\", \"C. 绩效管理\", \"D. 员工关系\"]','A,B,C,D','HRBP 覆盖组织发展、人才梯队、绩效、员工关系等模块，是全模块的业务伙伴角色。',1,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950137,950010,0,'B 端市场获客最关注的核心指标是？','[\"A. 曝光量\", \"B. 有效线索（MQL/SQL）转化与获客成本\", \"C. 粉丝数\", \"D. 转发量\"]','B','B 端营销围绕线索生命周期，关注线索质量、转化率与获客成本（CAC）。',1,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950138,950010,2,'如何为 SaaS 产品设计一场低成本的内容营销活动？',NULL,'参考要点：锚定目标用户痛点，产出白皮书/案例/直播等内容，通过 SEO 与社群分发，设置留资 CTA 并跟踪转化闭环。','考察营销策略与资源利用能力，强调目标人群、渠道与转化闭环。',2,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950139,950010,0,'衡量内容营销 ROI 时，最终应看哪个指标？','[\"A. 阅读量\", \"B. 带来的商机与付费收入\", \"C. 点赞数\", \"D. 播放量\"]','B','内容营销的 ROI 最终落到商机与收入，前链路指标（阅读/点赞/播放）只是过程指标。',2,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950140,950010,1,'常见的 B 端获客渠道包括哪些？','[\"A. SEO/SEM\", \"B. 行业展会\", \"C. 内容营销\", \"D. 合作伙伴渠道\"]','A,B,C,D','B 端获客是组合拳，搜索、展会、内容与渠道合作多渠道协同。',1,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950141,950011,0,'财务分析中，毛利率的计算公式是？','[\"A. (营业收入-营业成本)/营业收入\", \"B. 净利润/营业收入\", \"C. 营业收入-营业成本\", \"D. 营业成本/营业收入\"]','A','毛利率 = (营业收入 - 营业成本) / 营业收入，衡量产品或业务的基础盈利能力。',1,1,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950142,950011,2,'如果公司净利润增长但经营现金流为负，你如何解读并给出建议？',NULL,'参考要点：可能由应收账款增加、存货积压或一次性大额支出导致，需结合现金流结构分析，提示回款与资金链风险，并建议加强应收管理与库存周转。','考察利润与现金流的区分及风险意识，能落到应收、存货等科目更佳。',3,2,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950143,950011,0,'预算管理中，滚动预算的主要优点是？','[\"A. 编制简单快捷\", \"B. 持续滚动更新，更贴近实际经营\", \"C. 完全不需要调整\", \"D. 只覆盖自然年度\"]','B','滚动预算定期顺延更新，能及时反映经营变化，更贴近实际，但编制工作量相对较大。',2,3,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(950144,950011,1,'常用的经营分析指标体系包含哪些？','[\"A. 收入与毛利\", \"B. 费用率\", \"C. 现金流\", \"D. 人效（人均产出）\"]','A,B,C,D','经营分析涵盖收入、成本费用、现金流与人效等维度，支撑管理决策。',1,4,'2026-08-10 20:35:41','2026-08-10 20:35:41',NULL,NULL,NULL,NULL,0),(2086814704563376129,2086814704496267266,0,'技术基础题：在 Spring Cloud 微服务架构中，以下哪种组件主要用于实现服务熔断与降级？','[\"A. Nacos\", \"B. Sentinel\", \"C. OpenFeign\", \"D. Ribbon\"]','B','B，Sentinel 是阿里开源的流量治理组件，支持熔断、降级、限流等功能；Nacos 主要用于服务注册与配置中心，OpenFeign 和 Ribbon 属于服务调用与负载均衡。',1,0,'2026-08-10 21:59:31','2026-08-10 21:59:31',NULL,NULL,NULL,NULL,0),(2086814704873754625,2086814704496267266,0,'技术基础题：关于 JVM 的 G1 垃圾收集器，以下说法正确的是？','[\"A. G1 使用标记-清除算法，会产生大量内存碎片\", \"B. G1 将堆划分为多个大小固定的 Region，可预测停顿时间\", \"C. G1 不支持并发标记阶段\", \"D. G1 只适用于小内存（<4GB）应用\"]','B','B，G1 将堆划分为多个 Region，通过可预测的停顿时间模型进行垃圾回收，适合大内存、低延迟场景；其采用标记-整理算法避免碎片，且支持并发标记。',2,1,'2026-08-10 21:59:31','2026-08-10 21:59:31',NULL,NULL,NULL,NULL,0),(2086814704886337537,2086814704496267266,0,'技术基础题：在使用 Redis 实现分布式锁时，以下哪种做法能有效避免锁无法释放的问题？','[\"A. 使用 SETNX 命令加锁，不设置过期时间\", \"B. 先 SETNX 加锁，再 EXPIRE 设置过期时间\", \"C. 使用 SET key value NX PX 命令原子性地加锁并设置过期时间\", \"D. 由客户端在业务逻辑结束后手动删除锁，无需过期机制\"]','C','C，SET key value NX PX 是原子操作，可同时完成加锁和设置过期时间，防止因进程崩溃导致锁无法释放；B 选项非原子，存在加锁成功但未设过期的风险。',2,2,'2026-08-10 21:59:31','2026-08-10 21:59:31',NULL,NULL,NULL,NULL,0),(2086814704907309058,2086814704496267266,2,'项目经验题：请描述你在项目中如何设计一个高并发的秒杀系统，重点说明你如何解决超卖、接口防刷和数据库压力问题。','[]','关键得分点：1. 使用 Redis 预减库存 + Lua 脚本保证原子性；2. 通过令牌桶或限流注解（如 Sentinel）控制请求速率；3. 异步下单（消息队列削峰）；4. 库存与订单最终一致性保障；5. 热点 Key 分片或本地缓存优化。','关键得分点：1. 使用 Redis 预减库存 + Lua 脚本保证原子性；2. 通过令牌桶或限流注解（如 Sentinel）控制请求速率；3. 异步下单（消息队列削峰）；4. 库存与订单最终一致性保障；5. 热点 Key 分片或本地缓存优化。',2,3,'2026-08-10 21:59:31','2026-08-10 21:59:31',NULL,NULL,NULL,NULL,0),(2086814704915697666,2086814704496267266,2,'项目经验题：你在项目中是否遇到过微服务调用链路超时或雪崩的情况？你是如何定位问题并实施治理的？','[]','关键得分点：1. 使用 SkyWalking/Prometheus 定位慢接口或异常服务；2. 配置合理的超时时间和重试策略；3. 引入熔断（如 Sentinel 熔断规则）隔离故障；4. 对核心链路做降级预案；5. 压测验证治理效果。','关键得分点：1. 使用 SkyWalking/Prometheus 定位慢接口或异常服务；2. 配置合理的超时时间和重试策略；3. 引入熔断（如 Sentinel 熔断规则）隔离故障；4. 对核心链路做降级预案；5. 压测验证治理效果。',1,4,'2026-08-10 21:59:31','2026-08-10 21:59:31',NULL,NULL,NULL,NULL,0),(2086814704945057794,2086814704496267266,2,'项目经验题：请举例说明你在项目中如何优化一个慢 SQL 查询，包括分析过程和最终方案。','[]','关键得分点：1. 通过慢查询日志或 explain 分析执行计划；2. 检查是否命中索引、是否存在回表或文件排序；3. 添加联合索引或覆盖索引；4. 避免 select *、函数操作字段；5. 必要时分库分表或读写分离。','关键得分点：1. 通过慢查询日志或 explain 分析执行计划；2. 检查是否命中索引、是否存在回表或文件排序；3. 添加联合索引或覆盖索引；4. 避免 select *、函数操作字段；5. 必要时分库分表或读写分离。',2,5,'2026-08-10 21:59:31','2026-08-10 21:59:31',NULL,NULL,NULL,NULL,0),(2086814704961835009,2086814704496267266,2,'行为面试题：当你和团队成员在技术方案上出现分歧（例如选型 Kafka 还是 RocketMQ），你会如何处理？','[]','参考答案要点：1. 基于业务场景（吞吐量、延迟、事务支持等）客观对比；2. 组织技术评审会，听取各方意见；3. 必要时做 PoC 验证；4. 尊重团队决策，聚焦目标而非个人偏好。','参考答案要点：1. 基于业务场景（吞吐量、延迟、事务支持等）客观对比；2. 组织技术评审会，听取各方意见；3. 必要时做 PoC 验证；4. 尊重团队决策，聚焦目标而非个人偏好。',1,6,'2026-08-10 21:59:31','2026-08-10 21:59:31',NULL,NULL,NULL,NULL,0),(2086814704982806530,2086814704496267266,2,'行为面试题：请分享一次你在紧急线上故障中快速响应并解决问题的经历。','[]','参考答案要点：1. 快速定位（日志、监控、链路追踪）；2. 临时止损（回滚、降级、扩容）；3. 根本原因分析；4. 事后复盘与预防措施（如增加告警、完善测试）。','参考答案要点：1. 快速定位（日志、监控、链路追踪）；2. 临时止损（回滚、降级、扩容）；3. 根本原因分析；4. 事后复盘与预防措施（如增加告警、完善测试）。',2,7,'2026-08-10 21:59:31','2026-08-10 21:59:31',NULL,NULL,NULL,NULL,0),(2086814704999583746,2086814704496267266,2,'行为面试题：当你接手一个遗留系统，文档缺失且代码质量较差，你会如何开展后续开发或重构工作？','[]','参考答案要点：1. 先通过日志和监控理解核心链路；2. 编写单元测试或集成测试覆盖关键路径；3. 小步重构，优先解耦高风险模块；4. 补充文档和注释；5. 与原团队或业务方沟通确认逻辑。','参考答案要点：1. 先通过日志和监控理解核心链路；2. 编写单元测试或集成测试覆盖关键路径；3. 小步重构，优先解耦高风险模块；4. 补充文档和注释；5. 与原团队或业务方沟通确认逻辑。',2,8,'2026-08-10 21:59:31','2026-08-10 21:59:31',NULL,NULL,NULL,NULL,0),(2087442576025976833,2087442575967256577,0,'技术基础题：在 Spring Cloud 微服务架构中，以下哪种组件主要用于实现服务熔断与降级？','[\"A. Nacos\", \"B. Sentinel\", \"C. OpenFeign\", \"D. Ribbon\"]','B','B，Sentinel 是阿里巴巴开源的流量控制组件，支持熔断降级、系统负载保护等功能；Nacos 主要用于服务注册与配置中心，OpenFeign 和 Ribbon 属于服务调用与负载均衡组件。',1,0,'2026-08-12 15:34:28','2026-08-12 15:34:28',NULL,NULL,NULL,NULL,0),(2087442576088891394,2087442575967256577,0,'技术基础题：关于 JVM 的 G1 垃圾收集器，以下说法正确的是？','[\"A. G1 使用标记-清除算法，会产生大量内存碎片\", \"B. G1 将堆划分为多个大小固定的 Region，可预测停顿时间\", \"C. G1 不支持并发标记阶段\", \"D. G1 只适用于小内存（<4GB）应用\"]','B','B，G1 将堆划分为多个 Region，通过可预测的停顿时间模型（Pause Prediction Model）实现低延迟 GC；它使用标记-整理算法避免碎片，支持大内存场景，并具备并发标记能力。',2,1,'2026-08-12 15:34:28','2026-08-12 15:34:28',NULL,NULL,NULL,NULL,0),(2087442576151805953,2087442575967256577,0,'技术基础题：在使用 Redis 实现分布式锁时，以下哪种做法能有效避免锁无法释放的问题？','[\"A. 设置锁时不加过期时间，由业务逻辑手动释放\", \"B. 使用 SETNX 命令加锁后立即设置 EXPIRE\", \"C. 使用 SET key value NX PX 命令原子地设置锁和过期时间\", \"D. 仅依赖客户端超时机制自动放弃锁\"]','C','C，SET key value NX PX 是原子操作，可同时设置锁和过期时间，防止因进程崩溃导致锁无法释放；B 选项中 SETNX 与 EXPIRE 非原子，存在风险。',2,2,'2026-08-12 15:34:28','2026-08-12 15:34:28',NULL,NULL,NULL,NULL,0),(2087442576181166081,2087442575967256577,2,'项目经验题：请描述你在项目中如何设计一个高并发下单系统，重点说明库存扣减和防止超卖的方案。',NULL,'关键得分点：1. 使用 Redis 预减库存 + Lua 脚本保证原子性；2. 异步落库（消息队列）解耦；3. 数据库最终一致性校验；4. 限流熔断保护后端；5. 分布式锁或版本号控制兜底。','关键得分点：1. 使用 Redis 预减库存 + Lua 脚本保证原子性；2. 异步落库（消息队列）解耦；3. 数据库最终一致性校验；4. 限流熔断保护后端；5. 分布式锁或版本号控制兜底。',2,3,'2026-08-12 15:34:28','2026-08-12 15:34:28',NULL,NULL,NULL,NULL,0),(2087442576197943297,2087442575967256577,2,'项目经验题：你在微服务项目中是否遇到过服务雪崩问题？是如何通过技术手段进行治理的？',NULL,'关键得分点：1. 引入 Sentinel 或 Hystrix 实现熔断降级；2. 配置合理的超时与重试策略；3. 服务隔离（线程池/信号量）；4. 监控告警联动；5. 压测验证熔断阈值。','关键得分点：1. 引入 Sentinel 或 Hystrix 实现熔断降级；2. 配置合理的超时与重试策略；3. 服务隔离（线程池/信号量）；4. 监控告警联动；5. 压测验证熔断阈值。',1,4,'2026-08-12 15:34:28','2026-08-12 15:34:28',NULL,NULL,NULL,NULL,0),(2087442576231497729,2087442575967256577,2,'项目经验题：请举例说明你在项目中如何优化慢 SQL，以及如何判断索引是否生效。',NULL,'关键得分点：1. 使用 EXPLAIN 分析执行计划；2. 避免索引失效（如函数操作、隐式转换）；3. 覆盖索引减少回表；4. 慢查询日志定位；5. 联合索引最左前缀原则。','关键得分点：1. 使用 EXPLAIN 分析执行计划；2. 避免索引失效（如函数操作、隐式转换）；3. 覆盖索引减少回表；4. 慢查询日志定位；5. 联合索引最左前缀原则。',2,5,'2026-08-12 15:34:28','2026-08-12 15:34:28',NULL,NULL,NULL,NULL,0),(2087442576265052161,2087442575967256577,2,'行为面试题：当你和团队成员在技术方案上出现严重分歧时，你会如何处理？',NULL,'参考答案要点：1. 基于数据和场景客观分析各方方案优劣；2. 组织技术评审会充分讨论；3. 必要时做 PoC 验证；4. 尊重决策流程，即使未采纳也全力支持落地。','参考答案要点：1. 基于数据和场景客观分析各方方案优劣；2. 组织技术评审会充分讨论；3. 必要时做 PoC 验证；4. 尊重决策流程，即使未采纳也全力支持落地。',1,6,'2026-08-12 15:34:28','2026-08-12 15:34:28',NULL,NULL,NULL,NULL,0),(2087442576277635073,2087442575967256577,2,'行为面试题：请分享一次你主动发现并解决线上性能瓶颈的经历。',NULL,'参考答案要点：1. 通过监控/日志/链路追踪定位问题；2. 使用 Arthas/JProfiler 等工具分析 JVM 或代码瓶颈；3. 提出并实施优化方案；4. 验证效果并形成文档或规范。','参考答案要点：1. 通过监控/日志/链路追踪定位问题；2. 使用 Arthas/JProfiler 等工具分析 JVM 或代码瓶颈；3. 提出并实施优化方案；4. 验证效果并形成文档或规范。',2,7,'2026-08-12 15:34:28','2026-08-12 15:34:28',NULL,NULL,NULL,NULL,0),(2087442576319578113,2087442575967256577,2,'行为面试题：当你接手一个遗留系统且缺乏文档时，你会采取哪些步骤来快速理解并安全地进行改造？',NULL,'参考答案要点：1. 梳理核心链路与依赖关系；2. 补充单元测试/集成测试；3. 逐步重构高风险模块；4. 建立监控与回滚机制；5. 与原开发人员沟通关键设计意图。','参考答案要点：1. 梳理核心链路与依赖关系；2. 补充单元测试/集成测试；3. 逐步重构高风险模块；4. 建立监控与回滚机制；5. 与原开发人员沟通关键设计意图。',2,8,'2026-08-12 15:34:28','2026-08-12 15:34:28',NULL,NULL,NULL,NULL,0),(2087452651717365762,2087452651646062593,0,'技术基础题：在接口自动化测试中，以下哪种做法最有助于提升测试脚本的可维护性？','[\"A. 将所有请求URL硬编码在测试方法中\", \"B. 使用全局变量存储所有测试数据\", \"C. 封装HTTP客户端并统一管理鉴权逻辑\", \"D. 每次测试都重新登录获取token\"]','C','C，封装HTTP客户端和统一鉴权逻辑能显著降低重复代码，提高脚本可维护性和复用性。',1,0,'2026-08-12 16:14:30','2026-08-12 16:14:30',NULL,NULL,NULL,NULL,0),(2087452651788668930,2087452651646062593,0,'技术基础题：关于性能测试中的“容量规划”，以下说法正确的是？','[\"A. 容量规划只需关注CPU和内存使用率\", \"B. 容量规划是在系统上线后根据用户反馈动态调整\", \"C. 容量规划需结合业务增长模型预估未来资源需求\", \"D. 容量规划等同于单次压测的最大并发数\"]','C','C，容量规划需基于业务模型、历史数据和增长趋势，预判系统未来负载能力，提前规划资源。',1,1,'2026-08-12 16:14:30','2026-08-12 16:14:30',NULL,NULL,NULL,NULL,0),(2087452651805446146,2087452651646062593,1,'技术基础题：在构建持续集成流水线时，以下哪些措施属于有效的质量门禁实践？','[\"A. 单元测试覆盖率低于80%则阻断构建\", \"B. 自动化冒烟测试失败则禁止部署到测试环境\", \"C. 静态代码扫描发现高危漏洞则中断流程\", \"D. 所有功能测试通过才允许合并代码\", \"E. 每次提交都触发全量回归测试\"]','A,B,C','A、B、C，质量门禁应聚焦关键指标（如覆盖率、冒烟结果、安全漏洞），D过于严格影响效率，E成本过高不现实。',1,2,'2026-08-12 16:14:30','2026-08-12 16:14:30',NULL,NULL,NULL,NULL,0),(2087452651822223362,2087452651646062593,2,'项目经验题：请描述你参与过的一个自动化测试项目，你是如何设计测试框架以支持多环境（如dev/staging/prod）切换的？遇到了哪些挑战？',NULL,'参考答案要点：1. 使用配置文件或环境变量管理不同环境的URL/账号等参数；2. 抽象环境适配层；3. 挑战可能包括认证方式差异、测试数据隔离、网络策略限制等；4. 如何通过CI参数动态注入环境。','参考答案要点：1. 使用配置文件或环境变量管理不同环境的URL/账号等参数；2. 抽象环境适配层；3. 挑战可能包括认证方式差异、测试数据隔离、网络策略限制等；4. 如何通过CI参数动态注入环境。',1,3,'2026-08-12 16:14:30','2026-08-12 16:14:30',NULL,NULL,NULL,NULL,0),(2087452651834806274,2087452651646062593,2,'项目经验题：在一次性能测试中，你发现系统在高并发下响应时间急剧上升，但服务器资源（CPU/内存）并未打满。你会如何分析和定位瓶颈？',NULL,'参考答案要点：1. 检查数据库慢查询与连接池；2. 分析线程阻塞或锁竞争（如线程dump）；3. 查看中间件（如Tomcat/Nginx）配置；4. 检查外部依赖（如第三方API）延迟；5. 使用APM工具追踪调用链。','参考答案要点：1. 检查数据库慢查询与连接池；2. 分析线程阻塞或锁竞争（如线程dump）；3. 查看中间件（如Tomcat/Nginx）配置；4. 检查外部依赖（如第三方API）延迟；5. 使用APM工具追踪调用链。',1,4,'2026-08-12 16:14:30','2026-08-12 16:14:30',NULL,NULL,NULL,NULL,0),(2087452651855777793,2087452651646062593,2,'行为面试题：当你在项目临近上线时发现一个严重但非阻塞性的缺陷，而开发团队认为风险可控不愿修复，你会如何处理？',NULL,'参考答案要点：1. 评估缺陷对核心业务的实际影响；2. 与产品/项目经理沟通风险，提供数据支撑；3. 建议记录为已知问题并制定监控或回滚预案；4. 推动建立缺陷分级标准避免后续争议。','参考答案要点：1. 评估缺陷对核心业务的实际影响；2. 与产品/项目经理沟通风险，提供数据支撑；3. 建议记录为已知问题并制定监控或回滚预案；4. 推动建立缺陷分级标准避免后续争议。',1,5,'2026-08-12 16:14:30','2026-08-12 16:14:30',NULL,NULL,NULL,NULL,0),(2087452651872555009,2087452651646062593,2,'行为面试题：请举例说明你如何推动团队提升整体测试效率或质量保障水平？',NULL,'参考答案要点：1. 引入或优化自动化测试覆盖关键路径；2. 建立质量门禁规则；3. 推动测试左移（如参与需求评审）；4. 建立测试数据管理机制；5. 量化改进效果（如缺陷逃逸率下降、回归周期缩短）。','参考答案要点：1. 引入或优化自动化测试覆盖关键路径；2. 建立质量门禁规则；3. 推动测试左移（如参与需求评审）；4. 建立测试数据管理机制；5. 量化改进效果（如缺陷逃逸率下降、回归周期缩短）。',1,6,'2026-08-12 16:14:30','2026-08-12 16:14:30',NULL,NULL,NULL,NULL,0);


