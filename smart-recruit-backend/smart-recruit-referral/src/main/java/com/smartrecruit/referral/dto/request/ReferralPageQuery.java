package com.smartrecruit.referral.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 内推记录的分页查询参数。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
public class ReferralPageQuery {

    /** 页码（从1开始）。 */
    @Min(value = 1, message = "Page must be >= 1")
    private Integer page = 1;

    /** 每页大小。 */
    @Min(value = 1, message = "Size must be >= 1")
    @Max(value = 100, message = "Size must be <= 100")
    private Integer size = 20;

    /** 搜索关键词（匹配候选人姓名、邮箱、职位名称）。 */
    private String keyword;

    /** 按记录状态筛选。 */
    private Integer status;

    /** 按推荐人ID筛选。 */
    private Long referrerId;

    /** 按计划ID筛选。 */
    private Long programId;
}
