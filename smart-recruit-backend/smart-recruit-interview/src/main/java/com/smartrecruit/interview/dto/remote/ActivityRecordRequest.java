package com.smartrecruit.interview.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 写入活动动态请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRecordRequest {

    /** 活动类型编码。 */
    private Integer type;

    /** 动态标题。 */
    private String title;

    /** 动态描述。 */
    private String description;

    /** 操作人用户 ID。 */
    private Long actorId;

    /** 操作人姓名。 */
    private String actorName;

    /** 关联业务类型。 */
    private String relatedType;

    /** 关联业务 ID。 */
    private Long relatedId;
}
