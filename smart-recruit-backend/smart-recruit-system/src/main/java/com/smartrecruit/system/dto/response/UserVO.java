package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图 VO。
 *
 * @since 2026-04-26
 */
@JsonPropertyOrder({"id", "username", "realName", "email", "mobile", "avatar", "deptId",
        "departmentName", "roleName", "roleIds", "gender", "position", "jobLevel", "age",
        "status", "lastLoginTime", "createTime", "permissions"})
public record UserVO(
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

        @JsonProperty("deptId")
        Long deptId,

        @JsonProperty("departmentName")
        String departmentName,

        @JsonProperty("roleName")
        String roleName,

        @JsonProperty("roleIds")
        String roleIds,

        @JsonProperty("gender")
        Integer gender,

        @JsonProperty("position")
        String position,

        @JsonProperty("jobLevel")
        String jobLevel,

        @JsonProperty("age")
        Integer age,

        @JsonProperty("status")
        Integer status,

        @JsonProperty("lastLoginTime")
        LocalDateTime lastLoginTime,

        @JsonProperty("createTime")
        LocalDateTime createTime,

        @JsonProperty("permissions")
        List<String> permissions
) {

    /**
     * 返回携带权限列表的新 VO（权限来自角色授权，而非用户实体）。
     */
    public UserVO withPermissions(List<String> perms) {
        return new UserVO(id, username, realName, email, mobile, avatar, deptId, departmentName,
                roleName, roleIds, gender, position, jobLevel, age, status, lastLoginTime,
                createTime, perms);
    }
}
