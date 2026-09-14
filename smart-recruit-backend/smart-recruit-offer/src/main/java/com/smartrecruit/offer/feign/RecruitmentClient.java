package com.smartrecruit.offer.feign;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.offer.dto.remote.ApplicationDTO;
import com.smartrecruit.offer.dto.remote.CandidateProfileDTO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

/**
 * 招聘服务 HTTP 接口客户端，用于获取求职申请信息和写入活动动态。
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1")
public interface RecruitmentClient {

    /**
     * 根据候选人和职位查询求职申请记录。
     *
     * @param candidateId 候选人 ID
     * @param jobId       职位 ID
     * @return 申请记录，不存在时返回 {@code data=null}
     */
    @GetExchange("/applications")
    ApiResponse<ApplicationDTO> findApplication(@RequestParam Long candidateId,
                                                 @RequestParam Long jobId);

    /**
     * 查询候选人画像（学历/年限/来源等，供 AI 留任预测使用）。
     */
    @GetExchange("/candidates/{id}")
    ApiResponse<CandidateProfileDTO> getCandidate(@PathVariable("id") Long id);

    /** 写入活动动态（内部接口）。 */
    @PostExchange("/internal/activity-feed")
    ApiResponse<Void> recordActivity(@RequestBody com.smartrecruit.offer.dto.remote.ActivityRecordRequest body);

    /** 创建待办任务（内部接口）。 */
    @PostExchange("/internal/workbench/tasks")
    ApiResponse<Void> createTask(@RequestBody com.smartrecruit.offer.dto.remote.WorkbenchTaskRequest body);
}
