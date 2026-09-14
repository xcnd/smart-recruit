package com.smartrecruit.recruitment.entity;

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
import java.util.List;

/**
 * AI 简历筛选结果实体，映射 {@code rec_ai_screening_result} 表。
 *
 * <p>每次筛选（LLM 或启发式）产生一条记录，
 * 通过 {@code resume_id} 与简历关联。
 * 新结果会覆盖旧结果（upsert 语义）。</p>
 *
 * @since 1.1.0
 */
@Data
@TableName(value = "rec_ai_screening_result", autoResultMap = true)
public class AiScreeningResult implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联的简历 ID（rec_resume.id）。 */
    @TableField("resume_id")
    private Long resumeId;

    /** 综合匹配评分（0-100）。 */
    @TableField("overall_score")
    private BigDecimal overallScore;

    /** 多维度评分明细（雷达图数据）。 */
    @TableField(value = "dimensions", typeHandler = JacksonTypeHandler.class)
    private List<DimensionRecord> dimensions;

    /** 命中的关键词列表。 */
    @TableField(value = "matched_keywords", typeHandler = JacksonTypeHandler.class)
    private List<String> matchedKeywords;

    /** 缺失的关键词列表。 */
    @TableField(value = "missing_keywords", typeHandler = JacksonTypeHandler.class)
    private List<String> missingKeywords;

    /** 筛选建议：RECOMMEND、REVIEW、REJECT 等。 */
    private String suggestion;

    /** 筛选结论摘要文本。 */
    @TableField("summary_text")
    private String summary;

    /** 候选人优势列表。 */
    @TableField(value = "strengths", typeHandler = JacksonTypeHandler.class)
    private List<String> strengths;

    /** 候选人不足列表。 */
    @TableField(value = "weaknesses", typeHandler = JacksonTypeHandler.class)
    private List<String> weaknesses;

    /** 来源：LLM 或 HEURISTIC。 */
    private String source;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createdAt;

    /**
     * 雷达图维度数据。
     */
    @Data
    public static class DimensionRecord implements Serializable {
        /** 序列化版本号。 */
        @Serial
        private static final long serialVersionUID = 1L;

        /** 维度名称。 */
        private String name;
        /** 维度得分。 */
        private Integer score;
        /** 维度满分（默认 100）。 */
        private Integer maxScore = 100;
    }
}
