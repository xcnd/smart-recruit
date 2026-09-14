package com.smartrecruit.referral.service;

import com.smartrecruit.referral.dto.response.ReferralMatchVO;

import java.util.List;

/**
 * 内推智能匹配服务接口。
 *
 * @since 2026-04-06
 */
public interface ReferralMatchService {

    /**
     * 查询内推记录已保存的匹配推荐。
     */
    List<ReferralMatchVO> listMatches(Long recordId);

    /**
     * 触发重新匹配：调用 AI 引擎 Agent 能力网关，失败降级本地启发式，
     * 结果按匹配度保存 Top 3 并返回。
     */
    List<ReferralMatchVO> refreshMatches(Long recordId);
}
