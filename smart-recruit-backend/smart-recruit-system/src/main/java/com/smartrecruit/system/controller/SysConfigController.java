package com.smartrecruit.system.controller;

import com.smartrecruit.common.constant.ConfigKeys;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.system.dto.request.ConfigItemRequest;
import com.smartrecruit.system.dto.response.SysConfigVO;
import com.smartrecruit.system.service.SysConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 系统配置管理控制器。
 *
 * @since 2026-05-26
 */
@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
@Slf4j
public class SysConfigController {

    private final SysConfigService sysConfigService;

    /**
     * 公开端点：获取管理后台所需的全部公开配置（无需认证）。
     *
     * <p>返回系统名称、版权信息、密码策略、上传限制、入职配置、
     * AI 阈值等公开参数，前端加载后即时生效。</p>
     */
    @GetMapping("/public")
    public ApiResponse<Map<String, String>> getPublicConfigs() {
        return ApiResponse.success(sysConfigService.getPublicConfigs());
    }

    /**
     * 公开端点：获取系统邮箱后缀（无需认证，注册/找回密码/入职页面使用）。
     */
    @GetMapping("/public/email-suffix")
    public ApiResponse<String> getEmailSuffix() {
        String suffix = sysConfigService.getString(ConfigKeys.EMAIL_SUFFIX, "@company.com");
        return ApiResponse.success(suffix);
    }

    /**
     * 公开端点：获取招聘官网所有配置（无需认证，官网前端页面使用）。
     * 以 configKey → configValue 的 Map 形式返回所有 careers_* 前缀的配置项。
     */
    @GetMapping("/public/careers")
    public ApiResponse<Map<String, String>> getCareersConfig() {
        Map<String, String> configs = sysConfigService.getByPrefix(ConfigKeys.CAREERS_PREFIX);
        return ApiResponse.success(configs);
    }

    /**
     * 内部端点：供其他微服务按需获取系统配置（仅限服务间调用）。
     *
     * @param keys   逗号分隔的配置键集合（与 prefix 二选一）
     * @param prefix 配置键前缀（如 onboarding_）
     */
    @GetMapping("/internal/configs")
    @PreAuthorize("hasRole('SYSTEM')")
    public ApiResponse<Map<String, String>> getInternalConfigs(
            @RequestParam(value = "keys", required = false) String keys,
            @RequestParam(value = "prefix", required = false) String prefix) {
        if (prefix != null && !prefix.isBlank()) {
            return ApiResponse.success(sysConfigService.getByPrefix(prefix));
        }
        if (keys == null || keys.isBlank()) {
            return ApiResponse.success(Map.of());
        }
        Collection<String> keyList = new LinkedHashSet<>(
                Arrays.stream(keys.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        return ApiResponse.success(sysConfigService.getByKeys(keyList));
    }

    /**
     * 查询所有配置项。
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:settings', 'ROLE_300001')")
    public ApiResponse<List<SysConfigVO>> listAll() {
        List<SysConfigVO> configs = sysConfigService.listAll();
        return ApiResponse.success(configs);
    }

    /**
     * 批量更新配置项。
     */
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:settings:edit', 'ROLE_300001')")
    public ApiResponse<List<SysConfigVO>> batchUpdate(@Valid @RequestBody List<ConfigItemRequest> items) {
        List<SysConfigVO> configs = sysConfigService.batchUpdate(items);
        return ApiResponse.success(configs);
    }
}
