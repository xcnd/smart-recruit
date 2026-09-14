package com.smartrecruit.recruitment.dto.response;

import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 候选人列表展示的视图对象。
 *
 * @since 1.0.0
 */
@Data
public class CandidateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 候选人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 姓名。 */
    private String name;
    /** 邮箱。 */
    private String email;
    /** 手机号。 */
    private String phone;
    /** 最高学历。 */
    private Integer education;
    /** 工作年限。 */
    private Integer yearsOfExperience;
    /** 当前公司。 */
    private String currentCompany;
    /** 技能列表（JSON）。 */
    private List<String> skills;
    /** 来源渠道：DIRECT、REFERRAL、JOB_BOARD、HEADHUNTER 等。 */
    private Integer source;
    /** 当前招聘阶段。 */
    private Integer currentStage;
    /** AI 匹配度评分（0-100）。 */
    private Integer aiMatchScore;
    /** 投递时间。 */
    private LocalDateTime appliedAt;
    /** 简历文件 URL。 */
    private String resumeUrl;
    /** 标签列表（JSON 数组）。 */
    private List<String> tags;
    /** 关联职位 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobId;
    /** 职位名称。 */
    private String jobTitle;
    /** 内推人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long referrerId;
    /** 性别：0=未知, 1=男, 2=女。 */
    private Integer gender;
    /** 出生日期。 */
    private LocalDate birthDate;
    /** 头像背景色。 */
    private String avatarColor;
    /** 毕业院校。 */
    private String school;
    /** 专业。 */
    private String major;
    /** 当前职位。 */
    private String currentPosition;
    /** 当前月薪。 */
    private Integer currentSalary;
    /** 最低期望月薪。 */
    private Integer expectedSalaryMin;
    /** 最高期望月薪。 */
    private Integer expectedSalaryMax;
    /** 所在城市。 */
    private String city;
    /** 来源详情。 */
    private String sourceDetail;
    /** 最近活跃时间。 */
    private LocalDateTime lastActiveTime;
    /** 备注。 */
    private String remark;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
