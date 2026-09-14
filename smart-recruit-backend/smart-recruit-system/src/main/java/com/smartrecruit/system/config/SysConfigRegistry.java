package com.smartrecruit.system.config;

import com.smartrecruit.common.constant.ConfigKeys;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统配置项注册表。
 *
 * <p>作为系统设置功能的唯一事实来源，集中登记所有配置键的元信息。
 * 新增配置项时在此注册，即可自动获得类型校验、公开/内部接口暴露
 * 和默认值兜底能力。</p>
 *
 * @since 2026-04-05
 */
public final class SysConfigRegistry {

    private static final List<SysConfigDefinition> DEFINITIONS = List.of(
            // ---------- 通用设置 ----------
            SysConfigDefinition.of(ConfigKeys.SYSTEM_NAME, SysConfigGroup.GENERAL,
                    SysConfigType.STRING, true, "SmartRecruit",
                    "系统名称，显示在页面标题、邮件标题、通知等位置", 64),
            SysConfigDefinition.of(ConfigKeys.COMPANY_NAME, SysConfigGroup.GENERAL,
                    SysConfigType.STRING, true, "SmartRecruit 科技有限公司",
                    "公司名称（合同甲方名称）", 128),
            SysConfigDefinition.of(ConfigKeys.COMPANY_CREDIT_CODE, SysConfigGroup.GENERAL,
                    SysConfigType.STRING, true, "",
                    "统一社会信用代码（合同甲方信息）", 64),
            SysConfigDefinition.of(ConfigKeys.COMPANY_ADDRESS, SysConfigGroup.GENERAL,
                    SysConfigType.STRING, true, "",
                    "公司住所（合同甲方信息）", 256),
            SysConfigDefinition.of(ConfigKeys.COMPANY_LEGAL_REPRESENTATIVE, SysConfigGroup.GENERAL,
                    SysConfigType.STRING, true, "",
                    "法定代表人（合同甲方信息）", 64),
            SysConfigDefinition.of(ConfigKeys.COPYRIGHT_TEXT, SysConfigGroup.GENERAL,
                    SysConfigType.STRING, true, "© 2026 SmartRecruit. All rights reserved.",
                    "页面底部版权信息，支持 HTML", 256),

            // ---------- 邮件配置 ----------
            SysConfigDefinition.of(ConfigKeys.EMAIL_SUFFIX, SysConfigGroup.EMAIL,
                    SysConfigType.STRING, true, "@company.com",
                    "用户邮箱由【用户名 + 此后缀】组成", 128),
            SysConfigDefinition.of(ConfigKeys.EMAIL_SENDER_NAME, SysConfigGroup.EMAIL,
                    SysConfigType.STRING, true, "SmartRecruit",
                    "系统发出的邮件中显示的发送者名称", 64),
            SysConfigDefinition.of(ConfigKeys.EMAIL_SENDER_ADDRESS, SysConfigGroup.EMAIL,
                    SysConfigType.STRING, true, "",
                    "系统发出的邮件 From 地址（需与 SMTP 账号匹配）", 128),

            // ---------- 安全策略 ----------
            SysConfigDefinition.ranged(ConfigKeys.PASSWORD_MIN_LENGTH, SysConfigGroup.SECURITY,
                    SysConfigType.INT, true, "6", "密码允许的最短字符数", 4, 32),
            SysConfigDefinition.ranged(ConfigKeys.PASSWORD_MAX_LENGTH, SysConfigGroup.SECURITY,
                    SysConfigType.INT, true, "64", "密码允许的最长字符数", 8, 128),
            SysConfigDefinition.of(ConfigKeys.PASSWORD_REQUIRE_SPECIAL, SysConfigGroup.SECURITY,
                    SysConfigType.BOOLEAN, true, "0",
                    "是否要求密码包含特殊字符：0=否, 1=是", 1),
            SysConfigDefinition.ranged(ConfigKeys.LOGIN_MAX_ATTEMPTS, SysConfigGroup.SECURITY,
                    SysConfigType.INT, true, "5", "连续登录失败超过此次数后账号临时锁定", 1, 20),
            SysConfigDefinition.ranged(ConfigKeys.SESSION_TIMEOUT_MINUTES, SysConfigGroup.SECURITY,
                    SysConfigType.INT, true, "120", "会话超时时间（分钟），无操作后需重新登录", 5, 1440),
            SysConfigDefinition.of(ConfigKeys.WATERMARK_ENABLED, SysConfigGroup.SECURITY,
                    SysConfigType.BOOLEAN, true, "0",
                    "是否开启页面水印（显示当前账号、姓名与时间，防截图泄露）：0=否, 1=是", 1),

            // ---------- 文件管理 ----------
            SysConfigDefinition.ranged(ConfigKeys.UPLOAD_MAX_SIZE_MB, SysConfigGroup.FILE,
                    SysConfigType.INT, true, "10", "单个文件上传的最大体积（MB）", 1, 100),
            SysConfigDefinition.of(ConfigKeys.UPLOAD_ALLOWED_EXTENSIONS, SysConfigGroup.FILE,
                    SysConfigType.LIST, true,
                    ".pdf,.jpg,.jpeg,.png,.gif,.doc,.docx,.xls,.xlsx",
                    "允许上传的文件类型，逗号分隔的扩展名列表", 512),

            // ---------- 入职管理 ----------
            SysConfigDefinition.of(ConfigKeys.ONBOARDING_DEFAULT_PASSWORD, SysConfigGroup.ONBOARDING,
                    SysConfigType.STRING, true, "123456", "新员工入职账号默认密码", 32),
            SysConfigDefinition.of(ConfigKeys.ONBOARDING_TRAINING_MODULES, SysConfigGroup.ONBOARDING,
                    SysConfigType.LIST, true,
                    "公司文化与制度,信息安全培训,岗位技能培训,合规培训,团队介绍",
                    "入职培训模块列表，逗号分隔", 512),
            SysConfigDefinition.of(ConfigKeys.ONBOARDING_WELCOME_TEMPLATE, SysConfigGroup.ONBOARDING,
                    SysConfigType.STRING, true,
                    "亲爱的 {{employeeName}}：\n\n欢迎加入 {{departmentName}} 部门！"
                            + "\n您的职位：{{jobTitle}}（{{level}}）\n入职日期：{{onboardDate}}"
                            + "\n\n我们为您准备了完善的入职培训计划，您的导师和伙伴将协助您快速融入团队。"
                            + "\n\n期待与您共同成长！\n\n—— 人力资源部",
                    "入职欢迎消息模板，支持变量：{{employeeName}}, {{departmentName}}, "
                            + "{{jobTitle}}, {{level}}, {{onboardDate}}", 2048),

            // ---------- AI 引擎 ----------
            SysConfigDefinition.ranged(ConfigKeys.AI_RISK_THRESHOLD_LOW, SysConfigGroup.AI,
                    SysConfigType.DOUBLE, true, "0.25", "AI 留任预测-低风险阈值", 0, 100),
            SysConfigDefinition.ranged(ConfigKeys.AI_RISK_THRESHOLD_MEDIUM, SysConfigGroup.AI,
                    SysConfigType.DOUBLE, true, "0.50", "AI 留任预测-中风险阈值", 0, 100),
            SysConfigDefinition.ranged(ConfigKeys.AI_RISK_THRESHOLD_HIGH, SysConfigGroup.AI,
                    SysConfigType.DOUBLE, true, "0.75", "AI 留任预测-高风险阈值", 0, 100),

            // ---------- AI 简历筛选权重 ----------
            SysConfigDefinition.ranged(ConfigKeys.AI_SCREEN_WEIGHT_EDUCATION, SysConfigGroup.AI,
                    SysConfigType.INT, true, "20", "AI 简历筛选-学历维度权重（%），五项权重合计应为 100", 0, 100),
            SysConfigDefinition.ranged(ConfigKeys.AI_SCREEN_WEIGHT_SKILL, SysConfigGroup.AI,
                    SysConfigType.INT, true, "35", "AI 简历筛选-技能维度权重（%），五项权重合计应为 100", 0, 100),
            SysConfigDefinition.ranged(ConfigKeys.AI_SCREEN_WEIGHT_EXPERIENCE, SysConfigGroup.AI,
                    SysConfigType.INT, true, "20", "AI 简历筛选-经验维度权重（%），五项权重合计应为 100", 0, 100),
            SysConfigDefinition.ranged(ConfigKeys.AI_SCREEN_WEIGHT_BEHAVIOR, SysConfigGroup.AI,
                    SysConfigType.INT, true, "20", "AI 简历筛选-行为维度权重（%），五项权重合计应为 100", 0, 100),
            SysConfigDefinition.ranged(ConfigKeys.AI_SCREEN_WEIGHT_SEMANTIC, SysConfigGroup.AI,
                    SysConfigType.INT, true, "15", "AI 简历筛选-语义维度权重（%），五项权重合计应为 100", 0, 100),
            SysConfigDefinition.ranged(ConfigKeys.AI_SCREEN_PASS_SCORE, SysConfigGroup.AI,
                    SysConfigType.INT, true, "70", "AI 简历筛选-最低通过分数线（0-100），达到即视为通过初筛", 0, 100)
    );

