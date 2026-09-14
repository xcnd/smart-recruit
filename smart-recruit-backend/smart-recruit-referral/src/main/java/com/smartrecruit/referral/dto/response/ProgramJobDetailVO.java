package com.smartrecruit.referral.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 职位详情 VO（含计划上下文 + 完整职位数据）。
 *
 * @author xdh
 * @since 2026-04-01
 */
@Data
public class ProgramJobDetailVO {

    /** 计划-职位关联信息。 */
    private RefProgramJobVO programJob;

    /** 所属内推计划概要。 */
    private ProgramBrief program;

    /** 完整职位信息。 */
    private JobBrief jobPosition;

    @Data
    public static class ProgramBrief {
        private Long id;
        private String title;
        private String description;
        private BigDecimal bonusAmount;
        private String bonusStructure;
        private String startDate;
        private String endDate;
        private Integer status;
        private String createBy;
    }

    @Data
    public static class JobBrief {
        private Long id;
        private String title;
        private String description;
        /** JSON 字符串：岗位职责。 */
        private String responsibilities;
        /** JSON 字符串：任职要求。 */
        private String requirements;
        /** JSON 字符串：技能标签。 */
        private String skills;
        private Integer minSalary;
        private Integer maxSalary;
        private String location;
        private Integer positionType;
        private Integer experienceLevel;
        private Integer educationLevel;
        private Integer status;
        private Long departmentId;
        private String departmentName;
    }
}
