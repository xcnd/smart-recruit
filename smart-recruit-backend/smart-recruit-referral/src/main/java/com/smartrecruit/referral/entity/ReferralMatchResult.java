package com.smartrecruit.referral.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 内推智能匹配结果实体，映射 {@code ref_match_result} 表。
 *
 * @since 2026-04-06
 */
@Data
@TableName(value = "ref_match_result", autoResultMap = true)
public class ReferralMatchResult implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联的内推记录 ID（ref_referral_record.id）。 */
    private Long recordId;

    /** 候选人 ID（rec_candidate.id）。 */
    private Long candidateId;

    /** 候选人姓名。 */
    private String candidateName;

    /** 匹配的职位 ID（rec_job_position.id）。 */
    private Long matchedJobId;

    /** 匹配的职位名称。 */
    private String matchedJobTitle;

    /** 匹配评分（0-1 或 0-100，取决于来源）。 */
    private BigDecimal matchScore;

    /** 推荐建议：RECOMMEND、REVIEW、REJECT 等。 */
    private String recommendation;

    /** 建议的跟进方式/话术。 */
    private String suggestedApproach;

    /** 匹配维度明细（JSON）。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object matchDimensions;

    /** 匹配来源：AI 或 RULE。 */
    private String source;

    /** 匹配排名（1 起，按评分降序）。 */
    private Integer rankNo;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
