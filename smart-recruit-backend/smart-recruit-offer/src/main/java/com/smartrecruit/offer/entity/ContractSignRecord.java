package com.smartrecruit.offer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 合同签署记录实体，映射 {@code ofr_contract_sign_record} 表。
 *
 * @since 2026-04-09
 */
@Data
@TableName("ofr_contract_sign_record")
public class ContractSignRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long contractId;
    /** 签署方：0=HR,1=候选人。 */
    private Integer signerType;
    private Long signerId;
    private String signerName;
    /** 动作：1=签署,2=拒绝。 */
    private Integer action;
    private LocalDateTime signTime;
    private String signIp;
    private String remark;
}
