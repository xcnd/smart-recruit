package com.smartrecruit.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体，映射 {@code sys_user} 表。
 *
 * @since 2026-04-26
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户名，用于登录认证，全局唯一。 */
    @TableField("username")
    private String username;

    /** 加密后的登录密码（BCrypt）。 */
    @TableField("password")
    private String password;

    /** 用户真实姓名。 */
    @TableField("real_name")
    private String realName;

    /** 电子邮箱地址。 */
    @TableField("email")
    private String email;

    /** 手机号码。 */
    @TableField("mobile")
    private String mobile;

    /** 头像 URL 或存储路径。 */
    @TableField("avatar")
    private String avatar;

    /** 性别：0 = 未知，1 = 男，2 = 女。 */
    @TableField("gender")
    private Integer gender;

    /** 所属部门 ID，关联 sys_department.id。 */
    @TableField("department_id")
    private Long deptId;

    /** 账户状态：0 = 禁用，1 = 启用。 */
    @TableField("status")
    private Integer status;

    /** 最近一次登录时间。 */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /** 最近一次登录 IP 地址。 */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /** 注册时使用的内推码（通过分享链接注册时绑定）。 */
    @TableField("referral_code")
    private String referralCode;

    /** 职位名称。 */
    @TableField("position")
    private String position;

    /** 职级（如 P5/P6/M1 等）。 */
    @TableField("job_level")
    private String jobLevel;

    /** 年龄。 */
    @TableField("age")
    private Integer age;

    /** 备注信息。 */
    @TableField("remark")
    private String remark;

    /** 创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 ID。 */
    @TableField("create_user_id")
    private Long createUserId;

    /** 创建人姓名。 */
    @TableField("create_by")
    private String createBy;

    /** 更新人 ID。 */
    @TableField("update_user_id")
    private Long updateUserId;

    /** 更新人姓名。 */
    @TableField("update_by")
    private String updateBy;

    /** 逻辑删除标记：0 = 未删除，1 = 已删除。 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted = 0;

}
