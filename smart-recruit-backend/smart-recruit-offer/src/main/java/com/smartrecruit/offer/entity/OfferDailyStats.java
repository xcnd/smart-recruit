package com.smartrecruit.offer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Offer/入职日汇总实体，映射 {@code analytics_offer_daily} 表。
 *
 * <p>由定时任务按发送日期聚合生成，分析页查询只读本表。</p>
 *
 * @since 2026-04-06
 */
@Data
@TableName("analytics_offer_daily")
public class OfferDailyStats implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计日期（数据日，yyyy-MM-dd）。 */
    @TableId(type = IdType.INPUT)
    private LocalDate statDate;

    /** 当日发送 Offer 份数。 */
    private Integer sentCount;

    /** 当日候选人接受 Offer 份数。 */
    private Integer acceptedCount;

    /** 当日候选人拒绝 Offer 份数。 */
    private Integer declinedCount;

    /** 当日仍处于待回复状态的 Offer 份数。 */
    private Integer pendingCount;

    /** 当日完成入职人数。 */
    private Integer onboardCount;

    /** 当日已确认 Offer 的「发送→确认」累计天数，用于计算平均确认周期。 */
    private Long confirmTotalDays;

    /** 当日完成确认（接受/拒绝）的 Offer 份数。 */
    private Integer confirmCount;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
