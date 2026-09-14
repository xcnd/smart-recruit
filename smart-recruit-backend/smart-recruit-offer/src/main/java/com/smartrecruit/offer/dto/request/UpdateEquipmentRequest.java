package com.smartrecruit.offer.dto.request;

import lombok.Data;

/**
 * 更新入职设备状态的请求 DTO。
 *
 * @since 1.0.0
 */
@Data
public class UpdateEquipmentRequest {

    /** 设备记录 ID。 */
    private Long equipmentId;

    /** 设备状态：0=PENDING,1=ASSIGNED,2=SHIPPED,3=DELIVERED。 */
    private Integer status;

    /** 资产编号。 */
    private String assetNo;

    /** 备注。 */
    private String remark;
}
