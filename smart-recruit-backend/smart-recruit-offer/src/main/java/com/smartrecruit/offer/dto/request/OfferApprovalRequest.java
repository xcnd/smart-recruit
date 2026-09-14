package com.smartrecruit.offer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批或拒绝Offer的请求DTO。
 *
 * @since 1.0.0
 */
@Data
public class OfferApprovalRequest {

    /** 审批人 ID。 */
    @NotNull(message = "审批人ID不能为空")
    private Long approverId;

    /** 审批人姓名。 */
    @NotBlank(message = "审批人姓名不能为空")
    private String approverName;

    /** 审批人角色，如"团队负责人"、"HR经理"。 */
    private String approverRole;

    /** 审批级别（1, 2, 3...），默认为1以兼容旧版调用。 */
    private Integer approvalLevel;

    /** 审批结果：APPROVED、REJECTED。 */
    @NotNull(message = "审批结果不能为空")
    private Integer status;

    /** 审批意见。 */
    private String comment;
}
