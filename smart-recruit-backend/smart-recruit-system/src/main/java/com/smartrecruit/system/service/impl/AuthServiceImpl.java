package com.smartrecruit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.constant.ConfigKeys;
import com.smartrecruit.common.constant.Constants;
import com.smartrecruit.common.exception.AuthenticationException;
import com.smartrecruit.common.exception.BusinessException;
import com.smartrecruit.common.util.JwtUtil;
import com.smartrecruit.system.converter.UserConverter;
import com.smartrecruit.common.exception.DuplicateResourceException;
import com.smartrecruit.system.dto.request.LoginRequest;
import com.smartrecruit.system.dto.request.PhoneLoginRequest;
import com.smartrecruit.system.dto.request.RegisterRequest;
import com.smartrecruit.system.dto.request.ResetPasswordRequest;
import com.smartrecruit.system.dto.response.LoginResponse;
import com.smartrecruit.system.dto.response.UserVO;
import com.smartrecruit.system.entity.SysPermission;
import com.smartrecruit.system.entity.SysRole;
import com.smartrecruit.system.entity.SysUser;
import com.smartrecruit.system.entity.SysUserRole;
import com.smartrecruit.system.repository.SysPermissionMapper;
import com.smartrecruit.system.repository.SysRoleMapper;
import com.smartrecruit.system.repository.SysRolePermissionMapper;
import com.smartrecruit.system.repository.SysUserMapper;
import com.smartrecruit.system.repository.SysUserRoleMapper;
import com.smartrecruit.system.service.AuthService;
import com.smartrecruit.system.service.CaptchaService;
import com.smartrecruit.system.service.PasswordPolicy;
import com.smartrecruit.system.service.SysConfigService;
import com.smartrecruit.system.service.VerificationCodeService;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.common.util.SecurityUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 认证服务实现类。
 *
 * @since 2026-04-26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeService verificationCodeService;
    private final CaptchaService captchaService;
    private final StringRedisTemplate redisTemplate;
    private final SysConfigService sysConfigService;
    private final PasswordPolicy passwordPolicy;

    @Value("${jwt.secret}")
    private String jwtSecret;

    /** 用户登录。 */
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求: account={}", request.account());

        // 验证图形验证码（一次性使用）
        if (!captchaService.validate(request.captchaId(), request.captchaCode())) {
            log.warn("登录失败, 验证码错误: account={}", request.account());
            throw new AuthenticationException("验证码错误，请重新输入");
        }

        // 登录失败锁定检查
        checkLoginLock(request.account());

        // 支持邮箱或用户名登录：先按邮箱查，再按用户名查
        SysUser user = sysUserMapper.findByEmail(request.account());
        if (user == null) {
            user = sysUserMapper.findByUsername(request.account());
        }
        if (user == null) {
            log.warn("登录失败, 用户不存在: account={}", request.account());
            throw AuthenticationException.invalidCredentials();
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("登录失败, 密码错误: account={}", request.account());
            incrementLoginFailures(request.account());
            throw AuthenticationException.invalidCredentials();
        }

        if (user.getStatus() == null || user.getStatus() != Constants.USER_STATUS_ACTIVE) {
            log.warn("登录失败, 用户已禁用: account={}", request.account());
            throw new AuthenticationException("用户已被禁用，请联系管理员");
        }

        // Get user roles
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(user.getId());
        String roles = roleIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // Get permission codes for all user roles
        String permissions = getPermissionsByRoleIds(roleIds);

        // 会话超时时间取系统配置（分钟 → 秒），修改后立即对新的登录生效
        long sessionTimeoutSeconds =
                sysConfigService.getLong(ConfigKeys.SESSION_TIMEOUT_MINUTES, 120) * 60;
        SecretKey secretKey = JwtUtil.getSecretKey(jwtSecret);
        String accessToken = JwtUtil.createAccessToken(secretKey, user.getId(), user.getUsername(),
                user.getEmail(), roles, permissions, sessionTimeoutSeconds);
        String refreshToken = JwtUtil.createRefreshToken(secretKey, user.getId(), user.getUsername(), user.getEmail());

        // 登录成功，清除失败计数与锁定标记
        clearLoginFailures(request.account());

        // Update last login time and IP
        String clientIp = getClientIp();
        user.setLastLoginTime(DateUtils.now());
        user.setLastLoginIp(clientIp);
        sysUserMapper.updateById(user);

        UserVO userVO = attachPermissions(userConverter.toVO(user), permissions);
        log.info("用户登录成功: userId={}, email={}", user.getId(), user.getEmail());

        return LoginResponse.of(accessToken, refreshToken, sessionTimeoutSeconds, userVO);
    }

    /** 发送登录验证码。 */
    @Override
    public void sendVerificationCode(String email) {
        log.info("发送验证码请求: email={}", email);
        verificationCodeService.generateAndSend(email);
    }

    /** 发送登录验证码。 */
    @Override
    public void sendVerificationCode(String email, String purpose) {
        log.info("发送验证码请求: email={}, purpose={}", email, purpose);

        // 密码重置场景：验证用户存在
        if (Constants.VERIFICATION_PURPOSE_RESET.equals(purpose)) {
            SysUser user = sysUserMapper.findByEmail(email);
            if (user == null) {
                log.warn("密码重置请求失败, 用户不存在: email={}", email);
                throw new BusinessException("USER_NOT_FOUND", "该邮箱未注册，请先注册账号");
            }
        }

        verificationCodeService.generateAndSend(email, purpose);
    }

    /** 发送手机验证码。 */
    @Override
    public void sendPhoneCode(String mobile) {
        log.info("发送手机验证码请求: mobile={}", mobile);

        // 60s 防重发
        String resendKey = Constants.REDIS_KEY_PHONE_RESEND + mobile;
        Boolean canResend = redisTemplate.opsForValue()
                .setIfAbsent(resendKey, "1", Constants.VERIFICATION_CODE_RESEND_SECONDS, TimeUnit.SECONDS);
        if (canResend == null || !canResend) {
            Long remaining = redisTemplate.getExpire(resendKey, TimeUnit.SECONDS);
            throw new BusinessException("RATE_LIMITED",
                    "验证码已发送，请 " + (remaining != null ? remaining : 60) + " 秒后再试");
        }

        // 生成 6 位验证码
        int code = RANDOM.nextInt(900000) + 100000;
        String codeStr = String.valueOf(code);

        // 存入 Redis，5 分钟过期
        String codeKey = Constants.REDIS_KEY_PHONE_CODE + mobile;
        redisTemplate.opsForValue().set(codeKey, codeStr,
                Constants.VERIFICATION_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 开发/演示模式：验证码输出到控制台（后续接入阿里云短信替换此处）
        log.info("========================================");
        log.info("  [短信验证码] 手机: {}  验证码: {}", mobile, codeStr);
        log.info("========================================");
    }

    /** 手机号验证码登录。 */
    @Override
    public LoginResponse phoneLogin(PhoneLoginRequest request) {
        log.info("手机验证码登录请求: mobile={}", request.mobile());

        // 校验验证码
        String codeKey = Constants.REDIS_KEY_PHONE_CODE + request.mobile();
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            log.warn("手机验证码已过期或不存在: mobile={}", request.mobile());
            throw new BusinessException("VALIDATION_FAILED", "验证码已过期，请重新获取");
        }
        if (!storedCode.equals(request.verificationCode())) {
            log.warn("手机验证码错误: mobile={}", request.mobile());
            throw new BusinessException("VALIDATION_FAILED", "验证码错误，请重新输入");
        }
        // 验证通过，删除验证码
        redisTemplate.delete(codeKey);
        redisTemplate.delete(Constants.REDIS_KEY_PHONE_RESEND + request.mobile());

        // 查找已有用户
        SysUser user = sysUserMapper.findByMobile(request.mobile());

        if (user == null) {
            // 自动注册
            log.info("手机号未注册，自动创建用户: mobile={}", request.mobile());
            user = new SysUser();
            user.setUsername("u_" + request.mobile());
            user.setMobile(request.mobile());
            user.setRealName("用户" + request.mobile().substring(7));
            user.setEmail(null);
            user.setPassword(passwordEncoder.encode(generateRandomPassword()));
            user.setStatus(Constants.USER_STATUS_ACTIVE);
            user.setGender(0);
            sysUserMapper.insert(user);

            // 分配默认角色
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(Constants.DEFAULT_ROLE_CANDIDATE_ID);
            sysUserRoleMapper.insert(userRole);

            log.info("自动注册成功: userId={}, mobile={}, username={}",
                    user.getId(), request.mobile(), user.getUsername());
        } else {
            // 检查账号状态
            if (user.getStatus() == null || user.getStatus() != Constants.USER_STATUS_ACTIVE) {
                log.warn("手机登录失败，用户已禁用: mobile={}", request.mobile());
                throw new AuthenticationException("用户已被禁用，请联系管理员");
            }
        }

        // 生成 JWT token
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(user.getId());
        String roles = roleIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String permissions = getPermissionsByRoleIds(roleIds);

        long sessionTimeoutSeconds =
                sysConfigService.getLong(ConfigKeys.SESSION_TIMEOUT_MINUTES, 120) * 60;
        SecretKey secretKey = JwtUtil.getSecretKey(jwtSecret);
        String accessToken = JwtUtil.createAccessToken(secretKey, user.getId(), user.getUsername(),
                user.getEmail(), roles, permissions, sessionTimeoutSeconds);
        String refreshToken = JwtUtil.createRefreshToken(secretKey, user.getId(),
                user.getUsername(), user.getEmail());

        // 更新登录信息
        String clientIp = getClientIp();
        user.setLastLoginTime(DateUtils.now());
        user.setLastLoginIp(clientIp);
        sysUserMapper.updateById(user);

        UserVO userVO = attachPermissions(userConverter.toVO(user), permissions);
        log.info("手机验证码登录成功: userId={}, mobile={}", user.getId(), user.getMobile());

        return LoginResponse.of(accessToken, refreshToken,
                sessionTimeoutSeconds, userVO);
    }

    /** 用户注册。 */
    @Override
    public UserVO register(RegisterRequest request) {
        log.info("用户注册请求: email={}, username={}", request.email(), request.username());

        // 校验验证码
        verificationCodeService.verify(request.email(), request.verificationCode());

        // 检查邮箱是否已注册
        SysUser existingEmail = sysUserMapper.findByEmail(request.email());
        if (existingEmail != null) {
            log.warn("注册失败, 邮箱已存在: email={}", request.email());
            throw new DuplicateResourceException("用户", "邮箱", request.email());
        }

        // 自动生成全局唯一用户名（realName + 递增尾数）
        String username = generateUniqueUsername(request.username());

        // 密码策略校验（注册场景，配置修改后立即生效）
        passwordPolicy.validate(request.password());

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setStatus(Constants.USER_STATUS_ACTIVE);
        user.setGender(0);

        // 绑定内推码（通过分享链接注册时带入）
        if (request.referralCode() != null && !request.referralCode().isBlank()) {
            user.setReferralCode(request.referralCode().toUpperCase());
            log.info("用户通过内推码注册: email={}, referralCode={}", request.email(), request.referralCode());
        }

        sysUserMapper.insert(user);

        // 记录首次登录 IP 和时间
        String clientIp = getClientIp();
        user.setLastLoginTime(DateUtils.now());
        user.setLastLoginIp(clientIp);
        sysUserMapper.updateById(user);

        // 分配默认角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(Constants.DEFAULT_ROLE_EMPLOYEE_ID);
        sysUserRoleMapper.insert(userRole);

        log.info("用户注册成功: userId={}, email={}", user.getId(), user.getEmail());

        return userConverter.toVO(user);
    }

    /** 重置密码。 */
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        log.info("密码重置请求: email={}", request.email());

        // 校验密码重置验证码
        verificationCodeService.verify(request.email(), request.verificationCode(),
                Constants.VERIFICATION_PURPOSE_RESET);

        // 验证用户存在（深度防御：send-code 阶段已校验，此处再次确认）
        SysUser user = sysUserMapper.findByEmail(request.email());
        if (user == null) {
            log.error("密码重置异常, 验证码校验通过但用户不存在: email={}", request.email());
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }

        // 密码策略校验（重置密码场景，配置修改后立即生效）
        passwordPolicy.validate(request.newPassword());

        // 加密新密码并更新
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        sysUserMapper.updateById(user);

        log.info("密码重置成功: userId={}, email={}", user.getId(), user.getEmail());
    }

    /** 刷新访问令牌。 */
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        log.info("刷新 Token 请求");

        SecretKey secretKey = JwtUtil.getSecretKey(jwtSecret);
        if (!JwtUtil.isTokenValid(secretKey, refreshToken)) {
            throw AuthenticationException.tokenExpired();
        }

        Long userId = JwtUtil.getUserId(secretKey, refreshToken);
        String username = JwtUtil.getUsername(secretKey, refreshToken);
        String email = JwtUtil.getEmail(secretKey, refreshToken);

        if (userId == null || username == null) {
            throw AuthenticationException.tokenExpired();
        }

        // 直接查询用户（refresh 为公开端点，SecurityContext 不可用）
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }

        // Graceful degradation: if email is not in old token, use from DB
        if (email == null) {
            email = user.getEmail();
        }

        // Re-query roles and permissions from database (not from stale token)
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(userId);
        String roles = roleIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String permissions = getPermissionsByRoleIds(roleIds);

        long sessionTimeoutSeconds =
                sysConfigService.getLong(ConfigKeys.SESSION_TIMEOUT_MINUTES, 120) * 60;
        String newAccessToken = JwtUtil.createAccessToken(secretKey, userId, username,
                email, roles, permissions, sessionTimeoutSeconds);
        String newRefreshToken = JwtUtil.createRefreshToken(secretKey, userId, username, email);

        UserVO userVO = attachPermissions(userConverter.toVO(user), permissions);
        return LoginResponse.of(newAccessToken, newRefreshToken, sessionTimeoutSeconds, userVO);
    }

    /**
     * 校验账号是否处于登录失败锁定状态。
     */
    private void checkLoginLock(String account) {
        String lockKey = Constants.REDIS_KEY_LOGIN_LOCK + account;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long remaining = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
            log.warn("登录失败次数超限，账号已锁定: account={}", account);
            throw new AuthenticationException(
                    "登录失败次数过多，账号已临时锁定"
                            + (remaining != null ? "，请 " + remaining + " 分钟后重试" : "，请稍后重试"));
        }
    }

    /**
     * 记录登录失败次数，超过阈值后锁定账号（阈值读取系统配置，实时生效）。
     */
    private void incrementLoginFailures(String account) {
        String failKey = Constants.REDIS_KEY_LOGIN_FAIL_COUNT + account;
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1) {
            // 失败计数窗口：30 分钟
            redisTemplate.expire(failKey, 30, TimeUnit.MINUTES);
        }

        int maxAttempts = sysConfigService.getInt(ConfigKeys.LOGIN_MAX_ATTEMPTS, 5);
        if (count != null && count >= maxAttempts) {
            String lockKey = Constants.REDIS_KEY_LOGIN_LOCK + account;
            redisTemplate.opsForValue().set(lockKey, "1", 30, TimeUnit.MINUTES);
            redisTemplate.delete(failKey);
            log.warn("登录失败次数超限，账号已临时锁定: account={}, attempts={}", account, count);
        }
    }

    /**
     * 登录成功后清除失败计数与锁定标记。
     */
    private void clearLoginFailures(String account) {
        redisTemplate.delete(Constants.REDIS_KEY_LOGIN_FAIL_COUNT + account);
        redisTemplate.delete(Constants.REDIS_KEY_LOGIN_LOCK + account);
    }

    /** 查询当前登录用户信息。 */
    @Override
    public UserVO getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationException("未登录");
        }

        Object credentials = auth.getCredentials();
        if (credentials == null || credentials.toString().isEmpty()) {
            throw new AuthenticationException("用户凭证缺失");
        }

        Long userId = Long.parseLong(credentials.toString());
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }

        // 查询角色与权限，随 /auth/me 返回给前端做菜单与按钮鉴权
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(userId);
        String permissions = getPermissionsByRoleIds(roleIds);
        return attachPermissions(userConverter.toVO(user), permissions);
    }

    /**
     * 将权限码列表附加到 UserVO（权限来自角色授权，而非用户实体）。
     */
    private UserVO attachPermissions(UserVO vo, String permissionsCsv) {
        List<String> permList = permissionsCsv == null || permissionsCsv.isBlank()
                ? List.of()
                : Arrays.stream(permissionsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        return vo.withPermissions(permList);
    }

    /**
     * 根据角色 ID 列表查询所有权限码，返回逗号分隔的权限码字符串。
     */
    private String getPermissionsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return "";
        }

        Set<Long> permIdSet = new HashSet<>();
        for (Long roleId : roleIds) {
            List<Long> permIds = sysRolePermissionMapper.selectPermissionIdsByRoleId(roleId);
            permIdSet.addAll(permIds);
        }

        if (permIdSet.isEmpty()) {
            return "";
        }

        List<SysPermission> permissions = sysPermissionMapper.selectBatchIds(permIdSet);
        return permissions.stream()
                .filter(p -> p.getCode() != null && !p.getCode().isEmpty())
                .map(SysPermission::getCode)
                .distinct()
                .collect(Collectors.joining(","));
    }

    /**
     * 自动生成全局唯一的用户名。若 baseName 未占用则直接使用，
     * 否则拼接当前用户总数+1作为后缀，确保全局唯一。
     */
    private String generateUniqueUsername(String baseName) {
        SysUser existing = sysUserMapper.findByUsername(baseName);
        if (existing == null) {
            return baseName;
        }
        long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0));
        String candidate = baseName + (count + 1);
        // 极端情况：拼接后的用户名也被占用（并发），则继续递增
        while (sysUserMapper.findByUsername(candidate) != null) {
            count++;
            candidate = baseName + (count + 1);
        }
        return candidate;
    }

    /**
     * 为手机验证码自动注册的用户生成随机密码（仅用于满足数据库 NOT NULL 约束）。
     * 用户只能通过手机验证码登录，不会知晓此密码。
     */
    private String generateRandomPassword() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 从当前请求中提取客户端真实 IP（考虑反向代理/网关）。
     */
    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        return SecurityUtil.getClientIpAddress(request);
    }
}
