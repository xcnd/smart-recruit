package com.smartrecruit.recruitment.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建候选人的请求DTO。
 *
 * @since 1.0.0
 */
@Data
public class CreateCandidateRequest {

    @NotBlank(message = "候选人姓名不能为空")
    private String name;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
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

    /** 来源渠道：0=主动投递, 1=内推, 2=官网, 3=LinkedIn, 4=BOSS直聘, 5=拉勾, 6=猎聘, 7=其他。 */
    private Integer source;

    /** 来源详情。 */
    private String sourceDetail;

    /** 标签列表。 */
    private List<String> tags;

    /** 内推人 ID。 */
    private Long referrerId;
}
