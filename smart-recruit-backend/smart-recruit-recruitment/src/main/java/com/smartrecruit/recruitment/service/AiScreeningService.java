package com.smartrecruit.recruitment.service;

import com.smartrecruit.recruitment.dto.response.AiScreeningResultVO;
import com.smartrecruit.recruitment.dto.response.BatchProgressVO;

import java.util.List;

/**
 * AI 简历筛选服务接口。
 *
 * <p>支持 LLM 和启发式两种评分引擎，
 * LLM 不可用时自动降级为启发式评分。</p>
 *
 * @since 1.1.0
 */
public interface AiScreeningService {

    /**
     * 异步单份简历筛选（解析成功后自动触发）。
     *
     * @param resumeId 简历 ID
     */
    void screenAsync(Long resumeId);

    /**
     * 异步批量简历筛选。
     *
     * @param resumeIds 简历 ID 列表
     * @param jobId     可选，职位 ID 用于上下文评分
     * @return 批量任务 ID（用于进度查询）
     */
    String batchScreenAsync(List<Long> resumeIds, Long jobId);

    /**
     * 获取简历的 AI 筛选结果。
     *
     * @param resumeId 简历 ID
     * @return 筛选结果 VO，无结果时返回 null
     */
    AiScreeningResultVO getResult(Long resumeId);

    /**
     * 查询批量筛选进度。
     *
     * @param taskId 批量任务 ID
     * @return 进度视图对象
     */
    BatchProgressVO getBatchProgress(String taskId);
}
