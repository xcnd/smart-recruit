package com.smartrecruit.offer.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.offer.dto.request.CreateFlowConfigRequest;
import com.smartrecruit.offer.dto.request.UpdateFlowConfigRequest;
import com.smartrecruit.offer.dto.response.ApprovalFlowConfigVO;
import com.smartrecruit.offer.service.ApprovalFlowConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批流程配置 REST 控制器。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/offers/flow-configs")
@RequiredArgsConstructor
@Slf4j
public class ApprovalFlowConfigController {

    private final ApprovalFlowConfigService flowConfigService;

    /**
     * 查询全部审批流程配置列表。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<List<ApprovalFlowConfigVO>> list() {
        List<ApprovalFlowConfigVO> configs = flowConfigService.listAll();
        return ApiResponse.success(configs);
    }

    /**
     * 查询审批流程配置详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<ApprovalFlowConfigVO> detail(@PathVariable Long id) {
        ApprovalFlowConfigVO vo = flowConfigService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 按部门名称查询审批流程配置。
     */
    @GetMapping("/department/{departmentName}")
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<ApprovalFlowConfigVO> byDepartment(@PathVariable String departmentName) {
        ApprovalFlowConfigVO vo = flowConfigService.getByDepartment(departmentName);
        return ApiResponse.success(vo);
    }

    /**
     * 创建审批流程配置。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<ApprovalFlowConfigVO> create(@Valid @RequestBody CreateFlowConfigRequest request) {
        log.info("创建审批流程配置: departmentName={}, levels={}", request.getDepartmentName(), request.getNodes().size());
        ApprovalFlowConfigVO vo = flowConfigService.create(request);
        return ApiResponse.success(vo);
    }

    /**
     * 更新审批流程配置。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<ApprovalFlowConfigVO> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateFlowConfigRequest request) {
        log.info("更新审批流程配置: id={}", id);
        ApprovalFlowConfigVO vo = flowConfigService.update(id, request);
        return ApiResponse.success(vo);
    }

    /**
     * 删除审批流程配置。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除审批流程配置: id={}", id);
        flowConfigService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 启用/停用审批流程配置。
     */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<Void> toggleActive(@PathVariable Long id) {
        log.info("切换审批流程配置状态: id={}", id);
        flowConfigService.toggleActive(id);
        return ApiResponse.success();
    }
}
