package com.smartrecruit.recruitment.converter;

import com.smartrecruit.recruitment.dto.request.CreateJobRequest;
import com.smartrecruit.recruitment.dto.request.UpdateJobRequest;
import com.smartrecruit.recruitment.dto.response.JobDetailVO;
import com.smartrecruit.recruitment.dto.response.JobVO;
import com.smartrecruit.recruitment.entity.JobPosition;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;

/**
 * JobPosition 实体与相关 DTO 之间的 MapStruct 转换器。
 *
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface JobConverter {

    Map<Long, String> DEPARTMENT_NAMES = Map.of(
            100001L, "公司总部",
            100002L, "技术研发部",
            100003L, "产品部",
            100004L, "人力资源部",
            100005L, "财务部",
            100006L, "市场部",
            100007L, "销售部",
            100008L, "研发组",
            100009L, "测试组",
            100010L, "运维组"
    );

    @Mapping(target = "departmentName", ignore = true)
    JobVO toVO(JobPosition entity);

    List<JobVO> toVOList(List<JobPosition> entities);

    @AfterMapping
    default void fillDepartmentName(JobPosition entity, @MappingTarget JobVO vo) {
        if (entity.getDepartmentId() != null) {
            vo.setDepartmentName(DEPARTMENT_NAMES.getOrDefault(entity.getDepartmentId(), ""));
        }
    }

    @Mapping(target = "departmentName", ignore = true)
    JobDetailVO toDetailVO(JobPosition entity);

    @AfterMapping
    default void fillDetailDepartmentName(JobPosition entity, @MappingTarget JobDetailVO vo) {
        if (entity.getDepartmentId() != null) {
            vo.setDepartmentName(DEPARTMENT_NAMES.getOrDefault(entity.getDepartmentId(), ""));
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "0") // JobStatus.DRAFT
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "applicationCount", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    JobPosition toEntity(CreateJobRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "applicationCount", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntity(@MappingTarget JobPosition target, UpdateJobRequest source);
}
