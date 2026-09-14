package com.smartrecruit.system.audit;

import com.smartrecruit.system.entity.SysOperationLog;
import com.smartrecruit.system.repository.SysOperationLogMapper;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.common.util.ScopedValueContext;
import com.smartrecruit.common.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 审计日志 AOP 切面。
 *
 * <p>拦截标注了 {@link Auditable} 注解的方法，自动记录操作审计日志。</p>
 *
 * @since 2026-04-26
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    /** 动作名称 → 动作编码（0=CREATE,1=UPDATE,2=DELETE,3=EXPORT,4=IMPORT,5=LOGIN）。 */
    private static final Map<String, Integer> ACTION_CODES = Map.of(
            "CREATE", 0,
            "UPDATE", 1,
            "DELETE", 2,
            "EXPORT", 3,
            "IMPORT", 4,
            "LOGIN", 5);

    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(?i)(\"(?:password|oldPassword|newPassword|confirmPassword|token|refreshToken|"
                    + "accessToken|secret|authorization|verificationCode|captchaCode|smsCode|code)\""
                    + "\\s*:\\s*)(\"(?:[^\"\\\\]|\\\\.)*\"|\\d+|true|false|null)");

    private static final int MAX_PARAMS_LENGTH = 2000;

    private final SysOperationLogMapper sysOperationLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 环绕通知，处理方法执行并记录审计日志。
     */
    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = DateUtils.currentEpochMillis();
        String action = auditable.action();
        String resourceType = auditable.resourceType();

        // 提取请求信息
        HttpServletRequest request = getCurrentRequest();
        String requestUri = request != null ? request.getRequestURI() : "";
        String requestMethod = request != null ? request.getMethod() : "";
        String clientIp = SecurityUtil.getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "";

        // 提取当前用户信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String username = "anonymous";
        if (auth != null && auth.isAuthenticated()) {
            username = auth.getName();
            try {
                userId = Long.parseLong(
                        auth.getCredentials() != null ? auth.getCredentials().toString() : "0");
            } catch (Exception e) {
                // 忽略
            }
        }

        // 从方法参数中提取目标资源 ID
        Long resourceId = extractResourceId(joinPoint.getArgs());

        Object result = null;
        String errorMsg = null;
        Integer responseStatus = 200;

        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            errorMsg = t.getMessage();
            responseStatus = 500;
            throw t;
        } finally {
            long durationMs = DateUtils.currentEpochMillis() - startTime;

            try {
                SysOperationLog operationLog = new SysOperationLog();
                operationLog.setUserId(userId);
                operationLog.setUsername(username);
                operationLog.setModule(auditable.module());
                operationLog.setAction(resolveActionCode(auditable.action()));
                operationLog.setTargetType(resourceType);
                operationLog.setTargetId(resourceId);
                operationLog.setDescription(buildDescription(action, resourceType, resourceId));
                operationLog.setRequestMethod(requestMethod);
                operationLog.setRequestUri(requestUri);
                operationLog.setRequestParams(captureParams(joinPoint.getArgs()));
                operationLog.setResponseStatus(responseStatus);
                operationLog.setClientIp(clientIp);
                operationLog.setUserAgent(userAgent);
                operationLog.setDurationMs(durationMs);
                operationLog.setErrorMsg(errorMsg);
                operationLog.setTraceId(resolveTraceId(request));

                sysOperationLogMapper.insert(operationLog);
                log.debug("审计日志已记录: action={}, resourceType={}, durationMs={}",
                        action, resourceType, durationMs);
            } catch (Exception e) {
                log.error("审计日志写入失败: action={}, resourceType={}", action, resourceType, e);
            }
        }

        return result;
    }

    /**
     * 获取当前 HTTP 请求。
     */
    private HttpServletRequest getCurrentRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .orElse(null);
    }

    /**
     * 从方法参数中提取资源 ID（第一个 Long 类型参数）。
     */
    private Long extractResourceId(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Long) {
                    return (Long) arg;
                }
            }
        }
        return null;
    }

    /**
     * 动作名称 → 动作编码。
     */
    private int resolveActionCode(String action) {
        if (action == null) {
            return 0;
        }
        return ACTION_CODES.getOrDefault(action.toUpperCase(Locale.ROOT), 0);
    }

    /**
     * 构建操作描述，如「CREATE USER id=123」。
     */
    private String buildDescription(String action, String resourceType, Long resourceId) {
        StringBuilder sb = new StringBuilder()
                .append(action == null ? "" : action)
                .append(' ')
                .append(resourceType == null ? "" : resourceType);
        if (resourceId != null) {
            sb.append(" id=").append(resourceId);
        }
        return sb.toString();
    }

    /**
     * 捕获请求参数并脱敏（排除 Servlet/Multipart/BindingResult 等非业务对象）。
     */
    private String captureParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            List<Object> businessArgs = new ArrayList<>();
            for (Object arg : args) {
                if (arg == null
                        || arg instanceof HttpServletRequest
                        || arg instanceof HttpServletResponse
                        || arg instanceof MultipartFile
                        || arg instanceof BindingResult) {
                    continue;
                }
                businessArgs.add(arg);
            }
            if (businessArgs.isEmpty()) {
                return null;
            }
            String json = objectMapper.writeValueAsString(businessArgs);
            json = SENSITIVE_FIELD_PATTERN.matcher(json)
                    .replaceAll("$1\"***\"");
            return json.length() > MAX_PARAMS_LENGTH
                    ? json.substring(0, MAX_PARAMS_LENGTH) + "..."
                    : json;
        } catch (Exception e) {
            log.debug("审计参数序列化失败（忽略）: error={}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析全链路追踪 ID：优先请求头 X-Trace-Id，其次 ScopedValueContext，最后生成。
     */
    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request != null ? request.getHeader("X-Trace-Id") : null;
        if (traceId == null || traceId.isBlank()) {
            traceId = ScopedValueContext.getTraceId();
        }
        return traceId != null && !traceId.isBlank()
                ? traceId
                : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
