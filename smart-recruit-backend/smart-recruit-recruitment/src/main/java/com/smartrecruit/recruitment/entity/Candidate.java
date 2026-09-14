package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 候选人实体，映射到 {@code rec_candidate} 表。
 *
 * <p>存储候选人的基本信息、来源渠道以及当前的招聘流程阶段。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName(value = "rec_candidate", autoResultMap = true)
public class Candidate implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 系统用户 ID，用于关联 sys_user 表。 */
    @TableField("user_id")
    private Long userId;

    /** 候选人姓名。 */
    private String name;

    /** 邮箱地址（唯一）。 */
    private String email;

    /** 手机号码。 */
    @TableField("mobile")
    private String phone;

    /** 性别：0=未知, 1=男, 2=女。 */
    private Integer gender;

    /** 出生日期。 */
    @TableField("birth_date")
    private LocalDate birthDate;

    /** 头像背景色（前端显示用）。 */
    @TableField("avatar_color")
    private String avatarColor;

    /** 最高学历：HIGH_SCHOOL, ASSOCIATE, BACHELOR, MASTER, PHD。 */
    @TableField("education_level")
    private Integer education;

    /** 专业工作年限。 */
    @TableField("years_of_experience")
    private Integer yearsOfExperience;

    /** 最近或当前雇主。 */
    @TableField("current_company")
    private String currentCompany;

    /** 毕业院校。 */
    private String school;

    /** 专业。 */
    private String major;

    /** 当前职位。 */
    @TableField("current_position")
    private String currentPosition;

    /** 当前月薪（CNY）。 */
    @TableField("current_salary")
    private Integer currentSalary;

    /** 最低期望月薪。 */
    @TableField("expected_salary_min")
    private Integer expectedSalaryMin;

    /** 最高期望月薪。 */
    @TableField("expected_salary_max")
    private Integer expectedSalaryMax;

    /** 所在城市。 */
    private String city;

    /** 技能列表，以 JSON 数组形式存储，例如 ["Java","Spring","MySQL"]。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> skills;

    /** 来源渠道：DIRECT, REFERRAL, WEBSITE, LINKEDIN, BOSS, LAGOU, OTHER。 */
    private Integer source;

    /** 来源详情（如内推人姓名、具体渠道名称等）。 */
    @TableField("source_detail")
    private String sourceDetail;

    /** 备注。 */
    private String remark;

    /** 最近活跃时间。 */
    @TableField("last_active_time")
    private LocalDateTime lastActiveTime;

    /**
     * 当前招聘流程阶段。
     * 可选值：NEW, SCREENING, INTERVIEWING, OFFERED, HIRED, REJECTED, WITHDRAWN。
     */
    @TableField("status")
    private Integer currentStage;

    /** AI 生成的匹配分数（0-100）。 */
    @TableField("ai_match_score")
    private BigDecimal aiMatchScore;

    /** 投递时间。非物理列。 */
    @TableField(exist = false)
    private LocalDateTime appliedAt;

    /** 简历头像 URL。 */
    @TableField("avatar_url")
    private String avatarUrl;

    /** 上传的简历文件 URL。非物理列。 */
    @TableField(exist = false)
    private String resumeUrl;

    /** 标签列表，以 JSON 数组形式存储，用于分类。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /** 内部推荐人用户 ID（0 表示自主投递）。 */
    @TableField("referrer_id")
    private Long referrerId;

    /** 创建者用户名。 */
    @TableField("create_by")
    private String createdBy;

    /** 记录创建时间戳。 */
    @TableField("create_time")
    private LocalDateTime createdAt;

    /** 最后更新时间戳。 */
    @TableField("update_time")
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0=正常，1=已删除。 */
    @TableLogic
    private Integer deleted = 0;

}
