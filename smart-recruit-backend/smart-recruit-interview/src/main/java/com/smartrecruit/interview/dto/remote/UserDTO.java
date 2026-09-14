package com.smartrecruit.interview.dto.remote;

import lombok.Data;

/**
 * 系统服务用户 DTO - 对应 smart-recruit-system 的 UserDetailVO 响应。
 *
 * @since 1.0.0
 */
@Data
public class UserDTO {
    private Long id;
    private String realName;
}
