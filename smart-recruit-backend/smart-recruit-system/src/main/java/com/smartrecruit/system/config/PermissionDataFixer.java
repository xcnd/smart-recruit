package com.smartrecruit.system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时自动修复 sys_permission 表中为 null 的 perm_name 和 perm_code 字段，
 * 同时补录缺失的权限记录（INSERT ON DUPLICATE KEY UPDATE 幂等操作）。
 *
 * @since 2026-05-11
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionDataFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("[PermissionDataFixer] 检查 sys_permission 表数据完整性...");

        Integer nullCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_permission WHERE deleted = 0 AND (perm_name IS NULL OR perm_code IS NULL)",
                Integer.class
        );

        if (nullCount == null || nullCount == 0) {
            log.info("[PermissionDataFixer] 数据完整，无需修复");
        } else {
            log.info("[PermissionDataFixer] 发现 {} 条记录的 perm_name/perm_code 为空，开始修复...", nullCount);
        }

        // Level 1: Top-level Menus (parent_id=0, perm_type=1, sort_order sequential)
        fixOne(400001L, 0L, "工作台", "dashboard", 1, 0, 1);
        fixOne(400002L, 0L, "招聘管理", "recruitment", 1, 0, 2);
        fixOne(400003L, 0L, "人才库", "talent", 1, 4, 3);
        fixOne(400004L, 0L, "内推管理", "referral", 1, 8, 4);
        fixOne(400005L, 0L, "入职管理", "onboarding", 1, 7, 5);
        fixOne(400006L, 0L, "AI引擎", "ai", 1, 10, 6);
        fixOne(400007L, 0L, "数据分析", "analytics", 1, 5, 7);
        fixOne(400008L, 0L, "系统管理", "system", 1, 9, 8);

        // Level 2: Recruitment sub-menus (parent_id=400002, perm_type=1)
        fixOne(400101L, 400002L, "职位管理", "recruitment:job", 1, 0, 1);
        fixOne(400102L, 400002L, "简历筛选", "recruitment:resume", 1, 0, 2);
        fixOne(400103L, 400002L, "面试管理", "recruitment:interview", 1, 3, 3);
        fixOne(400104L, 400002L, "Offer管理", "recruitment:offer", 1, 6, 4);

        // Level 2: System sub-menus (parent_id=400008, perm_type=1, module=9)
        fixOne(400801L, 400008L, "用户管理", "system:user", 1, 9, 1);
        fixOne(400802L, 400008L, "角色管理", "system:role", 1, 9, 2);
        fixOne(400803L, 400008L, "权限管理", "system:permission", 1, 9, 3);
        fixOne(400804L, 400008L, "部门管理", "system:dept", 1, 9, 4);
        fixOne(400805L, 400008L, "操作日志", "system:log", 1, 9, 5);
        fixOne(400806L, 400008L, "系统设置", "system:settings", 1, 9, 6);

        // Level 3: Button - Job (parent_id=400101, perm_type=2, module=0)
        fixOne(401101L, 400101L, "创建职位", "job:create", 2, 0, 1);
        fixOne(401102L, 400101L, "编辑职位", "job:edit", 2, 0, 2);
        fixOne(401103L, 400101L, "删除职位", "job:delete", 2, 0, 3);
        fixOne(401104L, 400101L, "发布职位", "job:publish", 2, 0, 4);
        fixOne(401105L, 400101L, "关闭职位", "job:close", 2, 0, 5);
        fixOne(401106L, 400101L, "查看职位", "job:view", 2, 0, 6);

        // Level 3: Button - Candidate/Resume (parent_id=400102, module=2)
        fixOne(401201L, 400102L, "查看候选人", "candidate:view", 2, 2, 1);
        fixOne(401202L, 400102L, "编辑候选人", "candidate:edit", 2, 2, 2);
        fixOne(401203L, 400102L, "导入简历", "resume:import", 2, 2, 3);
        fixOne(401204L, 400102L, "解析简历", "resume:parse", 2, 2, 4);
        fixOne(401205L, 400102L, "筛选简历", "resume:screen", 2, 2, 5);
        fixOne(401517L, 400102L, "上传简历", "resume:upload", 2, 2, 6);
        fixOne(401518L, 400102L, "查看简历", "resume:view", 2, 2, 7);

        // Level 3: Button - Interview (parent_id=400103, module=3)
        fixOne(401301L, 400103L, "安排面试", "interview:schedule", 2, 3, 1);
        fixOne(401302L, 400103L, "取消面试", "interview:cancel", 2, 3, 2);
        fixOne(401303L, 400103L, "提交反馈", "interview:feedback", 2, 3, 3);
        fixOne(401304L, 400103L, "查看反馈", "interview:view_fb", 2, 3, 4);
        fixOne(401519L, 400103L, "查看面试", "interview:view", 2, 3, 5);
        fixOne(401520L, 400103L, "创建面试", "interview:create", 2, 3, 6);
        fixOne(401521L, 400103L, "编辑面试", "interview:edit", 2, 3, 7);

        // Level 3: Button - Offer (parent_id=400104, module=6)
        fixOne(401401L, 400104L, "创建Offer", "offer:create", 2, 6, 1);
        fixOne(401402L, 400104L, "审批Offer", "offer:approve", 2, 6, 2);
        fixOne(401403L, 400104L, "发送Offer", "offer:send", 2, 6, 3);
        fixOne(401404L, 400104L, "查看Offer", "offer:view", 2, 6, 4);
        fixOne(401522L, 400104L, "编辑Offer", "offer:edit", 2, 6, 5);

        // Level 3: Button - System: User (parent_id=400801, module=9)
        fixOne(401501L, 400801L, "查看用户", "user:view", 2, 9, 1);
        fixOne(401502L, 400801L, "创建用户", "user:create", 2, 9, 2);
        fixOne(401503L, 400801L, "编辑用户", "user:edit", 2, 9, 3);
        fixOne(401504L, 400801L, "删除用户", "user:delete", 2, 9, 4);

        // Level 3: Button - System: Role (parent_id=400802, module=9)
        fixOne(401505L, 400802L, "查看角色", "role:view", 2, 9, 1);
        fixOne(401506L, 400802L, "创建角色", "role:create", 2, 9, 2);
        fixOne(401507L, 400802L, "编辑角色", "role:edit", 2, 9, 3);
        fixOne(401508L, 400802L, "删除角色", "role:delete", 2, 9, 4);

        // Level 3: Button - System: Permission (parent_id=400803, module=9)
        fixOne(401509L, 400803L, "查看权限", "perm:view", 2, 9, 1);
        fixOne(401510L, 400803L, "编辑权限", "perm:edit", 2, 9, 2);

        // Level 3: Button - System: Department (parent_id=400804, module=9)
        fixOne(401511L, 400804L, "查看部门", "dept:view", 2, 9, 1);
        fixOne(401512L, 400804L, "创建部门", "dept:create", 2, 9, 2);
        fixOne(401513L, 400804L, "编辑部门", "dept:edit", 2, 9, 3);
        fixOne(401514L, 400804L, "删除部门", "dept:delete", 2, 9, 4);

        // Level 3: Button - Notifications (parent_id=400008, module=9)
        fixOne(401515L, 400008L, "查看通知", "notification:view", 2, 9, 6);
        fixOne(401516L, 400008L, "管理通知", "notification:edit", 2, 9, 7);

        // Level 3: Button - Dashboard (parent_id=400001, module=0)
        fixOne(401523L, 400001L, "查看工作台", "dashboard:view", 2, 0, 2);

        // Level 3: Button - Analytics (parent_id=400007, module=5)
        fixOne(401524L, 400007L, "查看分析", "analytics:view", 2, 5, 1);
        fixOne(401525L, 400007L, "导出分析", "analytics:export", 2, 5, 2);

        // Level 3: Button - Talent (parent_id=400003, module=4)
        fixOne(401526L, 400003L, "查看人才", "talent:view", 2, 4, 1);
        fixOne(401527L, 400003L, "编辑人才", "talent:edit", 2, 4, 2);

        // Level 3: Button - Referral (parent_id=400004, module=8)
        fixOne(401528L, 400004L, "查看内推", "referral:view", 2, 8, 1);
        fixOne(401529L, 400004L, "创建内推", "referral:create", 2, 8, 2);
        fixOne(401530L, 400004L, "编辑内推", "referral:edit", 2, 8, 3);

        // Level 3: Button - Onboarding (parent_id=400005, module=7)
        fixOne(401531L, 400005L, "查看入职", "onboarding:view", 2, 7, 1);
        fixOne(401532L, 400005L, "编辑入职", "onboarding:edit", 2, 7, 2);

        // Level 3: Button - AI (parent_id=400006, module=10)
        fixOne(401533L, 400006L, "查看Agent", "agent:view", 2, 10, 1);
        fixOne(401534L, 400006L, "管理Agent", "agent:manage", 2, 10, 2);

        // Level 3: Button - System Settings (parent_id=400806, module=9)
        fixOne(401535L, 400806L, "编辑系统设置", "system:settings:edit", 2, 9, 1);

        // 确保 Super Admin (300001) 拥有所有已启用权限（增量补录）
        ensureSuperAdminHasAllPermissions();

        log.info("[PermissionDataFixer] 修复完成");
    }

    private void fixOne(Long id, Long parentId, String name, String code, int permType, int module, int sortOrder) {
        int rows = jdbcTemplate.update(
                "INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, module, sort_order, status, visible, deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1, 0) " +
                "ON DUPLICATE KEY UPDATE " +
                "parent_id = VALUES(parent_id), " +
                "perm_name = IF(perm_name IS NULL, VALUES(perm_name), perm_name), " +
                "perm_code = IF(perm_code IS NULL, VALUES(perm_code), perm_code), " +
                "perm_type = VALUES(perm_type), " +
                "module = VALUES(module), " +
                "sort_order = VALUES(sort_order)",
                id, parentId, name, code, permType, module, sortOrder
        );
        if (rows > 0) {
            log.debug("[PermissionDataFixer] 已修复 id={}, name={}, code={}", id, name, code);
        }
    }

    /**
     * 确保 Super Admin 角色拥有所有已启用的权限。
     * 使用 INSERT IGNORE 避免重复插入，增量补录新增权限。
     */
    private void ensureSuperAdminHasAllPermissions() {
        int inserted = jdbcTemplate.update(
                "INSERT IGNORE INTO sys_role_permission (role_id, permission_id) " +
                "SELECT 300001, id FROM sys_permission WHERE status = 1 AND deleted = 0"
        );
        if (inserted > 0) {
            log.info("[PermissionDataFixer] 已为 Super Admin 补录 {} 条权限", inserted);
        }
    }
}
