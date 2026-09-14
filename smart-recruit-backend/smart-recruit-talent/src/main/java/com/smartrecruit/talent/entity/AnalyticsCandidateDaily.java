package com.smartrecruit.talent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 候选人日汇总快照，映射 {@code analytics_candidate_daily} 表。
 *
 * <p>由定时任务从招聘服务同步，供数据分析页本地聚合，
 * 避免每次请求实时跨服务查询。</p>
 *
 * @since 2026-04-10
 */
@Data
@TableName("analytics_candidate_daily")
public class AnalyticsCandidateDaily implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计日期。 */
    @TableId(type = IdType.INPUT)
    private LocalDate statDate;

    /** 候选人阶段（rec_candidate.status）。 */
    private Integer stage;

    /** 来源渠道（rec_candidate.source）。 */
    private Integer source;

    /** 当日新增候选人数。 */
    private Integer candidateCount;
}
