package com.smartrecruit.referral.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 奖金发放记录 VO。
 *
 * @author xdh
 * @since 2026-04-01
 */
@Data
public class BonusRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录 ID。 */
    private Long id;
    /** 关联的内推记录 ID。 */
    private Long refRecordId;
    /** 发放阶段：0=入职, 1=入职首月, 2=试用期, 3=转正, 4=全额。 */
    private Integer stage;
    /** 阶段名称。 */
    private String stageName;
    /** 本阶段奖金金额。 */
    private BigDecimal amount;
    /** 状态：0=待发放, 1=已发放, 2=已取消。 */
    private Integer status;
    /** 实际发放时间。 */
    private LocalDateTime paidTime;
}
