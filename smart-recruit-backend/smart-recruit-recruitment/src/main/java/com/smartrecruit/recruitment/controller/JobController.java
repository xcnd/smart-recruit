package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.request.CreateJobRequest;
import com.smartrecruit.recruitment.dto.request.JobPageQuery;
import com.smartrecruit.recruitment.dto.request.UpdateJobRequest;
import com.smartrecruit.recruitment.dto.request.UpdateStatusRequest;
import com.smartrecruit.recruitment.dto.request.AiGenerateJdRequest;
import com.smartrecruit.recruitment.dto.response.DepartmentJobStatVO;
import com.smartrecruit.recruitment.dto.response.JobDetailVO;
import com.smartrecruit.recruitment.dto.response.JobVO;
import com.smartrecruit.recruitment.dto.response.LevelJobStatVO;
import com.smartrecruit.recruitment.dto.response.JdGenerateTaskVO;
import com.smartrecruit.recruitment.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 职位管理的REST控制器。
 *
 * <p>提供职位的CRUD接口、状态管理、统计数据和AI驱动的职位描述生成功能。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;

    /**
     * 分页查询职位列表，支持多条件筛选。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('job:view')")
    public ApiResponse<PageResult<JobVO>> list(@Valid JobPageQuery query) {
        return ApiResponse.success(jobService.pageQuery(query));
    }

    /**
     * 查询职位详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('job:view')")
    public ApiResponse<JobDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(jobService.getById(id));
    }

    /**
     * 创建职位。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('job:create')")
    public ApiResponse<JobVO> create(@Valid @RequestBody CreateJobRequest request) {
        return ApiResponse.success(jobService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('job:edit')")
    public ApiResponse<JobVO> update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateJobRequest request) {
        return ApiResponse.success(jobService.update(id, request));
    }

    /**
     * 删除职位。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('job:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('job:edit')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody UpdateStatusRequest request) {
        jobService.updateStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    /**
     * AI驱动的职位描述生成接口。
     * <p>异步启动生成任务：基于职位标题、部门、经验要求、地点与技能，
     * 调用 AI 引擎生成结构化职位描述（LLM 优先，AI 引擎不可用时降级本地模板），
     * 避免 LLM 调用耗时过长导致请求超时。前端轮询
     * {@code GET /api/v1/jobs/ai-generate-jd/{taskId}} 获取结果。</p>
     */
    @PostMapping("/ai-generate-jd")
    @PreAuthorize("hasAuthority('job:create')")
    public ApiResponse<JdGenerateTaskVO> aiGenerateJd(@RequestBody AiGenerateJdRequest request) {
        log.info("AI JD generation requested: title={}, department={}, skills={}",
                request.getTitle(), request.getDepartment(), request.getSkills());
        return ApiResponse.success(jobService.getJdGenerateTask(
                jobService.generateJdAsync(request)));
    }

    /**
     * 查询 AI 生成 JD 异步任务状态与结果。
     *
     * @param taskId 任务 ID
     * @return 任务状态与生成结果
     */
    @GetMapping("/ai-generate-jd/{taskId}")
    @PreAuthorize("hasAuthority('job:create')")
    public ApiResponse<JdGenerateTaskVO> aiGenerateJdTask(@PathVariable String taskId) {
        return ApiResponse.success(jobService.getJdGenerateTask(taskId));
    }

    /**
     * 按部门统计职位数量。
     */
    @GetMapping("/stats/department")
    @PreAuthorize("hasAuthority('job:view')")
    public ApiResponse<List<DepartmentJobStatVO>> statsByDepartment() {
        return ApiResponse.success(jobService.getStatsByDepartment());
    }

    /**
     * 按职级统计职位数量。
     */
    @GetMapping("/stats/level")
    @PreAuthorize("hasAuthority('job:view')")
    public ApiResponse<List<LevelJobStatVO>> statsByLevel() {
        return ApiResponse.success(jobService.getStatsByLevel());
    }

}
