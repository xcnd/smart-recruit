package com.smartrecruit.offer.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建系统用户请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    /** 登录用户名。 */
    private String username;

    /** 真实姓名。 */
    private String realName;

    /** 邮箱。 */
    private String email;

    /** 初始密码。 */
    private String password;

    /** 部门 ID。 */
    private Long deptId;

    /** 角色 ID。 */
    private Long roleId;
}
