package com.smartrecruit.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * 合同详情 VO（含合同正文与签署记录）。
 *
 * @since 2026-04-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractDetailVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String contractNo;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long offerId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String jobTitle;
    private String departmentName;
    private String offerNo;
    private BigDecimal totalPackage;
    private String content;
    private Integer status;
    private String statusLabel;
    private String signedByHr;
    private String signedByCandidate;
    /** 候选人电子签章（手写签名 PNG data URL）。 */
    private String candidateSignature;
    private String candidateIdCard;
    private String candidatePhone;
    private String candidateAddress;
    private LocalDateTime signTime;
    private String rejectReason;
    private String voidReason;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;
    private List<ContractSignRecordVO> signRecords;
}
