package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.response.PendingTaskVO;
import com.smartrecruit.recruitment.service.WorkbenchTaskService;
import com.smartrecruit.recruitment.dto.request.CreateWorkbenchTaskRequest;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作台待办任务控制器。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class WorkbenchTaskController {

    private final WorkbenchTaskService workbenchTaskService;

    /** 查询待办任务。 */
    @GetMapping("/workbench/pending")
    public ApiResponse<List<PendingTaskVO>> getPending(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(workbenchTaskService.getPendingTasks(userId, Math.min(limit, 200)));
    }

    /** 完成待办。 */
    @PutMapping("/workbench/pending/{id}/complete")
    public ApiResponse<Void> completeTask(@PathVariable Long id) {
        workbenchTaskService.completeTask(id);
        return ApiResponse.success(null);
    }

    /** 忽略待办。 */
    @PutMapping("/workbench/pending/{id}/dismiss")
    public ApiResponse<Void> dismissTask(@PathVariable Long id) {
        workbenchTaskService.dismissTask(id);
        return ApiResponse.success(null);
    }

    /** 分页查询待办任务（带过滤条件）。 */
    @GetMapping("/workbench/tasks")
    public ApiResponse<PageResult<PendingTaskVO>> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(workbenchTaskService.pageQuery(
                page, Math.min(size, 100), userId, type, priority, status, keyword));
    }

    /** 内部接口：创建待办任务。 */
    @PostMapping("/internal/workbench/tasks")
    public ApiResponse<Void> createTask(@RequestBody CreateWorkbenchTaskRequest request) {
        workbenchTaskService.createTask(
                request.getUserId() != null ? request.getUserId() : 0L,
                request.getTitle() != null ? request.getTitle() : "",
                request.getDescription(),
                request.getType() != null ? request.getType() : 5,
                request.getPriority() != null ? request.getPriority() : 1,
                request.getRelatedType(),
                request.getRelatedId(),
                request.getCandidateName(),
                request.getDueDate());
        return ApiResponse.success(null);
    }
}
