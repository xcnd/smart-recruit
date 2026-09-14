package com.smartrecruit.referral.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 排行榜条目（顶级推荐人）的VO。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
public class LeaderboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 内推人 ID。 */
    private Long referrerId;
    /** 内推人姓名（需跨服务查询）。 */
    private String referrerName;
    /** 内推人部门。 */
    private String departmentName;
    /** 头像URL。 */
    private String avatar;
    /** 排名。 */
    private Integer rank;
    /** 内推总次数。 */
    private Long referralCount;
    /** 累计获得奖金（元）。 */
    private BigDecimal totalBonus;
}
