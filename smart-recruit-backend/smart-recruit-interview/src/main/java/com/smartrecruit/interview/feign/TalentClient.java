package com.smartrecruit.interview.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

/**
 * 人才库服务 HTTP 接口客户端，用于将候选人加入人才库。
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1/talent-pool")
public interface TalentClient {

    @PostExchange
    ApiResponse<Map<String, Object>> addToPool(@RequestBody com.smartrecruit.interview.dto.remote.AddToTalentPoolRequest body);
}
