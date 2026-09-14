package com.smartrecruit.system.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.system.dto.request.AuditLogPageQuery;
import com.smartrecruit.system.dto.response.AuditLogDetailVO;
import com.smartrecruit.system.dto.response.AuditLogStatsVO;
import com.smartrecruit.system.dto.response.AuditLogVO;
import com.smartrecruit.system.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询控制器。
 *
 * <p>提供分页查询、统计和详情查看能力，仅对拥有
 * {@code system:log} 权限的用户（系统管理员）开放。</p>
 *
 * @since 2026-04-08
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 分页查询审计日志（支持操作人/模块/动作/结果/时间范围/关键字筛选）。
     *
     * @param query 分页与筛选条件
     * @return 审计日志分页结果
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:log', 'ROLE_300001')")
    public ApiResponse<PageResult<AuditLogVO>> pageQuery(@Valid AuditLogPageQuery query) {
        log.info("分页查询审计日志: username={}, module={}, action={}, result={}, startDate={}, endDate={}",
                query.username(), query.module(), query.action(), query.result(),
                query.startDate(), query.endDate());
        return ApiResponse.success(auditLogService.pageQuery(query));
    }

    /**
     * 查询审计日志统计概览（总量/今日/成功/失败/成功率/平均耗时）。
     *
     * @return 统计结果
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('system:log', 'ROLE_300001')")
    public ApiResponse<AuditLogStatsVO> stats() {
        log.info("查询审计日志统计");
        return ApiResponse.success(auditLogService.getStats());
    }

    /**
     * 查询审计日志详情（含请求参数、User-Agent、链路 ID）。
     *
     * @param id 日志主键
     * @return 详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:log', 'ROLE_300001')")
    public ApiResponse<AuditLogDetailVO> detail(@PathVariable Long id) {
        log.info("查询审计日志详情: id={}", id);
        return ApiResponse.success(auditLogService.getDetail(id));
    }
}
