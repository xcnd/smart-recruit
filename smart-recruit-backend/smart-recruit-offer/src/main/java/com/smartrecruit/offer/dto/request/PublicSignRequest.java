package com.smartrecruit.offer.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 候选人公开签署请求。
 *
 * @param name   候选人签署姓名
 * @param accept 是否接受：true=签署，false=拒绝
 * @param remark 备注/拒绝原因
 * @param signature 候选人电子签章（手写签名 PNG data URL，签署时必填）
 * @param idCard 候选人身份证号码
 * @param phone 候选人联系电话
 * @param address 候选人通讯地址
 * @since 2026-04-09
 */
public record PublicSignRequest(
        @NotBlank(message = "签署姓名不能为空") String name,
        boolean accept,
        String remark,
        String signature,
        String idCard,
        String phone,
        String address) {
}
