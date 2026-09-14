package com.smartrecruit.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 面试日汇总实体，映射 {@code analytics_interview_daily} 表。
 *
 * <p>由定时任务按创建日期聚合生成，分析页查询只读本表。</p>
 *
 * @since 2026-04-06
 */
@Data
@TableName("analytics_interview_daily")
public class InterviewDailyStats implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计日期（数据日，yyyy-MM-dd）。 */
    @TableId(type = IdType.INPUT)
    private LocalDate statDate;

    /** 当日面试总场次。 */
    private Integer totalCount;

    /** 当日面试通过场次。 */
    private Integer passedCount;

    /** 当日面试取消场次。 */
    private Integer cancelledCount;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
