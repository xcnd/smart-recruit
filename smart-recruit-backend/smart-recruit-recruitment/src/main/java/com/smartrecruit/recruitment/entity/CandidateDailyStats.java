package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 候选人日汇总实体，映射 {@code analytics_candidate_daily} 表。
 *
 * <p>由定时任务按"日期 + 阶段 + 来源"聚合生成，
 * 分析页查询只读本表，避免对 {@code rec_candidate} 大表实时 GROUP BY。</p>
 *
 * @since 2026-04-06
 */
@Data
@TableName("analytics_candidate_daily")
public class CandidateDailyStats implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计日期（数据日，yyyy-MM-dd）。 */
    @TableId(type = IdType.INPUT)
    private LocalDate statDate;

    /** 候选人阶段编码（对应招聘流程阶段）。 */
    private Integer stage;

    /** 候选人来源编码（招聘渠道）。 */
    private Integer source;

    /** 当日该阶段 × 来源的新增候选人数量。 */
    private Integer candidateCount;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
