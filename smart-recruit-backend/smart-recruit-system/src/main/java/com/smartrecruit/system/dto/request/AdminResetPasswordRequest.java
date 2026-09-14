package com.smartrecruit.system.dto.request;

import lombok.Data;

/**
 * 管理员重置指定用户密码请求。
 *
 * @since 2026-04-07
 */
@Data
public class AdminResetPasswordRequest {

    /** 新密码（明文，服务端加密存储）。 */
    private String password;
}
