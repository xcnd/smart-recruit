package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情 VO。
 *
 * @since 2026-04-26
 */
public record UserDetailVO(
        @JsonProperty("id")
        Long id,

        @JsonProperty("username")
        String username,

        @JsonProperty("realName")
        String realName,

        @JsonProperty("email")
        String email,

        @JsonProperty("mobile")
        String mobile,

        @JsonProperty("avatar")
        String avatar,

        @JsonProperty("gender")
        Integer gender,

        @JsonProperty("deptId")
        Long deptId,

        @JsonProperty("departmentName")
        String departmentName,

        @JsonProperty("roleIds")
        List<Long> roleIds,

        @JsonProperty("roleNames")
        List<String> roleNames,

        @JsonProperty("status")
        Integer status,

        @JsonProperty("lastLoginTime")
        LocalDateTime lastLoginTime,

        @JsonProperty("lastLoginIp")
        String lastLoginIp,

        @JsonProperty("remark")
        String remark,

        @JsonProperty("createTime")
        LocalDateTime createTime,

        @JsonProperty("updateTime")
        LocalDateTime updateTime,

        @JsonProperty("position")
        String position,

        @JsonProperty("jobLevel")
        String jobLevel,

        @JsonProperty("age")
        Integer age
) {
}
