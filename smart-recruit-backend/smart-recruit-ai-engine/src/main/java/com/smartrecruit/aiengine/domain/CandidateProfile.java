package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 从简历分析中解析的候选人画像。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile {

    /**
     * 候选人姓名。
     */
    private String name;
    /** 候选人 ID（跨服务回传用）。 */
    private Long candidateId;
    /**
     * 电子邮箱。
     */
    private String email;
    /**
     * 手机号码。
     */
    private String phone;
    /**
     * 最高学历：HIGH_SCHOOL、ASSOCIATE、BACHELOR、MASTER、PHD。
     */
    private String educationLevel;
    /**
     * 毕业院校。
     */
    private String school;
    /**
     * 所学专业。
     */
    private String major;
    /**
     * 工作年限。
     */
    private Integer yearsOfExperience;
    /**
     * 当前任职公司。
     */
    private String currentCompany;
    /**
     * 当前职位名称。
     */
    private String currentPosition;
    /**
     * 技能列表。
     */
    private List<String> skills;
    /**
     * 技能熟练度映射（技能名 → 熟练度等级）。
     */
    private Map<String, Integer> skillProficiency;
    /**
     * 证书/资质列表。
     */
    private List<String> certifications;
    /**
     * 语言能力列表。
     */
    private List<String> languages;
    /**
     * 候选人综合摘要。
     */
    private String summary;
    /**
     * 候选人综合评分（0-100）。
     */
    private Double overallScore;
}
