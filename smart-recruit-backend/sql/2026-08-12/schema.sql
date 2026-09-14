-- ============================================================================
-- SmartRecruit Platform - 最新建表语句（7 库 55 表）
-- 生成时间: 2026-08-12 17:45:49
-- 来源: 本地 MySQL 实库 SHOW CREATE TABLE 导出
-- ============================================================================

-- ============================================================================
-- 数据库: `smart_recruit_system`
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `smart_recruit_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_recruit_system`;

-- 表: `smart_recruit_system`.`sys_config`
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

-- 表: `smart_recruit_system`.`sys_department`
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

-- 表: `smart_recruit_system`.`sys_notification`
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

-- 表: `smart_recruit_system`.`sys_operation_log`
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

-- 表: `smart_recruit_system`.`sys_permission`
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

-- 表: `smart_recruit_system`.`sys_role`
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

-- 表: `smart_recruit_system`.`sys_role_permission`
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `role_id` bigint NOT NULL COMMENT '角色ID，关联sys_role.id',
  `permission_id` bigint NOT NULL COMMENT '权限ID，关联sys_permission.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

-- 表: `smart_recruit_system`.`sys_user`
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

-- 表: `smart_recruit_system`.`sys_user_role`
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL COMMENT '主键ID（Snowflake雪花算法）',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联sys_user.id',
  `role_id` bigint NOT NULL COMMENT '角色ID，关联sys_role.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';


-- ============================================================================
-- 数据库: `smart_recruit_recruitment`
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `smart_recruit_recruitment` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_recruit_recruitment`;

-- 表: `smart_recruit_recruitment`.`analytics_candidate_daily`
CREATE TABLE `analytics_candidate_daily` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `stage` tinyint NOT NULL COMMENT '候选人当前阶段（rec_candidate.status）',
  `source` tinyint NOT NULL COMMENT '来源渠道（rec_candidate.source）',
  `candidate_count` int NOT NULL DEFAULT '0' COMMENT '当日新增候选人数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`stat_date`,`stage`,`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人日汇总（定时任务统计）';

-- 表: `smart_recruit_recruitment`.`careers_job_position`
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

-- 表: `smart_recruit_recruitment`.`rec_activity_feed`
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

-- 表: `smart_recruit_recruitment`.`rec_ai_screening_result`
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

-- 表: `smart_recruit_recruitment`.`rec_application`
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

-- 表: `smart_recruit_recruitment`.`rec_candidate`
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

-- 表: `smart_recruit_recruitment`.`rec_candidate_stage_history`
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

-- 表: `smart_recruit_recruitment`.`rec_communication_log`
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

-- 表: `smart_recruit_recruitment`.`rec_job_channel`
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

-- 表: `smart_recruit_recruitment`.`rec_job_position`
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

-- 表: `smart_recruit_recruitment`.`rec_resume`
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

-- 表: `smart_recruit_recruitment`.`rec_workbench_task`
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


-- ============================================================================
-- 数据库: `smart_recruit_interview`
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `smart_recruit_interview` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_recruit_interview`;

-- 表: `smart_recruit_interview`.`analytics_interview_daily`
CREATE TABLE `analytics_interview_daily` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '当日创建面试数',
  `passed_count` int NOT NULL DEFAULT '0' COMMENT '当日创建且结果为通过数',
  `cancelled_count` int NOT NULL DEFAULT '0' COMMENT '当日创建且已取消数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试日汇总（定时任务统计）';

-- 表: `smart_recruit_interview`.`rec_interview`
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

-- 表: `smart_recruit_interview`.`rec_interview_assessment`
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

-- 表: `smart_recruit_interview`.`rec_interview_feedback`
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

-- 表: `smart_recruit_interview`.`rec_interview_question`
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

-- 表: `smart_recruit_interview`.`rec_online_assessment`
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

-- 表: `smart_recruit_interview`.`rec_question_bank`
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

-- 表: `smart_recruit_interview`.`rec_question_bank_item`
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


-- ============================================================================
-- 数据库: `smart_recruit_offer`
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `smart_recruit_offer` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_recruit_offer`;

-- 表: `smart_recruit_offer`.`analytics_offer_daily`
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

-- 表: `smart_recruit_offer`.`ofr_contract`
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

-- 表: `smart_recruit_offer`.`ofr_contract_sign_record`
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

-- 表: `smart_recruit_offer`.`rec_approval_flow_config`
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

-- 表: `smart_recruit_offer`.`rec_offer`
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

-- 表: `smart_recruit_offer`.`rec_offer_approval`
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

-- 表: `smart_recruit_offer`.`rec_onboarding`
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

-- 表: `smart_recruit_offer`.`rec_onboarding_document`
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

-- 表: `smart_recruit_offer`.`rec_onboarding_equipment`
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


-- ============================================================================
-- 数据库: `smart_recruit_talent`
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `smart_recruit_talent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_recruit_talent`;

-- 表: `smart_recruit_talent`.`analytics_candidate_daily`
CREATE TABLE `analytics_candidate_daily` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `stage` tinyint NOT NULL COMMENT '候选人阶段（rec_candidate.status）',
  `source` tinyint NOT NULL COMMENT '来源渠道（rec_candidate.source）',
  `candidate_count` int NOT NULL DEFAULT '0' COMMENT '当日新增候选人数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`stat_date`,`stage`,`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人日汇总快照（talent 本地）';

-- 表: `smart_recruit_talent`.`analytics_interview_daily`
CREATE TABLE `analytics_interview_daily` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '当日创建面试数',
  `passed_count` int NOT NULL DEFAULT '0' COMMENT '当日创建且结果为通过数',
  `cancelled_count` int NOT NULL DEFAULT '0' COMMENT '当日创建且已取消数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试日汇总快照（talent 本地）';

-- 表: `smart_recruit_talent`.`analytics_offer_daily`
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

-- 表: `smart_recruit_talent`.`rec_talent_campaign`
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

-- 表: `smart_recruit_talent`.`rec_talent_pool`
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


-- ============================================================================
-- 数据库: `smart_recruit_referral`
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `smart_recruit_referral` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_recruit_referral`;

-- 表: `smart_recruit_referral`.`ref_bonus_record`
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

-- 表: `smart_recruit_referral`.`ref_leaderboard`
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

-- 表: `smart_recruit_referral`.`ref_program`
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

-- 表: `smart_recruit_referral`.`ref_program_job`
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

-- 表: `smart_recruit_referral`.`ref_record`
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

-- 表: `smart_recruit_referral`.`ref_share_token`
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


-- ============================================================================
-- 数据库: `smart_recruit_ai`
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `smart_recruit_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_recruit_ai`;

-- 表: `smart_recruit_ai`.`ai_agent_info`
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

-- 表: `smart_recruit_ai`.`ai_agent_metric`
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

-- 表: `smart_recruit_ai`.`ai_agent_task`
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

-- 表: `smart_recruit_ai`.`ai_event_log`
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

-- 表: `smart_recruit_ai`.`ai_interview_log`
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

-- 表: `smart_recruit_ai`.`ai_resume_parse_log`
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

