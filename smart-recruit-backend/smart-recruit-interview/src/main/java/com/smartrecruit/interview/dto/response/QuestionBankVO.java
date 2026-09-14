package com.smartrecruit.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试题库-套题视图对象（含题目列表）。
 *
 * @since 2026-04-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankVO {

    private Long id;

    /** 套题名称。 */
    private String bankName;

    /** 部门 ID。 */
    private Long departmentId;

    /** 部门名称。 */
    private String departmentName;

    /** 职位 ID。 */
    private Long jobPositionId;

    /** 职位名称。 */
    private String jobTitle;

    /** 套题类型：0=技术面,1=项目面,2=行为/HR面,3=综合面。 */
    private Integer questionType;

    /** 难度：1=简单,2=中等,3=困难。 */
    private Integer difficulty;

    /** 套题说明。 */
    private String description;

    /** 题目数量。 */
    private Integer questionCount;

    /** 状态：0=草稿,1=启用,2=停用。 */
    private Integer status;

    private LocalDateTime createTime;
    private String createBy;

    /** 题目列表（详情接口返回）。 */
    private List<QuestionBankItemVO> items;
}
