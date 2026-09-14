package com.smartrecruit.system.converter;

import com.smartrecruit.system.dto.request.CreateDeptRequest;
import com.smartrecruit.system.dto.response.DepartmentTreeVO;
import com.smartrecruit.system.dto.response.DepartmentVO;
import com.smartrecruit.system.entity.SysDepartment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 部门对象转换器（MapStruct）。
 *
 * @since 2026-04-26
 */
@Mapper(componentModel = "spring")
public interface DepartmentConverter {

    DepartmentConverter INSTANCE = Mappers.getMapper(DepartmentConverter.class);

    DepartmentVO toVO(SysDepartment entity);

    List<DepartmentVO> toVOList(List<SysDepartment> entities);

    @Mapping(target = "children", ignore = true)
    DepartmentTreeVO toTreeVO(SysDepartment entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    SysDepartment toEntity(CreateDeptRequest request);
}
