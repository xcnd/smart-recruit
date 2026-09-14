package com.smartrecruit.system.service;

import com.smartrecruit.system.dto.request.CreateDeptRequest;
import com.smartrecruit.system.dto.request.UpdateDeptRequest;
import com.smartrecruit.system.dto.response.DepartmentTreeVO;
import com.smartrecruit.system.dto.response.DepartmentVO;

import java.util.List;

/**
 * 部门管理服务接口。
 *
 * @since 2026-04-26
 */
public interface DepartmentService {

    /**
     * 查询部门树形结构。
     */
    List<DepartmentTreeVO> listTree();

    /**
     * 根据 ID 查询部门详情。
     */
    DepartmentVO getById(Long id);

    /**
     * 创建部门。
     */
    DepartmentVO create(CreateDeptRequest request);

    /**
     * 更新部门信息。
     */
    DepartmentVO update(Long id, UpdateDeptRequest request);

    /**
     * 删除部门（检查无子部门、无用户）。
     */
    void delete(Long id);
}
