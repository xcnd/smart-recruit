package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 候选人的分页查询筛选条件。
 *
 * @since 1.0.0
 */
@Data
public class CandidatePageQuery {

    /** 当前页码（从1开始）。 */
    private int page = 1;

    /** 每页大小，最大100。 */
    private int size = 20;

    /** 按候选人姓名筛选（模糊匹配）。 */
    private String name;

    /** 按当前阶段筛选：新入库、筛选中、面试中等。 */
    private Integer stage;

    /** 按来源渠道筛选：主动投递、内推等。 */
    private Integer source;

    /** 按学历筛选。 */
    private Integer education;

    /** 关键词搜索（匹配姓名、邮箱、手机、职位）。 */
    private String keyword;

    /** 按AI匹配最低分数筛选。 */
    private Integer aiMatchScoreMin;

    /** 按AI匹配最高分数筛选。 */
    private Integer aiMatchScoreMax;

    /** 按最低工作年限筛选。 */
    private Integer experienceMin;

    /** 按最高工作年限筛选。 */
    private Integer experienceMax;

    /** 按城市筛选。 */
    private String city;

    /** 按投递开始日期筛选（YYYY-MM-DD）。 */
    private String applyDateStart;

    /** 按投递结束日期筛选（YYYY-MM-DD）。 */
    private String applyDateEnd;

    /** 按简历筛选状态过滤：0=待筛选, 1=已通过, 2=已淘汰。null 不过滤。 */
    private Integer screeningStatus;
}
