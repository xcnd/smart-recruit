package com.smartrecruit.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合同列表 VO。
 *
 * @since 2026-04-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractVO {

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
    private Integer status;
    private String statusLabel;
    private String signedByHr;
    private String signedByCandidate;
    private String voidReason;
    private LocalDateTime signTime;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String createBy;
    private LocalDateTime createTime;
    private String remark;
}
