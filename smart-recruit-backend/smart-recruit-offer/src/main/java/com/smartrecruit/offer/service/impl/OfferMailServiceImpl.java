package com.smartrecruit.offer.service.impl;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.constant.ConfigKeys;
import com.smartrecruit.offer.entity.Offer;
import com.smartrecruit.offer.entity.Contract;
import com.smartrecruit.offer.service.RemoteConfigService;
import com.smartrecruit.offer.feign.SystemClient;
import com.smartrecruit.offer.dto.remote.EmailRequest;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import com.smartrecruit.offer.service.OfferMailService;

/**
 * Offer 邮件服务，负责构建并发送 Offer 录用通知书邮件。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OfferMailServiceImpl implements OfferMailService {

    private final SystemClient systemClient;
    private final RemoteConfigService remoteConfigService;

    @Value("${smart-recruit.frontend-url}")
    private String frontendUrl;

    /**
     * 发送 Offer 录用通知书给候选人。
     *
     * @param offer         Offer 实体
     * @param approvalFlow  审批流程描述（如"审批已通过"）
     */
    public void sendOfferLetter(Offer offer, String approvalFlow) {
        if (offer.getCandidateEmail() == null || offer.getCandidateEmail().isBlank()) {
            log.warn("候选人邮箱为空，无法发送 Offer 邮件: offerId={}, candidateName={}",
                    offer.getId(), offer.getCandidateName());
            return;
        }

        String confirmUrl = offer.getConfirmToken() != null
                ? frontendUrl + "/offer-confirm/" + offer.getConfirmToken()
                : null;
        String subject = "【" + systemName() + "】录用通知书 — " + offer.getPositionTitle();
        String html = buildOfferLetterHtml(offer, approvalFlow, confirmUrl);

        try {
            EmailRequest request =
                    new EmailRequest(offer.getCandidateEmail(), subject, html, true);

            ApiResponse<Void> response = systemClient.sendEmail(request);
            if (response.ok()) {
                log.info("Offer 邮件已发送: offerId={}, candidateEmail={}, offerNo={}",
                        offer.getId(), offer.getCandidateEmail(), offer.getOfferNo());
            } else {
                log.error("Offer 邮件发送失败: offerId={}, code={}, message={}",
                        offer.getId(), response.code(), response.message());
            }
        } catch (Exception e) {
            log.error("Offer 邮件发送异常: offerId={}, candidateEmail={}",
                    offer.getId(), offer.getCandidateEmail(), e);
        }
    }

    /**
     * 发送录用合同到候选人邮箱（附签署链接）。
     */
    @Override
    public void sendContractForSigning(Contract contract) {
        if (contract.getCandidateEmail() == null || contract.getCandidateEmail().isBlank()) {
            log.warn("候选人邮箱为空，无法发送合同邮件: contractNo={}, candidateName={}",
                    contract.getContractNo(), contract.getCandidateName());
            return;
        }
        if (contract.getSignToken() == null || contract.getSignToken().isBlank()) {
            log.warn("合同签署 Token 为空，无法发送签署邮件: contractNo={}", contract.getContractNo());
            return;
        }

        String signUrl = frontendUrl + "/contract-sign/" + contract.getSignToken();
        String subject = "【" + systemName() + "】录用合同待签署 — "
                + (contract.getJobTitle() != null ? contract.getJobTitle() : "录用合同");
        String html = buildContractSigningHtml(contract, signUrl);

        try {
            EmailRequest request =
                    new EmailRequest(contract.getCandidateEmail(), subject, html, true);
            ApiResponse<Void> response = systemClient.sendEmail(request);
            if (response.ok()) {
                log.info("合同邮件已发送: contractNo={}, candidateEmail={}",
                        contract.getContractNo(), contract.getCandidateEmail());
            } else {
                log.error("合同邮件发送失败: contractNo={}, code={}, message={}",
                        contract.getContractNo(), response.code(), response.message());
            }
        } catch (Exception e) {
            log.error("合同邮件发送异常: contractNo={}, candidateEmail={}",
                    contract.getContractNo(), contract.getCandidateEmail(), e);
        }
    }

    /** 构建合同签署通知 HTML 邮件。 */
    private String buildContractSigningHtml(Contract contract, String signUrl) {
        String totalPackage = contract.getTotalPackage() != null
                ? String.format("%,.0f", contract.getTotalPackage()) + " 元" : "面议";
        String validPeriod = (contract.getValidFrom() != null
                ? DateUtils.formatCnDate(contract.getValidFrom()) : "—")
                + " 至 " + (contract.getValidUntil() != null
                ? DateUtils.formatCnDate(contract.getValidUntil()) : "—");

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:'PingFang SC','Microsoft YaHei',Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f1f5f9;padding:32px 0;">
                  <tr><td align="center">
                    <table width="620" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(15,23,42,0.08);">
                      <tr><td style="background:linear-gradient(135deg,#4f46e5,#6366f1);padding:28px 40px;">
                        <div style="color:#ffffff;font-size:20px;font-weight:700;letter-spacing:1px;">%s</div>
                        <div style="color:#e0e7ff;font-size:13px;margin-top:6px;">录用合同签署通知</div>
                      </td></tr>
                      <tr><td style="padding:28px 40px 8px;">
                        <p style="margin:0 0 8px;font-size:15px;color:#1e293b;line-height:1.8;">尊敬的 %s：</p>
                        <p style="margin:0 0 16px;font-size:14px;color:#475569;line-height:1.8;">
                          您好！我们诚挚地邀请您加入 %s。您的录用合同已生成，请点击下方按钮查看合同详情并完成签署。</p>
                        <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;margin-bottom:20px;">
                          <tr><td style="padding:14px 20px;font-size:14px;color:#334155;line-height:2;">
                            <b>合同编号：</b>%s<br/>
                            <b>职位：</b>%s（%s）<br/>
                            <b>合同期限：</b>%s<br/>
                            <b>年薪总包：</b>%s<br/>
                            <b>Offer 编号：</b>%s
                          </td></tr>
                        </table>
                        <p style="margin:0 0 20px;text-align:center;">
                          <a href="%s" style="display:inline-block;padding:13px 40px;background-color:#16a34a;color:#ffffff;text-decoration:none;border-radius:8px;font-size:15px;font-weight:600;">&#9998; 前往签署合同</a>
                        </p>
                        <p style="margin:0 0 6px;font-size:12px;color:#94a3b8;line-height:1.8;">
                          若您对合同内容有疑问，或无法点击上方按钮，请将以下链接复制到浏览器打开：<br/>
                          <span style="color:#64748b;word-break:break-all;">%s</span>
                        </p>
                        <p style="margin:0 0 24px;font-size:12px;color:#94a3b8;line-height:1.8;">
                          此邮件由系统自动发送，请勿直接回复。签署链接在合同审批通过后生效，仅限本人使用。
                        </p>
                      </td></tr>
                      <tr><td style="padding:16px 40px;background-color:#f8fafc;border-top:1px solid #e2e8f0;">
                        <div style="font-size:12px;color:#94a3b8;">%s · 智能招聘管理系统</div>
                      </td></tr>
                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(
                systemName(),
                contract.getCandidateName(),
                systemName(),
                contract.getContractNo(),
                contract.getJobTitle() != null ? contract.getJobTitle() : "—",
                contract.getDepartmentName() != null ? contract.getDepartmentName() : "—",
                validPeriod,
                totalPackage,
                contract.getOfferNo() != null ? contract.getOfferNo() : "—",
                signUrl,
                signUrl,
                systemName());
    }

    /**
     * 构建录用通知书 HTML 邮件。
     */
    private String buildOfferLetterHtml(Offer offer, String approvalFlow, String confirmUrl) {
        String salaryInfo = extractSalaryInfo(offer);
        String onboardDate = offer.getExpectedOnboardDate() != null
                ? DateUtils.formatCnDate(offer.getExpectedOnboardDate()) : "另行通知";
        String validDate = offer.getValidUntil() != null
                ? DateUtils.formatCnDate(offer.getValidUntil()) : "—";

        String actionSection = confirmUrl != null
                ? """
                <!-- Candidate Actions -->
                <tr>
                    <td style="padding:4px 40px 20px;text-align:center;">
                        <p style="margin:0 0 16px;font-size:15px;color:#333333;font-weight:500;">请确认是否接受此 Offer：</p>
                        <a href="%s?action=accept" style="display:inline-block;padding:13px 32px;background-color:#16a34a;color:#ffffff;text-decoration:none;border-radius:8px;font-size:15px;font-weight:600;margin-right:20px;border:2px solid #16a34a;">&#x2705; 接受 Offer</a>
                        <a href="%s?action=reject" style="display:inline-block;padding:13px 32px;background-color:#ffffff;color:#dc2626;text-decoration:none;border-radius:8px;font-size:15px;font-weight:600;border:2px solid #dc2626;">&#x274C; 拒绝 Offer</a>
                        <p style="margin:12px 0 0;font-size:12px;color:#999999;">有效期限：%s</p>
                    </td>
                </tr>
                """.formatted(escapeHtml(confirmUrl), escapeHtml(confirmUrl), validDate)
                : "";

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#f5f7fa;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f5f7fa;padding:30px 0;">
                        <tr>
                            <td align="center">
                                <table width="640" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.06);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#1a73e8 0%%,#4a90d9 100%%);padding:32px 40px;text-align:center;">
                                            <h1 style="margin:0;font-size:24px;font-weight:700;color:#ffffff;letter-spacing:1px;">录用通知书</h1>
                                            <p style="margin:8px 0 0;font-size:14px;color:rgba(255,255,255,0.85);">%s · 智能化招聘管理平台</p>
                                        </td>
                                    </tr>

                                    <!-- Greeting -->
                                    <tr>
                                        <td style="padding:28px 40px 0;">
                                            <p style="margin:0;font-size:15px;color:#333333;line-height:1.8;">
                                                <strong>%s</strong>，您好！
                                            </p>
                                            <p style="margin:12px 0 0;font-size:14px;color:#555555;line-height:1.8;">
                                                感谢您对 %s 的信任与认可。经过严格的面试选拔与综合评估，我们非常高兴地通知您，<strong>您已被正式录用</strong>。具体信息如下：
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Info Card -->
                                    <tr>
                                        <td style="padding:24px 40px;">
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc;border:1px solid #e8edf2;border-radius:8px;">
                                                <tr>
                                                    <td style="padding:20px 24px;">
                                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                                            %s
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Approval Note -->
                                    <tr>
                                        <td style="padding:0 40px 16px;">
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;">
                                                <tr>
                                                    <td style="padding:12px 20px;font-size:13px;color:#166534;text-align:center;">
                                                        %s
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Candidate Actions (Accept/Reject) -->
                                    %s

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding:8px 40px 28px;font-size:12px;color:#999999;line-height:1.8;text-align:center;">
                                            <p style="margin:0;">此邮件由 %s 系统自动发送，请勿回复。</p>
                                            <p style="margin:4px 0 0;">如有疑问请联系 HR：hr@smartrecruit.com</p>
                                            <p style="margin:8px 0 0;color:#bbbbbb;">&copy; 2026 SmartRecruit. All rights reserved.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(systemName()),
                escapeHtml(offer.getCandidateName()),
                escapeHtml(systemName()),
                buildInfoRows(offer, onboardDate, validDate),
                approvalFlow,
                actionSection,
                escapeHtml(systemName())
        );
    }

    private String buildInfoRows(Offer offer, String onboardDate, String validDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(infoRow("Offer 编号", offer.getOfferNo()));
        sb.append(infoRow("录用职位", offer.getPositionTitle()));
        sb.append(infoRow("所属部门", offer.getDepartmentName()));
        if (offer.getLevel() != null && !offer.getLevel().isBlank()) {
            sb.append(infoRow("录用职级", offer.getLevel()));
        }
        sb.append(infoRow("入职日期", onboardDate));
        sb.append(infoRow("Offer 有效期至", validDate));
        sb.append(infoRow("薪酬方案", extractSalaryInfo(offer)));
        return sb.toString();
    }

    private String infoRow(String label, String value) {
        return """
                <tr>
                    <td style="padding:8px 0;font-size:14px;color:#666666;width:120px;vertical-align:top;">%s</td>
                    <td style="padding:8px 0;font-size:14px;color:#222222;font-weight:500;">%s</td>
                </tr>
                """.formatted(label, value != null ? value : "—");
    }

    @SuppressWarnings("unchecked")
    private String extractSalaryInfo(Offer offer) {
        if (!(offer.getSalaryStructure() instanceof Map<?, ?> map)) {
            return "详见正式 Offer 文件";
        }
        StringBuilder sb = new StringBuilder();
        Object base = map.get("baseSalary");
        if (base instanceof Number) {
            sb.append("月薪 ¥").append(String.format("%,.0f", ((Number) base).doubleValue()));
        }
        Object bonus = map.get("bonusMonths");
        if (bonus instanceof Number) {
            if (!sb.isEmpty()) sb.append("，");
            sb.append("奖金 ").append(((Number) bonus).intValue()).append(" 个月");
        }
        Object stock = map.get("stockOptions");
        if (stock instanceof Number && ((Number) stock).doubleValue() > 0) {
            if (!sb.isEmpty()) sb.append("，");
            BigDecimal s = BigDecimal.valueOf(((Number) stock).doubleValue());
            sb.append("期权 ").append(s.toPlainString()).append(" 股");
        }
        Object total = map.get("total");
        if (total instanceof Number) {
            double t = ((Number) total).doubleValue();
            sb.append("（年薪总包 ¥").append(String.format("%,.0f", t / 10000)).append(" 万）");
        }
        return sb.isEmpty() ? "详见正式 Offer 文件" : sb.toString();
    }

    // ---- 审批流程通知邮件 ----

    /**
     * 发送审批请求邮件给审批人。
     */
    public void sendApprovalRequestEmail(Offer offer, String approverEmail, String approverName,
                                         String nodeName, int level, int totalLevels) {
        String offerDetailUrl = frontendUrl + "/offers/" + offer.getId() + "/approval-review";
        String subject = "【" + systemName() + "】待审批 Offer — " + offer.getCandidateName()
                + "(" + offer.getDepartmentName() + ") " + offer.getOfferNo() + "（第" + level + "级审批）";
        String html = buildNotificationHtml(
                "审批请求",
                offer,
                """
                <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top:16px;">
                    <tr>
                        <td style="padding:16px 20px;background-color:#f0f7ff;border-left:4px solid #1a73e8;border-radius:4px;">
                            <p style="margin:0;font-size:15px;color:#1a73e8;"><strong>%s</strong>，您好</p>
                            <p style="margin:8px 0 0;font-size:14px;color:#333;line-height:1.8;">
                                有一份 Offer 需要您审批（<strong>第 %d/%d 级 · %s</strong>），请及时登录系统处理。
                            </p>
                        </td>
                    </tr>
                </table>
                """.formatted(escapeHtml(approverName), level, totalLevels, escapeHtml(nodeName)),
                """
                <a href="%s" style="display:inline-block;padding:12px 32px;background-color:#1a73e8;color:#ffffff;
                text-decoration:none;border-radius:6px;font-size:15px;font-weight:600;margin-top:20px;">前往审批</a>
                """.formatted(escapeHtml(offerDetailUrl))
        );
        sendNotificationEmail(approverEmail, subject, html);
    }

    /**
     * 发送审批进度通知给 Offer 创建人（某一级通过，但非最后一级）。
     */
    public void sendApprovalProgressToCreator(Offer offer, String creatorEmail,
                                              String approverName, String nodeName, int level, int totalLevels) {
        String subject = "【" + systemName() + "】Offer 审批进度 — " + offer.getCandidateName()
                + "(" + offer.getDepartmentName() + ") " + offer.getOfferNo() + "（第" + level + "级已通过）";
        String html = buildNotificationHtml(
                "审批进度通知",
                offer,
                """
                <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top:16px;">
                    <tr>
                        <td style="padding:16px 20px;background-color:#f0fdf4;border-left:4px solid #16a34a;border-radius:4px;">
                            <p style="margin:0;font-size:15px;color:#16a34a;"><strong>审批进度更新</strong></p>
                            <p style="margin:8px 0 0;font-size:14px;color:#333;line-height:1.8;">
                                <strong>%s</strong> 已完成 <strong>第 %d/%d 级（%s）</strong> 审批，审批结果为 <span style="color:#16a34a;font-weight:600;">通过</span>。<br/>
                                已自动流转至下一级审批人，请关注后续进度。
                            </p>
                        </td>
                    </tr>
                </table>
                """.formatted(escapeHtml(approverName), level, totalLevels, escapeHtml(nodeName)),
                ""
        );
        sendNotificationEmail(creatorEmail, subject, html);
    }

    /**
     * 发送全部审批完成通知给 Offer 创建人。
     */
    public void sendApprovalCompleteToCreator(Offer offer, String creatorEmail) {
        String subject = "【" + systemName() + "】Offer 审批全部通过 — " + offer.getCandidateName()
                + "(" + offer.getDepartmentName() + ") " + offer.getOfferNo();
        String html = buildNotificationHtml(
                "审批完成",
                offer,
                """
                <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top:16px;">
                    <tr>
                        <td style="padding:16px 20px;background-color:#f0fdf4;border-left:4px solid #16a34a;border-radius:4px;">
                            <p style="margin:0;font-size:15px;color:#16a34a;"><strong>全部审批已通过</strong></p>
                            <p style="margin:8px 0 0;font-size:14px;color:#333;line-height:1.8;">
                                该 Offer 的<strong>全部审批流程已完成</strong>，审批结果为 <span style="color:#16a34a;font-weight:600;">通过</span>。<br/>
                                Offer 录用通知已同步发送给候选人，请知悉。
                            </p>
                        </td>
                    </tr>
                </table>
                """,
                ""
        );
        sendNotificationEmail(creatorEmail, subject, html);
    }

    /**
     * 发送审批驳回通知给 Offer 创建人。
     */
    public void sendOfferRejectedToCreator(Offer offer, String creatorEmail,
                                           String approverName, String role, String comment) {
        String subject = "【" + systemName() + "】Offer 审批被驳回 — " + offer.getCandidateName()
                + "(" + offer.getDepartmentName() + ") " + offer.getOfferNo();
        String commentHtml = (comment != null && !comment.isBlank())
                ? """
                <p style="margin:8px 0 0;font-size:13px;color:#666;line-height:1.6;">
                    <strong>审批意见：</strong>%s
                </p>
                """.formatted(escapeHtml(comment))
                : "";
        String html = buildNotificationHtml(
                "审批驳回",
                offer,
                """
                <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top:16px;">
                    <tr>
                        <td style="padding:16px 20px;background-color:#fef2f2;border-left:4px solid #dc2626;border-radius:4px;">
                            <p style="margin:0;font-size:15px;color:#dc2626;"><strong>审批被驳回</strong></p>
                            <p style="margin:8px 0 0;font-size:14px;color:#333;line-height:1.8;">
                                <strong>%s</strong>（%s）驳回了该 Offer 的审批申请。<br/>
                                Offer 已退回至草稿状态，您可以根据审批意见修改后重新提交。
                            </p>
                            %s
                        </td>
                    </tr>
                </table>
                """.formatted(escapeHtml(approverName), escapeHtml(role != null ? role : "审批人"), commentHtml),
                ""
        );
        sendNotificationEmail(creatorEmail, subject, html);
    }

    // ---- 通知邮件私有辅助 ----

    /**
     * 构建通知邮件的通用 HTML 骨架。
     */
    private String buildNotificationHtml(String title, Offer offer, String bodyHtml, String actionButton) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#f5f7fa;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f5f7fa;padding:30px 0;">
                        <tr>
                            <td align="center">
                                <table width="640" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.06);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#1a73e8 0%%,#4a90d9 100%%);padding:24px 40px;text-align:center;">
                                            <h1 style="margin:0;font-size:20px;font-weight:700;color:#ffffff;">%s</h1>
                                        </td>
                                    </tr>
                                    <!-- Offer 摘要卡片 -->
                                    <tr>
                                        <td style="padding:20px 40px 0;">
                                            %s
                                        </td>
                                    </tr>
                                    <!-- 正文区域 -->
                                    <tr>
                                        <td style="padding:0 40px 20px;">
                                            %s
                                            %s
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding:8px 40px 28px;font-size:12px;color:#999999;line-height:1.8;text-align:center;">
                                            <p style="margin:0;">此邮件由 %s 系统自动发送，请勿回复。</p>
                                            <p style="margin:8px 0 0;color:#bbbbbb;">&copy; 2026 SmartRecruit. All rights reserved.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(title),
                buildCompactOfferInfoHtml(offer),
                bodyHtml,
                actionButton,
                escapeHtml(systemName())
        );
    }

    /**
     * 构建紧凑的 Offer 摘要信息（用于通知邮件）。
     */
    private String buildCompactOfferInfoHtml(Offer offer) {
        return """
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc;border:1px solid #e8edf2;border-radius:8px;">
                    <tr>
                        <td style="padding:14px 20px;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                                %s
                                %s
                                %s
                                %s
                                %s
                            </table>
                        </td>
                    </tr>
                </table>
                """.formatted(
                compactInfoRow("Offer 编号", offer.getOfferNo()),
                compactInfoRow("候选人", offer.getCandidateName()),
                compactInfoRow("录用职位", offer.getPositionTitle()),
                compactInfoRow("所属部门", offer.getDepartmentName()),
                compactInfoRow("薪酬方案", extractSalaryInfo(offer))
        );
    }

    private String compactInfoRow(String label, String value) {
        return """
                <tr>
                    <td style="padding:4px 0;font-size:13px;color:#888;width:80px;white-space:nowrap;">%s</td>
                    <td style="padding:4px 0;font-size:13px;color:#333;font-weight:500;">%s</td>
                </tr>
                """.formatted(label, value != null ? value : "—");
    }

    /**
     * 统一发送通知邮件（best-effort，失败不抛异常）。
     */
    private void sendNotificationEmail(String to, String subject, String html) {
        if (to == null || to.isBlank()) {
            log.warn("收件人邮箱为空，跳过邮件发送: subject={}", subject);
            return;
        }
        try {
            EmailRequest request = new EmailRequest(to, subject, html, true);

            ApiResponse<Void> response = systemClient.sendEmail(request);
            if (response.ok()) {
                log.info("通知邮件已发送: to={}, subject={}", to, subject);
            } else {
                log.error("通知邮件发送失败: to={}, code={}, message={}", to, response.code(), response.message());
            }
        } catch (Exception e) {
            log.warn("通知邮件发送异常: to={}, subject={}", to, subject, e);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /**
     * 系统名称：优先读取系统配置，缺失时回退默认值。
     */
    private String systemName() {
        return remoteConfigService.getString(ConfigKeys.SYSTEM_NAME, "SmartRecruit");
    }
}
