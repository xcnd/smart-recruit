package com.smartrecruit.system.service.impl;

import com.smartrecruit.common.constant.ConfigKeys;
import com.smartrecruit.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.smartrecruit.system.service.PasswordPolicy;
import com.smartrecruit.system.service.SysConfigService;

/**
 * 密码策略校验器。
 *
 * <p>从系统配置实时读取密码最小/最大长度及特殊字符要求，
 * 在注册、重置密码、修改密码等场景统一执行，配置修改后立即生效。</p>
 *
 * @since 2026-04-05
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicyImpl implements PasswordPolicy {

    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}|;:'\",.<>/?~`";

    private final SysConfigService sysConfigService;

    /**
     * 校验密码是否符合当前系统配置的密码策略。
     *
     * @param password 待校验密码
     * @throws ValidationException 密码不符合策略时抛出
     */
    public void validate(String password) {
        int minLength = sysConfigService.getInt(ConfigKeys.PASSWORD_MIN_LENGTH, 6);
        int maxLength = sysConfigService.getInt(ConfigKeys.PASSWORD_MAX_LENGTH, 64);
        boolean requireSpecial = sysConfigService.getBoolean(ConfigKeys.PASSWORD_REQUIRE_SPECIAL, false);

        if (password == null || password.isEmpty()) {
            throw new ValidationException("密码不能为空");
        }
        if (password.length() < minLength) {
            throw new ValidationException("密码长度不能少于 " + minLength + " 位");
        }
        if (password.length() > maxLength) {
            throw new ValidationException("密码长度不能超过 " + maxLength + " 位");
        }
        if (requireSpecial && !containsSpecial(password)) {
            throw new ValidationException("密码必须包含至少一个特殊字符（如 !@#$%^&*）");
        }
    }

    private boolean containsSpecial(String password) {
        for (char c : password.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
}
