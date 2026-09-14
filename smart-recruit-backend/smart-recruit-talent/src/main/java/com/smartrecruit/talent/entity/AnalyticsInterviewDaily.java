package com.smartrecruit.talent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 面试日汇总快照，映射 {@code analytics_interview_daily} 表。
 *
 * @since 2026-04-10
 */
@Data
@TableName("analytics_interview_daily")
public class AnalyticsInterviewDaily implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计日期。 */
    @TableId(type = IdType.INPUT)
    private LocalDate statDate;

    /** 当日创建面试数。 */
    private Integer totalCount;

    /** 当日创建且结果为通过数。 */
    private Integer passedCount;

    /** 当日创建且已取消数。 */
    private Integer cancelledCount;
}
