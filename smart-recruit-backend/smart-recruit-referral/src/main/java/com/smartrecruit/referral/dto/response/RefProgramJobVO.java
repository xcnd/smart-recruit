package com.smartrecruit.referral.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计划-职位关联 VO。
 *
 * @author xdh
 * @since 2026-05-31
 */
@Data
public class RefProgramJobVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long programId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobPositionId;
    private Integer isEnabled;
    private BigDecimal bonusAmount;
    private Integer tag;
    private String jobTitle;
    private Integer minSalary;
    private Integer maxSalary;
    private Integer headCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
