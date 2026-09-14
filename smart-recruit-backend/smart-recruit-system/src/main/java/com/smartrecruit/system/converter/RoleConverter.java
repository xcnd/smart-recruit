package com.smartrecruit.system.converter;

import com.smartrecruit.system.dto.request.CreateRoleRequest;
import com.smartrecruit.system.dto.response.RoleDetailVO;
import com.smartrecruit.system.dto.response.RoleVO;
import com.smartrecruit.system.entity.SysRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 角色对象转换器（MapStruct）。
 *
 * @since 2026-04-26
 */
@Mapper(componentModel = "spring")
public interface RoleConverter {

    RoleConverter INSTANCE = Mappers.getMapper(RoleConverter.class);

    RoleVO toVO(SysRole entity);

    List<RoleVO> toVOList(List<SysRole> entities);

    @Mapping(target = "permissionIds", ignore = true)
    RoleDetailVO toDetailVO(SysRole entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    SysRole toEntity(CreateRoleRequest request);
}
