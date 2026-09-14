package com.smartrecruit.interview.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 六维度雷达图分析的面试报告。
 *
 * <p>维度及其权重：
 * <ul>
 *   <li>技术深度 (Technical Depth) - 25%</li>
 *   <li>沟通表达 (Communication) - 20%</li>
 *   <li>问题解决 (Problem Solving) - 20%</li>
 *   <li>团队协作 (Team Collaboration) - 15%</li>
 *   <li>学习能力 (Learning Ability) - 10%</li>
 *   <li>抗压能力 (Stress Tolerance) - 10%</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReportVO {

    /** 面试ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long interviewId;

    /** 候选人ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateId;

    /** 候选人姓名。 */
    private String candidateName;

    /** 应聘职位。 */
    private String jobTitle;

    /** 应聘职位ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobId;

    /** 面试类型编码。 */
    private Integer type;

    /** 面试轮次。 */
    private Integer round;

    /** 面试时间。 */
    private LocalDateTime scheduledAt;

    /** 候选人基本信息。 */
    private CandidateInfo candidate;

    /**
     * 候选人基本信息。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateInfo {
        /** 性别：0=未知, 1=男, 2=女 */
        private Integer gender;
        /** 年龄 */
        private Integer age;
        /** 学历文本（如"本科"、"硕士"） */
        private String education;
        /** 毕业院校 */
        private String school;
        /** 专业 */
        private String major;
        /** 所在城市 */
        private String city;
        /** 手机号 */
        private String phone;
        /** 邮箱 */
        private String email;
        /** 当前公司 */
        private String currentCompany;
        /** 当前职位 */
        private String currentPosition;
        /** 工作年限 */
        private Integer yearsOfExperience;
    }

    /** 综合评分（0-100）。 */
    private Integer overallScore;

    /** 综合评定：通过、不通过、待定。 */
    private Integer result;

    /** 建议：ADVANCE/RETEST/REJECT。 */
    private String suggestion;

    /** 完整反馈文本。 */
    private String feedback;

    /** 雷达图的六维度评分。 */
    private List<DimensionScore> dimensions;

    /** AI生成的优点总结。 */
    private String strengths;

    /** AI生成的待改进领域。 */
    private String weaknesses;

    /** 该候选人是否已有下一轮面试（已安排或进行中），用于控制前端"进入下一轮"按钮显隐。 */
    private Boolean hasNextRound;

    /** AI评估状态: PROCESSING=评估中, COMPLETED=已完成, FAILED=失败, null=未评估。 */
    private String evaluationStatus;

    /**
     * 雷达图中使用的单个维度评分。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScore {

        /** 维度名称（中文）。 */
        private String name;

        /** 维度权重百分比（例如25）。 */
        private Integer weight;

        /** 该维度的得分（0-100）。 */
        private Integer score;
    }
}
