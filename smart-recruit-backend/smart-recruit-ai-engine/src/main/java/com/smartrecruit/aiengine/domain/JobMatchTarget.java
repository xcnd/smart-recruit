package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 内推匹配的职位匹配目标——一个纯POJO（非数据库实体），
 * 供AI匹配智能体评估候选人匹配度使用。
 *
 * @author xdh
 * @since 2026-05-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchTarget {

    /**
     * 职位 ID。
     */
    private Long jobId;
    /**
     * 职位名称。
     */
    private String title;
    /**
     * 所属部门名称。
     */
    private String department;
    /**
     * 经验要求：ENTRY、JUNIOR、MID、SENIOR、LEAD。
     */
    private String experienceLevel;
    /**
     * 职位要求的技能列表。
     */
    private List<String> requiredSkills;
    /**
     * 工作地点。
     */
    private String location;
    /**
     * 最高薪资（元）。
     */
    private Double salaryMax;
    /**
     * 紧急程度：LOW、MEDIUM、HIGH、CRITICAL。
     */
    private Integer urgency;
}