    /** 招聘官网配置：前缀匹配，任意 careers_* 键。 */
    private static final SysConfigDefinition CAREERS_WILDCARD =
            SysConfigDefinition.of(ConfigKeys.CAREERS_PREFIX + "*", SysConfigGroup.CAREERS,
                    SysConfigType.STRING, true, null, "招聘官网展示内容配置", 4096);

    private static final Map<String, SysConfigDefinition> BY_KEY = DEFINITIONS.stream()
            .collect(Collectors.toUnmodifiableMap(SysConfigDefinition::key, Function.identity()));

    private SysConfigRegistry() {
        throw new UnsupportedOperationException("Registry cannot be instantiated");
    }

    /**
     * 根据配置键获取定义。
     *
     * @param key 配置键
     * @return 配置定义，未知键返回 {@link Optional#empty()}
     */
    public static Optional<SysConfigDefinition> find(String key) {
        SysConfigDefinition def = BY_KEY.get(key);
        if (def == null && key != null && key.startsWith(ConfigKeys.CAREERS_PREFIX)) {
            def = CAREERS_WILDCARD;
        }
        return Optional.ofNullable(def);
    }

    /**
     * 查询所有平台侧公开配置键。
     */
    public static List<String> publicKeys() {
        return DEFINITIONS.stream()
                .filter(SysConfigDefinition::publicVisible)
                .map(SysConfigDefinition::key)
                .toList();
    }

    /**
     * 全部平台配置定义。
     */
    public static List<SysConfigDefinition> all() {
        return DEFINITIONS;
    }
}
