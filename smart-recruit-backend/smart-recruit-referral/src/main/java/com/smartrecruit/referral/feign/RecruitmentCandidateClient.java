package com.smartrecruit.referral.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Map;

/**
 * 招聘服务候选人客户端（内推模块）。
 *
 * @since 2026-04-06
 */
@HttpExchange("/api/v1/candidates")
public interface RecruitmentCandidateClient {

    /** 查询候选人信息（含技能、工作年限等，用于智能匹配）。 */
    @GetExchange("/{id}")
    ApiResponse<Map<String, Object>> getCandidate(@PathVariable("id") Long id);
}
