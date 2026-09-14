package com.smartrecruit.offer.dto.remote;

import lombok.Data;

/**
 * 候选人画像 DTO（来自招聘服务候选人详情接口，供留任预测使用）。
 *
 * @since 2026-04-10
 */
@Data
public class CandidateProfileDTO {

    /** 候选人 ID。 */
    private Long id;

    /** 姓名。 */
    private String name;

    /** 最高学历：0=高中,1=大专,2=本科,3=硕士,4=博士。 */
    private Integer education;

    /** 工作年限。 */
    private Integer yearsOfExperience;

    /** 历史跳槽次数。 */
    private Integer previousJobCount;

    /** 历史平均在职时长（月）。 */
    private Double avgTenureMonths;

    /** 来源渠道：1=内推。 */
    private Integer source;

    /** 当前公司。 */
    private String currentCompany;

    /** 当前职位。 */
    private String currentPosition;
}
