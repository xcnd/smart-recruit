package com.smartrecruit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.constant.Constants;
import com.smartrecruit.common.exception.DuplicateResourceException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.system.audit.Auditable;
import com.smartrecruit.system.converter.PermissionConverter;
import com.smartrecruit.system.dto.request.CreatePermissionRequest;
import com.smartrecruit.system.dto.request.UpdatePermissionRequest;
import com.smartrecruit.system.dto.response.PermissionTreeVO;
import com.smartrecruit.system.dto.response.PermissionVO;
import com.smartrecruit.system.dto.response.PermissionGroupVO;
import com.smartrecruit.system.entity.SysPermission;
import com.smartrecruit.system.entity.SysRole;
import com.smartrecruit.system.entity.SysRolePermission;
import com.smartrecruit.system.repository.SysPermissionMapper;
import com.smartrecruit.system.repository.SysRoleMapper;
import com.smartrecruit.system.repository.SysRolePermissionMapper;
import com.smartrecruit.system.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限管理服务实现类。
 *
 * @since 2026-04-26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    /**
     * 权限名称和编码的兜底映射 —— 当数据库 perm_name/perm_code 为 null 时回退使用。
     */
    private static final Map<Long, String[]> PERM_FALLBACK = Map.ofEntries(
            // Level 1: Top-level Menus
            Map.entry(400001L, arr("工作台", "dashboard")),
            Map.entry(400002L, arr("招聘管理", "recruitment")),
            Map.entry(400003L, arr("人才库", "talent")),
            Map.entry(400004L, arr("内推管理", "referral")),
            Map.entry(400005L, arr("入职管理", "onboarding")),
            Map.entry(400006L, arr("AI引擎", "ai")),
            Map.entry(400007L, arr("数据分析", "analytics")),
            Map.entry(400008L, arr("系统管理", "system")),
            // Level 2: Recruitment sub-menus
            Map.entry(400101L, arr("职位管理", "recruitment:job")),
            Map.entry(400102L, arr("简历筛选", "recruitment:resume")),
            Map.entry(400103L, arr("面试管理", "recruitment:interview")),
            Map.entry(400104L, arr("Offer管理", "recruitment:offer")),
            // Level 2: System sub-menus
            Map.entry(400801L, arr("用户管理", "system:user")),
            Map.entry(400802L, arr("角色管理", "system:role")),
            Map.entry(400803L, arr("权限管理", "system:permission")),
            Map.entry(400804L, arr("部门管理", "system:dept")),
            Map.entry(400805L, arr("操作日志", "system:log")),
            // Level 3: Button - Job
            Map.entry(401101L, arr("创建职位", "job:create")),
            Map.entry(401102L, arr("编辑职位", "job:edit")),
            Map.entry(401103L, arr("删除职位", "job:delete")),
            Map.entry(401104L, arr("发布职位", "job:publish")),
            Map.entry(401105L, arr("关闭职位", "job:close")),
            Map.entry(401106L, arr("查看职位", "job:view")),
            // Level 3: Button - Candidate/Resume
            Map.entry(401201L, arr("查看候选人", "candidate:view")),
            Map.entry(401202L, arr("编辑候选人", "candidate:edit")),
            Map.entry(401203L, arr("导入简历", "resume:import")),
            Map.entry(401204L, arr("解析简历", "resume:parse")),
            Map.entry(401205L, arr("筛选简历", "resume:screen")),
            Map.entry(401517L, arr("上传简历", "resume:upload")),
            Map.entry(401518L, arr("查看简历", "resume:view")),
            // Level 3: Button - Interview
            Map.entry(401301L, arr("安排面试", "interview:schedule")),
            Map.entry(401302L, arr("取消面试", "interview:cancel")),
            Map.entry(401303L, arr("提交反馈", "interview:feedback")),
            Map.entry(401304L, arr("查看反馈", "interview:view_fb")),
            Map.entry(401519L, arr("查看面试", "interview:view")),
            Map.entry(401520L, arr("创建面试", "interview:create")),
            Map.entry(401521L, arr("编辑面试", "interview:edit")),
            // Level 3: Button - Offer
            Map.entry(401401L, arr("创建Offer", "offer:create")),
            Map.entry(401402L, arr("审批Offer", "offer:approve")),
            Map.entry(401403L, arr("发送Offer", "offer:send")),
            Map.entry(401404L, arr("查看Offer", "offer:view")),
            Map.entry(401522L, arr("编辑Offer", "offer:edit")),
            // Level 3: Button - System: User
            Map.entry(401501L, arr("查看用户", "user:view")),
            Map.entry(401502L, arr("创建用户", "user:create")),
            Map.entry(401503L, arr("编辑用户", "user:edit")),
            Map.entry(401504L, arr("删除用户", "user:delete")),
            // Level 3: Button - System: Role
            Map.entry(401505L, arr("查看角色", "role:view")),
            Map.entry(401506L, arr("创建角色", "role:create")),
            Map.entry(401507L, arr("编辑角色", "role:edit")),
            Map.entry(401508L, arr("删除角色", "role:delete")),
            // Level 3: Button - System: Permission
            Map.entry(401509L, arr("查看权限", "perm:view")),
            Map.entry(401510L, arr("编辑权限", "perm:edit")),
            // Level 3: Button - System: Department
            Map.entry(401511L, arr("查看部门", "dept:view")),
            Map.entry(401512L, arr("创建部门", "dept:create")),
            Map.entry(401513L, arr("编辑部门", "dept:edit")),
            Map.entry(401514L, arr("删除部门", "dept:delete")),
            // Level 3: Button - Notifications
            Map.entry(401515L, arr("查看通知", "notification:view")),
            Map.entry(401516L, arr("管理通知", "notification:edit")),
            // Level 3: Button - Dashboard
            Map.entry(401523L, arr("查看工作台", "dashboard:view")),
            // Level 3: Button - Analytics
            Map.entry(401524L, arr("查看分析", "analytics:view")),
            Map.entry(401525L, arr("导出分析", "analytics:export")),
            // Level 3: Button - Talent
            Map.entry(401526L, arr("查看人才", "talent:view")),
            Map.entry(401527L, arr("编辑人才", "talent:edit")),
            // Level 3: Button - Referral
            Map.entry(401528L, arr("查看内推", "referral:view")),
            Map.entry(401529L, arr("创建内推", "referral:create")),
            Map.entry(401530L, arr("编辑内推", "referral:edit")),
            // Level 3: Button - Onboarding
            Map.entry(401531L, arr("查看入职", "onboarding:view")),
            Map.entry(401532L, arr("编辑入职", "onboarding:edit")),
            // Level 3: Button - AI
            Map.entry(401533L, arr("查看Agent", "agent:view")),
            Map.entry(401534L, arr("管理Agent", "agent:manage"))
    );

    private static String[] arr(String name, String code) {
        return new String[]{name, code};
    }

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final PermissionConverter permissionConverter;

    /** 查询按模块分组的权限列表。 */
    @Override
    public List<PermissionGroupVO> listAllGrouped() {
        log.info("查询所有权限（按模块分组）");

        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(SysPermission::getSortOrder);
        List<SysPermission> permissions = sysPermissionMapper.selectList(wrapper);

        // Group by module (derived from code prefix)
        Map<String, List<PermissionVO>> grouped = new LinkedHashMap<>();
        for (SysPermission perm : permissions) {
            String module = extractModule(perm);
            PermissionVO vo = buildPermissionVO(perm, 0);
            grouped.computeIfAbsent(module, k -> new ArrayList<>()).add(vo);
        }

        log.info("权限分组完成, 模块数: {}", grouped.size());
        return grouped.entrySet().stream()
                .map(e -> new PermissionGroupVO(e.getKey(), e.getValue()))
                .toList();
    }

    /** 查询权限树形结构。 */
    @Override
    public List<PermissionTreeVO> listAllTree() {
        log.info("查询所有权限（树形结构）");

        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(SysPermission::getSortOrder);
        List<SysPermission> all = sysPermissionMapper.selectList(wrapper);

        // Group by parentId
        Map<Long, List<SysPermission>> childrenMap = all.stream()
                .filter(p -> p.getParentId() != null && p.getParentId() != 0)
                .collect(Collectors.groupingBy(SysPermission::getParentId));

        // Build top-level nodes with recursive children
        List<PermissionTreeVO> tree = all.stream()
                .filter(p -> p.getParentId() == null || p.getParentId() == 0)
                .map(p -> toTreeVO(p, childrenMap))
                .toList();

        log.info("权限树构建完成, 顶层节点数: {}", tree.size());
        return tree;
    }

    /** 根据主键查询详情。 */
    @Override
    public PermissionVO getById(Long id) {
        log.info("查询权限详情: id={}", id);

        SysPermission perm = sysPermissionMapper.selectById(id);
        if (perm == null) {
            throw new ResourceNotFoundException("权限不存在: id=" + id);
        }

        return buildPermissionVO(perm, perm.getModule());
    }

    /** 根据角色 ID 查询权限列表。 */
    @Override
    public List<PermissionVO> getByRoleId(Long roleId) {
        log.info("查询角色权限: roleId={}", roleId);

        List<SysPermission> permissions = sysPermissionMapper.findByRoleId(roleId);
        return permissions.stream()
                .map(p -> buildPermissionVO(p, 0))
                .toList();
    }

    /** 创建记录。 */
    @Override
    @Auditable(action = "CREATE", resourceType = "PERMISSION", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public PermissionVO create(CreatePermissionRequest request) {
        log.info("创建权限: name={}, code={}", request.name(), request.code());

        // Check code uniqueness
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getCode, request.code());
        if (sysPermissionMapper.selectCount(wrapper) > 0) {
            throw new DuplicateResourceException("权限", "code", request.code());
        }

        SysPermission perm = permissionConverter.toEntity(request);
        if (perm.getStatus() == null) {
            perm.setStatus(Constants.STATUS_ENABLED);
        }
        if (perm.getVisible() == null) {
            perm.setVisible(1);
        }
        if (perm.getParentId() == null) {
            perm.setParentId(0L);
        }
        if (perm.getSortOrder() == null) {
            perm.setSortOrder(0);
        }
        sysPermissionMapper.insert(perm);

        log.info("权限创建成功: permId={}, name={}", perm.getId(), perm.getName());
        return buildPermissionVO(perm, perm.getModule());
    }

    /** 更新记录。 */
    @Override
    @Auditable(action = "UPDATE", resourceType = "PERMISSION", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public PermissionVO update(Long id, UpdatePermissionRequest request) {
        log.info("更新权限: id={}", id);

        SysPermission perm = sysPermissionMapper.selectById(id);
        if (perm == null) {
            throw new ResourceNotFoundException("权限不存在: id=" + id);
        }

        // Partial update: only set non-null fields
        if (request.name() != null) perm.setName(request.name());
        if (request.code() != null) perm.setCode(request.code());
        if (request.permType() != null) perm.setPermType(request.permType());
        if (request.module() != null) perm.setModule(request.module());
        if (request.parentId() != null) perm.setParentId(request.parentId());
        if (request.path() != null) perm.setPath(request.path());
        if (request.component() != null) perm.setComponent(request.component());
        if (request.icon() != null) perm.setIcon(request.icon());
        if (request.method() != null) perm.setMethod(request.method());
        if (request.apiPath() != null) perm.setApiPath(request.apiPath());
        if (request.sortOrder() != null) perm.setSortOrder(request.sortOrder());
        if (request.status() != null) perm.setStatus(request.status());
        if (request.visible() != null) perm.setVisible(request.visible());

        sysPermissionMapper.updateById(perm);

        log.info("权限更新成功: permId={}", id);
        return buildPermissionVO(perm, perm.getModule());
    }

    /** 根据主键删除记录。 */
    @Override
    @Auditable(action = "DELETE", resourceType = "PERMISSION", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("删除权限: id={}", id);

        SysPermission perm = sysPermissionMapper.selectById(id);
        if (perm == null) {
            throw new ResourceNotFoundException("权限不存在: id=" + id);
        }

        // Logical delete
        sysPermissionMapper.deleteById(id);

        // Clean up role-permission associations
        sysRolePermissionMapper.deleteByPermissionId(id);

        log.info("权限删除成功: permId={}", id);
    }

    /** 更新角色权限关系。 */
    @Override
    @Auditable(action = "UPDATE", resourceType = "ROLE_PERMISSION", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public void updateRolePermissions(Long roleId, List<Long> permIds) {
        log.info("更新角色权限: roleId={}, permCount={}", roleId, permIds != null ? permIds.size() : 0);

        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new ResourceNotFoundException("角色不存在: id=" + roleId);
        }

        // Delete existing permissions
        sysRolePermissionMapper.deleteByRoleId(roleId);

        // Insert new permissions
        if (permIds != null && !permIds.isEmpty()) {
            List<SysRolePermission> rolePermissions = permIds.stream()
                    .map(permId -> {
                        SysRolePermission rp = new SysRolePermission();
                        rp.setRoleId(roleId);
                        rp.setPermissionId(permId);
                        return rp;
                    })
                    .toList();
            for (SysRolePermission rp : rolePermissions) {
                sysRolePermissionMapper.insert(rp);
            }
        }

        log.info("角色权限更新成功: roleId={}, permCount={}", roleId, permIds != null ? permIds.size() : 0);
    }

    /**
     * 递归构建树节点。
     */
    private PermissionTreeVO toTreeVO(SysPermission entity, Map<Long, List<SysPermission>> childrenMap) {
        List<PermissionTreeVO> children = childrenMap
                .getOrDefault(entity.getId(), List.of())
                .stream()
                .map(child -> toTreeVO(child, childrenMap))
                .toList();

        return new PermissionTreeVO(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.getPermType(),
                entity.getModule(),
                entity.getParentId(),
                entity.getPath(),
                entity.getComponent(),
                entity.getIcon(),
                entity.getMethod(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getVisible(),
                entity.getCreateTime(),
                children
        );
    }

    /**
     * 从权限编码或实体 module 字段提取模块名。
     * 优先使用编码前缀，编码为 null 时回退到实体 module 字段映射。
     */
    private String extractModule(SysPermission perm) {
        if (perm.getCode() != null && perm.getCode().contains(":")) {
            return perm.getCode().substring(0, perm.getCode().indexOf(':'));
        }
        return moduleToKey(perm.getModule());
    }

    /**
     * 将实体 module 整型值映射为前端所用的模块 key。
     */
    private String moduleToKey(Integer module) {
        if (module == null) return "other";
        return switch (module) {
            case 0 -> "recruit";
            case 1 -> "job";
            case 2 -> "candidate";
            case 3 -> "interview";
            case 4 -> "talent";
            case 5 -> "analytics";
            case 6 -> "offer";
            case 7 -> "onboarding";
            case 8 -> "referral";
            case 9 -> "system";
            case 10 -> "ai";
            default -> "other";
        };
    }

    /**
     * 从实体构建包含模块信息的 PermissionVO。
     * <p>当实体 name/code 为 null 时，回退到硬编码映射表（兜底方案）。</p>
     */
    private PermissionVO buildPermissionVO(SysPermission entity, Integer module) {
        String name = entity.getName();
        String code = entity.getCode();
        if (name == null || code == null) {
            String[] fallback = PERM_FALLBACK.get(entity.getId());
            if (fallback != null) {
                if (name == null) name = fallback[0];
                if (code == null) code = fallback[1];
            }
        }
        return new PermissionVO(
                entity.getId(),
                name,
                code,
                entity.getPermType(),
                entity.getModule(),
                entity.getParentId(),
                entity.getPath(),
                entity.getComponent(),
                entity.getIcon(),
                entity.getMethod(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getVisible(),
                entity.getCreateTime()
        );
    }
}
