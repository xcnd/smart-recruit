package com.smartrecruit.offer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合同实体，映射 {@code ofr_contract} 表。
 *
 * <p>管理招聘环节的 Offer 合同：从 Offer 生成合同、审批、
 * 发送候选人签署、归档全生命周期。</p>
 *
 * @since 2026-04-09
 */
@Data
@TableName("ofr_contract")
public class Contract implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String contractNo;
    private Long offerId;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String jobTitle;
    private String departmentName;
    private String offerNo;
    private BigDecimal totalPackage;
    private String content;
    private Integer status;
    private String signToken;
    private String signedByHr;
    private String signedByCandidate;
    /** 候选人电子签章（手写签名 PNG data URL）。 */
    private String candidateSignature;
    /** 候选人身份证号码。 */
    private String candidateIdCard;
    /** 候选人联系电话。 */
    private String candidatePhone;
    /** 候选人通讯地址。 */
    private String candidateAddress;
    private LocalDateTime signTime;
    private String rejectReason;
    /** 作废原因。 */
    private String voidReason;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    private String createBy;

    /** 创建人ID。 */
    private Long createUserId;

    private String updateBy;

    /** 更新人ID。 */
    private Long updateUserId;
    @TableLogic
    private Integer deleted;
}
