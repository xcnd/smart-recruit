package com.smartrecruit.referral.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 生成分享令牌请求。
 *
 * @author xdh
 * @since 2026-04-01
 */
@Data
public class ShareTokenRequest {

    @NotNull(message = "Program ID is required")
    private Long programId;

    private String referrerName;

    private String source;
}
