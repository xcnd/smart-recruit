package com.smartrecruit.system.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.system.service.EmailService;
import com.smartrecruit.system.dto.request.SendEmailRequest;
import com.smartrecruit.system.dto.request.SendAssessmentEmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 内部邮件发送控制器 — 供其他微服务通过 Feign 调用。
 *
 * <p>所有端点仅允许内部服务间调用，携带 INNER-REQUEST: true 头部。</p>
 *
 * <p>核心接口：{@code POST /api/v1/internal/email/send}，接受通用的
 * {to, subject, content, html} 参数，其他服务自行构建邮件内容后调用此接口。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/internal/email")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;

    // ================================================================
    // 通用发送接口
    // ================================================================

    /**
     * 发送邮件（通用接口）。
     *
     * <p>请求参数：</p>
     * <ul>
     *   <li>{@code to}       — 收件人邮箱（必填）</li>
     *   <li>{@code subject}  — 邮件主题（必填）</li>
     *   <li>{@code content}  — 邮件内容（必填）</li>
     *   <li>{@code html}     — 是否为 HTML 格式（可选，默认 true）</li>
     * </ul>
     */
    @PostMapping("/send")
    public ApiResponse<Void> send(@RequestBody SendEmailRequest request) {
        String to = request.getTo();
        String subject = request.getSubject();
        String content = request.getContent();
        boolean html = !Boolean.FALSE.equals(request.getHtml());

        if (to == null || to.isBlank()) {
            return ApiResponse.error(400, "收件人邮箱不能为空");
        }
        if (subject == null || subject.isBlank()) {
            return ApiResponse.error(400, "邮件主题不能为空");
        }
        if (content == null || content.isBlank()) {
            return ApiResponse.error(400, "邮件内容不能为空");
        }

        log.info("发送邮件: to={}, subject={}, html={}", to, subject, html);
        emailService.sendEmail(to, subject, content, html);
        return ApiResponse.success("邮件已发送", null);
    }

    // ================================================================
    // 业务特定接口（便捷封装）
    // ================================================================

    /**
     * 发送在线测评邀请邮件。
     */
    @PostMapping("/send-assessment")
    public ApiResponse<Void> sendAssessmentEmail(@RequestBody @Valid SendAssessmentEmailRequest request) {
        String to = request.getTo();
        String candidateName = request.getCandidateName() != null ? request.getCandidateName() : "";
        String jobTitle = request.getJobTitle() != null ? request.getJobTitle() : "";
        String typeLabel = request.getTypeLabel() != null ? request.getTypeLabel() : "";
        String assessmentUrl = request.getAssessmentUrl();

        if (to == null || to.isBlank()) {
            return ApiResponse.error(400, "收件人邮箱不能为空");
        }
        if (assessmentUrl == null || assessmentUrl.isBlank()) {
            return ApiResponse.error(400, "测评链接不能为空");
        }

        log.info("发送测评邮件: to={}, candidateName={}, jobTitle={}", to, candidateName, jobTitle);
        emailService.sendAssessmentEmail(to, candidateName, jobTitle, typeLabel, assessmentUrl);
        return ApiResponse.success("测评邮件已发送", null);
    }
}
