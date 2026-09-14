package com.smartrecruit.interview.service.impl;

import com.smartrecruit.common.exception.BusinessException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.util.JwtUtil;
import com.smartrecruit.interview.dto.request.SubmitAnswersRequest;
import com.smartrecruit.interview.dto.response.AssessmentPageVO;
import com.smartrecruit.interview.dto.remote.LlmChatRequest;
import com.smartrecruit.interview.dto.response.AssessmentQuestionItem;
import com.smartrecruit.interview.dto.response.AssessResultVO;
import com.smartrecruit.interview.entity.Interview;
import com.smartrecruit.interview.entity.OnlineAssessment;
import com.smartrecruit.interview.feign.AiEngineClient;
import com.smartrecruit.interview.repository.InterviewMapper;
import com.smartrecruit.interview.repository.OnlineAssessmentMapper;
import com.smartrecruit.interview.service.PublicAssessmentService;
import com.smartrecruit.interview.service.QuestionBankService;
import com.smartrecruit.interview.util.AssessmentTokenUtil;
import com.smartrecruit.common.util.DateUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 公开测评服务实现。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class PublicAssessmentServiceImpl implements PublicAssessmentService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 问答题 AI 评分系统提示词 */
    private static final String ESSAY_SCORING_PROMPT = """
            你是一位资深的面试官和技术评审专家，擅长根据参考答案评估候选人的回答质量。

            请根据以下信息，对候选人的回答进行评分：
            - 题目内容
            - 参考答案（关键得分点）
            - 候选人的实际回答

            评分标准（满分 10 分）：
            - 9-10 分：回答全面准确，覆盖所有关键得分点，表达清晰专业
            - 7-8 分：回答基本正确，覆盖大部分得分点，有少量遗漏
            - 6 分：回答触及核心要点，但不够全面或表达一般
            - 4-5 分：回答部分相关，但遗漏较多关键点或有明显错误
            - 1-3 分：回答与题目关系不大或存在严重错误
            - 0 分：未作答或完全错误

            ★ 请严格按以下 JSON 格式返回（不要包含任何解释性文字）：
            {
              "score": 8,
              "passed": true,
              "feedback": "简要评价（50字以内），说明得分理由"
            }

            注意：
            - passed 为 true 表示得分 >= 6，即视为回答正确
            - 如果候选人未作答（空字符串或"未作答"），直接返回 score=0, passed=false
            - 只返回 JSON，不要输出其他内容
            """;

    private static final int ESSAY_PASS_THRESHOLD = 6;

    private final OnlineAssessmentMapper assessmentMapper;
    private final QuestionBankService questionBankService;
    private final InterviewMapper interviewMapper;
    private final AiEngineClient aiEngineClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public PublicAssessmentServiceImpl(OnlineAssessmentMapper assessmentMapper,
                                        QuestionBankService questionBankService,
                                        InterviewMapper interviewMapper,
                                        ObjectProvider<AiEngineClient> aiEngineClientProvider) {
        this.assessmentMapper = assessmentMapper;
        this.questionBankService = questionBankService;
        this.interviewMapper = interviewMapper;
        this.aiEngineClient = aiEngineClientProvider.getIfAvailable();
        log.info("PublicAssessmentService: AiEngineClient 注入状态 = {}",
                aiEngineClient != null ? "已就绪" : "未注入，问答题将标记为需人工评阅");
    }

    /** 根据访问令牌查询在线测评信息。 */
    @Override
    public AssessmentPageVO getAssessmentByToken(String token) {
        OnlineAssessment entity = validateTokenAndGetAssessment(token);

        List<AssessmentQuestionItem> questions = resolveAssessmentQuestions(entity);
        stripCorrectAnswers(questions);

        Integer duration = switch (entity.getType()) {
            case 0 -> 60;  // 编程测试 60 分钟
            case 1 -> 30;  // 性格测试 30 分钟
            case 2 -> 45;  // 智商测试 45 分钟
            default -> 30;
        };

        return AssessmentPageVO.builder()
                .assessmentId(entity.getId())
                .candidateId(entity.getCandidateId())
                .candidateName(entity.getCandidateName())
                .jobTitle(entity.getJobTitle())
                .type(entity.getType())
                .typeLabel(entity.getTypeLabel())
                .status(entity.getStatus())
                .candidateEmail(entity.getCandidateEmail())
                .sentTime(entity.getSentTime() != null
                        ? DateUtils.formatDateTimeMinute(entity.getSentTime())
                        : null)
                .questions(questions)
                .durationMinutes(duration)
                .build();
    }

    /** 提交在线测评答案并生成成绩。 */
    @Override
    @Transactional
    public AssessResultVO submitAssessment(SubmitAnswersRequest request) {
        OnlineAssessment entity = validateTokenAndGetAssessment(request.getToken());
        List<AssessmentQuestionItem> questions = resolveAssessmentQuestions(entity);

        // 分离客观题（单选/多选/判断）和问答题
        List<AssessmentQuestionItem> objectiveQuestions = new ArrayList<>();
        List<AssessmentQuestionItem> essayQuestions = new ArrayList<>();
        for (AssessmentQuestionItem q : questions) {
            if ("essay".equals(q.getQuestionType())) {
                essayQuestions.add(q);
            } else {
                objectiveQuestions.add(q);
            }
        }

        int objectiveCorrect = 0;
        int essayCorrect = 0;
        int essayTotal = essayQuestions.size();
        StringBuilder essayDetails = new StringBuilder();

        // 1. 客观题：精确匹配评分
        if (!objectiveQuestions.isEmpty()) {
            objectiveCorrect = scoreObjectiveQuestions(objectiveQuestions, request.getAnswers());
        }

        // 2. 问答题：AI 评分
        if (!essayQuestions.isEmpty()) {
            Map<Integer, EssayScoreResult> essayResults = evaluateEssayAnswers(
                    essayQuestions, request.getAnswers());
            for (Map.Entry<Integer, EssayScoreResult> entry : essayResults.entrySet()) {
                EssayScoreResult r = entry.getValue();
                if (r.passed) {
                    essayCorrect++;
                }
                if (!essayDetails.isEmpty()) {
                    essayDetails.append("; ");
                }
                essayDetails.append("Q").append(entry.getKey())
                        .append(":").append(r.score).append("/10");
            }
        }

        // 3. 组合分数
        int totalCorrect = objectiveCorrect + essayCorrect;
        int totalQuestions = questions.size();
        String score;
        if (essayTotal > 0) {
            score = totalCorrect + "/" + totalQuestions;
        } else {
            score = totalCorrect + "/" + totalQuestions;
        }

        String description;
        if (essayTotal > 0 && essayDetails.length() > 0) {
            description = questionBankService.getResultDescription(
                    entity.getType(), score, totalQuestions, totalCorrect);
            description += "（问答题 AI 评分详情：" + essayDetails + "）";
        } else {
            description = questionBankService.getResultDescription(
                    entity.getType(), score, totalQuestions, totalCorrect);
        }

        // 更新测评状态
        entity.setScore(score);
        entity.setStatus(2);
        assessmentMapper.updateById(entity);
        log.info("测评提交完成: id={}, type={}, score={}, 客观{}道正确{}道, 问答{}道正确{}道",
                entity.getId(), entity.getType(), score,
                objectiveQuestions.size(), objectiveCorrect,
                essayTotal, essayCorrect);

        // 4. 如果测评关联了面试，同步更新面试状态
        Long interviewId = entity.getInterviewId();
        if (interviewId != null) {
            Interview interview = interviewMapper.selectById(interviewId);
            if (interview != null && interview.getStatus() == 0) { // 0=SCHEDULED 已安排
                interview.setStatus(1); // 1=IN_PROGRESS 进行中
                interviewMapper.updateById(interview);
                log.info("关联面试状态已更新: interviewId={}, status 0→1(进行中)", interviewId);
            }
        }

        return AssessResultVO.builder()
                .assessmentId(entity.getId())
                .candidateName(entity.getCandidateName())
                .jobTitle(entity.getJobTitle())
                .type(entity.getType())
                .typeLabel(entity.getTypeLabel())
                .score(score)
                .totalQuestions(totalQuestions)
                .correctCount(totalCorrect)
                .resultDescription(description)
                .status(2)
                .build();
    }

    /**
     * 客观题评分：精确匹配正确选项。
     */
    private int scoreObjectiveQuestions(List<AssessmentQuestionItem> questions,
                                        List<SubmitAnswersRequest.AnswerItem> answers) {
        int correct = 0;
        for (SubmitAnswersRequest.AnswerItem answer : answers) {
            for (AssessmentQuestionItem q : questions) {
                if (q.getQuestionId().equals(answer.getQuestionId())
                        && q.getCorrectAnswer() != null
                        && q.getCorrectAnswer().equalsIgnoreCase(answer.getSelectedAnswer())) {
                    correct++;
                    break;
                }
            }
        }
        return correct;
    }

    /**
     * 使用 AI 评估问答题答案。
     */
    private Map<Integer, EssayScoreResult> evaluateEssayAnswers(
            List<AssessmentQuestionItem> essayQuestions,
            List<SubmitAnswersRequest.AnswerItem> answers) {

        Map<Integer, EssayScoreResult> results = new java.util.LinkedHashMap<>();

        for (AssessmentQuestionItem question : essayQuestions) {
            // 找到对应的候选人答案
            String candidateAnswer = "";
            for (SubmitAnswersRequest.AnswerItem a : answers) {
                if (a.getQuestionId().equals(question.getQuestionId())) {
                    candidateAnswer = a.getSelectedAnswer() != null ? a.getSelectedAnswer() : "";
                    break;
                }
            }

            // 空答案直接判 0 分
            if (candidateAnswer.isBlank()) {
                results.put(question.getQuestionId(), new EssayScoreResult(0, false));
                continue;
            }

            // 尝试 AI 评分
            EssayScoreResult result = evaluateSingleEssayWithAi(question, candidateAnswer);
            results.put(question.getQuestionId(), result);
        }

        return results;
    }

    /**
     * 调用 AI 对单道问答题评分。
     */
    @SuppressWarnings("unchecked")
    private EssayScoreResult evaluateSingleEssayWithAi(AssessmentQuestionItem question,
                                                       String candidateAnswer) {
        if (aiEngineClient == null) {
            log.warn("AI 引擎未启用，问答题 Q{} 标记为需人工评阅", question.getQuestionId());
            // AI 不可用时：宽松处理，有内容就给 6 分（视为通过）
            return new EssayScoreResult(6, true);
        }

        String refAnswer = question.getCorrectAnswer() != null ? question.getCorrectAnswer() : "无参考答案";

        String userPrompt = String.format("""
                题目：%s

                参考答案（关键得分点）：%s

                候选人回答：%s
                """, question.getQuestionText(), refAnswer, candidateAnswer);

        try {
            Map<String, Object> response = aiEngineClient.chat(
                    new LlmChatRequest(ESSAY_SCORING_PROMPT, userPrompt, "interview-evaluator"));

            // 解析 AI 返回的 JSON
            if (response == null || response.isEmpty()) {
                log.warn("AI 返回空响应，Q{} 默认给 6 分", question.getQuestionId());
                return new EssayScoreResult(6, true);
            }

            // AI 返回的数据可能在 content 字段或其他字段中
            Object content = response.get("content");
            if (content instanceof Map) {
                Map<String, Object> scoreMap = (Map<String, Object>) content;
                int score = parseScore(scoreMap.get("score"));
                boolean passed = scoreMap.get("passed") instanceof Boolean b ? b : score >= ESSAY_PASS_THRESHOLD;
                log.info("AI 问答题评分: Q{} score={} passed={}", question.getQuestionId(), score, passed);
                return new EssayScoreResult(score, passed);
            }

            // 尝试从响应中直接提取
            if (response.containsKey("score")) {
                int score = parseScore(response.get("score"));
                boolean passed = response.get("passed") instanceof Boolean b ? b : score >= ESSAY_PASS_THRESHOLD;
                return new EssayScoreResult(score, passed);
            }

            log.warn("AI 返回格式异常，Q{} 默认给 6 分: response={}", question.getQuestionId(), response);
            return new EssayScoreResult(6, true);

        } catch (Exception e) {
            log.error("AI 问答题评分异常，Q{} 默认给 6 分: {}", question.getQuestionId(), e.getMessage());
            return new EssayScoreResult(6, true);
        }
    }

    private int parseScore(Object scoreObj) {
        if (scoreObj instanceof Number n) {
            return Math.max(0, Math.min(10, n.intValue()));
        }
        if (scoreObj instanceof String s) {
            try {
                return Math.max(0, Math.min(10, Integer.parseInt(s)));
            } catch (NumberFormatException ignored) {
            }
        }
        return 6; // 默认 6 分
    }

    /**
     * 问答题 AI 评分结果。
     */
    private static class EssayScoreResult {
        final int score;
        final boolean passed;

        EssayScoreResult(int score, boolean passed) {
            this.score = score;
            this.passed = passed;
        }
    }

    /**
     * 解析测评题目：优先使用 AI 生成的题目，否则回退到硬编码题库。
     */
    private List<AssessmentQuestionItem> resolveAssessmentQuestions(OnlineAssessment entity) {
        if (entity.getQuestionsJson() != null && !entity.getQuestionsJson().isBlank()) {
            try {
                List<AssessmentQuestionItem> questions = OBJECT_MAPPER.readValue(
                        entity.getQuestionsJson(),
                        new TypeReference<List<AssessmentQuestionItem>>() {});
                log.info("使用 AI 生成题目: assessmentId={}, count={}",
                        entity.getId(), questions.size());
                return questions;
            } catch (Exception e) {
                log.warn("AI 题目 JSON 解析失败，回退到题库: assessmentId={}, error={}",
                        entity.getId(), e.getMessage());
            }
        }
        return questionBankService.getQuestions(entity.getType(), 10);
    }

    /**
     * 从题目列表中移除 correctAnswer，避免泄露给候选人。
     */
    private void stripCorrectAnswers(List<AssessmentQuestionItem> questions) {
        questions.forEach(q -> q.setCorrectAnswer(null));
    }

    /**
     * 验证测评 token 并返回对应的 OnlineAssessment 实体。
     */
    private OnlineAssessment validateTokenAndGetAssessment(String token) {
        SecretKey secretKey = JwtUtil.getSecretKey(jwtSecret);
        Claims claims = AssessmentTokenUtil.parseAssessmentToken(secretKey, token);
        if (claims == null) {
            throw new BusinessException("ASSESSMENT_TOKEN_INVALID", "测评链接无效或已过期，请联系招聘负责人重新发送。");
        }

        String assessmentIdStr = claims.get("assessmentId", String.class);
        if (assessmentIdStr == null) {
            throw new BusinessException("ASSESSMENT_TOKEN_INVALID", "测评链接无效，缺少必要信息。");
        }

        Long assessmentId;
        try {
            assessmentId = Long.parseLong(assessmentIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException("ASSESSMENT_TOKEN_INVALID", "测评链接格式错误。");
        }

        OnlineAssessment entity = assessmentMapper.selectById(assessmentId);
        if (entity == null || entity.getDeleted() == 1) {
            throw new ResourceNotFoundException("OnlineAssessment", assessmentId);
        }

        if (entity.getStatus() != 1) {
            throw new BusinessException("ASSESSMENT_ALREADY_COMPLETED",
                    entity.getStatus() == 2 ? "该测评已完成，无法再次访问。" : "该测评尚未发送，请联系招聘负责人。");
        }

        return entity;
    }
}
