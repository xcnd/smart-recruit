package com.smartrecruit.common.constant;

/**
 * 系统常量定义。
 *
 * @author xdh
 * @since 2026-04-26
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Constant class cannot be instantiated");
    }

    /** 系统默认时区 */
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    /** JWT 相关 */
    public static final String JWT_ISSUER = "smart-recruit";
    public static final long ACCESS_TOKEN_EXPIRE_SECONDS = 7200;     // 2 小时
    public static final long REFRESH_TOKEN_EXPIRE_SECONDS = 604800;  // 7 天
    public static final String TOKEN_TYPE_BEARER = "Bearer";
    public static final String TOKEN_HEADER = "Authorization";

    /** 权限前缀 */
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String PERMISSION_PREFIX = "";

    /** 分页默认值 */
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    /** 通用状态 */
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    /** 用户状态 */
    public static final int USER_STATUS_ACTIVE = 1;
    public static final int USER_STATUS_DISABLED = 0;

    /** 默认注册角色 */
    public static final long DEFAULT_ROLE_EMPLOYEE_ID = 300006L;

    /** 求职者角色（手机号快捷登录自动注册用户） */
    public static final long DEFAULT_ROLE_CANDIDATE_ID = 300008L;

    /** 求职者角色编码（Spring Security @PreAuthorize hasRole 使用） */
    public static final String CANDIDATE_ROLE = "300008";

    /** 邮箱验证码 */
    public static final int VERIFICATION_CODE_EXPIRE_SECONDS = 300;     // 5 分钟
    public static final int VERIFICATION_CODE_LENGTH = 6;
    public static final int VERIFICATION_CODE_RESEND_SECONDS = 60;     // 60 秒后可重发

    /** Redis Key 前缀 */
    public static final String REDIS_KEY_VERIFICATION_CODE = "verification:code:";
    public static final String REDIS_KEY_VERIFICATION_RESEND = "verification:resend:";

    /** 密码重置验证码 Redis Key 前缀（独立前缀避免与注册验证码冲突） */
    public static final String REDIS_KEY_VERIFICATION_CODE_RESET = "verification:code:reset:";
    public static final String REDIS_KEY_VERIFICATION_RESEND_RESET = "verification:resend:reset:";

    /** 验证码用途 */
    public static final String VERIFICATION_PURPOSE_REGISTER = "register";
    public static final String VERIFICATION_PURPOSE_RESET = "reset";

    /** 手机短信验证码 Redis Key */
    public static final String REDIS_KEY_PHONE_CODE = "phone:code:";
    public static final String REDIS_KEY_PHONE_RESEND = "phone:resend:";

    /** 手机验证码用途 */
    public static final String VERIFICATION_PURPOSE_PHONE_LOGIN = "phone_login";

    /** 图片验证码 */
    public static final int CAPTCHA_EXPIRE_SECONDS = 120;        // 2 分钟
    public static final int CAPTCHA_CHAR_LENGTH = 4;
    public static final String REDIS_KEY_CAPTCHA = "captcha:";

    /** 登录失败计数 / 锁定 Redis Key */
    public static final String REDIS_KEY_LOGIN_FAIL_COUNT = "login:fail:count:";
    public static final String REDIS_KEY_LOGIN_LOCK = "login:lock:";

    /** 逻辑删除 */
    public static final int NOT_DELETED = 0;
    public static final int DELETED = 1;

    /** 业务状态码 */
    public static final int CODE_SUCCESS = 200;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_CONFLICT = 409;
    public static final int CODE_VALIDATION_ERROR = 422;
    public static final int CODE_INTERNAL_ERROR = 500;
    public static final int CODE_SERVICE_UNAVAILABLE = 503;

    /** AI Agent 相关 */
    public static final int AGENT_DEFAULT_TIMEOUT_SECONDS = 300;
    public static final int AGENT_MAX_RETRIES = 3;
    public static final int AGENT_BATCH_SIZE = 100;
    public static final double AGENT_SCREENING_PASS_THRESHOLD = 70.0;

    /** 文件上传 */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String[] ALLOWED_RESUME_TYPES = {".pdf", ".doc", ".docx"};

    /** 请求追踪 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
}
