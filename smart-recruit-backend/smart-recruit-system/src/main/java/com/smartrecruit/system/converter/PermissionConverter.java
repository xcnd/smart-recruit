package com.smartrecruit.system.converter;

import com.smartrecruit.system.dto.request.CreatePermissionRequest;
import com.smartrecruit.system.dto.response.PermissionVO;
import com.smartrecruit.system.entity.SysPermission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 权限对象转换器（MapStruct）。
 *
 * @since 2026-04-26
 */
@Mapper(componentModel = "spring")
public interface PermissionConverter {

    PermissionConverter INSTANCE = Mappers.getMapper(PermissionConverter.class);

    /**
     * Entity → PermissionVO。
     * module 字段从权限编码前缀派生（例如 "user:create" → "user"）。
     */
    @Mapping(target = "module", ignore = true)
    PermissionVO toVO(SysPermission entity);

    List<PermissionVO> toVOList(List<SysPermission> entities);

    /**
     * CreatePermissionRequest → SysPermission。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    SysPermission toEntity(CreatePermissionRequest request);
}
