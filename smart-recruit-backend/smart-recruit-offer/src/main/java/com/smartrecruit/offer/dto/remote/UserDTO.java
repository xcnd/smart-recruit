package com.smartrecruit.offer.dto.remote;

import lombok.Data;

/**
 * 系统服务用户 DTO — 映射 smart-recruit-system 的 {@code GET /api/v1/users/{id}} 响应。
 *
 * <p>用于审批流程中获取审批人和Offer创建人的邮箱。</p>
 *
 * @since 1.0.0
 */
@Data
public class UserDTO {
    /** 用户 ID。 */
    private Long id;
    /** 真实姓名。 */
    private String realName;
    /** 邮箱地址。 */
    private String email;
}
