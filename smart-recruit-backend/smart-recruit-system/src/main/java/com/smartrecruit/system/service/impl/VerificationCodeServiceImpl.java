package com.smartrecruit.system.service.impl;

import com.smartrecruit.common.constant.Constants;
import com.smartrecruit.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import com.smartrecruit.system.service.EmailService;
import com.smartrecruit.system.service.VerificationCodeService;

/**
 * 邮箱验证码服务。
 *
 * <p>基于 Redis 存储验证码，支持生成、校验和重发限流。
 * 通过 {@code purpose} 参数区分注册和密码重置场景，
 * 使用独立的 Redis Key 前缀避免两种场景的验证码互相覆盖。</p>
 *
 * @author xdh
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成验证码并发送邮件（注册场景，保持向后兼容）。
     *
     * @param email 目标邮箱
     */
    public void generateAndSend(String email) {
        generateAndSend(email, Constants.VERIFICATION_PURPOSE_REGISTER);
    }

    /**
     * 生成验证码并发送邮件（支持指定用途）。
     *
     * <p>60 秒内不允许对同一邮箱重复发送。</p>
     *
     * @param email   目标邮箱
     * @param purpose 验证码用途（register / reset）
     */
    public void generateAndSend(String email, String purpose) {
        String resendKeyPrefix = Constants.VERIFICATION_PURPOSE_RESET.equals(purpose)
                ? Constants.REDIS_KEY_VERIFICATION_RESEND_RESET
                : Constants.REDIS_KEY_VERIFICATION_RESEND;
        String codeKeyPrefix = Constants.VERIFICATION_PURPOSE_RESET.equals(purpose)
                ? Constants.REDIS_KEY_VERIFICATION_CODE_RESET
                : Constants.REDIS_KEY_VERIFICATION_CODE;

        // 检查重发间隔
        String resendKey = resendKeyPrefix + email;
        Boolean canResend = redisTemplate.opsForValue()
                .setIfAbsent(resendKey, "1", Constants.VERIFICATION_CODE_RESEND_SECONDS, TimeUnit.SECONDS);
        if (canResend == null || !canResend) {
            Long remaining = redisTemplate.getExpire(resendKey, TimeUnit.SECONDS);
            throw new BusinessException("RATE_LIMITED",
                    "验证码已发送，请 " + (remaining != null ? remaining : 60) + " 秒后再试");
        }

        // 生成 6 位数字验证码
        String code = generateCode();

        // 存入 Redis，5 分钟过期
        String codeKey = codeKeyPrefix + email;
        redisTemplate.opsForValue().set(codeKey, code, Constants.VERIFICATION_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 发送邮件（根据用途选择模板）
        if (Constants.VERIFICATION_PURPOSE_RESET.equals(purpose)) {
            emailService.sendPasswordResetCode(email, code);
        } else {
            emailService.sendVerificationCode(email, code);
        }
        log.info("验证码已生成并发送: email={}, purpose={}", email, purpose);
    }

    /**
     * 校验验证码（注册场景，保持向后兼容）。
     */
    public void verify(String email, String code) {
        verify(email, code, Constants.VERIFICATION_PURPOSE_REGISTER);
    }

    /**
     * 校验验证码（支持指定用途）。
     *
     * <p>验证成功后删除 Redis 中的验证码，防止重复使用。</p>
     *
     * @param email   邮箱
     * @param code    用户输入的验证码
     * @param purpose 验证码用途
     * @throws BusinessException 验证码错误或已过期
     */
    public void verify(String email, String code, String purpose) {
        String codeKeyPrefix = Constants.VERIFICATION_PURPOSE_RESET.equals(purpose)
                ? Constants.REDIS_KEY_VERIFICATION_CODE_RESET
                : Constants.REDIS_KEY_VERIFICATION_CODE;
        String resendKeyPrefix = Constants.VERIFICATION_PURPOSE_RESET.equals(purpose)
                ? Constants.REDIS_KEY_VERIFICATION_RESEND_RESET
                : Constants.REDIS_KEY_VERIFICATION_RESEND;

        String codeKey = codeKeyPrefix + email;
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            log.warn("验证码已过期或不存在: email={}, purpose={}", email, purpose);
            throw new BusinessException("VALIDATION_FAILED", "验证码已过期，请重新获取");
        }

        if (!storedCode.equals(code)) {
            log.warn("验证码错误: email={}, expected={}, actual={}, purpose={}",
                    email, storedCode, code, purpose);
            throw new BusinessException("VALIDATION_FAILED", "验证码错误，请重新输入");
        }

        // 验证成功后删除验证码和重发限制
        redisTemplate.delete(codeKey);
        redisTemplate.delete(resendKeyPrefix + email);
        log.info("验证码校验成功: email={}, purpose={}", email, purpose);
    }

    /**
     * 生成 6 位随机数字验证码。
     */
    private String generateCode() {
        int code = RANDOM.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(code);
    }
}
