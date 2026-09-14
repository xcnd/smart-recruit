package com.smartrecruit.referral.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 批量保存计划职位的请求条目。
 *
 * @author xdh
 * @since 2026-05-31
 */
@Data
public class BatchSaveProgramJobRequest {

    /** 关联ID（更新时传，新增时不传）。前端传字符串以避免JS精度丢失。 */
    private String id;

    /** 职位ID（新增时必须传）。 */
    @NotNull(message = "职位ID不能为空")
    private Long jobPositionId;

    /** 是否启用：0=禁用，1=启用。 */
    private Integer isEnabled;

    /** 职位级奖金覆盖（null表示使用计划默认奖金）。 */
    private BigDecimal bonusAmount;

    /** 职位标签：0=URGENT, 1=HIGH_BONUS, 2=TECH, 3=INTERN。 */
    private Integer tag;

    /** 职位标题快照。 */
    private String jobTitle;

    /** 最低薪资快照。 */
    private Integer minSalary;

    /** 最高薪资快照。 */
    private Integer maxSalary;

    /** 招聘人数。 */
    private Integer headCount;
}
