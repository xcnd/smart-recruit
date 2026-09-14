package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 登录响应 DTO。
 *
 * @since 2026-04-26
 */
@JsonPropertyOrder({"accessToken", "refreshToken", "tokenType", "expiresIn", "userInfo"})
public record LoginResponse(
        @JsonProperty("accessToken")
        String accessToken,

        @JsonProperty("refreshToken")
        String refreshToken,

        @JsonProperty("tokenType")
        String tokenType,

        @JsonProperty("expiresIn")
        long expiresIn,

        @JsonProperty("userInfo")
        UserVO userInfo
) {
    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn, UserVO userInfo) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, userInfo);
    }
}
