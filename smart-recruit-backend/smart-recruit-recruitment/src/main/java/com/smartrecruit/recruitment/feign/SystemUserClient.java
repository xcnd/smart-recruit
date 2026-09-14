package com.smartrecruit.recruitment.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Map;

/**
 * 调用 smart-recruit-system 用户服务。
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1/users")
public interface SystemUserClient {

    @GetExchange("/{id}")
    ApiResponse<Map<String, Object>> getUserById(@PathVariable Long id);
}
