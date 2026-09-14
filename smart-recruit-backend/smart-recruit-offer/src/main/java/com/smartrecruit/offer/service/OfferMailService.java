package com.smartrecruit.offer.service;

import com.smartrecruit.offer.entity.Offer;
import com.smartrecruit.offer.entity.Contract;

/**
 * Offer 邮件服务，负责构建并发送 Offer 录用通知书邮件。
 *
 * @since 1.0.0
 */
public interface OfferMailService {

    /**
     * 发送 Offer 录用通知书给候选人。
     *
     * @param offer         Offer 实体
     * @param approvalFlow  审批流程描述（如"审批已通过"）
     */
    void sendOfferLetter(Offer offer, String approvalFlow);

    /**
     * 发送录用合同到候选人邮箱（附签署链接）。
     *
     * @param contract 待签署合同
     */
    void sendContractForSigning(Contract contract);

    /**
     * 发送审批请求邮件给审批人。
     */
    void sendApprovalRequestEmail(Offer offer, String approverEmail, String approverName,
                                  String nodeName, int level, int totalLevels);

    /**
     * 发送审批进度通知邮件给 Offer 发起人。
     */
    void sendApprovalProgressToCreator(Offer offer, String creatorEmail,
                                       String approverName, String nodeName, int level, int totalLevels);

    /**
     * 审批完成后向发起人发送通知邮件。
     */
    void sendApprovalCompleteToCreator(Offer offer, String creatorEmail);

    /**
     * 发送审批被驳回通知邮件给 Offer 发起人。
     */
    void sendOfferRejectedToCreator(Offer offer, String creatorEmail,
                                    String approverName, String role, String comment);
}
