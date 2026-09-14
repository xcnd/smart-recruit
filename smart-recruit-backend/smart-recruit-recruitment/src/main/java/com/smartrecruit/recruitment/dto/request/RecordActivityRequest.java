package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 写入工作台活动动态请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
public class RecordActivityRequest {

    /** 活动类型编码。 */
    private Integer type;

    /** 动态标题。 */
    private String title;

    /** 动态描述。 */
    private String description;

    /** 操作人用户 ID（缺省 0，表示系统）。 */
    private Long actorId;

    /** 操作人姓名（缺省 system）。 */
    private String actorName;

    /** 关联业务类型：CANDIDATE、INTERVIEW、OFFER、ONBOARDING 等。 */
    private String relatedType;

    /** 关联业务 ID。 */
    private Long relatedId;
}
