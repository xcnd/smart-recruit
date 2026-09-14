package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.io.Serializable;

/**
 * 按部门分组的聚合职位统计。
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentJobStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 部门ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long departmentId;

    /** 该部门活跃职位总数。 */
    private long jobCount;

    /** 所有职位的开放招聘总人数。 */
    private long totalHeadCount;

    /** 紧急/关键职位的数量。 */
    private long urgentCount;
}
