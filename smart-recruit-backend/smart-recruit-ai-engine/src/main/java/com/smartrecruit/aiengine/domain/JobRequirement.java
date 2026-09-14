package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用于匹配的职位需求规范。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequirement {

    /**
     * 职位 ID。
     */
    private Long jobId;
    /**
     * 职位名称。
     */
    private String jobTitle;
    /**
     * 所属部门。
     */
    private String department;
    /**
     * 经验要求：ENTRY、JUNIOR、MID、SENIOR、LEAD。
     */
    private String experienceLevel;
    /**
     * 学历要求：ASSOCIATE、BACHELOR、MASTER、PHD。
     */
    private String educationLevel;
    /**
     * 必备技能列表。
     */
    private List<String> requiredSkills;
    /**
     * 加分技能列表。
     */
    private List<String> preferredSkills;
    /**
     * 最低工作年限要求。
     */
    private Integer minYearsOfExperience;
    /**
     * 工作地点。
     */
    private String location;
    /**
     * 最低薪资（元）。
     */
    private Double salaryMin;
    /**
     * 最高薪资（元）。
     */
    private Double salaryMax;
}
