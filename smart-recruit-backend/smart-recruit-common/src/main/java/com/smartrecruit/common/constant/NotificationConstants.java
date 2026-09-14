package com.smartrecruit.common.constant;

/**
 * 系统通知常量定义。
 *
 * <p>统一维护通知业务分类与业务子类型，供各微服务
 * 在产生业务事件时通过系统模块发送站内通知。</p>
 *
 * @since 2026-04-05
 */
public final class NotificationConstants {

    private NotificationConstants() {
        throw new UnsupportedOperationException("Constant class cannot be instantiated");
    }

    // ==================== 通知业务分类（sys_notification.type） ====================

    /** 系统消息。 */
    public static final int TYPE_SYSTEM = 0;

    /** 面试相关。 */
    public static final int TYPE_INTERVIEW = 1;

    /** Offer 相关。 */
    public static final int TYPE_OFFER = 2;

    /** 入职相关。 */
    public static final int TYPE_ONBOARDING = 3;

    /** 内推相关。 */
    public static final int TYPE_REFERRAL = 4;

    /** 人才库相关。 */
    public static final int TYPE_TALENT = 5;

    /** 招聘/简历相关。 */
    public static final int TYPE_RECRUITMENT = 6;

    /** 合同相关。 */
    public static final int TYPE_CONTRACT = 7;

    // ==================== 业务子类型（sys_notification.business_type） ====================

    /** Offer：待审批。 */
    public static final String BIZ_OFFER_APPROVAL_REQUEST = "OFFER_APPROVAL_REQUEST";

    /** Offer：审批进度更新。 */
    public static final String BIZ_OFFER_APPROVAL_PROGRESS = "OFFER_APPROVAL_PROGRESS";

    /** Offer：全部审批完成并已发送。 */
    public static final String BIZ_OFFER_APPROVAL_COMPLETE = "OFFER_APPROVAL_COMPLETE";

    /** Offer：审批被驳回。 */
    public static final String BIZ_OFFER_REJECTED = "OFFER_REJECTED";

    /** 面试：已安排。 */
    public static final String BIZ_INTERVIEW_SCHEDULED = "INTERVIEW_SCHEDULED";

    /** 面试：已完成并生成报告。 */
    public static final String BIZ_INTERVIEW_COMPLETED = "INTERVIEW_COMPLETED";

    /** 简历：新简历待筛选。 */
    public static final String BIZ_RESUME_UPLOADED = "RESUME_UPLOADED";

    /** 简历：筛选通过。 */
    public static final String BIZ_RESUME_SCREEN_PASSED = "RESUME_SCREEN_PASSED";

    /** 简历：筛选未通过。 */
    public static final String BIZ_RESUME_SCREEN_REJECTED = "RESUME_SCREEN_REJECTED";

    /** 合同：提交审批。 */
    public static final String BIZ_CONTRACT_APPROVAL_REQUEST = "CONTRACT_APPROVAL_REQUEST";

    /** 合同：审批完成。 */
    public static final String BIZ_CONTRACT_APPROVAL_COMPLETE = "CONTRACT_APPROVAL_COMPLETE";

    /** 合同：候选人已签署。 */
    public static final String BIZ_CONTRACT_SIGNED = "CONTRACT_SIGNED";

    /** 合同：候选人拒绝签署。 */
    public static final String BIZ_CONTRACT_REJECTED = "CONTRACT_REJECTED";

    /** 入职：风险预警。 */
    public static final String BIZ_ONBOARDING_RISK = "ONBOARDING_RISK";

    /** 入职：流程完成。 */
    public static final String BIZ_ONBOARDING_COMPLETED = "ONBOARDING_COMPLETED";

    /** 系统公告。 */
    public static final String BIZ_ANNOUNCEMENT = "ANNOUNCEMENT";
}
