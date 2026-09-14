package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 更新候选人的请求DTO。
 * 所有字段均为可选，仅更新传入的非 null 字段。
 *
 * @since 1.0.0
 */
@Data
public class UpdateCandidateRequest {

    /** 候选人姓名。 */
    private String name;

    /** 邮箱地址。 */
    private String email;

    /** 手机号码。 */
    private String phone;

    /** 性别：0=未知, 1=男, 2=女。 */
    private Integer gender;

    /** 出生日期。 */
    private LocalDate birthDate;

    /** 最高学历：0=高中, 1=大专, 2=本科, 3=硕士, 4=博士。 */
    private Integer education;

    /** 毕业院校。 */
    private String school;

    /** 专业。 */
    private String major;

    /** 工作年限。 */
    private Integer yearsOfExperience;

    /** 当前公司。 */
    private String currentCompany;

    /** 当前职位。 */
    private String currentPosition;

    /** 当前月薪（CNY）。 */
    private Integer currentSalary;

    /** 最低期望月薪。 */
    private Integer expectedSalaryMin;

    /** 最高期望月薪。 */
    private Integer expectedSalaryMax;

    /** 所在城市。 */
    private String city;

    /** 技能列表。 */
    private List<String> skills;

    /** 来源渠道。 */
    private Integer source;

    /** 来源详情。 */
    private String sourceDetail;

    /** 标签列表。 */
    private List<String> tags;

    /** 内推人 ID。 */
    private Long referrerId;

    /** 备注。 */
    private String remark;
}
