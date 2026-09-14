package com.smartrecruit.system.service;

/**
 * 密码策略校验器，从系统配置实时读取密码强度要求。
 *
 * @since 2026-04-05
 */
public interface PasswordPolicy {

    /**
     * 校验密码是否符合当前系统配置的密码策略。
     *
     * @param password 待校验密码
     * @throws com.smartrecruit.common.exception.ValidationException 密码不符合策略时抛出
     */
    void validate(String password);
}
