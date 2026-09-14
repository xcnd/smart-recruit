package com.smartrecruit.system.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.system.dto.request.CreateDeptRequest;
import com.smartrecruit.system.dto.request.UpdateDeptRequest;
import com.smartrecruit.system.dto.response.DepartmentTreeVO;
import com.smartrecruit.system.dto.response.DepartmentVO;
import com.smartrecruit.system.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器。
 *
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 查询部门树形结构。
     */
    @GetMapping
    public ApiResponse<List<DepartmentTreeVO>> tree() {
        List<DepartmentTreeVO> tree = departmentService.listTree();
        return ApiResponse.success(tree);
    }

    /**
     * 查询部门详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<DepartmentVO> detail(@PathVariable Long id) {
        DepartmentVO vo = departmentService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 创建部门。
     */
    @PostMapping
    public ApiResponse<DepartmentVO> create(@Valid @RequestBody CreateDeptRequest request) {
        DepartmentVO vo = departmentService.create(request);
        return ApiResponse.success(vo);
    }

    /**
     * 更新部门。
     */
    @PutMapping("/{id}")
    public ApiResponse<DepartmentVO> update(@PathVariable Long id,
                                             @Valid @RequestBody UpdateDeptRequest request) {
        DepartmentVO vo = departmentService.update(id, request);
        return ApiResponse.success(vo);
    }

    /**
     * 删除部门。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success();
    }
}
