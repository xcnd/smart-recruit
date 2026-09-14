package com.smartrecruit.offer.dto.request;

import lombok.Data;

/**
 * 创建系统账号请求（步骤3-账号开通）。
 *
 * @since 1.0.0
 */
@Data
public class UpdateAccountRequest {

    /** 系统账号用户名。 */
    private String username;

    /** 系统账号邮箱。 */
    private String email;

    /** 初始密码。 */
    private String initialPassword;
}
