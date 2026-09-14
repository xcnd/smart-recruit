package com.smartrecruit.recruitment.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 部门招聘进度视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentProgressVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 部门ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long departmentId;

    /** 部门名称 */
    private String departmentName;

    /** 在招职位数 */
    private Long activeJobCount;

    /** 计划招聘人数 */
    private Long plannedHeadCount;

    /** 已入职人数 */
    private Long hiredCount;

    /** 招聘进度百分比 (0-100) */
    private Double progressPercent;
}
