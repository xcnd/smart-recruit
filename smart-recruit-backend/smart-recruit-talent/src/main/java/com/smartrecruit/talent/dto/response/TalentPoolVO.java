package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 人才库候选人视图对象。
 *
 * <p>字段名与前端 {@code TalentPoolVO} TypeScript 接口对齐。</p>
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TalentPoolVO {

    /** 人才库记录 ID。 */
    private Long id;
    /** 候选人 ID。 */
    private Long candidateId;
    /** 候选人姓名。 */
    private String candidateName;
    /** 邮箱。 */
    private String email;
    /** 手机号。 */
    private String phone;
    /** 最高学历（字符串："大专"/"本科"/"硕士"/"博士"）。 */
    private String education;
    /** 工作年限。 */
    private Integer experience;
    /** 最近职位。 */
    private String lastPosition;
    /** 最近公司。 */
    private String currentCompany;
    /** 匹配度评分（0-100）。 */
    private Integer matchScore;
    /** AI 匹配维度明细（skillMatch/roleMatch/experienceMatch/educationMatch/semanticMatch）。 */
    private Map<String, Double> matchDimensions;
    /** 技能列表。 */
    private List<String> skills;
    /** 标签列表。 */
    private List<String> tags;
    /** AI 标签列表。 */
    private List<String> aiTags;
    /** 来源渠道。 */
    private Integer source;
    /** 人才池类型：GENERAL、TECH、MANAGEMENT、DESIGN、PRODUCT。 */
    private Integer poolType;
    /** 技能评级：BASIC、INTERMEDIATE、ADVANCED、EXPERT、MASTER。 */
    private Integer skillLevel;
    /** 可入职状态：ACTIVE、PASSIVE、NOT_AVAILABLE。 */
    private Integer availability;
    /** 期望职位。 */
    private String expectedPosition;
    /** 期望工作地点。 */
    private String expectedLocation;
    /** 期望最低薪资（元）。 */
    private Integer expectedSalaryMin;
    /** 期望最高薪资（元）。 */
    private Integer expectedSalaryMax;
    /** 人才状态：0=AVAILABLE（可联系）、1=CONTACTED（已联系）、2=ENGAGED（已沟通）。 */
    private Integer status;
    /** AI 综合评分（0-100）。 */
    private Integer aiScore;
    /** 最后活跃时间。 */
    private LocalDateTime lastActiveAt;
    /** 最后联系时间。 */
    private LocalDateTime lastContactAt;
    /** 入库时间。 */
    private LocalDateTime createdAt;
}
