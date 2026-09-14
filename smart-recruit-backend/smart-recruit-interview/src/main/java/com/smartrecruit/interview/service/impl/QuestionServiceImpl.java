package com.smartrecruit.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.interview.dto.request.QuestionGenerateRequest;
import com.smartrecruit.interview.dto.request.SendQuestionsEmailRequest;
import com.smartrecruit.interview.dto.remote.EmailMessageRequest;
import com.smartrecruit.interview.dto.remote.LlmChatRequest;
import com.smartrecruit.interview.dto.response.QuestionGenerateResult;
import com.smartrecruit.interview.dto.response.QuestionGenerateResult.QuestionItem;
import com.smartrecruit.interview.dto.response.QuestionGenerateTaskVO;
import com.smartrecruit.interview.dto.response.CompletedQuestionResultVO;
import com.smartrecruit.interview.entity.InterviewQuestion;
import com.smartrecruit.interview.enums.InterviewEnums.QuestionCategory;
import com.smartrecruit.interview.enums.InterviewEnums.QuestionDifficulty;
import com.smartrecruit.interview.enums.InterviewEnums.QuestionPositionType;
import com.smartrecruit.interview.feign.AiEngineClient;
import com.smartrecruit.interview.feign.SystemClient;
import com.smartrecruit.interview.repository.InterviewQuestionMapper;
import com.smartrecruit.interview.service.QuestionService;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI 智能出题服务实现。
 *
 * <p>先从题库中查询已有题目；若大模型已启用，则由 AI 异步生成题目补充并入库。
 * 前端通过任务 ID 轮询获取出题结果。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private static final String SYSTEM_PROMPT = """
            你是一位资深的面试官，擅长根据岗位级别和职责设计高质量的面试题目。

            请为指定的岗位生成面试题目，按以下 JSON 格式返回（不要包含任何文字或解释）：

            {
              "techQuestions": [
                { "question": "以下哪项不属于 JVM 垃圾收集器？\\nA. Serial GC\\nB. G1 GC\\nC. Redis\\nD. CMS GC", "type": "single_choice", "difficulty": "medium", "referenceAnswer": "C，Redis 是缓存中间件，不是 JVM 垃圾收集器。" },
                { "question": "以下哪些场景适合使用消息队列？\\nA. 异步解耦\\nB. 流量削峰\\nC. 分布式事务最终一致性\\nD. 实时日志采集\\nE. 替换数据库主键", "type": "multiple_choice", "difficulty": "hard", "referenceAnswer": "A、B、C、D，消息队列不能替换数据库主键，E 是错误说法。" },
                { "question": "CAS（Compare And Swap）是乐观锁的一种实现方式。", "type": "true_false", "difficulty": "easy", "referenceAnswer": "正确，CAS 通过比较并交换实现无锁并发，是乐观锁的典型实现。" },
                { "question": "请描述 Spring Cloud Alibaba Sentinel 的熔断降级策略及其适用场景。", "type": "essay", "difficulty": "medium", "referenceAnswer": "关键得分点：1.慢调用比例 2.异常比例 3.异常数 4.与 Hystrix 的对比 5.热点参数限流" }
              ],
              "projectQuestions": [
                { "question": "...", "difficulty": "easy|medium|hard", "referenceAnswer": "参考答案要点..." }
              ],
              "behavioralQuestions": [
                { "question": "...", "difficulty": "easy|medium|hard", "referenceAnswer": "参考答案要点..." }
              ]
            }

            出题规则（必须严格遵守）：

            ★★★ 技术基础题型规则（最重要）★★★
            - 每道 techQuestions 必须包含 "type" 字段：single_choice / multiple_choice / true_false / essay
            - 用户会指定各题型数量，你必须严格按指定数量生成
            - single_choice（单选）：question 字段格式为"题干\\nA. 选项A\\nB. 选项B\\nC. 选项C\\nD. 选项D"，共4个选项，用 \\n 换行分隔
            - multiple_choice（多选）：question 字段格式为"题干\\nA. 选项A\\nB. 选项B\\nC. 选项C\\nD. 选项D\\nE. 选项E"，4-6个选项
            - true_false（判断）：question 字段为陈述句（非疑问句），referenceAnswer 写"正确"或"错误"及理由
            - essay（问答）：question 为开放式问题，referenceAnswer 列出关键得分点
            - 单选和多选的选项必须写在 question 字段里，用 \\n 换行分隔，不要放在单独的 options 数组里

            【岗位匹配】
            题目必须与该岗位技术栈强相关，高级岗位不出现入行基础题。

            【禁止重复】
            同一分类下每题必须是不同知识点，严禁重复或高度相似。

            - 只返回 JSON，不要包含任何解释性文字
            """;

    /** 任务过期时间：10 分钟 */
    private static final long TASK_TTL_MS = 10 * 60 * 1000;

    private final InterviewQuestionMapper questionMapper;
    private final AiEngineClient aiEngineClient;
    private final SystemClient systemClient;
    private final ConcurrentHashMap<String, GenerateTask> taskMap = new ConcurrentHashMap<>();

    public QuestionServiceImpl(InterviewQuestionMapper questionMapper,
                               ObjectProvider<AiEngineClient> aiEngineClientProvider,
                               ObjectProvider<SystemClient> systemClientProvider) {
        this.questionMapper = questionMapper;
        this.aiEngineClient = aiEngineClientProvider.getIfAvailable();
        this.systemClient = systemClientProvider.getIfAvailable();
        log.info("AiEngineClient 注入状态: {} | SystemClient 注入状态: {}",
                aiEngineClient != null ? "已就绪" : "未注入",
                systemClient != null ? "已就绪" : "未注入");
    }

    // ---- 内部任务模型 ----

    private static class GenerateTask {
        volatile String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED
        volatile QuestionGenerateResult result;
        volatile String errorMessage;
        final long createdAt = DateUtils.currentEpochMillis();
    }

    // ---- 同步（已废弃，保留兼容）----

    /** 生成面试题。 */
    @Override
    @Deprecated
    public QuestionGenerateResult generate(QuestionGenerateRequest request) {
        return doGenerate(request);
    }

    // ---- 异步出题 ----

    /** 异步生成面试题（AI 生成任务）。 */
    @Override
    public String generateAsync(QuestionGenerateRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        taskMap.put(taskId, new GenerateTask());
        cleanExpiredTasks();

        // 使用虚拟线程异步执行 LLM 出题，不阻塞 HTTP 响应
        Thread.startVirtualThread(() -> {
            GenerateTask task = taskMap.get(taskId);
            if (task == null) return;
            task.status = "PROCESSING";
            try {
                QuestionGenerateResult result = doGenerate(request);
                task.result = result;
                task.status = "COMPLETED";
                log.info("[异步出题] 任务完成: taskId={}", taskId);
            } catch (Exception e) {
                log.error("[异步出题] 任务失败: taskId={}, error={}", taskId, e.getMessage());
                task.errorMessage = e.getMessage();
                task.status = "FAILED";
            }
        });

        log.info("[异步出题] 任务已创建: taskId={}, positionType={}", taskId, request.getPositionType());
        return taskId;
    }

    /** 查询异步生成任务的结果。 */
    @Override
    public QuestionGenerateTaskVO getGenerateResult(String taskId) {
        GenerateTask task = taskMap.get(taskId);
        if (task == null) {
            return QuestionGenerateTaskVO.builder()
                    .taskId(taskId)
                    .status("NOT_FOUND")
                    .build();
        }
        return QuestionGenerateTaskVO.builder()
                .taskId(taskId)
                .status(task.status)
                .result(task.result)
                .errorMessage(task.errorMessage)
                .build();
    }

    /** 查询已完成生成任务的结果列表。 */
    @Override
    public List<CompletedQuestionResultVO> listCompletedResults() {
        cleanExpiredTasks();
        return taskMap.entrySet().stream()
                .filter(e -> "COMPLETED".equals(e.getValue().status)
                        && e.getValue().result != null)
                .map(e -> {
                    QuestionGenerateResult r = e.getValue().result;
                    return new CompletedQuestionResultVO(
                            e.getKey(), r.getPositionLabel(), r.getPositionType());
                })
                .toList();
    }

    // ---- 发送面试题邮件 ----

    /** 将题目通过邮件发送给候选人。 */
    @Override
    public void sendQuestionsByEmail(SendQuestionsEmailRequest request) {
        if (systemClient == null) {
            throw new IllegalStateException("邮件服务不可用，系统服务未连接");
        }

        String subject = "【SmartRecruit】" + request.getPositionLabel() + " — AI 智能面试题";
        String htmlContent = buildQuestionsEmailHtml(request);

        systemClient.sendEmail(new EmailMessageRequest(
                request.getEmail(), subject, htmlContent, true));
        log.info("面试题邮件发送成功: to={}, positionLabel={}, 技术{}道, 项目{}道, 行为{}道",
                request.getEmail(), request.getPositionLabel(),
                request.getTechQuestions().size(),
                request.getProjectQuestions() != null ? request.getProjectQuestions().size() : 0,
                request.getBehavioralQuestions() != null ? request.getBehavioralQuestions().size() : 0);
    }

    /**
     * 构建专业的面试题 HTML 邮件。
     */
    private String buildQuestionsEmailHtml(SendQuestionsEmailRequest request) {
        String greeting = request.getInterviewerName() != null && !request.getInterviewerName().isBlank()
                ? request.getInterviewerName() + " 您好，"
                : "您好，";

        StringBuilder techRows = buildQuestionRows(request.getTechQuestions());
        StringBuilder projectRows = request.getProjectQuestions() != null
                ? buildQuestionRows(request.getProjectQuestions()) : new StringBuilder();
        StringBuilder behavioralRows = request.getBehavioralQuestions() != null
                ? buildQuestionRows(request.getBehavioralQuestions()) : new StringBuilder();

        boolean hasProject = request.getProjectQuestions() != null && !request.getProjectQuestions().isEmpty();
        boolean hasBehavioral = request.getBehavioralQuestions() != null && !request.getBehavioralQuestions().isEmpty();

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
                                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                                    <!-- 头部 -->
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#1890ff,#722ed1);padding:32px 40px;text-align:center;">
                                            <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px;">
                                                SmartRecruit
                                            </h1>
                                            <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:13px;">
                                                智能招聘管理平台 &mdash; AI 智能面试题
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- 内容区 -->
                                    <tr>
                                        <td style="padding:32px 40px;">
                                            <p style="margin:0 0 4px;color:#1a1a2e;font-size:16px;font-weight:600;">
                                                %s
                                            </p>
                                            <p style="margin:0 0 24px;color:#475569;font-size:14px;line-height:1.8;">
                                                以下是针对 <strong style="color:#1890ff;">%s</strong> 岗位的 AI 智能生成的面试题目，涵盖技术基础、项目经验和行为面试三个维度，每个维度包含简单、中等、困难三个难度等级。
                                            </p>

                                            <!-- 技术基础题 -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:24px;">
                                                <tr>
                                                    <td style="padding:10px 16px;background:linear-gradient(135deg,#1890ff,#40a9ff);border-radius:8px 8px 0 0;">
                                                        <span style="color:#ffffff;font-size:14px;font-weight:700;">技术基础题</span>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td style="padding:16px;background-color:#f8fafc;border:1px solid #e2e8f0;border-top:none;border-radius:0 0 8px 8px;">
                                                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                                            %s
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- 项目经验题 -->
                                            %s

                                            <!-- 行为面试题 -->
                                            %s

                                            <!-- 操作提示 -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:24px;">
                                                <tr>
                                                    <td style="padding:14px 16px;background-color:#f0f5ff;border-left:3px solid #1890ff;border-radius:4px;">
                                                        <p style="margin:0;color:#475569;font-size:12px;line-height:1.8;">
                                                            <strong>使用建议：</strong><br>
                                                            &bull; 可根据候选人的实际水平灵活调整题目难度<br>
                                                            &bull; 技术题建议结合候选人简历中的技术栈进行追问<br>
                                                            &bull; 行为题建议使用 STAR 法则引导候选人回答<br>
                                                            &bull; 每题建议预留 5-10 分钟的回答和追问时间
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <!-- 底部 -->
                                    <tr>
                                        <td style="background-color:#fafafa;padding:20px 40px;text-align:center;border-top:1px solid #eef0f6;">
                                            <p style="margin:0;color:#94a3b8;font-size:11px;line-height:1.8;">
                                                SmartRecruit &mdash; AI-Driven Intelligent Recruitment Platform<br>
                                                此为系统自动生成的面试题，由 AI 引擎根据岗位需求智能匹配<br>
                                                &copy; 2026 SmartRecruit Team. All rights reserved.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                greeting,
                request.getPositionLabel(),
                techRows.toString(),
                hasProject ? buildSectionBlock("项目经验题", "#4f46e5", "#6366f1", projectRows.toString()) : "",
                hasBehavioral ? buildSectionBlock("行为面试题", "#0891b2", "#06b6d4", behavioralRows.toString()) : ""
        );
    }

    private String buildSectionBlock(String title, String color1, String color2, String rows) {
        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:24px;">
                    <tr>
                        <td style="padding:10px 16px;background:linear-gradient(135deg,%s,%s);border-radius:8px 8px 0 0;">
                            <span style="color:#ffffff;font-size:14px;font-weight:700;">%s</span>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:16px;background-color:#f8fafc;border:1px solid #e2e8f0;border-top:none;border-radius:0 0 8px 8px;">
                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                %s
                            </table>
                        </td>
                    </tr>
                </table>
                """.formatted(color1, color2, title, rows);
    }

    private StringBuilder buildQuestionRows(List<QuestionItem> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            QuestionItem q = items.get(i);
            String diffColor = switch (q.getDifficultyCode() != null ? q.getDifficultyCode() : 1) {
                case 0 -> "#52c41a"; // easy
                case 2 -> "#ff4d4f"; // hard
                default -> "#fa8c16"; // medium
            };
            String diffLabel = q.getDifficulty() != null ? q.getDifficulty() : "中等";

            // 题型标签
            String typeLabel = "";
            String typeColor = "#94a3b8";
            if (q.getQuestionType() != null) {
                switch (q.getQuestionType()) {
                    case "single_choice" -> { typeLabel = "单选"; typeColor = "#1890ff"; }
                    case "multiple_choice" -> { typeLabel = "多选"; typeColor = "#722ed1"; }
                    case "true_false" -> { typeLabel = "判断"; typeColor = "#fa8c16"; }
                    case "essay" -> { typeLabel = "问答"; typeColor = "#52c41a"; }
                }
            }
            String typeTagHtml = !typeLabel.isEmpty()
                    ? "<span style=\"display:inline-block;margin-left:6px;padding:1px 6px;font-size:10px;font-weight:600;color:" + typeColor + ";background-color:" + typeColor + "18;border-radius:3px;\">" + typeLabel + "</span>"
                    : "";

            // 选项 HTML
            StringBuilder optionsHtml = new StringBuilder();
            List<String> opts = q.getOptions();
            if (opts != null && !opts.isEmpty()) {
                optionsHtml.append("<div style=\"margin-top:6px;padding:8px 12px;background-color:#f8fafc;border-radius:6px;border:1px solid #e2e8f0;\">");
                for (String opt : opts) {
                    optionsHtml.append("<span style=\"display:inline-block;margin-right:16px;font-size:13px;color:#334155;\">")
                            .append(escapeHtml(opt))
                            .append("</span>");
                }
                optionsHtml.append("</div>");
            }

            // 参考答案
            StringBuilder answerHtml = new StringBuilder();
            String refAnswer = q.getReferenceAnswer();
            if (refAnswer != null && !refAnswer.isBlank()) {
                answerHtml.append("<div style=\"margin-top:6px;padding:6px 10px;background-color:#f0fdf4;border-left:3px solid #22c55e;border-radius:4px;\">")
                        .append("<span style=\"font-size:11px;color:#16a34a;font-weight:600;\">参考答案：</span>")
                        .append("<span style=\"font-size:12px;color:#166534;\">").append(escapeHtml(refAnswer)).append("</span>")
                        .append("</div>");
            }

            // 难度标签 HTML
            String diffTagHtml = "<span style=\"display:inline-block;margin-left:8px;padding:2px 8px;font-size:11px;font-weight:600;color:" + diffColor + ";background-color:" + diffColor + "1A;border-radius:4px;\">" + diffLabel + "</span>";

            sb.append("""
                    <tr>
                        <td style="padding:10px 0;border-bottom:1px solid #eef0f6;vertical-align:top;">
                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td style="width:28px;vertical-align:top;padding-top:2px;">
                                        <span style="display:inline-block;width:22px;height:22px;background-color:#1890ff;color:#fff;font-size:11px;font-weight:700;text-align:center;line-height:22px;border-radius:50%%;">%d</span>
                                    </td>
                                    <td style="font-size:14px;color:#0f172a;line-height:1.7;">
                                        %s %s %s
                                        <br>%s%s
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    """.formatted(i + 1,
                    typeTagHtml, diffTagHtml,
                    escapeHtml(q.getQuestion()),
                    optionsHtml.toString(), answerHtml.toString()));
        }
        return sb;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    // ---- 核心出题逻辑 ----

    private QuestionGenerateResult doGenerate(QuestionGenerateRequest request) {
        Integer positionType = request.getPositionType();
        QuestionPositionType posType = QuestionPositionType.fromCode(positionType);
        String positionLabel = posType != null ? posType.getLabel() : "综合";

        // 解析用户选择的分类和每类数量
        List<String> selectedCategories = request.getCategories() != null && !request.getCategories().isEmpty()
                ? request.getCategories()
                : List.of("tech", "project", "behavioral");
        Map<String, Integer> counts = request.getCategoryQuestionCounts() != null
                ? request.getCategoryQuestionCounts()
                : Map.of();

        log.info("AI 出题: positionType={}, label={}, categories={}, counts={}",
                positionType, positionLabel, selectedCategories, counts);

        int techTarget = counts.getOrDefault("tech", 3);
        int projectTarget = counts.getOrDefault("project", 3);
        int behavioralTarget = counts.getOrDefault("behavioral", 3);

        List<QuestionItem> techItems = new ArrayList<>();
        List<QuestionItem> projectItems = new ArrayList<>();
        List<QuestionItem> behavioralItems = new ArrayList<>();
        boolean aiEnhanced = false;

        // 1. 优先由 AI 生成题目（实时生成，确保与岗位和参数匹配）
        if (aiEngineClient != null) {
            try {
                long aiStart = DateUtils.currentEpochMillis();
                Map<String, List<QuestionItem>> aiResult = generateByLlm(request, positionLabel);
                List<QuestionItem> aiTech = aiResult.getOrDefault("techQuestions", List.of());
                List<QuestionItem> aiProject = aiResult.getOrDefault("projectQuestions", List.of());
                List<QuestionItem> aiBehavioral = aiResult.getOrDefault("behavioralQuestions", List.of());

                if (selectedCategories.contains("tech") && !aiTech.isEmpty()) {
                    List<QuestionItem> truncated = aiTech.size() > techTarget
                            ? aiTech.subList(0, techTarget) : aiTech;
                    techItems.addAll(reNumber(truncated, 1));
                }
                if (selectedCategories.contains("project") && !aiProject.isEmpty()) {
                    List<QuestionItem> truncated = aiProject.size() > projectTarget
                            ? aiProject.subList(0, projectTarget) : aiProject;
                    projectItems.addAll(reNumber(truncated, 1));
                }
                if (selectedCategories.contains("behavioral") && !aiBehavioral.isEmpty()) {
                    List<QuestionItem> truncated = aiBehavioral.size() > behavioralTarget
                            ? aiBehavioral.subList(0, behavioralTarget) : aiBehavioral;
                    behavioralItems.addAll(reNumber(truncated, 1));
                }

                aiEnhanced = !aiTech.isEmpty() || !aiProject.isEmpty() || !aiBehavioral.isEmpty();
                log.info("[LLM] AI 出题完成: 技术{}道, 项目{}道, 行为{}道, 耗时 {}ms",
                        aiTech.size(), aiProject.size(), aiBehavioral.size(),
                        DateUtils.currentEpochMillis() - aiStart);
            } catch (Exception e) {
                log.error("LLM 出题失败，回退到题库", e);
            }
        }

        // 2. AI 不足时，从题库补充缺口（DB 只做兜底，不做主源）
        if (selectedCategories.contains("tech") && techItems.size() < techTarget) {
            int needed = techTarget - techItems.size();
            List<QuestionItem> dbItems = toItems(
                    queryDbByCategory(positionType, QuestionCategory.TECHNOLOGY.getCode()), 1);
            List<QuestionItem> supplement = dbItems.size() > needed
                    ? dbItems.subList(0, needed) : dbItems;
            if (!supplement.isEmpty()) {
                techItems.addAll(reNumber(supplement, techItems.size() + 1));
                log.info("题库补充: 技术+{}道", supplement.size());
            }
        }
        if (selectedCategories.contains("project") && projectItems.size() < projectTarget) {
            int needed = projectTarget - projectItems.size();
            List<QuestionItem> dbItems = toItems(
                    queryDbByCategory(positionType, QuestionCategory.PROJECT.getCode()), 1);
            List<QuestionItem> supplement = dbItems.size() > needed
                    ? dbItems.subList(0, needed) : dbItems;
            if (!supplement.isEmpty()) {
                projectItems.addAll(reNumber(supplement, projectItems.size() + 1));
                log.info("题库补充: 项目+{}道", supplement.size());
            }
        }
        if (selectedCategories.contains("behavioral") && behavioralItems.size() < behavioralTarget) {
            int needed = behavioralTarget - behavioralItems.size();
            List<QuestionItem> dbItems = toItems(
                    queryDbByCategory(positionType, QuestionCategory.BEHAVIORAL.getCode()), 1);
            List<QuestionItem> supplement = dbItems.size() > needed
                    ? dbItems.subList(0, needed) : dbItems;
            if (!supplement.isEmpty()) {
                behavioralItems.addAll(reNumber(supplement, behavioralItems.size() + 1));
                log.info("题库补充: 行为+{}道", supplement.size());
            }
        }

        log.info("出题结果: 技术{}道, 项目{}道, 行为{}道 (目标: 技术{}道, 项目{}道, 行为{}道, AI增强={})",
                techItems.size(), projectItems.size(), behavioralItems.size(),
                techTarget, projectTarget, behavioralTarget, aiEnhanced);

        return QuestionGenerateResult.builder()
                .positionType(positionType)
                .positionLabel(positionLabel)
                .techQuestions(techItems)
                .projectQuestions(projectItems)
                .behavioralQuestions(behavioralItems)
                .aiEnhanced(aiEnhanced)
                .build();
    }

    /** 按分类查询库中题目。 */
    private List<InterviewQuestion> queryDbByCategory(Integer positionType, int category) {
        return questionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>()
                        .eq(InterviewQuestion::getPositionType, positionType)
                        .eq(InterviewQuestion::getCategory, category)
                        .eq(InterviewQuestion::getStatus, 1));
    }

    /**
     * 调用大模型生成题目并入库，返回按分类分组的 QuestionItem 列表。
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<QuestionItem>> generateByLlm(QuestionGenerateRequest request, String positionLabel) {
        Integer positionType = request.getPositionType();
        String difficultyLevel = request.getDifficultyLevel() != null ? request.getDifficultyLevel() : "mixed";
        List<String> categories = request.getCategories() != null && !request.getCategories().isEmpty()
                ? request.getCategories()
                : List.of("tech", "project", "behavioral");
        Map<String, Integer> counts = request.getCategoryQuestionCounts() != null
                ? request.getCategoryQuestionCounts()
                : Map.of();

        // 构建分类描述（含各自数量）
        StringBuilder categoryDesc = new StringBuilder();
        for (String cat : categories) {
            int n = counts.getOrDefault(cat, 3);
            String name = switch (cat) {
                case "tech" -> "技术基础题";
                case "project" -> "项目经验题";
                case "behavioral" -> "行为面试题";
                default -> cat;
            };
            categoryDesc.append("、").append(name).append(" ").append(n).append(" 道");
        }
        String catNames = categoryDesc.isEmpty() ? "" : categoryDesc.substring(1);

        // 构建难度描述
        String diffDesc = switch (difficultyLevel) {
            case "easy" -> "全部为简单难度";
            case "medium" -> "全部为中等难度";
            case "hard" -> "全部为困难难度";
            default -> "简单、中等、困难各占约三分之一";
        };

        String positionContext = buildPositionContext(positionType, positionLabel);

        // 构建技术基础题型描述
        String techTypeSpec = "";
        if (categories.contains("tech") && request.getTechQuestionTypes() != null
                && !request.getTechQuestionTypes().isEmpty()) {
            StringBuilder typeDesc = new StringBuilder();
            Map<String, Integer> techTypes = request.getTechQuestionTypes();
            int sc = techTypes.getOrDefault("single_choice", 0);
            int mc = techTypes.getOrDefault("multiple_choice", 0);
            int tf = techTypes.getOrDefault("true_false", 0);
            int es = techTypes.getOrDefault("essay", 0);
            if (sc > 0) typeDesc.append("、单选题 ").append(sc).append(" 道（题干后 \\n 换行接 A. B. C. D. 四个选项）");
            if (mc > 0) typeDesc.append("、多选题 ").append(mc).append(" 道（题干后 \\n 换行接 A. B. C. D. E. 等 4-6 个选项）");
            if (tf > 0) typeDesc.append("、判断题 ").append(tf).append(" 道（陈述句，不要用疑问句）");
            if (es > 0) typeDesc.append("、问答题 ").append(es).append(" 道（开放式问题）");
            if (!typeDesc.isEmpty()) {
                techTypeSpec = """

                        ★ 技术基础题共 %d 道，必须严格按以下分配生成，选项写在 question 字段里用 \\n 换行分隔：
                        %s
                        """.formatted(counts.getOrDefault("tech", 10), typeDesc.substring(1));
            }
        }

        String userPrompt = positionContext + "\n\n"
                + "请为上述岗位生成面试题目。"
                + "题目分类及数量：" + catNames + "。"
                + "难度要求：" + diffDesc + "。"
                + techTypeSpec
                + "每道技术题必须包含 type 字段。选项写在 question 字段中，\\n 换行。不要单独用 options 数组。"
                + "每道题必须是不同的知识点，严禁重复。题目深度必须与岗位级别匹配。";

        log.info("[Feign LLM] 开始调用 ai-engine: positionType={}, positionLabel={}, categories={}, counts={}, diff={}",
                positionType, positionLabel, categories, counts, difficultyLevel);

        long feignStart = DateUtils.currentEpochMillis();
        Map<String, Object> response = aiEngineClient.chat(
                new LlmChatRequest(SYSTEM_PROMPT, userPrompt, "interview-question"));
        long feignElapsed = DateUtils.currentEpochMillis() - feignStart;

        log.info("[Feign LLM] ai-engine 响应: elapsed={}ms, responseKeys={}, responseSize={}",
                feignElapsed, response.keySet(), response.toString().length());

        if (response.isEmpty()) {
            log.warn("[Feign LLM] 返回空响应: elapsed={}ms", feignElapsed);
            return Map.of();
        }

        // 只解析请求的分类
        List<QuestionItem> techItems = categories.contains("tech")
                ? parseAiQuestionItems(response.get("techQuestions"), positionType, QuestionCategory.TECHNOLOGY.getCode())
                : List.of();
        List<QuestionItem> projectItems = categories.contains("project")
                ? parseAiQuestionItems(response.get("projectQuestions"), positionType, QuestionCategory.PROJECT.getCode())
                : List.of();
        List<QuestionItem> behavioralItems = categories.contains("behavioral")
                ? parseAiQuestionItems(response.get("behavioralQuestions"), positionType, QuestionCategory.BEHAVIORAL.getCode())
                : List.of();

        return Map.of(
                "techQuestions", techItems,
                "projectQuestions", projectItems,
                "behavioralQuestions", behavioralItems
        );
    }

    /**
     * 根据岗位类型构建详细的技术栈与考察重点上下文，注入到 LLM prompt 中，
     * 确保生成的题目与岗位实际职责匹配。
     */
    private String buildPositionContext(Integer positionType, String positionLabel) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("岗位名称：「").append(positionLabel).append("」");

        QuestionPositionType pt = QuestionPositionType.fromCode(positionType);
        if (pt != null) {
            switch (pt) {
                case BACKEND -> ctx.append("""

                        该岗位核心技术栈：Java、Spring Boot/Spring Cloud 微服务架构、JVM 原理与性能调优、
                        并发编程（线程池、锁、AQS）、分布式系统（CAP 理论、分布式事务、分布式锁）、
                        消息队列（Kafka/RabbitMQ/RocketMQ）、数据库（MySQL 索引与锁、Redis 数据结构与集群）、
                        系统设计（高并发、高可用、弹性伸缩）。
                        高级岗位聚焦：微服务治理（服务发现、熔断降级、限流）、分布式一致性协议（Raft/Paxos）、
                        JVM 内存模型与 GC 调优、中间件底层原理、DDD 领域驱动设计。
                        注意：不要出 HTTP 状态码含义、面向对象四大特性等入行基础题。""");
                case FRONTEND -> ctx.append("""

                        该岗位核心技术栈：HTML5/CSS3/JavaScript ES6+、React/Vue/Angular 框架、
                        TypeScript、Webpack/Vite 构建优化、浏览器渲染原理与性能优化、
                        前端工程化（Monorepo、微前端）、Node.js 中间层。
                        高级岗位聚焦：渲染性能调优、复杂状态管理架构设计、微前端方案落地、
                        AST 与编译原理、前端安全防护（XSS/CSRF）、大规模组件库设计。""");
                case AI -> ctx.append("""

                        该岗位核心技术栈：Python、机器学习/深度学习框架（PyTorch/TensorFlow）、
                        NLP/CV/推荐算法、模型训练与微调（LoRA/P-Tuning）、模型部署与推理优化（ONNX/TensorRT）、
                        特征工程与数据清洗、向量数据库与 RAG、Prompt Engineering。""");
                case PM -> ctx.append("""

                        该岗位核心技术栈：产品设计方法论、需求分析与 PRD 撰写、数据分析与埋点体系、
                        项目管理（Scrum/Kanban）、用户研究与可用性测试、A/B 实验设计、
                        商业化与增长策略、跨团队协作与利益相关者管理。""");
                case DEVOPS -> ctx.append("""

                        该岗位核心技术栈：Linux 系统管理、Docker/Kubernetes 容器编排与调度、
                        CI/CD 流水线设计（Jenkins/GitLab CI/GitHub Actions）、
                        云平台（AWS/阿里云/Azure）、监控与可观测性（Prometheus/Grafana/OpenTelemetry）、
                        IaC（Terraform/Ansible/Pulumi）、服务网格（Istio）。
                        高级岗位聚焦：多集群管理、混沌工程、FinOps 成本优化、零信任安全架构。""");
                case DATA_ENGINEER -> ctx.append("""

                        该岗位核心技术栈：Python/Java/Scala、大数据计算框架（Spark/Flink/Hadoop）、
                        数据仓库建模（星型/雪花模型）、ETL/ELT 管线设计、SQL 复杂查询优化、
                        实时流处理、数据湖架构（Iceberg/Hudi/Delta Lake）、数据治理与血缘管理。""");
                case FULLSTACK -> ctx.append("""

                        该岗位核心技术栈：前后端全链路开发、React/Vue + Node.js/Spring Boot、
                        数据库设计与 SQL/NoSQL 选型、RESTful/GraphQL/gRPC API 设计、
                        DevOps 基础（Docker/CI/CD）、系统架构设计、云服务集成。""");
                case MOBILE -> ctx.append("""

                        该岗位核心技术栈：iOS(Swift/SwiftUI) / Android(Kotlin/Jetpack Compose)、
                        移动端架构（MVVM/MVI/Clean Architecture）、性能优化与内存管理、
                        跨平台框架（Flutter/React Native/KMM）、App 安全与逆向防护、
                        网络优化与离线缓存策略。""");
                case QA -> ctx.append("""

                        该岗位核心技术栈：软件测试理论（测试用例设计方法、缺陷生命周期、测试计划与策略）、
                        自动化测试（Selenium/Playwright/Appium/Pytest/JUnit）、接口测试（Postman/RestAssured/JMeter）、
                        性能测试（压测脚本编写、性能瓶颈分析与调优、容量规划）、
                        测试框架与持续集成（Jenkins/GitLab CI 质量门禁）、SQL 校验与测试数据管理、
                        质量保障体系（代码覆盖率、冒烟/回归/探索性测试）。
                        高级岗位聚焦：测试平台与测试框架搭建、精准测试与质量度量、性能监控体系、DevOps 质量门禁。""");
            }
        } else {
            ctx.append("\n请根据岗位名称自行推断所需技术栈和考察重点，确保题目深度与岗位级别匹配。");
        }

        // 从岗位名称中提取级别提示
        String levelHint = "";
        if (positionLabel.contains("高级") || positionLabel.contains("资深") || positionLabel.contains("架构师")) {
            levelHint = "\n该岗位为高级岗位，题目应有深度，聚焦原理、架构、优化和最佳实践，禁止出过于基础的入门题。";
        } else if (positionLabel.contains("初级") || positionLabel.contains("实习") || positionLabel.contains("助理")) {
            levelHint = "\n该岗位为初级岗位，题目可侧重基础知识、学习能力和成长潜力，难度不宜过高。";
        } else {
            levelHint = "\n该岗位为中级岗位，题目应侧重实际应用能力、常见场景解决方案和一定的技术深度。";
        }
        ctx.append(levelHint);

        return ctx.toString();
    }

    /**
     * 解析 AI 返回的单个分类题目列表，并入库。
     */
    @SuppressWarnings("unchecked")
    private List<QuestionItem> parseAiQuestionItems(Object rawList, Integer positionType, int category) {
        if (!(rawList instanceof List<?>)) return List.of();
        List<Map<String, Object>> items = (List<Map<String, Object>>) rawList;
        List<QuestionItem> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            try {
                Map<String, Object> item = items.get(i);
                String rawQuestionText = String.valueOf(item.getOrDefault("question", ""));
                String questionType = String.valueOf(item.getOrDefault("type", ""));
                String difficultyStr = String.valueOf(item.getOrDefault("difficulty", "medium"));
                String referenceAnswer = String.valueOf(item.getOrDefault("referenceAnswer", ""));
                int difficultyCode = parseDifficulty(difficultyStr);

                // 如果 LLM 没有返回 type，从题干文本推断
                if (questionType.isEmpty() || "null".equals(questionType) || questionType.length() > 20) {
                    questionType = inferQuestionType(rawQuestionText);
                }

                // 从 question 文本中提取行内选项（格式: "题干\nA. xxx\nB. xxx"）
                List<String> optionsList = null;
                String questionText = rawQuestionText;
                // 同时兼容 LLM 返回独立的 options 数组
                Object rawOptions = item.get("options");
                if (rawOptions instanceof List<?> optList && !optList.isEmpty()) {
                    optionsList = new ArrayList<>();
                    for (Object o : optList) {
                        optionsList.add(String.valueOf(o));
                    }
                }
                // 若 options 为空，从 question 文本中提取行内选项
                if ((optionsList == null || optionsList.isEmpty())
                        && ("single_choice".equals(questionType) || "multiple_choice".equals(questionType))) {
                    // 尝试按 \n 分割（处理 JSON 中的转义换行和实际换行）
                    String[] lines = rawQuestionText.split("\\\\n|\\n");
                    List<String> opts = new ArrayList<>();
                    List<String> stems = new ArrayList<>();
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.matches("^[A-Ea-e][\\.\\)、）]\\s.*")) {
                            opts.add(trimmed);
                        } else if (!trimmed.isEmpty()) {
                            stems.add(trimmed);
                        }
                    }
                    if (!opts.isEmpty()) {
                        optionsList = opts;
                        questionText = String.join("\n", stems);
                        log.info("[选项提取] type={}, questionText中提取到{}个选项: {}",
                                questionType, opts.size(), opts);
                    } else {
                        log.warn("[选项缺失] type={}, questionText未找到行内选项, rawText长度={}, 前100字={}",
                                questionType, rawQuestionText.length(),
                                rawQuestionText.substring(0, Math.min(100, rawQuestionText.length())));
                    }
                }
                // 判断题：自动生成"正确/错误"选项
                if ((optionsList == null || optionsList.isEmpty()) && "true_false".equals(questionType)) {
                    optionsList = List.of("A. 正确", "B. 错误");
                    log.info("[判断题选项] 自动生成正确/错误选项");
                }

                InterviewQuestion entity = new InterviewQuestion();
                entity.setPositionType(positionType);
                entity.setCategory(category);
                entity.setDifficulty(difficultyCode);
                entity.setQuestionText(questionText);
                entity.setReferenceAnswer(referenceAnswer);
                entity.setIsAiGenerated(1);
                entity.setStatus(1);
                entity.setCreateTime(DateUtils.now());
                questionMapper.insert(entity);

                QuestionDifficulty diff = QuestionDifficulty.fromCode(difficultyCode);
                result.add(QuestionItem.builder()
                        .number(i + 1)
                        .question(questionText)
                        .questionType("null".equals(questionType) || questionType.isEmpty() ? null : questionType)
                        .options(optionsList)
                        .difficulty(diff != null ? diff.getLabel() : "中等")
                        .difficultyCode(difficultyCode)
                        .referenceAnswer(referenceAnswer)
                        .build());
            } catch (Exception e) {
                log.warn("解析 AI 题目失败: index={}, error={}", i, e.getMessage());
            }
        }
        return result;
    }

    private int parseDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isEmpty() || "null".equals(difficulty)) {
            return QuestionDifficulty.MEDIUM.getCode();
        }
        return switch (difficulty.toLowerCase()) {
            case "easy" -> QuestionDifficulty.EASY.getCode();
            case "hard" -> QuestionDifficulty.HARD.getCode();
            default -> QuestionDifficulty.MEDIUM.getCode();
        };
    }

    /**
     * 从题目文本推断题型，用于 LLM 未返回 type 字段或返回无效值时的降级方案。
     */
    private String inferQuestionType(String questionText) {
        if (questionText == null || questionText.isBlank()) return "essay";

        // 检查文本中是否包含选项标识（A. B. C. D. 等），说明是选择题
        boolean hasOptions = questionText.matches("(?s).*[A-Ea-e][\\.\\)、）]\\s.*[A-Ea-e][\\.\\)、）]\\s.*");

        // "哪些" → 多选
        if (questionText.contains("哪些") && hasOptions) {
            return "multiple_choice";
        }

        // 选择题特征词或检测到选项格式 → 单选
        if (questionText.contains("哪项")
                || questionText.contains("哪个")
                || questionText.contains("不属于")
                || questionText.contains("不正确的一项")
                || questionText.contains("正确的一项")
                || (questionText.contains("以下") && !questionText.contains("哪些"))
                || hasOptions) {
            return "single_choice";
        }

        // 不含问号且不含常见问答引导词 → 判断
        if (!questionText.contains("？") && !questionText.contains("?")
                && !questionText.contains("请") && !questionText.contains("简述")
                && !questionText.contains("描述") && !questionText.contains("如何")
                && !questionText.contains("什么") && !questionText.contains("为什么")) {
            return "true_false";
        }

        // 默认：问答
        return "essay";
    }

    private List<QuestionItem> reNumber(List<QuestionItem> items, int startNum) {
        List<QuestionItem> result = new ArrayList<>();
        for (QuestionItem item : items) {
            result.add(QuestionItem.builder()
                    .number(startNum++)
                    .question(item.getQuestion())
                    .questionType(item.getQuestionType())
                    .options(item.getOptions())
                    .difficulty(item.getDifficulty())
                    .difficultyCode(item.getDifficultyCode())
                    .referenceAnswer(item.getReferenceAnswer())
                    .build());
        }
        return result;
    }

    private List<QuestionItem> toItems(List<InterviewQuestion> questions, int startNum) {
        List<QuestionItem> items = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            InterviewQuestion q = questions.get(i);
            QuestionDifficulty diff = QuestionDifficulty.fromCode(q.getDifficulty());
            items.add(QuestionItem.builder()
                    .number(startNum + i)
                    .question(q.getQuestionText())
                    .difficulty(diff != null ? diff.getLabel() : "中等")
                    .difficultyCode(q.getDifficulty())
                    .referenceAnswer(q.getReferenceAnswer())
                    .build());
        }
        return items;
    }

    /** 清理超过 10 分钟的过期任务。 */
    private void cleanExpiredTasks() {
        long now = DateUtils.currentEpochMillis();
        Iterator<Map.Entry<String, GenerateTask>> it = taskMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, GenerateTask> entry = it.next();
            if (now - entry.getValue().createdAt > TASK_TTL_MS) {
                it.remove();
            }
        }
    }
}
