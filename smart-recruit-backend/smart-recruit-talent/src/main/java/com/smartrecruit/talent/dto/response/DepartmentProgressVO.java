package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 部门级招聘进度视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentProgressVO {

    /** 部门ID。 */
    private Long departmentId;

    /** 部门名称。 */
    private String departmentName;

    /** 开放编制数。 */
    private Integer headcount;

    /** 招聘流程中候选人数量。 */
    private Integer inPipeline;

    /** 面试中候选人数量。 */
    private Integer interviewing;

    /** 已发Offer候选人数量。 */
    private Integer offered;

    /** 已入职候选人数量。 */
    private Integer hired;

    /** 完成进度百分比（入职数/编制数 * 100）。 */
    private Double progressPercentage;
}
