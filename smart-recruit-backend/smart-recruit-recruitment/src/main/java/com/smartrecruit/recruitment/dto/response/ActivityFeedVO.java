package com.smartrecruit.recruitment.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动动态视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
public class ActivityFeedVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 活动类型码：0=投递,1=筛选,2=面试,3=Offer,4=入职,5=内推,6=系统 */
    private Integer type;

    /** 活动类型标签 */
    private String typeLabel;

    /** 活动标题 */
    private String title;

    /** 活动描述 */
    private String description;

    /** 操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long actorId;

    /** 操作人姓名 */
    private String actorName;

    /** 关联业务类型 */
    private String relatedType;

    /** 关联业务ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long relatedId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
