package com.smartrecruit.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

/**
 * 合同签署记录 VO。
 *
 * @since 2026-04-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSignRecordVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long contractId;
    /** 签署方：0=HR,1=候选人。 */
    private Integer signerType;
    private String signerTypeLabel;
    private String signerName;
    /** 动作：1=签署,2=拒绝。 */
    private Integer action;
    private String actionLabel;
    private LocalDateTime signTime;
    private String signIp;
    private String remark;
}
