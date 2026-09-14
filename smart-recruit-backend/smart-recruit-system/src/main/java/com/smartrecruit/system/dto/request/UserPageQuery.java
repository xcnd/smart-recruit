package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 用户分页查询 DTO。
 *
 * @since 2026-04-26
 */
public record UserPageQuery(
        @Min(value = 1, message = "页码最小为1")
        Integer page,

        @Min(value = 1, message = "每页条数最小为1")
        @Max(value = 100, message = "每页条数最大为100")
        Integer size,

        String username,

        String email,

        Long userId,

        Integer status,

        Long deptId,

        String roleIds
) {
    public UserPageQuery {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }
    }
}
