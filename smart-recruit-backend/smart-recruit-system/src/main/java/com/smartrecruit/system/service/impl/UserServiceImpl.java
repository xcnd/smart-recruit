package com.smartrecruit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.constant.Constants;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.AuthenticationException;
import com.smartrecruit.common.exception.DuplicateResourceException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.exception.ValidationException;
import com.smartrecruit.system.audit.Auditable;
import com.smartrecruit.system.converter.UserConverter;
import com.smartrecruit.system.dto.request.CreateUserRequest;
import com.smartrecruit.system.dto.request.UpdateProfileRequest;
import com.smartrecruit.system.dto.request.UpdateUserRequest;
import com.smartrecruit.system.dto.request.UserPageQuery;
import com.smartrecruit.system.dto.response.UserDetailVO;
import com.smartrecruit.system.dto.response.UserStatsVO;
import com.smartrecruit.system.dto.response.UserVO;
import com.smartrecruit.system.entity.SysDepartment;
import com.smartrecruit.system.entity.SysRole;
import com.smartrecruit.system.entity.SysUser;
import com.smartrecruit.system.entity.SysUserRole;
import com.smartrecruit.system.repository.SysDepartmentMapper;
import com.smartrecruit.system.repository.SysRoleMapper;
import com.smartrecruit.system.repository.SysUserMapper;
import com.smartrecruit.system.repository.SysUserRoleMapper;
import com.smartrecruit.system.service.PasswordPolicy;
import com.smartrecruit.system.service.UserService;
import com.smartrecruit.system.service.file.FileStorageService;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.common.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 用户管理服务实现类。
 *
 * @since 2026-04-26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final PasswordPolicy passwordPolicy;

    /** 头像允许的 MIME 类型。 */
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    /** 头像最大大小：2MB。 */
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<UserVO> pageQuery(UserPageQuery query) {
        log.info("分页查询用户: username={}, email={}, userId={}, status={}, deptId={}, roleIds={}",
                query.username(), query.email(), query.userId(), query.status(), query.deptId(), query.roleIds());

        Page<SysUser> page = new Page<>(query.page(), query.size());
        var result = sysUserMapper.pageQuery(
                page, query.username(), query.email(), query.userId(), query.status(), query.deptId(), query.roleIds());

        return PageResult.from(result);
    }

    /** 根据主键查询详情。 */
    @Override
    public UserDetailVO getById(Long id) {
        log.info("查询用户详情: id={}", id);

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在: id=" + id);
        }

        UserDetailVO vo = userConverter.toDetailVO(user);

        // Resolve department name
        String departmentName = null;
        if (user.getDeptId() != null) {
            SysDepartment dept = sysDepartmentMapper.selectById(user.getDeptId());
            if (dept != null) {
                departmentName = dept.getName();
            }
        }

        // Fill role info
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(id);
        List<String> roleNames = Collections.emptyList();
        if (!roleIds.isEmpty()) {
            roleNames = sysRoleMapper.selectBatchIds(roleIds).stream()
                    .map(SysRole::getName)
                    .toList();
        }
        return new UserDetailVO(
                vo.id(), vo.username(), vo.realName(), vo.email(), vo.mobile(),
                vo.avatar(), vo.gender(), vo.deptId(), departmentName,
                roleIds, roleNames, vo.status(), vo.lastLoginTime(), vo.lastLoginIp(),
                vo.remark(), vo.createTime(), vo.updateTime(),
                vo.position(), vo.jobLevel(), vo.age());
    }

    /** 创建记录。 */
    @Override
    @Auditable(action = "CREATE", resourceType = "USER", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(CreateUserRequest request) {
        log.info("创建用户: username={}, email={}", request.username(), request.email());

        // Check duplicate email
        SysUser existing = sysUserMapper.findByEmail(request.email());
        if (existing != null) {
            throw new DuplicateResourceException("用户", "email", request.email());
        }

        // Validate department
        if (request.deptId() != null && request.deptId() > 0) {
            // Department existence check delegated to department service
        }

        // 校验并解析角色（支持多角色：roleIds 优先，兼容单一 roleId）
        List<Long> roleIds = resolveRoleIds(request.roleIds(), request.roleId());

        SysUser user = userConverter.toEntity(request);
        // 自动生成全局唯一用户名
        user.setUsername(generateUniqueUsername(request.username()));
        // 密码策略校验（管理员创建用户场景，配置修改后立即生效）
        passwordPolicy.validate(request.password());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDeptId(request.deptId());
        user.setMobile(request.mobile());
        user.setStatus(Constants.USER_STATUS_ACTIVE);
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.position() != null) {
            user.setPosition(request.position());
        }
        if (request.jobLevel() != null) {
            user.setJobLevel(request.jobLevel());
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }
        sysUserMapper.insert(user);

        // Assign roles
        assignRoles(user.getId(), roleIds);

        UserVO vo = userConverter.toVO(user);
        log.info("用户创建成功: userId={}, email={}", user.getId(), user.getEmail());
        return vo;
    }

    /** 更新记录。 */
    @Override
    @Auditable(action = "UPDATE", resourceType = "USER", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public UserVO update(Long id, UpdateUserRequest request) {
        log.info("更新用户: id={}", id);

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在: id=" + id);
        }

        if (request.realName() != null) {
            user.setRealName(request.realName());
        }
        if (request.email() != null) {
            // 校验邮箱唯一：检查所有未删除用户中是否有「非本人」占用该邮箱
            List<SysUser> existingList = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getEmail, request.email())
                            .eq(SysUser::getDeleted, 0));
            boolean conflict = existingList.stream()
                    .anyMatch(e -> !e.getId().equals(id));
            if (conflict) {
                throw new DuplicateResourceException("用户", "email", request.email());
            }
            user.setEmail(request.email());
        }
        if (request.mobile() != null) {
            user.setMobile(request.mobile());
        }
        if (request.deptId() != null) {
            user.setDeptId(request.deptId());
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.position() != null) {
            user.setPosition(request.position());
        }
        if (request.jobLevel() != null) {
            user.setJobLevel(request.jobLevel());
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }

        sysUserMapper.updateById(user);

        // Update role assignment if specified（roleIds 优先，兼容单一 roleId）
        List<Long> roleIds = resolveRoleIds(request.roleIds(), request.roleId());
        if (!roleIds.isEmpty()) {
            LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserRole::getUserId, id);
            sysUserRoleMapper.delete(wrapper);
            assignRoles(id, roleIds);
        }

        UserVO vo = userConverter.toVO(user);
        log.info("用户更新成功: userId={}", id);
        return vo;
    }

    /** 解析角色 ID 列表：roleIds 优先，回退单一 roleId，两者都空则报错。 */
    private List<Long> resolveRoleIds(List<Long> roleIds, Long roleId) {
        List<Long> resolved = roleIds != null && !roleIds.isEmpty()
                ? roleIds.stream().distinct().toList()
                : (roleId != null ? List.of(roleId) : List.of());
        if (resolved.isEmpty()) {
            throw new ValidationException("角色不能为空");
        }
        for (Long rid : resolved) {
            if (sysRoleMapper.selectById(rid) == null) {
                throw new ValidationException("角色不存在: id=" + rid);
            }
        }
        return resolved;
    }

    /** 批量写入用户角色关系。 */
    private void assignRoles(Long userId, List<Long> roleIds) {
        for (Long rid : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(rid);
            sysUserRoleMapper.insert(userRole);
        }
    }

    /** 查询拥有「面试官」角色的用户（供面试安排时选择面试官）。 */
    @Override
    public List<UserVO> listInterviewers() {
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getCode, "ROLE_INTERVIEWER")
                        .eq(SysRole::getDeleted, 0)
                        .last("LIMIT 1"));
        if (role == null) {
            return List.of();
        }
        PageResult<UserVO> result = pageQuery(
                new UserPageQuery(null, 100, null, null, null, 1, null,
                        String.valueOf(role.getId())));
        return result.records();
    }

    /** 更新用户启用/停用状态。 */
    @Override
    @Auditable(action = "UPDATE", resourceType = "USER", module = 0)
    public void updateStatus(Long id, Integer status) {
        log.info("更新用户状态: id={}, status={}", id, status);

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在: id=" + id);
        }

        if (status != Constants.USER_STATUS_ACTIVE && status != Constants.USER_STATUS_DISABLED) {
            throw new ValidationException("无效的用户状态: " + status);
        }

        user.setStatus(status);
        sysUserMapper.updateById(user);
        log.info("用户状态更新成功: userId={}, status={}", id, status);
    }

    /** 根据主键删除记录。 */
    @Override
    @Auditable(action = "DELETE", resourceType = "USER", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("删除用户: id={}", id);

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在: id=" + id);
        }

        // Logical delete
        sysUserMapper.deleteById(id);

        // Clean up user-role associations
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, id);
        sysUserRoleMapper.delete(wrapper);

        log.info("用户删除成功: userId={}", id);
    }

    /** 查询当前登录用户的个人信息。 */
    @Override
    public UserDetailVO getCurrentUserProfile() {
        SysUser user = getCurrentUserEntity();

        // 如果 lastLoginIp 为空，用当前请求 IP 填充
        if (user.getLastLoginIp() == null || user.getLastLoginIp().isEmpty()) {
            String clientIp = getClientIp();
            user.setLastLoginIp(clientIp);
            if (user.getLastLoginTime() == null) {
                user.setLastLoginTime(DateUtils.now());
            }
            sysUserMapper.updateById(user);
        }

        UserDetailVO vo = userConverter.toDetailVO(user);
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(user.getId());
        List<String> roleNames = Collections.emptyList();
        if (!roleIds.isEmpty()) {
            roleNames = sysRoleMapper.selectBatchIds(roleIds).stream()
                    .map(SysRole::getName)
                    .toList();
        }

        log.debug("获取当前用户个人信息: userId={}, email={}", user.getId(), user.getEmail());
        return new UserDetailVO(
                vo.id(), vo.username(), vo.realName(), vo.email(), vo.mobile(),
                vo.avatar(), vo.gender(), vo.deptId(), vo.departmentName(),
                roleIds, roleNames, vo.status(), vo.lastLoginTime(), vo.lastLoginIp(),
                vo.remark(), vo.createTime(), vo.updateTime(),
                vo.position(), vo.jobLevel(), vo.age());
    }

    /** 更新当前登录用户的个人信息。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateCurrentUserProfile(UpdateProfileRequest request) {
        SysUser user = getCurrentUserEntity();

        if (request.realName() != null) {
            user.setRealName(request.realName());
        }
        if (request.mobile() != null) {
            user.setMobile(request.mobile());
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }

        sysUserMapper.updateById(user);

        UserVO vo = userConverter.toVO(user);
        log.info("用户个人信息更新成功: userId={}, email={}", user.getId(), user.getEmail());
        return vo;
    }

    /** 修改密码。 */
    @Override
    public void changePassword(String oldPassword, String newPassword) {
        SysUser user = getCurrentUserEntity();

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AuthenticationException("当前密码不正确");
        }

        // 新密码不能与旧密码相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ValidationException("新密码不能与当前密码相同");
        }

        // 密码策略校验（修改密码场景，配置修改后立即生效）
        passwordPolicy.validate(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        log.info("用户密码修改成功: userId={}", user.getId());
    }

    /** 修改当前用户邮箱。 */
    @Override
    public void changeEmail(String newEmail, String verificationCode) {
        SysUser user = getCurrentUserEntity();

        // 检查新邮箱未被占用
        SysUser existing = sysUserMapper.findByEmail(newEmail);
        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new DuplicateResourceException("用户", "邮箱", newEmail);
        }

        user.setEmail(newEmail);
        sysUserMapper.updateById(user);
        log.info("用户邮箱修改成功: userId={}, newEmail={}", user.getId(), newEmail);
    }

    /**
     * 自动生成全局唯一的用户名。若 baseName 未占用则直接使用，
     * 否则拼接当前用户总数+1作为后缀，确保全局唯一。
     */
    private String generateUniqueUsername(String baseName) {
        SysUser existing = sysUserMapper.findByUsername(baseName);
        if (existing == null) {
            return baseName;
        }
        long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0));
        String candidate = baseName + (count + 1);
        while (sysUserMapper.findByUsername(candidate) != null) {
            count++;
            candidate = baseName + (count + 1);
        }
        return candidate;
    }

    /**
     * 从 SecurityContext 获取当前登录用户实体（基于 JWT 中的 userId）。
     */
    private SysUser getCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(auth.getCredentials().toString());
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return user;
    }

    /**
     * 从当前请求中提取客户端真实 IP（考虑反向代理/网关）。
     */
    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        return SecurityUtil.getClientIpAddress(request);
    }

    /** 重置指定用户密码。 */
    @Override
    @Auditable(action = "UPDATE", resourceType = "USER", module = 0)
    public void resetPassword(Long id, String newPassword) {
        log.info("管理员重置用户密码: id={}", id);

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在: id=" + id);
        }

        // 密码策略校验（管理员重置密码场景，配置修改后立即生效）
        passwordPolicy.validate(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        log.info("管理员重置用户密码成功: userId={}", id);
    }

    /** 查询用户统计信息。 */
    @Override
    public UserStatsVO getUserStats() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        long total = sysUserMapper.selectCount(wrapper);
        long active = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        long frozen = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 0));
        return new UserStatsVO(total, active, frozen);
    }

    /** 上传当前用户头像。 */
    @Override
    public String uploadAvatar(MultipartFile file) {
        SysUser user = getCurrentUserEntity();

        // 业务层校验：头像仅支持图片格式，最大 2MB
        if (file.isEmpty()) {
            throw new ValidationException("文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new ValidationException("头像仅支持 JPG、PNG、GIF、WebP 格式");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new ValidationException("头像大小不能超过 2 MB");
        }

        // 生成 RustFS 存储路径：avatars/{userId}_{uuid}.{ext}
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String relativePath = "avatars/" + user.getId() + "_" + UUID.randomUUID() + ext;

        // 删除旧头像（如果存在）
        String oldAvatar = user.getAvatar();
        if (oldAvatar != null && oldAvatar.startsWith("/files/")) {
            String oldPath = oldAvatar.substring("/files/".length());
            // 兼容 /files/download/avatars/xxx → key = avatars/xxx
            if (oldPath.startsWith("download/")) {
                oldPath = oldPath.substring("download/".length());
            }
            fileStorageService.delete(oldPath);
        }

        // 上传到 RustFS
        String avatarUrl = fileStorageService.upload(file, relativePath);

        // 更新数据库
        user.setAvatar(avatarUrl);
        sysUserMapper.updateById(user);

        log.info("用户头像上传成功: userId={}, url={}", user.getId(), avatarUrl);
        return avatarUrl;
    }
}
