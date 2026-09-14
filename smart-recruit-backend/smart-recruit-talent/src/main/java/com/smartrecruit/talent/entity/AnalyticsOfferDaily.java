package com.smartrecruit.talent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Offer/入职日汇总快照，映射 {@code analytics_offer_daily} 表。
 *
 * @since 2026-04-10
 */
@Data
@TableName("analytics_offer_daily")
public class AnalyticsOfferDaily implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计日期。 */
    @TableId(type = IdType.INPUT)
    private LocalDate statDate;

    /** 当日发送 Offer 数。 */
    private Integer sentCount;

    /** 当日发送且被接受数。 */
    private Integer acceptedCount;

    /** 当日发送且被拒绝数。 */
    private Integer declinedCount;

    /** 当日发送且待回复数。 */
    private Integer pendingCount;

    /** 当日完成入职人数。 */
    private Integer onboardCount;

    /** 当日发送的 Offer 确认周期天数合计。 */
    private Long confirmTotalDays;

    /** 当日发送且有确认周期的 Offer 数。 */
    private Integer confirmCount;
}
