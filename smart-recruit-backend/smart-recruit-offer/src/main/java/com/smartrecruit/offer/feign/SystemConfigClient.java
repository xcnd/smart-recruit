package com.smartrecruit.offer.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Map;

/**
 * 系统服务配置 HTTP 接口客户端。
 *
 * <p>供 Offer 模块按需拉取系统设置中的参数（入职模板、默认密码、
 * AI 阈值、系统名称等），保证后台修改后立即生效。</p>
 *
 * @since 2026-04-05
 */
@HttpExchange("/api/v1/configs/internal/configs")
public interface SystemConfigClient {

    /**
     * 按配置键集合拉取配置。
     *
     * @param keys 逗号分隔的配置键集合
     * @return key=configKey, value=configValue 的 Map
     */
    @GetExchange
    ApiResponse<Map<String, String>> getConfigs(@RequestParam("keys") String keys);
}
