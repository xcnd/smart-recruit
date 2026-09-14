package com.smartrecruit.interview.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.BusinessException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.util.JwtUtil;
import com.smartrecruit.common.util.UserContextUtil;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.smartrecruit.interview.config.LlmProperties;
import com.smartrecruit.interview.dto.remote.CandidateDTO;
import com.smartrecruit.interview.dto.request.CreateAssessmentRequest;
import com.smartrecruit.interview.dto.remote.EmailMessageRequest;
import com.smartrecruit.interview.dto.remote.LlmChatRequest;
import com.smartrecruit.interview.dto.response.AssessmentQuestionItem;
import com.smartrecruit.interview.dto.response.OnlineAssessmentVO;
import com.smartrecruit.interview.entity.OnlineAssessment;
import com.smartrecruit.interview.feign.AiEngineClient;
import com.smartrecruit.interview.feign.RecruitmentClient;
import com.smartrecruit.interview.feign.SystemClient;
import com.smartrecruit.interview.repository.OnlineAssessmentMapper;
import com.smartrecruit.interview.service.OnlineAssessmentService;
import com.smartrecruit.interview.service.QuestionBankService;
import com.smartrecruit.interview.util.AssessmentTokenUtil;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线测评管理服务实现。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class OnlineAssessmentServiceImpl implements OnlineAssessmentService {

    private static final String MCQ_SYSTEM_PROMPT = """
            你是一位资深的技术面试官和出题专家，擅长根据职位要求设计编程能力测评题目。
            请为指定岗位生成 10 道单项选择题，按以下 JSON 格式返回（不要包含其他文字）：

            {
              "questions": [
                {
                  "questionText": "题目内容",
                  "options": [
                    {"key": "A", "value": "选项内容A"},
                    {"key": "B", "value": "选项内容B"},
                    {"key": "C", "value": "选项内容C"},
                    {"key": "D", "value": "选项内容D"}
                  ],
                  "correctAnswer": "A",
                  "difficulty": "easy|medium|hard"
                }
              ]
            }

            要求：
            - 生成 10 道题（简单 4 道、中等 4 道、困难 2 道）
            - 题目覆盖该岗位核心技术栈的关键知识点
            - 每个选项的 key 必须使用 A, B, C, D（大写字母）
            - correctAnswer 必须是单个大写字母（A/B/C/D）
            - 选项表述清晰，干扰项要有一定迷惑性但不超过正确答案的合理性
            - 只返回 JSON，不要包含任何解释性文字
            """;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OnlineAssessmentMapper assessmentMapper;
    private final RecruitmentClient recruitmentClient;
    private final SystemClient systemClient;
    private final AiEngineClient aiEngineClient;
    private final LlmProperties llmProperties;
    private final QuestionBankService questionBankService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${smart-recruit.assessment.base-url:http://localhost:3000}")
    private String assessmentBaseUrl;

    public OnlineAssessmentServiceImpl(OnlineAssessmentMapper assessmentMapper,
                                       RecruitmentClient recruitmentClient,
                                       SystemClient systemClient,
                                       ObjectProvider<AiEngineClient> aiEngineClientProvider,
                                       LlmProperties llmProperties,
                                       QuestionBankService questionBankService) {
        this.assessmentMapper = assessmentMapper;
        this.recruitmentClient = recruitmentClient;
        this.systemClient = systemClient;
        this.aiEngineClient = aiEngineClientProvider.getIfAvailable();
        this.llmProperties = llmProperties;
        this.questionBankService = questionBankService;
    }

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<OnlineAssessmentVO> pageQuery(Page<?> page, Map<String, Object> params) {
        Page<OnlineAssessment> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<OnlineAssessment> result = assessmentMapper.selectPageWithFilters(mpPage, params);
        List<OnlineAssessmentVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(vos, result.getTotal(), result.getSize(),
                result.getCurrent(), result.getPages());
    }

    /** 创建记录。 */
    @Override
    @Transactional
    public OnlineAssessmentVO create(CreateAssessmentRequest request) {
        OnlineAssessment entity = new OnlineAssessment();
        entity.setInterviewId(request.getInterviewId());
        entity.setCandidateId(request.getCandidateId());
        entity.setCandidateName(request.getCandidateName() != null ? request.getCandidateName() : "");
        entity.setJobTitle(request.getJobTitle() != null ? request.getJobTitle() : "");
        entity.setType(request.getType());
        entity.setTypeLabel(resolveTypeLabel(request.getType()));
        entity.setStatus(0); // 未发送
        entity.setCandidateEmail(request.getCandidateEmail());
        entity.setCreateUserId(currentUserId());
        entity.setCreateBy(currentUsername());
        entity.setUpdateUserId(currentUserId());
        entity.setUpdateBy(currentUsername());

        assessmentMapper.insert(entity);
        log.info("创建在线测评: id={}, type={}, candidateId={}",
                entity.getId(), entity.getType(), entity.getCandidateId());
        return toVO(entity);
    }

    /** 发送通知。 */
    @Override
    @Transactional
    public void send(Long id) {
        OnlineAssessment entity = assessmentMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("OnlineAssessment", id);
        }
        if (entity.getStatus() != 0) {
            throw new BusinessException("ASSESSMENT_ALREADY_SENT",
                    "该测评已发送，无法重复发送");
        }

        // 1. 获取候选人邮箱（优先使用实体中已有邮箱，否则从招聘服务查询）
        String candidateEmail = entity.getCandidateEmail();
        if (candidateEmail == null || candidateEmail.isBlank()) {
            candidateEmail = fetchCandidateEmail(entity.getCandidateId());
        }

        // 2. 生成测评访问 JWT Token
        SecretKey secretKey = JwtUtil.getSecretKey(jwtSecret);
        String accessToken = AssessmentTokenUtil.createAssessmentToken(
                secretKey, entity.getId(), entity.getCandidateId(), candidateEmail);
        String assessmentUrl = AssessmentTokenUtil.buildAssessmentUrl(assessmentBaseUrl, accessToken);

        // 3. 持久化 token 和邮箱
        entity.setAccessToken(accessToken);
        entity.setCandidateEmail(candidateEmail);
        entity.setStatus(1); // 待完成
        entity.setSentTime(DateUtils.now());
        assessmentMapper.updateById(entity);
        log.info("发送在线测评: id={}, email={}, token已生成", id, candidateEmail);

        // 4. 发送邮件（不阻塞主流程）
        try {
            String emailHtml = buildAssessmentEmailHtml(
                    entity.getCandidateName(), entity.getJobTitle(),
                    entity.getTypeLabel(), candidateEmail, assessmentUrl);
            String subject = "【SmartRecruit】在线测评邀请 — " + entity.getJobTitle();

            EmailMessageRequest emailRequest =
                    new EmailMessageRequest(candidateEmail, subject, emailHtml, true);
            ApiResponse<Void> response = systemClient.sendEmail(emailRequest);
            log.info("测评邮件发送结果: id={}, success={}", id, response.ok());
        } catch (Exception e) {
            log.error("测评邮件发送失败: id={}, email={}, error={}", id, candidateEmail, e.getMessage());
        }
    }

    /**
     * 从招聘服务获取候选人邮箱。
     */
    private String fetchCandidateEmail(Long candidateId) {
        try {
            ApiResponse<CandidateDTO> response = recruitmentClient.getCandidate(candidateId);
            if (response.ok() && response.data() != null) {
                CandidateDTO candidate = response.data();
                if (candidate.getEmail() != null && !candidate.getEmail().isBlank()) {
                    log.info("从招聘服务获取候选人邮箱: candidateId={}, email={}", candidateId, candidate.getEmail());
                    return candidate.getEmail();
                }
            }
        } catch (Exception e) {
            log.warn("获取候选人邮箱失败: candidateId={}, error={}", candidateId, e.getMessage());
        }
        // fallback: 使用候选人的默认邮箱格式
        log.warn("无法获取候选人邮箱，使用默认格式: candidateId={}", candidateId);
        return "candidate" + candidateId + "@example.com";
    }

    /** 生成在线测评题目（AI 生成优先，内置模板兜底）。 */
    @Override
    @Transactional
    public void generateQuestions(Long id) {
        OnlineAssessment entity = assessmentMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("OnlineAssessment", id);
        }
        if (entity.getType() != 0) {
            throw new BusinessException("ASSESSMENT_TYPE_NOT_SUPPORTED",
                    "仅编程测试（type=0）支持 AI 生成题目");
        }
        if (entity.getStatus() != 0) {
            throw new BusinessException("ASSESSMENT_ALREADY_SENT",
                    "已发送或已完成的测评不可修改题目");
        }
        if (aiEngineClient == null || !llmProperties.isEnabled()) {
            throw new BusinessException("AI_ENGINE_UNAVAILABLE",
                    "AI 引擎未启用，无法生成题目");
        }

        try {
            String userPrompt = "请为「" + entity.getJobTitle() + "」岗位生成编程能力测评题目，"
                    + "考察候选人对该岗位核心技术栈的掌握程度。";

            log.info("[AI 出题·测评] 开始调用 LLM: assessmentId={}, jobTitle={}", id, entity.getJobTitle());
            long start = DateUtils.currentEpochMillis();

            Map<String, Object> response = aiEngineClient.chat(
                    new LlmChatRequest(MCQ_SYSTEM_PROMPT, userPrompt, "interview-question"));

            log.info("[AI 出题·测评] LLM 响应: elapsed={}ms", DateUtils.currentEpochMillis() - start);

            List<AssessmentQuestionItem> questions = parseMcqResponse(response);
            if (questions.isEmpty()) {
                throw new BusinessException("AI_QUESTION_GENERATION_FAILED",
                        "AI 未能生成有效题目，请稍后重试");
            }

            String questionsJson = OBJECT_MAPPER.writeValueAsString(questions);
            entity.setQuestionsJson(questionsJson);
            assessmentMapper.updateById(entity);

            log.info("[AI 出题·测评] 题目已生成并入库: assessmentId={}, count={}, jsonSize={}",
                    id, questions.size(), questionsJson.length());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI 出题·测评] 生成失败: assessmentId={}, error={}", id, e.getMessage(), e);
            throw new BusinessException("AI_QUESTION_GENERATION_FAILED",
                    "AI 题目生成失败：" + e.getMessage());
        }
    }

    /** 查询测评题目列表。 */
    @Override
    public List<AssessmentQuestionItem> getQuestions(Long id) {
        OnlineAssessment entity = assessmentMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("OnlineAssessment", id);
        }
        return resolveQuestionsFromEntity(entity);
    }

    /**
     * 解析测评题目：优先使用 AI 生成的题目，否则回退到硬编码题库。
     */
    private List<AssessmentQuestionItem> resolveQuestionsFromEntity(OnlineAssessment entity) {
        if (entity.getQuestionsJson() != null && !entity.getQuestionsJson().isBlank()) {
            try {
                return OBJECT_MAPPER.readValue(
                        entity.getQuestionsJson(),
                        new TypeReference<List<AssessmentQuestionItem>>() {});
            } catch (Exception e) {
                log.warn("AI 题目 JSON 解析失败，回退到题库: assessmentId={}, error={}",
                        entity.getId(), e.getMessage());
            }
        }
        return questionBankService.getQuestions(entity.getType(), 10);
    }

    @SuppressWarnings("unchecked")
    private List<AssessmentQuestionItem> parseMcqResponse(Map<String, Object> response) {
        Object questionsObj = response.get("questions");
        if (!(questionsObj instanceof List<?> rawList)) {
            log.warn("[AI 出题·测评] LLM 响应中未找到 questions 数组");
            return List.of();
        }

        List<AssessmentQuestionItem> items = new ArrayList<>();
        for (int i = 0; i < rawList.size() && i < 10; i++) {
            try {
                Map<String, Object> item = (Map<String, Object>) rawList.get(i);
                List<Map<String, String>> options = parseOptions(item.get("options"));

                String difficulty = String.valueOf(item.getOrDefault("difficulty", "中等"));
                items.add(AssessmentQuestionItem.builder()
                        .questionId(i + 1)
                        .type(0)
                        .questionText(String.valueOf(item.getOrDefault("questionText", "")))
                        .difficulty("null".equals(difficulty) ? "中等" : difficulty)
                        .questionType("single_choice")
                        .correctAnswer(String.valueOf(item.getOrDefault("correctAnswer", "")).toUpperCase())
                        .options(options)
                        .score(1)
                        .build());
            } catch (Exception e) {
                log.warn("[AI 出题·测评] 解析第{}题失败: {}", i + 1, e.getMessage());
            }
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseOptions(Object optionsObj) {
        if (!(optionsObj instanceof List<?> rawList)) return List.of();
        List<Map<String, String>> result = new ArrayList<>();
        for (Object obj : rawList) {
            Map<String, Object> m = (Map<String, Object>) obj;
            Map<String, String> opt = new HashMap<>();
            opt.put("key", String.valueOf(m.getOrDefault("key", "")));
            opt.put("value", String.valueOf(m.getOrDefault("value", "")));
            result.add(opt);
        }
        return result;
    }

    /** 保存 AI 生成的测评题目 JSON。 */
    @Override
    @Transactional
    public void saveQuestions(Long id, List<AssessmentQuestionItem> questions) {
        OnlineAssessment entity = assessmentMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("OnlineAssessment", id);
        }
        if (entity.getStatus() != 0) {
            throw new BusinessException("ASSESSMENT_ALREADY_SENT",
                    "已发送或已完成的测评不可修改题目");
        }
        try {
            String questionsJson = OBJECT_MAPPER.writeValueAsString(questions);
            entity.setQuestionsJson(questionsJson);
            assessmentMapper.updateById(entity);
            log.info("测评题目已保存: assessmentId={}, count={}, jsonSize={}",
                    id, questions.size(), questionsJson.length());
        } catch (Exception e) {
            log.error("保存测评题目失败: assessmentId={}, error={}", id, e.getMessage(), e);
            throw new BusinessException("QUESTION_SAVE_FAILED",
                    "题目保存失败：" + e.getMessage());
        }
    }

    /** 更新测评成绩。 */
    @Override
    @Transactional
    public void updateScore(Long id, String score) {
        OnlineAssessment entity = assessmentMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("OnlineAssessment", id);
        }
        entity.setScore(score);
        entity.setStatus(2); // 已完成
        assessmentMapper.updateById(entity);
        log.info("更新测评成绩: id={}, score={}", id, score);
    }

    private OnlineAssessmentVO toVO(OnlineAssessment entity) {
        return OnlineAssessmentVO.builder()
                .id(entity.getId())
                .interviewId(entity.getInterviewId())
                .candidateId(entity.getCandidateId())
                .candidateName(entity.getCandidateName())
                .jobTitle(entity.getJobTitle())
                .type(entity.getType())
                .typeLabel(entity.getTypeLabel())
                .sentTime(entity.getSentTime())
                .status(entity.getStatus())
                .statusLabel(resolveStatusLabel(entity.getStatus()))
                .score(entity.getScore())
                .candidateEmail(entity.getCandidateEmail())
                .createTime(entity.getCreateTime())
                .build();
    }

    private String resolveTypeLabel(Integer type) {
        return switch (type) {
            case 0 -> "编程测试";
            case 1 -> "性格测试";
            case 2 -> "智商测试";
            default -> "未知类型";
        };
    }

    private String resolveStatusLabel(Integer status) {
        return switch (status) {
            case 0 -> "未发送";
            case 1 -> "待完成";
            case 2 -> "已完成";
            default -> "未知状态";
        };
    }

    /**
     * 构建在线测评邀请邮件的 HTML 内容。
     */
    private String buildAssessmentEmailHtml(String candidateName, String jobTitle,
                                             String typeLabel, String candidateEmail,
                                             String assessmentUrl) {
        String typeIcon = "";

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="color-scheme" content="light">
                    <meta name="supported-color-schemes" content="light">
                </head>
                <body style="margin:0;padding:0;background-color:#f5f7fa;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,'PingFang SC','Microsoft YaHei',sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f5f7fa;padding:40px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="520" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#4f46e5,#7c3aed);padding:36px 40px;text-align:center;">
                                            <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px;">SmartRecruit</h1>
                                            <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:13px;">AI-Driven Intelligent Recruitment Platform</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:36px 40px;">
                                            <p style="margin:0 0 4px;color:#1a1a2e;font-size:16px;font-weight:600;">%1$s 您好，</p>
                                            <p style="margin:0 0 24px;color:#475569;font-size:14px;line-height:1.8;">感谢您对 <strong>%2$s</strong> 职位的关注。为更全面地评估您的能力匹配度，诚邀您完成以下在线测评：</p>
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;margin-bottom:24px;">
                                                <tr>
                                                    <td style="padding:16px 20px;">
                                                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                                            <tr><td style="padding:6px 0;font-size:13px;color:#64748b;width:80px;">应聘职位</td><td style="padding:6px 0;font-size:14px;color:#0f172a;font-weight:600;">%2$s</td></tr>
                                                            <tr><td style="padding:6px 0;font-size:13px;color:#64748b;">测评类型</td><td style="padding:6px 0;font-size:14px;color:#0f172a;">%3$s %4$s</td></tr>
                                                            <tr><td style="padding:6px 0;font-size:13px;color:#64748b;">发送邮箱</td><td style="padding:6px 0;font-size:13px;color:#0f172a;">%5$s</td></tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:24px;">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%6$s" target="_blank" rel="noopener noreferrer" style="display:inline-block;padding:14px 48px;background:linear-gradient(135deg,#4f46e5,#6366f1);color:#ffffff;font-size:15px;font-weight:700;text-decoration:none;border-radius:8px;letter-spacing:0.5px;box-shadow:0 4px 12px rgba(79,70,229,0.35);">&#x25B6; 开始在线测评</a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:20px;">
                                                <tr>
                                                    <td style="padding:12px 16px;background-color:#f0f5ff;border:1px solid #e0e7ff;border-radius:6px;">
                                                        <p style="margin:0 0 6px;color:#475569;font-size:12px;font-weight:600;">如按钮无法点击，请复制以下链接到浏览器打开：</p>
                                                        <p style="margin:0;color:#4f46e5;font-size:12px;word-break:break-all;line-height:1.6;">%6$s</p>
                                                    </td>
                                                </tr>
                                            </table>
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:4px;">
                                                <tr>
                                                    <td style="padding:14px 16px;background-color:#fffbeb;border-left:3px solid #f59e0b;border-radius:4px;">
                                                        <p style="margin:0;color:#92400e;font-size:12px;line-height:1.8;">
                                                            &#x23F0; <strong>有效期：</strong>本链接 7 天内有效，请及时完成测评<br>
                                                            <strong>设备建议：</strong>编程类测评建议使用电脑端完成<br>
                                                            <strong>安全提示：</strong>本链接与您的个人信息绑定，请勿转发他人
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin:24px 0 0;color:#94a3b8;font-size:12px;line-height:1.6;">此为系统自动发送的邮件，请勿直接回复。如有任何疑问，请联系招聘负责人。</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#fafafa;padding:20px 40px;text-align:center;border-top:1px solid #f1f5f9;">
                                            <p style="margin:0;color:#94a3b8;font-size:11px;line-height:1.8;">SmartRecruit &mdash; AI-Driven Intelligent Recruitment Platform<br>&copy; 2026 SmartRecruit Team. All rights reserved.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                  candidateName,     // %1$s
                  jobTitle,          // %2$s
                  typeIcon,          // %3$s
                  typeLabel,         // %4$s
                  candidateEmail,    // %5$s
                  assessmentUrl      // %6$s (used twice)
        );
    }

    /** 当前登录用户 ID（网关注入 X-User-Id 请求头）。 */
    private Long currentUserId() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return UserContextUtil.getUserIdFromHeader(attrs.getRequest());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 当前登录用户名（网关注入 X-Username 请求头）。 */
    private String currentUsername() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String username = attrs.getRequest().getHeader("X-Username");
                return username == null || username.isBlank() ? null : username;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
