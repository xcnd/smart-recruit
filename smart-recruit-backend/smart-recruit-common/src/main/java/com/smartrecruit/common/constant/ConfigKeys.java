package com.smartrecruit.common.constant;

/**
 * 系统配置键常量表。
 *
 * <p>统一维护所有可配置项的键名，供系统服务读写、
 * 其他微服务消费时引用，避免魔法字符串散落各处。</p>
 *
 * @since 2026-04-05
 */
public final class ConfigKeys {

    private ConfigKeys() {
        throw new UnsupportedOperationException("Constant class cannot be instantiated");
    }

    // ==================== 通用设置 ====================

    /** 系统名称：页面标题、邮件标题、品牌展示。 */
    public static final String SYSTEM_NAME = "system_name";

    /** 公司名称（合同甲方名称）。 */
    public static final String COMPANY_NAME = "company_name";
    /** 统一社会信用代码（合同甲方信息）。 */
    public static final String COMPANY_CREDIT_CODE = "company_credit_code";
    /** 公司住所（合同甲方信息）。 */
    public static final String COMPANY_ADDRESS = "company_address";
    /** 法定代表人（合同甲方信息）。 */
    public static final String COMPANY_LEGAL_REPRESENTATIVE = "company_legal_representative";

    /** 版权信息：页面底部版权声明。 */
    public static final String COPYRIGHT_TEXT = "copyright_text";

    // ==================== 邮件配置 ====================

    /** 邮箱后缀：用户邮箱由【用户名 + 此后缀】组成。 */
    public static final String EMAIL_SUFFIX = "email_suffix";

    /** 邮件发件人显示名称。 */
    public static final String EMAIL_SENDER_NAME = "email_sender_name";

    /** 邮件发件人邮箱地址（需与 SMTP 账号匹配）。 */
    public static final String EMAIL_SENDER_ADDRESS = "email_sender_address";

    // ==================== 安全策略 ====================

    /** 密码最小长度。 */
    public static final String PASSWORD_MIN_LENGTH = "password_min_length";

    /** 密码最大长度。 */
    public static final String PASSWORD_MAX_LENGTH = "password_max_length";

    /** 密码是否必须包含特殊字符：0=否，1=是。 */
    public static final String PASSWORD_REQUIRE_SPECIAL = "password_require_special";

    /** 登录失败最大尝试次数，超过后账号临时锁定。 */
    public static final String LOGIN_MAX_ATTEMPTS = "login_max_attempts";

    /** 会话超时时间（分钟），无操作后需重新登录。 */
    public static final String SESSION_TIMEOUT_MINUTES = "session_timeout_minutes";

    /** 页面水印开关：0=关闭，1=开启（显示当前账号、姓名与时间，防截图泄露）。 */
    public static final String WATERMARK_ENABLED = "watermark_enabled";

    // ==================== 文件管理 ====================

    /** 文件上传大小限制（MB）。 */
    public static final String UPLOAD_MAX_SIZE_MB = "upload_max_size_mb";

    /** 允许上传的文件类型，逗号分隔的扩展名列表。 */
    public static final String UPLOAD_ALLOWED_EXTENSIONS = "upload_allowed_extensions";

    // ==================== 入职管理 ====================

    /** 新员工入职账号默认密码。 */
    public static final String ONBOARDING_DEFAULT_PASSWORD = "onboarding_default_password";

    /** 入职培训模块列表，逗号分隔。 */
    public static final String ONBOARDING_TRAINING_MODULES = "onboarding_training_modules";

    /** 入职欢迎消息模板，支持 {{employeeName}} 等变量。 */
    public static final String ONBOARDING_WELCOME_TEMPLATE = "onboarding_welcome_template";

    // ==================== AI 引擎 ====================

    /** AI 留任预测-低风险阈值。 */
    public static final String AI_RISK_THRESHOLD_LOW = "ai_risk_threshold_low";

    /** AI 留任预测-中风险阈值。 */
    public static final String AI_RISK_THRESHOLD_MEDIUM = "ai_risk_threshold_medium";

    /** AI 留任预测-高风险阈值。 */
    public static final String AI_RISK_THRESHOLD_HIGH = "ai_risk_threshold_high";

    // ==================== AI 简历筛选权重 ====================

    /** AI 简历筛选-学历维度权重（%）。 */
    public static final String AI_SCREEN_WEIGHT_EDUCATION = "ai_screen_weight_education";
    /** AI 简历筛选-技能维度权重（%）。 */
    public static final String AI_SCREEN_WEIGHT_SKILL = "ai_screen_weight_skill";
    /** AI 简历筛选-经验维度权重（%）。 */
    public static final String AI_SCREEN_WEIGHT_EXPERIENCE = "ai_screen_weight_experience";
    /** AI 简历筛选-行为维度权重（%）。 */
    public static final String AI_SCREEN_WEIGHT_BEHAVIOR = "ai_screen_weight_behavior";
    /** AI 简历筛选-语义维度权重（%）。 */
    public static final String AI_SCREEN_WEIGHT_SEMANTIC = "ai_screen_weight_semantic";
    /** AI 简历筛选-最低通过分数线（0-100）。 */
    public static final String AI_SCREEN_PASS_SCORE = "ai_screen_pass_score";

    // ==================== 招聘官网 ====================

    /** 招聘官网配置键前缀，公开给官网前端消费。 */
    public static final String CAREERS_PREFIX = "careers_";
}
