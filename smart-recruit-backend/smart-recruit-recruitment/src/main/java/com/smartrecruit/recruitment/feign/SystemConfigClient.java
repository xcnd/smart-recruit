package com.smartrecruit.recruitment.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Map;

/**
 * 系统服务配置读取 HTTP 接口客户端。
 *
 * <p>供招聘模块按需读取系统配置（如 AI 简历筛选维度权重、最低通过分数线），
 * 配置在系统设置页修改后立即生效。</p>
 *
 * @since 2026-04-09
 */
@HttpExchange("/api/v1/configs/internal/configs")
public interface SystemConfigClient {

    /**
     * 按配置键批量读取配置（逗号分隔）。
     *
     * @param keys 配置键列表，如 "ai_screen_weight_skill,ai_screen_pass_score"
     * @return configKey → configValue
     */
    @GetExchange
    ApiResponse<Map<String, String>> getByKeys(@RequestParam("keys") String keys);
}
