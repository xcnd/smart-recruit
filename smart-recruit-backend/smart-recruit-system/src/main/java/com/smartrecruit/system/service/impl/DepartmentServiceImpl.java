package com.smartrecruit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.constant.Constants;
import com.smartrecruit.common.exception.DuplicateResourceException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.exception.ValidationException;
import com.smartrecruit.system.audit.Auditable;
import com.smartrecruit.system.converter.DepartmentConverter;
import com.smartrecruit.system.dto.request.CreateDeptRequest;
import com.smartrecruit.system.dto.request.UpdateDeptRequest;
import com.smartrecruit.system.dto.response.DepartmentTreeVO;
import com.smartrecruit.system.dto.response.DepartmentVO;
import com.smartrecruit.system.entity.SysDepartment;
import com.smartrecruit.system.entity.SysUser;
import com.smartrecruit.system.repository.SysDepartmentMapper;
import com.smartrecruit.system.repository.SysUserMapper;
import com.smartrecruit.system.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门管理服务实现类。
 *
 * @since 2026-04-26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final SysDepartmentMapper sysDepartmentMapper;
    private final SysUserMapper sysUserMapper;
    private final DepartmentConverter departmentConverter;

    /** 查询部门树形结构。 */
    @Override
    public List<DepartmentTreeVO> listTree() {
        log.info("查询部门树形结构");

        LambdaQueryWrapper<SysDepartment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartment::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(SysDepartment::getSortOrder);
        List<SysDepartment> allDepts = sysDepartmentMapper.selectList(wrapper);

        // Build tree
        Map<Long, List<SysDepartment>> parentMap = allDepts.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() != null ? d.getParentId() : 0L));

        List<DepartmentTreeVO> tree = buildTree(0L, parentMap);
        log.info("部门树查询完成, 根节点数: {}", tree.size());
        return tree;
    }

    private List<DepartmentTreeVO> buildTree(Long parentId, Map<Long, List<SysDepartment>> parentMap) {
        List<SysDepartment> children = parentMap.getOrDefault(parentId, List.of());
        List<DepartmentTreeVO> result = new ArrayList<>();
        for (SysDepartment dept : children) {
            DepartmentTreeVO vo = departmentConverter.toTreeVO(dept);
            List<DepartmentTreeVO> childNodes = buildTree(dept.getId(), parentMap);
            result.add(new DepartmentTreeVO(
                    vo.id(), vo.name(), vo.code(), vo.parentId(),
                    vo.leaderId(), vo.sortOrder(), vo.status(), vo.remark(),
                    childNodes.isEmpty() ? List.of() : childNodes));
        }
        return result;
    }

    /** 根据主键查询详情。 */
    @Override
    public DepartmentVO getById(Long id) {
        log.info("查询部门详情: id={}", id);

        SysDepartment dept = sysDepartmentMapper.selectById(id);
        if (dept == null) {
            throw new ResourceNotFoundException("部门不存在: id=" + id);
        }

        return departmentConverter.toVO(dept);
    }

    /** 创建记录。 */
    @Override
    @Auditable(action = "CREATE", resourceType = "DEPT", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO create(CreateDeptRequest request) {
        log.info("创建部门: name={}, code={}", request.name(), request.code());

        // Check code uniqueness
        LambdaQueryWrapper<SysDepartment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartment::getCode, request.code());
        if (sysDepartmentMapper.selectCount(wrapper) > 0) {
            throw new DuplicateResourceException("部门", "code", request.code());
        }

        SysDepartment dept = departmentConverter.toEntity(request);
        dept.setParentId(request.parentId() != null ? request.parentId() : 0L);
        dept.setLeaderId(request.leaderId());
        dept.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        dept.setDescription(request.remark());
        dept.setStatus(Constants.STATUS_ENABLED);
        sysDepartmentMapper.insert(dept);

        log.info("部门创建成功: deptId={}, name={}", dept.getId(), dept.getName());
        return departmentConverter.toVO(dept);
    }

    /** 更新记录。 */
    @Override
    @Auditable(action = "UPDATE", resourceType = "DEPT", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO update(Long id, UpdateDeptRequest request) {
        log.info("更新部门: id={}", id);

        SysDepartment dept = sysDepartmentMapper.selectById(id);
        if (dept == null) {
            throw new ResourceNotFoundException("部门不存在: id=" + id);
        }

        if (request.name() != null) {
            dept.setName(request.name());
        }
        if (request.parentId() != null) {
            dept.setParentId(request.parentId());
        }
        if (request.leaderId() != null) {
            dept.setLeaderId(request.leaderId());
        }
        if (request.sortOrder() != null) {
            dept.setSortOrder(request.sortOrder());
        }
        if (request.remark() != null) {
            dept.setDescription(request.remark());
        }

        sysDepartmentMapper.updateById(dept);

        log.info("部门更新成功: deptId={}", id);
        return departmentConverter.toVO(dept);
    }

    /** 根据主键删除记录。 */
    @Override
    @Auditable(action = "DELETE", resourceType = "DEPT", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("删除部门: id={}", id);

        SysDepartment dept = sysDepartmentMapper.selectById(id);
        if (dept == null) {
            throw new ResourceNotFoundException("部门不存在: id=" + id);
        }

        // Check for child departments
        LambdaQueryWrapper<SysDepartment> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(SysDepartment::getParentId, id);
        if (sysDepartmentMapper.selectCount(childWrapper) > 0) {
            throw new ValidationException("该部门下存在子部门，无法删除");
        }

        // Check for users in department
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getDeptId, id);
        if (sysUserMapper.selectCount(userWrapper) > 0) {
            throw new ValidationException("该部门下存在用户，无法删除");
        }

        // Logical delete
        sysDepartmentMapper.deleteById(id);

        log.info("部门删除成功: deptId={}", id);
    }
}
