package com.smartrecruit.interview.feign;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.interview.dto.remote.CandidateDTO;
import com.smartrecruit.interview.dto.remote.JobDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.Map;

/**
 * 招聘服务 HTTP 接口客户端，用于获取候选人、职位信息及更新候选人状态。
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1")
public interface RecruitmentClient {

    @GetExchange("/candidates/{id}")
    ApiResponse<CandidateDTO> getCandidate(@PathVariable("id") Long id);

    @GetExchange("/jobs/{id}")
    ApiResponse<JobDTO> getJob(@PathVariable("id") Long id);

    @PutExchange("/candidates/{id}/stage")
    ApiResponse<Void> updateCandidateStage(@PathVariable("id") Long id,
                                           @RequestBody com.smartrecruit.interview.dto.remote.UpdateCandidateStageRequest body);

    /** 写入活动动态（内部接口）。 */
    @PostExchange("/internal/activity-feed")
    ApiResponse<Void> recordActivity(@RequestBody com.smartrecruit.interview.dto.remote.ActivityRecordRequest body);

    /** 创建待办任务（内部接口）。 */
    @PostExchange("/internal/workbench/tasks")
    ApiResponse<Void> createTask(@RequestBody com.smartrecruit.interview.dto.remote.WorkbenchTaskRequest body);
}
