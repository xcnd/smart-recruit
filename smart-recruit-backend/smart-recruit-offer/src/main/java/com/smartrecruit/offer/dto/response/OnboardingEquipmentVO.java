package com.smartrecruit.offer.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 入职设备结构化视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingEquipmentVO {

    /** 设备记录 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 设备类型码：0=笔记本电脑,1=显示器,2=手机,3=门禁卡,4=工位。 */
    private Integer equipmentType;

    /** 设备名称。 */
    private String equipmentName;

    /** 状态码：0=待分配,1=已分配,2=已发出,3=已签收。 */
    private Integer status;

    /** 状态中文标签。 */
    private String statusLabel;

    /** 资产编号。 */
    private String assetNo;

    /** 分配人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assignedBy;

    /** 分配人姓名。 */
    private String assignedByName;

    /** 分配时间。 */
    private LocalDateTime assignedTime;

    /** 交付/签收时间。 */
    private LocalDateTime deliveredTime;

    /** 备注。 */
    private String remark;
}
