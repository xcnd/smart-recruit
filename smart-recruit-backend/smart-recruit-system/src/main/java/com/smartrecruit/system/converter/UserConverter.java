package com.smartrecruit.system.converter;

import com.smartrecruit.system.dto.request.CreateUserRequest;
import com.smartrecruit.system.dto.response.UserDetailVO;
import com.smartrecruit.system.dto.response.UserVO;
import com.smartrecruit.system.entity.SysUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户对象转换器（MapStruct）。
 *
 * @since 2026-04-26
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    /**
     * Entity → UserVO。
     */
    @Mapping(target = "departmentName", ignore = true)
    @Mapping(target = "roleName", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(source = "email", target = "email")
    @Mapping(source = "mobile", target = "mobile")
    UserVO toVO(SysUser entity);

    List<UserVO> toVOList(List<SysUser> entities);

    /**
     * Entity → UserDetailVO。
     */
    @Mapping(target = "departmentName", ignore = true)
    @Mapping(target = "roleIds", ignore = true)
    @Mapping(target = "roleNames", ignore = true)
    @Mapping(source = "email", target = "email")
    @Mapping(source = "mobile", target = "mobile")
    UserDetailVO toDetailVO(SysUser entity);

    /**
     * CreateUserRequest → Entity。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastLoginTime", ignore = true)
    @Mapping(target = "lastLoginIp", ignore = true)
    @Mapping(target = "remark", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "mobile", ignore = true)
    SysUser toEntity(CreateUserRequest request);
}
