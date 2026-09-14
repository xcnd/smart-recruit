package com.smartrecruit.offer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 入职设备实体，映射 {@code rec_onboarding_equipment} 表。
 *
 * <p>管理入职流程中分配给新员工的 IT 设备和资产，包括笔记本电脑、显示器及配件。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName("rec_onboarding_equipment")
public class OnboardingEquipment implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 入职流程 ID。 */
    private Long onboardingId;

    /** 设备类别：LAPTOP、MONITOR、KEYBOARD、MOUSE、HEADSET、DOCK。 */
    private Integer equipmentType;

    /** 设备显示名称。 */
    private String equipmentName;

    /** 设备状态：PENDING、ASSIGNED、DELIVERED、RETURNED。 */
    private Integer status;

    /** 资产追踪编号。 */
    private String assetNo;

    /** 分配设备的用户 ID。 */
    private Long assignedBy;

    /** 设备分配时间。 */
    private LocalDateTime assignedTime;

    /** 设备交付给员工的时间。 */
    private LocalDateTime deliveredTime;

    /** 备注或附加说明。 */
    private String remark;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID。 */
    private Long createUserId;

    /** 创建人用户名。 */
    private String createBy;

    /** 更新人ID。 */
    private Long updateUserId;

    /** 更新人用户名。 */
    private String updateBy;
}
