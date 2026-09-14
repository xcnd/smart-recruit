package com.smartrecruit.offer.dto.request;

import lombok.Data;

/**
 * 更新导师/伙伴请求。
 *
 * @since 1.0.0
 */
@Data
public class UpdateMentorRequest {

    /** 导师用户 ID。 */
    private Long mentorId;

    /** 伙伴用户 ID。 */
    private Long buddyId;
}
