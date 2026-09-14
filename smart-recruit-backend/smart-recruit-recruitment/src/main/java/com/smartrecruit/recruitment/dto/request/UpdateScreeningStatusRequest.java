package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 更新简历筛选状态请求。
 *
 * @since 2026-04-07
 */
@Data
public class UpdateScreeningStatusRequest {

    /** 目标筛选状态编码。 */
    private Integer screeningStatus;
}
