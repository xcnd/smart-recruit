package com.smartrecruit.referral.dto.response;

import lombok.Data;

/**
 * 分享令牌生成响应。
 *
 * @author xdh
 * @since 2026-04-01
 */
@Data
public class ShareTokenResponse {
    private String token;
    private String url;
    private String referralCode;
    private String referrerName;
    private String source;

    public static ShareTokenResponse of(String token, String url, String referralCode,
                                        String referrerName, String source) {
        ShareTokenResponse resp = new ShareTokenResponse();
        resp.setToken(token);
        resp.setUrl(url);
        resp.setReferralCode(referralCode);
        resp.setReferrerName(referrerName);
        resp.setSource(source);
        return resp;
    }
}
