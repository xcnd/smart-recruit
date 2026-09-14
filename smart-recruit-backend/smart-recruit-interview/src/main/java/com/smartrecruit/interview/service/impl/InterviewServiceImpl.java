package com.smartrecruit.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.constant.NotificationConstants;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.interview.enums.InterviewEnums;
import com.smartrecruit.common.exception.BusinessException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.interview.dto.request.AssessmentRequest;
import com.smartrecruit.interview.dto.request.CreateInterviewRequest;
import com.smartrecruit.interview.dto.request.InterviewFeedbackRequest;
import com.smartrecruit.interview.dto.request.SubmitInterviewRequest;
import com.smartrecruit.interview.dto.request.UpdateInterviewRequest;
import com.smartrecruit.interview.dto.response.AiStatsVO;
import com.smartrecruit.interview.dto.response.AssessedCandidateVO;
import com.smartrecruit.interview.dto.response.InterviewDetailVO;
import com.smartrecruit.interview.dto.response.InterviewReportVO;
import com.smartrecruit.interview.dto.response.InterviewReportVO.CandidateInfo;
import com.smartrecruit.interview.dto.response.InterviewReportVO.DimensionScore;
import com.smartrecruit.interview.dto.response.InterviewScheduleVO;
import com.smartrecruit.interview.dto.response.InterviewStatsVO;
import com.smartrecruit.interview.dto.response.InterviewVO;
import com.smartrecruit.interview.dto.response.NextRoundVO;
import com.smartrecruit.interview.entity.Interview;
import com.smartrecruit.interview.entity.InterviewFeedback;
import com.smartrecruit.interview.repository.InterviewFeedbackMapper;
import com.smartrecruit.interview.repository.InterviewMapper;
import com.smartrecruit.interview.dto.remote.CandidateDTO;
import com.smartrecruit.interview.dto.remote.AddToTalentPoolRequest;
import com.smartrecruit.interview.dto.remote.UpdateCandidateStageRequest;
import com.smartrecruit.interview.dto.remote.ActivityRecordRequest;
import com.smartrecruit.interview.dto.remote.WorkbenchTaskRequest;
import com.smartrecruit.interview.dto.remote.AgentQuestionRequest;
import com.smartrecruit.interview.dto.remote.SendNotificationRequest;
import com.smartrecruit.interview.feign.RecruitmentClient;
import com.smartrecruit.interview.feign.AiAgentCapabilityClient;
import com.smartrecruit.interview.feign.SystemClient;
import com.smartrecruit.interview.feign.TalentClient;
import com.smartrecruit.interview.service.AiEvaluationService;
import com.smartrecruit.interview.service.AsyncEvaluationService;
import com.smartrecruit.interview.service.AssessmentService;
import com.smartrecruit.interview.service.InterviewService;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 面试管理服务实现。
 *
 * <p>包含模拟 AI 生成的问题和六维度评估评分。
 * 在生产环境中，AI 生成部分将调用 LLM 服务。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewMapper interviewMapper;
    private final InterviewFeedbackMapper feedbackMapper;
    private final AssessmentService assessmentService;
    private final AiEvaluationService aiEvaluationService;
    private final AsyncEvaluationService asyncEvaluationService;
    private final RecruitmentClient recruitmentClient;
    private final TalentClient talentClient;
    private final SystemClient systemClient;
    private final AiAgentCapabilityClient aiAgentCapabilityClient;

    public InterviewServiceImpl(InterviewMapper interviewMapper,
                                InterviewFeedbackMapper feedbackMapper,
                                AssessmentService assessmentService,
                                AiEvaluationService aiEvaluationService,
                                AsyncEvaluationService asyncEvaluationService,
                                RecruitmentClient recruitmentClient,
                                TalentClient talentClient,
                                SystemClient systemClient,
                                AiAgentCapabilityClient aiAgentCapabilityClient) {
        this.interviewMapper = interviewMapper;
        this.feedbackMapper = feedbackMapper;
        this.assessmentService = assessmentService;
        this.aiEvaluationService = aiEvaluationService;
        this.asyncEvaluationService = asyncEvaluationService;
        this.recruitmentClient = recruitmentClient;
        this.talentClient = talentClient;
        this.systemClient = systemClient;
        this.aiAgentCapabilityClient = aiAgentCapabilityClient;
    }

    private static final Map<Integer, List<String>> AI_QUESTION_TEMPLATES = Map.of(
            InterviewEnums.InterviewType.AI.getCode(), List.of( // AI面试
                    "请简单介绍一下你的技术背景和主要项目经验。",
                    "描述一个你解决过的复杂技术难题，以及你的解决思路。",
                    "你是如何保持技术学习的？最近在学习什么新技术？",
                    "请解释一下你对微服务架构的理解以及实际应用经验。",
                    "描述一次你在团队中推动技术改进的经历。"
            ),
            InterviewEnums.InterviewType.TECHNICAL.getCode(), List.of( // 技术面试
                    "请介绍你过去项目中使用的技术栈。",
                    "简述你对高并发系统的理解和设计思路。",
                    "你如何保证代码质量和系统稳定性？",
                    "描述你常用的设计模式及其应用场景。",
                    "在项目开发中，你如何处理需求变更和技术债务？"
            ),
            InterviewEnums.InterviewType.HR.getCode(), List.of( // HR面试
                    "请分享一个你带领团队完成挑战性目标的案例。",
                    "描述一次你与同事发生分歧的经历，你是如何处理的？",
                    "你如何设定个人成长目标并确保达成？",
                    "面对紧迫的截止日期，你如何排定优先级？",
                    "谈谈你对公司价值观的理解以及你的契合点。"
            ),
            InterviewEnums.InterviewType.LEADERSHIP.getCode(), List.of( // 领导面试
                    "请分享你带领团队攻克一个重大挑战的经历。",
                    "你是如何培养和激励团队成员的？",
                    "描述你的管理风格以及它如何影响团队绩效。",
                    "在资源有限的情况下，你如何排定优先级并做出决策？",
                    "你如何处理团队中的低绩效成员？"
            )
    );

    private static final List<String> DEFAULT_QUESTIONS = List.of(
            "请做一下自我介绍。",
            "描述一个你最引以为豪的项目。",
            "你对我们公司有什么了解？",
            "你对薪资有什么期望？",
            "你有什么优势适合这个岗位？"
    );

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<InterviewVO> pageQuery(Page<?> page, Map<String, Object> params) {
        // 面试官筛选：先根据姓名查用户 ID，再用 ID 过滤 interviewer_ids JSON 列
        if (params.containsKey("interviewerName")) {
            Object nameObj = params.get("interviewerName");
            if (nameObj instanceof String name && !name.isBlank()) {
                List<Long> userIds = interviewMapper.selectUserIdsByName(name.trim());
                params.remove("interviewerName");
                params.put("interviewerIds", userIds.isEmpty() ? List.of(-1L) : userIds);
            }
        }
        Page<Interview> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<Interview> result = interviewMapper.selectPageWithFilters(mpPage, params);
        List<Interview> records = result.getRecords();

        List<InterviewVO> vos = records.stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(vos, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    /** 创建记录。 */
    @Override
    @Transactional
    public InterviewVO create(CreateInterviewRequest request) {
        Interview interview = new Interview();

        // 检查候选人是否已达到面试终点（终面或第7轮及以上，完成状态）
        // 未满1年 → 阻止创建；超过1年 → 重置为第1轮
        boolean resetRound = false;
        Map<String, Object> ended = interviewMapper.getLatestEndedInterview(request.getCandidateId());
        if (ended != null) {
            Object updateTimeObj = ended.get("update_time");
            if (updateTimeObj instanceof LocalDateTime completedAt) {
                if (completedAt.plusYears(1).isAfter(DateUtils.now())) {
                    Object isFinalObj = ended.get("is_final_round");
                    Object roundObj = ended.get("round");
                    boolean isFinal = isFinalObj instanceof Number && ((Number) isFinalObj).intValue() == 1;
                    int rounds = roundObj instanceof Number ? ((Number) roundObj).intValue() : 0;
                    String reason = isFinal
                            ? "该候选人的终面已于 " + completedAt.toLocalDate() + " 完成"
                            : "该候选人第" + rounds + "轮面试已于 " + completedAt.toLocalDate() + " 完成";
                    throw new BusinessException("CANDIDATE_INTERVIEW_COMPLETED",
                            reason + "，1年内无需再安排面试。");
                }
                // 超过1年，允许重新开始，强制从第1轮算起
                resetRound = true;
                log.info("候选人 {} 的终面/第7轮面试已超过1年，重置为第1轮", request.getCandidateId());
            }
        }

        // 检查该轮次是否已存在（一年内同一轮次不可重复安排）
        int requestRound = request.getRound() != null ? request.getRound() : 1;
        if (!resetRound) {
            List<Integer> existingRounds = interviewMapper.selectExistingRounds(
                    request.getCandidateId(), DateUtils.now().minusYears(1));
            if (existingRounds.contains(requestRound)) {
                throw new BusinessException("DUPLICATE_INTERVIEW_ROUND",
                        "该候选人第" + requestRound + "轮面试已存在，一年内不可重复安排同一轮次");
            }
        }

        interview.setApplicationId(0L); // 占位符 - 申请记录将稍后关联
        interview.setCandidateId(request.getCandidateId());
        interview.setCandidateName(request.getCandidateName() != null ? request.getCandidateName() : "");
        interview.setJobPositionId(request.getJobId());
        interview.setJobTitle(request.getJobTitle() != null ? request.getJobTitle() : "");
        interview.setType(request.getInterviewType());
        interview.setScheduledTime(request.getScheduledAt());
        interview.setDurationMinutes(request.getDurationMin());
        interview.setRound(resetRound ? 1 : (request.getRound() != null ? request.getRound() : 1));
        interview.setIsFinalRound(request.getIsFinalRound() != null ? request.getIsFinalRound() : 0);
        interview.setStatus(InterviewEnums.InterviewStatus.SCHEDULED.getCode());
        interview.setSubject(determineSubject(request.getInterviewType()));
        interview.setInterviewerIds(request.getInterviewerId() != null
                ? List.of(request.getInterviewerId())
                : List.of(200001L)); // 默认分配面试官(admin)

        // 先生成内置模板题目作为兜底，AI 出题改为异步执行，避免同步调用超时
        interview.setAiTranscript(String.join("\n",
                AI_QUESTION_TEMPLATES.getOrDefault(request.getInterviewType(), DEFAULT_QUESTIONS)));
        // AI 出题功能临时屏蔽：状态置为 0（模板题目），不再自动触发 AI 生成
        interview.setAiQuestionStatus(0);

        interviewMapper.insert(interview);
        log.info("面试已创建: id={}, type={}, candidateId={}",
                interview.getId(), interview.getType(), interview.getCandidateId());

        // AI 出题功能临时屏蔽：不再自动调用 asyncGenerateQuestions，
        // 需要时恢复下面这行即可
        // asyncGenerateQuestions(interview, request.getInterviewType(), requestRound,
        //         request.getJobTitle(), request.getCandidateName());

        // 站内通知面试官
        notifyInterviewers(interview,
                "新面试安排 - " + interviewPersonLabel(interview),
                "候选人 " + interviewPersonLabel(interview) + " 的「" + interview.getJobTitle()
                        + "」面试将于 " + formatScheduledTime(interview.getScheduledTime()) + " 开始，请准时参加。",
                NotificationConstants.BIZ_INTERVIEW_SCHEDULED,
                "/interviews");

        // 站内通知管理员：新面试安排
        notifyAdmins(interview,
                "新面试安排 - " + interviewPersonLabel(interview),
                "候选人 " + interviewPersonLabel(interview) + " 的「" + interview.getJobTitle()
                        + "」面试已安排：" + formatScheduledTime(interview.getScheduledTime()) + "。",
                NotificationConstants.BIZ_INTERVIEW_SCHEDULED,
                "/interviews");

        return toVO(interview);
    }

    /** 查询下一轮面试建议。 */
    @Override
    public NextRoundVO getSuggestedNextRound(Long candidateId) {
        List<Integer> existingRounds = interviewMapper.selectExistingRounds(
                candidateId, DateUtils.now().minusYears(1));
        int nextRound = 1;
        if (!existingRounds.isEmpty()) {
            int maxRound = existingRounds.stream().mapToInt(Integer::intValue).max().orElse(0);
            nextRound = Math.min(maxRound + 1, 7);
        }
        return new NextRoundVO(nextRound, existingRounds);
    }

    /** 根据主键查询详情。 */
    @Override
    public InterviewDetailVO getById(Long id) {
        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new ResourceNotFoundException("面试", id);
        }

        List<String> aiQuestions = new ArrayList<>();
        if (interview.getAiTranscript() != null) {
            aiQuestions = List.of(interview.getAiTranscript().split("\n"));
        }

        return InterviewDetailVO.builder()
                .id(interview.getId())
                .applicationId(interview.getApplicationId())
                .candidateId(interview.getCandidateId())
                .jobPositionId(interview.getJobPositionId())
                .round(interview.getRound())
                .type(interview.getType())
                .subject(interview.getSubject())
                .scheduledTime(interview.getScheduledTime())
                .durationMinutes(interview.getDurationMinutes())
                .location(interview.getLocation())
                .status(interview.getStatus())
                .result(interview.getResult())
                .feedback(interview.getFeedback())
                .score(interview.getScore())
                .evaluation(interview.getEvaluation())
                .aiQuestions(aiQuestions)
                .aiQuestionStatus(interview.getAiQuestionStatus())
                .aiAnalysis(interview.getAiAnalysis())
                .recordingUrl(null) // 录像 URL 占位符
                .createTime(interview.getCreateTime())
                .updateTime(interview.getUpdateTime())
                .build();
    }

    /** 查询面试的 AI 面试题目列表（按行拆分）。 */
    @Override
    public List<String> getAiQuestions(Long id) {
        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new ResourceNotFoundException("面试", id);
        }
        if (interview.getAiTranscript() == null || interview.getAiTranscript().isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(interview.getAiTranscript().split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** 生成并查询面试评估报告。 */
    @Override
    public InterviewReportVO getReport(Long id) {
        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new ResourceNotFoundException("面试", id);
        }

        if (InterviewEnums.InterviewStatus.COMPLETED.getCode() != interview.getStatus()) {
            throw new BusinessException("INTERVIEW_NOT_COMPLETED",
                    "面试尚未完成，暂无报告可查看。");
        }

        // 解析 evaluation 数据（兼容 JacksonTypeHandler 反序列化为 Map 和保持原始 String 两种情况）
        // 评估中（PROCESSING）或已有数据时正常解析，不再做同步 lazy-init 阻塞
        String evalStatus = interview.getEvaluationStatus();
        Map<?, ?> evalMap = toEvalMap(interview.getEvaluation());
        log.info("getReport evalMap: interviewId={}, status={}, evalMap={}",
                id, evalStatus, evalMap != null ? evalMap.keySet() : "null");
        List<DimensionScore> dimensions = List.of();
        List<String> strengthsList = List.of();
        List<String> weaknessesList = List.of();
        if (evalMap != null) {
            dimensions = parseDimensionsFromEvaluation(evalMap);
            strengthsList = readStringList(evalMap.get("strengths"));
            weaknessesList = readStringList(evalMap.get("weaknesses"));
        }

        int overallScore = (int) Math.round(
                dimensions.stream()
                        .mapToDouble(d -> d.getScore() * d.getWeight() / 100.0)
                        .sum());

        String suggestion = mapResultToSuggestion(interview.getResult());

        CandidateInfo candidateInfo = fetchCandidateInfo(interview.getCandidateId());

        // 检查候选人是否已有下一轮面试（已安排或进行中）
        boolean hasNextRound = false;
        Long cid = interview.getCandidateId();
        Integer currentRound = interview.getRound();
        if (cid != null && currentRound != null) {
            LambdaQueryWrapper<Interview> nextRoundQuery = new LambdaQueryWrapper<>();
            nextRoundQuery.eq(Interview::getCandidateId, cid)
                          .gt(Interview::getRound, currentRound)
                          .ne(Interview::getStatus, InterviewEnums.InterviewStatus.CANCELLED.getCode());
            hasNextRound = interviewMapper.selectCount(nextRoundQuery) > 0;
        }

        return InterviewReportVO.builder()
                .interviewId(interview.getId())
                .candidateId(interview.getCandidateId())
                .candidateName(interview.getCandidateName())
                .jobTitle(interview.getJobTitle())
                .jobId(interview.getJobPositionId())
                .type(interview.getType())
                .round(interview.getRound())
                .scheduledAt(interview.getScheduledTime())
                .candidate(candidateInfo)
                .overallScore(overallScore)
                .result(interview.getResult())
                .suggestion(suggestion)
                .feedback(interview.getFeedback())
                .dimensions(dimensions)
                .strengths(String.join("\n", strengthsList))
                .weaknesses(String.join("\n", weaknessesList))
                .hasNextRound(hasNextRound)
                .evaluationStatus(evalStatus)
                .build();
    }

    /** 提交面试结果。 */
    @Override
    @Transactional
    public void submitResult(Long id, SubmitInterviewRequest request) {
        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new ResourceNotFoundException("面试", id);
        }

        if (InterviewEnums.InterviewStatus.CANCELLED.getCode() == interview.getStatus()) {
            throw new BusinessException("INTERVIEW_CANCELLED",
                    "已取消的面试无法提交结果。");
        }

        // 使用启发式评分快速生成基础评估，不阻塞 HTTP 响应
        // 走完整评估链：AgentScope2 HarnessAgent → LLM 网关 → 启发式评分
        AssessmentRequest assessReq = aiEvaluationService.evaluate(interview);
        int overallScore = assessReq.getOverallScore() != null ? assessReq.getOverallScore().intValue() : 0;

        // 维度评分用于 evaluationMap（从 AssessmentRequest 反推 0-100 分数）
        List<DimensionScore> mockDimensions = dimensionsFromAssessRequest(assessReq);

        Map<String, Object> evaluationMap = new LinkedHashMap<>();
        evaluationMap.put("dimensions", mockDimensions.stream().map(d -> Map.of(
                "name", d.getName(),
                "weight", d.getWeight(),
                "score", d.getScore()
        )).toList());
        evaluationMap.put("totalScore", overallScore);
        evaluationMap.put("strengths", assessReq.getStrengths());
        evaluationMap.put("weaknesses", assessReq.getWeaknesses());
        evaluationMap.put("assessedAt", DateUtils.now().toString());

        Integer result = overallScore >= 70
                ? InterviewEnums.InterviewResult.PASS.getCode()
                : overallScore >= 60
                        ? InterviewEnums.InterviewResult.HOLD.getCode()
                        : InterviewEnums.InterviewResult.FAIL.getCode();

        interview.setStatus(InterviewEnums.InterviewStatus.COMPLETED.getCode());
        interview.setResult(result);
        interview.setFeedback(request.getFeedback());
        interview.setScore(overallScore);
        interview.setEvaluation(evaluationMap);

        interviewMapper.updateById(interview);
        log.info("面试结果已提交: id={}, score={}, result={}", id, overallScore, result);

        // 站内通知面试官查看报告
        notifyInterviewers(interview,
                "面试已完成 - " + interviewPersonLabel(interview),
                "候选人 " + interviewPersonLabel(interview) + " 的「" + interview.getJobTitle()
                        + "」面试已完成，综合评分 " + overallScore + " 分，请查看面试报告。",
                NotificationConstants.BIZ_INTERVIEW_COMPLETED,
                "/interviews/" + id + "/report");

        // 创建包含维度评分的反馈记录
        InterviewFeedback feedback = new InterviewFeedback();
        feedback.setInterviewId(id);
        feedback.setInterviewerId(0L); // 系统/AI 生成
        feedback.setOverallRating(Math.clamp(overallScore / 20, 1, 5));
        feedback.setDimensions(evaluationMap);
        feedback.setStrengths(generateStrengths(mockDimensions));
        feedback.setWeaknesses(generateWeaknesses(mockDimensions));
        feedback.setSuggestions("建议候选人针对薄弱环节进行针对性提升。");
        feedback.setOverallComment(request.getFeedback());
        feedback.setTechnologyDepth(assessReq.getTechnologyDepth());
        feedback.setCommunication(assessReq.getCommunication());
        feedback.setProblemSolving(assessReq.getProblemSolving());
        feedback.setLearningAbility(assessReq.getLearningAbility());
        feedback.setTeamwork(assessReq.getTeamwork());
        feedback.setHireRecommendation(
                InterviewEnums.InterviewResult.PASS.getCode() == result
                        ? InterviewEnums.HireRecommendation.HIRE.getCode()
                        : InterviewEnums.InterviewResult.HOLD.getCode() == result
                                ? InterviewEnums.HireRecommendation.HOLD.getCode()
                                : InterviewEnums.HireRecommendation.REJECT.getCode());
        feedbackMapper.insert(feedback);

        // 保存基础评估到 rec_interview_assessment
        assessmentService.save(id, assessReq);

        // 异步触发 AI 评估，用 LLM 结果覆盖基础评估
        asyncEvaluationService.evaluateAsync(id, overallScore);

        // 后续 Feign 调用异步执行，不阻塞响应
        final int finalResult = result;
        final int finalScore = overallScore;
        Thread.startVirtualThread(() -> {
            syncCandidateStage(interview);
            syncToTalentPool(interview);
            recordActivity(interview, "面试完成", "候选人 " + interview.getCandidateName()
                    + " 的「" + interview.getSubject() + "」面试已完成，得分 " + finalScore);

            String name = interview.getCandidateName() != null ? interview.getCandidateName() : "未知";
            String jobTitle = interview.getJobTitle() != null ? interview.getJobTitle() : "未知职位";
            if (finalResult == InterviewEnums.InterviewResult.PASS.getCode()) {
                createTask(interview,
                        "反馈评审 - " + name,
                        "候选人 " + name + " 的「" + jobTitle + "」面试已通过，分数：" + finalScore + "，请评审反馈。",
                        4, 1, 1);
            } else if (finalResult == InterviewEnums.InterviewResult.HOLD.getCode()) {
                createTask(interview,
                        "反馈评审 - " + name,
                        "候选人 " + name + " 的「" + jobTitle + "」面试结果为待定，请评审反馈。",
                        4, 0, 1);
            } else {
                createTask(interview,
                        "反馈评审 - " + name,
                        "候选人 " + name + " 的「" + jobTitle + "」面试未通过，分数：" + finalScore + "，请确认。",
                        4, 2, 2);
            }
        });
    }

    /** 取消记录。 */
    @Override
    @Transactional
    public void cancel(Long id) {
        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new ResourceNotFoundException("面试", id);
        }

        if (InterviewEnums.InterviewStatus.SCHEDULED.getCode() != interview.getStatus()) {
            throw new BusinessException("INTERVIEW_STATUS_INVALID",
                    "只有已安排的面试才能取消，当前状态: " + interview.getStatus());
        }

        // 使用 LambdaUpdateWrapper 只更新 status 字段，避免 updateById 全字段更新
        // 导致 JacksonTypeHandler 序列化 interviewerIds 等 JSON 字段时数据损坏
        LambdaUpdateWrapper<Interview> wrapper =
                new LambdaUpdateWrapper<>();
        wrapper.eq(Interview::getId, id)
               .set(Interview::getStatus, InterviewEnums.InterviewStatus.CANCELLED.getCode());
        interviewMapper.update(wrapper);
        log.info("面试已取消: id={}", id);
    }

    /** 根据主键删除记录。 */
    @Override
    @Transactional
    public void delete(Long id) {
        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new ResourceNotFoundException("面试", id);
        }

        if (InterviewEnums.InterviewStatus.IN_PROGRESS.getCode() == interview.getStatus()) {
            throw new BusinessException("INTERVIEW_IN_PROGRESS",
                    "无法删除正在进行的面试。");
        }

        // 逻辑删除：MyBatis-Plus 的 @TableLogic 会将 deleted 设为 1
        interviewMapper.deleteById(id);
        log.info("面试已删除: id={}", id);
    }

    /** 查询统计信息。 */
    @Override
    public InterviewStatsVO getStats() {
        Map<String, Object> row = interviewMapper.selectStats();
        return InterviewStatsVO.builder()
                .total(toLong(row.get("total")))
                .today(toLong(row.get("today")))
                .passed(toLong(row.get("passed")))
                .cancelled(toLong(row.get("cancelled")))
                .build();
    }

    /** 查询统计信息。 */
    @Override
    public InterviewStatsVO getStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> row = start == null && end == null
                ? interviewMapper.selectStats()
                : interviewMapper.selectStatsRange(start, end);
        return InterviewStatsVO.builder()
                .total(toLong(row.get("total")))
                .today(toLong(row.get("today")))
                .passed(toLong(row.get("passed")))
                .cancelled(toLong(row.get("cancelled")))
                .build();
    }

    /** 更新记录。 */
    @Override
    @Transactional
    public InterviewVO update(Long id, UpdateInterviewRequest request) {
        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new ResourceNotFoundException("面试", id);
        }

        if (InterviewEnums.InterviewStatus.CANCELLED.getCode() == interview.getStatus()) {
            throw new BusinessException("INTERVIEW_CANCELLED",
                    "已取消的面试无法修改。");
        }

        // 仅更新非空字段
        if (request.getCandidateId() != null) {
            interview.setCandidateId(request.getCandidateId());
        }
        if (request.getCandidateName() != null) {
            interview.setCandidateName(request.getCandidateName());
        }
        if (request.getJobId() != null) {
            interview.setJobPositionId(request.getJobId());
        }
        if (request.getJobTitle() != null) {
            interview.setJobTitle(request.getJobTitle());
        }
        if (request.getInterviewType() != null) {
            interview.setType(request.getInterviewType());
            interview.setSubject(determineSubject(request.getInterviewType()));
        }
        if (request.getRound() != null) {
            interview.setRound(request.getRound());
        }
        if (request.getInterviewerId() != null) {
            interview.setInterviewerIds(List.of(request.getInterviewerId()));
        }
        if (request.getScheduledAt() != null) {
            interview.setScheduledTime(request.getScheduledAt());
        }
        if (request.getDurationMin() != null) {
            interview.setDurationMinutes(request.getDurationMin());
        }
        if (request.getIsFinalRound() != null) {
            interview.setIsFinalRound(request.getIsFinalRound());
        }

        interviewMapper.updateById(interview);
        log.info("面试已更新: id={}", id);

        return toVO(interview);
    }

    /** 查询面试日程。 */
    @Override
    public List<InterviewScheduleVO> getSchedule(String date) {
        List<Interview> interviews = interviewMapper.selectScheduleByDate(date);
        Map<Long, String> interviewerNames = fetchInterviewerNameMap(interviews);
        return interviews.stream().map(entity -> {
            String startTime = entity.getScheduledTime() != null
                    ? DateUtils.formatTime(entity.getScheduledTime()) : "";
            String endTime = "";
            if (entity.getScheduledTime() != null && entity.getDurationMinutes() != null) {
                endTime = DateUtils.formatTime(
                        entity.getScheduledTime().plusMinutes(entity.getDurationMinutes()));
            }
            String timeSlot = !startTime.isEmpty() && !endTime.isEmpty()
                    ? startTime + " - " + endTime : startTime;
            return InterviewScheduleVO.builder()
                    .id(entity.getId())
                    .timeSlot(timeSlot)
                    .startTime(startTime)
                    .endTime(endTime)
                    .candidateName(entity.getCandidateName() != null ? entity.getCandidateName() : "")
                    .jobTitle(entity.getJobTitle() != null ? entity.getJobTitle() : "")
                    .interviewerName(resolveInterviewerName(entity.getInterviewerIds(), interviewerNames))
                    .typeLabel(resolveTypeLabel(entity.getType()))
                    .statusTag(resolveStatusTag(entity.getStatus()))
                    .statusCode(entity.getStatus())
                    .scheduledDate(entity.getScheduledTime() != null
                            ? entity.getScheduledTime().format(DateUtils.DATE_FORMATTER) : date)
                    .build();
        }).toList();
    }

    /** 查询周面试日程。 */
    @Override
    public List<InterviewScheduleVO> getWeekSchedule(String startDate, String endDate) {
        List<Interview> interviews = interviewMapper.selectScheduleByDateRange(startDate, endDate);
        Map<Long, String> interviewerNames = fetchInterviewerNameMap(interviews);
        return interviews.stream().map(entity -> {
            String startTime = entity.getScheduledTime() != null
                    ? DateUtils.formatTime(entity.getScheduledTime()) : "";
            String endTime = "";
            if (entity.getScheduledTime() != null && entity.getDurationMinutes() != null) {
                endTime = DateUtils.formatTime(
                        entity.getScheduledTime().plusMinutes(entity.getDurationMinutes()));
            }
            String timeSlot = !startTime.isEmpty() && !endTime.isEmpty()
                    ? startTime + " - " + endTime : startTime;
            return InterviewScheduleVO.builder()
                    .id(entity.getId())
                    .timeSlot(timeSlot)
                    .startTime(startTime)
                    .endTime(endTime)
                    .candidateName(entity.getCandidateName() != null ? entity.getCandidateName() : "")
                    .jobTitle(entity.getJobTitle() != null ? entity.getJobTitle() : "")
                    .interviewerName(resolveInterviewerName(entity.getInterviewerIds(), interviewerNames))
                    .typeLabel(resolveTypeLabel(entity.getType()))
                    .statusTag(resolveStatusTag(entity.getStatus()))
                    .statusCode(entity.getStatus())
                    .scheduledDate(entity.getScheduledTime() != null
                            ? entity.getScheduledTime().format(DateUtils.DATE_FORMATTER) : "")
                    .build();
        }).toList();
    }

    /** 查询 AI 面试统计数据。 */
    @Override
    public AiStatsVO getAiStats() {
        Map<String, Object> row = interviewMapper.selectAiStats();
        return AiStatsVO.builder()
                .todayInterviews(toLong(row.get("todayInterviews")))
                .completed(toLong(row.get("completed")))
                .aiAssessed(toLong(row.get("aiAssessed")))
                .passRate(toDouble(row.get("passRate")))
                .build();
    }

    /** 查询已评估候选人列表。 */
    @Override
    public List<AssessedCandidateVO> getAssessedCandidates() {
        List<Map<String, Object>> rows = interviewMapper.selectAssessedCandidates();
        return rows.stream().map(row -> AssessedCandidateVO.builder()
                .interviewId(toLong(row.get("interview_id")))
                .candidateId(toLong(row.get("candidate_id")))
                .departmentId(row.get("department_id") instanceof Number n ? n.longValue() : null)
                .candidateName((String) row.get("candidate_name"))
                .jobTitle((String) row.get("job_title"))
                .overallScore(row.get("overall_score") instanceof BigDecimal bd ? bd : null)
                .assessedAt((String) row.get("assessed_at"))
                .build()).toList();
    }

    /** 提交面试反馈。 */
    @Override
    @Transactional
    public void submitFeedback(Long id, InterviewFeedbackRequest request) {
        log.info("▶▶▶ submitFeedback 开始执行 (V2): interviewId={}, hireRecommendation={}, techRating={}, commRating={}, solveRating={}, learnRating={}, teamRating={}",
                id, request.getHireRecommendation(), request.getTechRating(), request.getCommRating(),
                request.getSolveRating(), request.getLearnRating(), request.getTeamRating());

        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            log.error("▶▶▶ submitFeedback: 面试记录不存在! id={}", id);
            throw new ResourceNotFoundException("面试", id);
        }
        log.info("▶▶▶ submitFeedback: 已加载面试记录, currentStatus={}, currentResult={}",
                interview.getStatus(), interview.getResult());

        // 1. 保存反馈记录到 rec_interview_feedback
        log.info("▶▶▶ submitFeedback 步骤1: 保存反馈记录...");
        InterviewFeedback feedback = new InterviewFeedback();
        feedback.setInterviewId(id);
        feedback.setInterviewerId(0L); // 系统/当前用户
        feedback.setTechnologyDepth(toBigDecimal(request.getTechRating()));
        feedback.setCommunication(toBigDecimal(request.getCommRating()));
        feedback.setProblemSolving(toBigDecimal(request.getSolveRating()));
        feedback.setLearningAbility(toBigDecimal(request.getLearnRating()));
        feedback.setTeamwork(toBigDecimal(request.getTeamRating()));
        feedback.setOverallRating(calculateAverageRating(request));
        feedback.setDimensions(buildDimensionsMap(request));
        feedback.setOverallComment(request.getComments());
        feedback.setHireRecommendation(request.getHireRecommendation());
        feedbackMapper.insert(feedback);
        log.info("▶▶▶ submitFeedback 步骤1完成: 反馈记录已保存, feedbackId={}", feedback.getId());

        // 2. 根据反馈评分构建评估数据
        // 录用建议 → 面试结果: 0/1=通过, 2=待定, 3=淘汰
        log.info("▶▶▶ submitFeedback 步骤2: 构建评估数据...");
        int result;
        if (request.getHireRecommendation() == null) {
            result = InterviewEnums.InterviewResult.HOLD.getCode();
        } else {
            result = switch (request.getHireRecommendation()) {
                case 0, 1 -> InterviewEnums.InterviewResult.PASS.getCode();
                case 2 -> InterviewEnums.InterviewResult.HOLD.getCode();
                default -> InterviewEnums.InterviewResult.FAIL.getCode();
            };
        }

        int score = calculateScoreFromRatings(request);
        log.info("▶▶▶ submitFeedback 步骤2: result={}, score={}", result, score);

        // 3. 更新面试记录：状态、结果、评分、评语
        log.info("▶▶▶ submitFeedback 步骤3: 更新面试记录...");
        LambdaUpdateWrapper<Interview> wrapper =
                new LambdaUpdateWrapper<>();
        wrapper.eq(Interview::getId, id)
               .set(Interview::getStatus, InterviewEnums.InterviewStatus.COMPLETED.getCode())
               .set(Interview::getResult, result)
               .set(Interview::getScore, score)
               .set(Interview::getFeedback, request.getComments());
        interviewMapper.update(null, wrapper);
        log.info("▶▶▶ submitFeedback 步骤3完成: 面试状态/结果/评分/评语 已更新");

        // 4. 先持久化基于反馈的基础评估，再异步触发 AI 补充优劣势
        log.info("▶▶▶ submitFeedback 步骤4: 写入基础评估并触发异步AI...");
        try {
            // 以反馈表星级评分构建维度得分（每星 = 20分）
            Map<String, Object> evaluationMap = buildEvaluationFromFeedback(request);
            evaluationMap.put("assessedAt", DateUtils.now().toString());

            String evaluationJson = new ObjectMapper()
                    .writeValueAsString(evaluationMap);
            interviewMapper.updateEvaluation(id, evaluationJson);
            interviewMapper.updateEvaluationStatus(id, "PROCESSING");
            log.info("▶▶▶ submitFeedback 步骤4: 基础评估已写入，状态=PROCESSING");

            // 保存基础评估到 rec_interview_assessment
            AssessmentRequest saveReq = new AssessmentRequest();
            saveReq.setCandidateId(interview.getCandidateId());
            saveReq.setCandidateName(interview.getCandidateName());
            saveReq.setJobTitle(interview.getJobTitle());
            saveReq.setOverallScore(BigDecimal.valueOf(score));
            saveReq.setStrengths((List<String>) evaluationMap.get("strengths"));
            saveReq.setWeaknesses((List<String>) evaluationMap.get("weaknesses"));
            assessmentService.save(id, saveReq);
            log.info("▶▶▶ submitFeedback 步骤4: 基础评估已保存到评估表");
        } catch (Exception e) {
            log.error("▶▶▶ submitFeedback 步骤4: 基础评估持久化失败: interviewId={}", id, e);
        }

        // 异步触发 AI 优劣势分析（不阻塞主流程）
        try {
            asyncEvaluationService.evaluateAsync(id, score);
            log.info("▶▶▶ submitFeedback 步骤4: 已触发异步AI评估 interviewId={}", id);
        } catch (Exception e) {
            log.warn("▶▶▶ submitFeedback 步骤4: 触发异步AI评估失败（不影响主流程）: interviewId={}", id, e);
        }

        // 5-7. 异步同步候选人阶段、人才库、活动动态、待办任务
        // 这些 Feign 调用不影响反馈提交结果，通过虚拟线程异步执行
        final int finalResult = result;
        final int finalScore = score;
        Thread.startVirtualThread(() -> {
            Interview updated = interviewMapper.selectById(id);
            try {
                syncCandidateStage(updated);
            } catch (Exception e) {
                log.warn("同步候选人阶段失败: interviewId={}", id, e.getMessage());
            }
            try {
                syncToTalentPool(updated);
            } catch (Exception e) {
                log.warn("同步人才库失败: interviewId={}", id, e.getMessage());
            }
            String resultLabel = switch (finalResult) {
                case 0 -> "通过";
                case 1 -> "未通过";
                default -> "待定";
            };
            try {
                recordActivity(updated,
                        updated.getCandidateName() + " 面试" + resultLabel,
                        "候选人 " + updated.getCandidateName() + " 的「" + updated.getJobTitle()
                                + "」面试已完成，结果：" + resultLabel + "，评分：" + finalScore + "分。");
            } catch (Exception e) {
                log.warn("记录活动动态失败: interviewId={}", id, e.getMessage());
            }
            try {
                if (finalResult == InterviewEnums.InterviewResult.PASS.getCode()) {
                    createTask(updated,
                            "反馈评审 - " + updated.getCandidateName(),
                            "候选人 " + updated.getCandidateName() + " 的「" + updated.getJobTitle()
                                    + "」面试已通过，分数：" + finalScore + "，请评审反馈。",
                            4, 1, 1);
                } else if (finalResult == InterviewEnums.InterviewResult.HOLD.getCode()) {
                    createTask(updated,
                            "反馈评审 - " + updated.getCandidateName(),
                            "候选人 " + updated.getCandidateName() + " 的「" + updated.getJobTitle()
                                    + "」面试结果为待定，请评审反馈决定下一步。",
                            4, 0, 1);
                } else {
                    createTask(updated,
                            "反馈评审 - " + updated.getCandidateName(),
                            "候选人 " + updated.getCandidateName() + " 的「" + updated.getJobTitle()
                                    + "」面试未通过，分数：" + finalScore + "，请确认评审结果。",
                            4, 2, 2);
                }
            } catch (Exception e) {
                log.warn("创建待办任务失败: interviewId={}", id, e.getMessage());
            }
        });

        // 站内通知管理员：面试反馈已提交
        String resultLabel = switch (result) {
            case 0 -> "通过";
            case 1 -> "未通过";
            default -> "待定";
        };
        notifyAdmins(interview,
                "面试反馈已提交 - " + interview.getCandidateName(),
                "候选人 " + interview.getCandidateName() + " 的「" + interview.getJobTitle()
                        + "」面试反馈已提交，结果：" + resultLabel + "，评分：" + score + "分。",
                NotificationConstants.BIZ_INTERVIEW_COMPLETED,
                "/interviews/" + id);

        log.info("▶▶▶ submitFeedback 全部完成! interviewId={}, result={}, score={}, status=已完成", id, result, score);
    }

    /** 从反馈评分计算综合分（1-5 → 0-100）。 */
    private int calculateScoreFromRatings(InterviewFeedbackRequest request) {
        int sum = 0;
        int count = 0;
        if (request.getTechRating() != null && request.getTechRating() > 0) { sum += request.getTechRating(); count++; }
        if (request.getCommRating() != null && request.getCommRating() > 0) { sum += request.getCommRating(); count++; }
        if (request.getSolveRating() != null && request.getSolveRating() > 0) { sum += request.getSolveRating(); count++; }
        if (request.getLearnRating() != null && request.getLearnRating() > 0) { sum += request.getLearnRating(); count++; }
        if (request.getTeamRating() != null && request.getTeamRating() > 0) { sum += request.getTeamRating(); count++; }
        return count > 0 ? Math.round((float) sum / count * 20) : 60;
    }

    /** 从反馈评分构建 evaluation JSON Map。 */
    private Map<String, Object> buildEvaluationFromFeedback(InterviewFeedbackRequest request) {
        int tech = request.getTechRating() != null ? request.getTechRating() * 20 : 60;
        int comm = request.getCommRating() != null ? request.getCommRating() * 20 : 60;
        int solve = request.getSolveRating() != null ? request.getSolveRating() * 20 : 60;
        int learn = request.getLearnRating() != null ? request.getLearnRating() * 20 : 60;
        int team = request.getTeamRating() != null ? request.getTeamRating() * 20 : 60;
        // 抗压能力取其他维度的平均值
        int stress = (int) Math.round((tech + comm + solve + learn + team) / 5.0);

        List<Map<String, Object>> dimensions = List.of(
                Map.of("name", "技术深度", "weight", 25, "score", tech),
                Map.of("name", "沟通表达", "weight", 20, "score", comm),
                Map.of("name", "问题解决", "weight", 20, "score", solve),
                Map.of("name", "团队协作", "weight", 15, "score", team),
                Map.of("name", "学习能力", "weight", 10, "score", learn),
                Map.of("name", "抗压能力", "weight", 10, "score", stress)
        );

        List<String> strengths = buildStrengthListFromRatings(request);
        List<String> weaknesses = buildWeaknessListFromRatings(request);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("dimensions", dimensions);
        map.put("totalScore", calculateScoreFromRatings(request));
        map.put("strengths", strengths);
        map.put("weaknesses", weaknesses);
        map.put("assessedAt", DateUtils.now().toString());
        return map;
    }

    private List<String> buildStrengthListFromRatings(InterviewFeedbackRequest request) {
        List<String> list = new ArrayList<>();
        if (request.getTechRating() != null && request.getTechRating() >= 4)
            list.add("技术深度突出，专业基础扎实");
        if (request.getCommRating() != null && request.getCommRating() >= 4)
            list.add("沟通表达清晰，逻辑条理好");
        if (request.getSolveRating() != null && request.getSolveRating() >= 4)
            list.add("问题解决能力强，思路敏捷");
        if (request.getTeamRating() != null && request.getTeamRating() >= 4)
            list.add("团队协作意识好");
        if (request.getLearnRating() != null && request.getLearnRating() >= 4)
            list.add("学习能力强，可快速成长");
        if (list.isEmpty()) list.add("综合表现达到预期");
        return list;
    }

    private List<String> buildWeaknessListFromRatings(InterviewFeedbackRequest request) {
        List<String> list = new ArrayList<>();
        if (request.getTechRating() != null && request.getTechRating() <= 3)
            list.add("技术深度有待提升");
        if (request.getCommRating() != null && request.getCommRating() <= 3)
            list.add("沟通表达能力需加强");
        if (request.getSolveRating() != null && request.getSolveRating() <= 3)
            list.add("问题解决能力有待提高");
        if (request.getTeamRating() != null && request.getTeamRating() <= 3)
            list.add("团队协作方面需改善");
        if (request.getLearnRating() != null && request.getLearnRating() <= 3)
            list.add("学习能力和技术敏感度需提升");
        if (list.isEmpty()) list.add("建议在更多项目实践中积累经验");
        return list;
    }

    /** 开始面试。 */
    @Override
    @Transactional
    public void startInterview(Long id) {
        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new ResourceNotFoundException("面试", id);
        }
        if (interview.getStatus() != InterviewEnums.InterviewStatus.SCHEDULED.getCode()) {
            throw new BusinessException("INTERVIEW_STATUS_INVALID",
                    "只有已安排的面试才能开始，当前状态: " + interview.getStatus());
        }
        // 使用 LambdaUpdateWrapper 只更新 status，避免 updateById 全字段更新时
        // JacksonTypeHandler 序列化 interviewerIds 等 JSON 字段导致数据损坏
        LambdaUpdateWrapper<Interview> wrapper =
                new LambdaUpdateWrapper<>();
        wrapper.eq(Interview::getId, id)
               .set(Interview::getStatus, InterviewEnums.InterviewStatus.IN_PROGRESS.getCode());
        interviewMapper.update(wrapper);
        log.info("面试已开始: id={}", id);
    }

    /** 更新面试结果（面试结果、评分与评语）。 */
    @Override
    @Transactional
    public void updateResult(Long id, Integer result, Integer score, String feedback) {
        Interview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new ResourceNotFoundException("面试", id);
        }

        InterviewEnums.InterviewResult resultEnum = InterviewEnums.InterviewResult.fromCode(result);
        if (resultEnum == null) {
            throw new BusinessException("INVALID_RESULT", "无效的面试结果编码: " + result);
        }

        LambdaUpdateWrapper<Interview> wrapper =
                new LambdaUpdateWrapper<>();
        wrapper.eq(Interview::getId, id)
               .set(Interview::getResult, result);
        if (score != null) {
            wrapper.set(Interview::getScore, score);
        }
        if (feedback != null && !feedback.isBlank()) {
            wrapper.set(Interview::getFeedback, feedback);
        }
        if (result == InterviewEnums.InterviewResult.PASS.getCode()
                || result == InterviewEnums.InterviewResult.FAIL.getCode()
                || result == InterviewEnums.InterviewResult.HOLD.getCode()) {
            wrapper.set(Interview::getStatus, InterviewEnums.InterviewStatus.COMPLETED.getCode());
        }
        interviewMapper.update(null, wrapper);
        log.info("面试结果已更新: id={}, result={}, score={}, hasFeedback={}",
                id, resultEnum.getLabel(), score, feedback != null);

        // 重新加载实体以获取完整数据供后续同步使用
        final Interview updated = interviewMapper.selectById(id);

        // 异步：Feign 同步 + AI 评估，不阻塞 HTTP 响应
        Thread.startVirtualThread(() -> {
            syncCandidateStage(updated);
            syncToTalentPool(updated);
            aiEvaluateAndPersist(updated, score, feedback);
        });
    }

    /**
     * AI 评估面试并持久化报告数据。
     * <p>评估失败时静默降级，不影响面试结果提交的主流程。</p>
     */
    private void aiEvaluateAndPersist(Interview interview, Integer manualScore, String feedbackText) {
        log.info("开始 AI 评估持久化: interviewId={}, manualScore={}", interview.getId(), manualScore);
        try {
            AssessmentRequest assessReq = aiEvaluationService.evaluate(interview);

            // 如果人工评了分，优先使用人工评分作为综合分
            if (manualScore != null && manualScore > 0) {
                assessReq.setOverallScore(BigDecimal.valueOf(manualScore));
            }

            // 构建 evaluation JSON
            List<DimensionScore> dims = dimensionsFromAssessRequest(assessReq);
            Map<String, Object> evaluationMap = new LinkedHashMap<>();
            evaluationMap.put("dimensions", dims.stream()
                    .map(d -> Map.of("name", d.getName(), "weight", d.getWeight(), "score", d.getScore()))
                    .toList());
            evaluationMap.put("totalScore", assessReq.getOverallScore() != null
                    ? assessReq.getOverallScore().intValue() : 0);
            evaluationMap.put("strengths", assessReq.getStrengths());
            evaluationMap.put("weaknesses", assessReq.getWeaknesses());
            evaluationMap.put("assessedAt", DateUtils.now().toString());

            // 只更新 evaluation 列，避免 updateById 全字段更新时
            // JacksonTypeHandler 序列化 interviewerIds 等字段导致数据异常
            String evaluationJson = new ObjectMapper()
                    .writeValueAsString(evaluationMap);
            int updated = interviewMapper.updateEvaluation(interview.getId(), evaluationJson);
            log.info("AI 评估已写入面试评估字段: interviewId={}, rowsUpdated={}, overallScore={}",
                    interview.getId(), updated, assessReq.getOverallScore());

            // 持久化到 rec_interview_assessment
            assessmentService.save(interview.getId(), assessReq);
            log.info("AI 评估已保存到评估表: interviewId={}", interview.getId());
        } catch (Exception e) {
            log.warn("AI 评估失败（已降级），面试结果已正常提交: interviewId={}",
                    interview.getId(), e);
        }
    }

    /**
     * 同步更新候选人招聘阶段。
     */
    private void syncCandidateStage(Interview interview) {
        if (recruitmentClient == null) return;
        try {
            String stageName;
            if (interview.getResult() != null && interview.getResult() == InterviewEnums.InterviewResult.PASS.getCode()) {
                stageName = "OFFER";
            } else if (interview.getResult() != null && interview.getResult() == InterviewEnums.InterviewResult.FAIL.getCode()) {
                stageName = "REJECTED";
            } else {
                stageName = null;
            }
            if (stageName != null) {
                recruitmentClient.updateCandidateStage(
                        interview.getCandidateId(), new UpdateCandidateStageRequest(stageName));
                log.info("候选人阶段已同步: candidateId={}, stage={}", interview.getCandidateId(), stageName);
            }
        } catch (Exception e) {
            log.error("同步候选人阶段失败: candidateId={}, error={}", interview.getCandidateId(), e.getMessage());
        }
    }

    /**
     * 将候选人加入人才库（即使面试未通过）
     */
    private void syncToTalentPool(Interview interview) {
        if (talentClient == null) return;
        try {
            // 根据面试结果生成标签
            List<String> tags = new ArrayList<>();
            if (interview.getResult() != null && interview.getResult() == InterviewEnums.InterviewResult.PASS.getCode()) {
                tags.add("面试通过");
            }
            tags.add(interview.getSubject() != null ? interview.getSubject() : "面试候选人");
            talentClient.addToPool(new AddToTalentPoolRequest(interview.getCandidateId(), tags));
            log.info("候选人已加入人才库: candidateId={}", interview.getCandidateId());
        } catch (Exception e) {
            log.error("加入人才库失败: candidateId={}, error={}", interview.getCandidateId(), e.getMessage());
        }
    }

    /**
     * 记录活动动态到招聘服务（通过 Feign），用于工作台"最近动态"展示。
     */
    private void recordActivity(Interview interview, String title, String description) {
        if (recruitmentClient == null) return;
        try {
            recruitmentClient.recordActivity(new ActivityRecordRequest(
                    2, title, description, 0L, "system", "INTERVIEW", interview.getId()));
            log.info("Activity recorded: interviewId={}", interview.getId());
        } catch (Exception e) {
            log.warn("Failed to record activity feed for interview: {}", e.getMessage());
        }
    }

    /**
     * 创建待办任务到招聘服务（通过 Feign）。
     */
    private void createTask(Interview interview, String title, String description,
                            int taskType, int priority, long daysUntilDue) {
        if (recruitmentClient == null) return;
        try {
            WorkbenchTaskRequest task = new WorkbenchTaskRequest();
            task.setUserId(0L);
            task.setTitle(title);
            task.setDescription(description);
            task.setType(taskType);
            task.setPriority(priority);
            task.setRelatedType("INTERVIEW");
            task.setRelatedId(interview.getId());
            task.setCandidateName(interview.getCandidateName());
            if (daysUntilDue > 0) {
                task.setDueDate(DateUtils.now().plusDays(daysUntilDue));
            }
            recruitmentClient.createTask(task);
            log.info("Task created: interviewId={}, title={}", interview.getId(), title);
        } catch (Exception e) {
            log.warn("Failed to create workbench task for interview: {}", e.getMessage());
        }
    }

    // ========== 私有辅助方法 ==========

    private InterviewVO toVO(Interview entity) {
        return InterviewVO.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .candidateId(entity.getCandidateId())
                .candidateName(entity.getCandidateName() != null ? entity.getCandidateName() : "")
                .jobPositionId(entity.getJobPositionId())
                .jobTitle(entity.getJobTitle() != null ? entity.getJobTitle() : "")
                .interviewerIds(entity.getInterviewerIds())
                .interviewerName(fetchInterviewerName(entity.getInterviewerIds()))
                .round(entity.getRound())
                .isFinalRound(entity.getIsFinalRound())
                .type(entity.getType())
                .subject(entity.getSubject())
                .scheduledTime(entity.getScheduledTime())
                .durationMinutes(entity.getDurationMinutes())
                .location(entity.getLocation())
                .status(entity.getStatus())
                .statusLabel(resolveStatusLabel(entity.getStatus()))
                .result(entity.getResult())
                .resultLabel(resolveResultLabel(entity.getResult()))
                .typeLabel(resolveTypeLabel(entity.getType()))
                .score(entity.getScore())
                .aiQuestionStatus(entity.getAiQuestionStatus())
                .createTime(entity.getCreateTime())
                .build();
    }

    @SuppressWarnings("unchecked")
    private String fetchInterviewerName(Object interviewerIds) {
        if (interviewerIds == null) return "";
        // 1. 已经是 List，直接取第一个元素
        if (interviewerIds instanceof List<?> l) {
            if (l.isEmpty()) return "";
            return resolveUserRealName(l.get(0));
        }
        // 2. 字符串形式：可能是 JSON 数组 "[200001]" 或纯数字 "200001"
        if (interviewerIds instanceof String s && !s.isEmpty()) {
            String trimmed = s.trim();
            // JSON 数组格式
            if (trimmed.startsWith("[")) {
                try {
                    List<?> list = new ObjectMapper().readValue(trimmed, List.class);
                    if (!list.isEmpty()) return resolveUserRealName(list.get(0));
                } catch (Exception e) {
                    log.warn("解析 interviewer_ids JSON 失败: {}", s, e);
                }
            } else {
                // 纯数字字符串
                return resolveUserRealName(trimmed);
            }
        }
        // 3. 直接是数字
        if (interviewerIds instanceof Number n) {
            return resolveUserRealName(n);
        }
        return "";
    }

    /** 将单个元素（Number 或 String）解析为用户真实姓名。 */
    private String resolveUserRealName(Object element) {
        Long id = null;
        if (element instanceof Number n) {
            id = n.longValue();
        } else if (element instanceof String s) {
            try { id = Long.parseLong(s); } catch (NumberFormatException e) { return ""; }
        }
        if (id == null || id == 0) return "";
        String name = interviewMapper.selectUserRealName(id);
        return name != null ? name : "";
    }

    /**
     * 批量查询面试官姓名：一次跨库查询收集全部面试官 ID → 姓名映射，
     * 避免按面试逐条查询造成的 N+1 性能问题。
     */
    private Map<Long, String> fetchInterviewerNameMap(List<Interview> interviews) {
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        for (Interview interview : interviews) {
            collectInterviewerIds(interview.getInterviewerIds(), ids);
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> nameMap = new HashMap<>();
        for (Map<String, Object> row : interviewMapper.selectRealNamesByIds(new ArrayList<>(ids))) {
            Object id = row.get("id");
            Object name = row.get("real_name");
            if (id instanceof Number n && name != null) {
                nameMap.put(n.longValue(), String.valueOf(name));
            }
        }
        return nameMap;
    }

    /** 收集面试官 ID（兼容 List / JSON 数组字符串 / 纯数字字符串）。 */
    private void collectInterviewerIds(Object interviewerIds, java.util.Set<Long> ids) {
        if (interviewerIds instanceof List<?> l) {
            for (Object o : l) {
                Long id = toUserId(o);
                if (id != null) {
                    ids.add(id);
                }
            }
        } else if (interviewerIds instanceof String s && !s.isBlank()) {
            String trimmed = s.trim();
            if (trimmed.startsWith("[")) {
                try {
                    for (Object o : new ObjectMapper().readValue(trimmed, List.class)) {
                        Long id = toUserId(o);
                        if (id != null) {
                            ids.add(id);
                        }
                    }
                } catch (Exception ignored) {
                    // 解析失败按无面试官处理
                }
            } else {
                Long id = toUserId(trimmed);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
    }

    /** 从姓名映射中取首个面试官姓名（无则返回空串）。 */
    private String resolveInterviewerName(Object interviewerIds, Map<Long, String> nameMap) {
        Long first = null;
        if (interviewerIds instanceof List<?> l) {
            if (!l.isEmpty()) {
                first = toUserId(l.get(0));
            }
        } else if (interviewerIds instanceof String s && !s.isBlank()) {
            String trimmed = s.trim();
            if (trimmed.startsWith("[")) {
                try {
                    List<?> list = new ObjectMapper().readValue(trimmed, List.class);
                    if (!list.isEmpty()) {
                        first = toUserId(list.get(0));
                    }
                } catch (Exception ignored) {
                    // 忽略
                }
            } else {
                first = toUserId(trimmed);
            }
        }
        return first == null ? "" : nameMap.getOrDefault(first, "");
    }

    /** 元素转用户 ID（Number 或纯数字字符串）。 */
    private Long toUserId(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        if (o instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String determineSubject(Integer interviewType) {
        if (InterviewEnums.InterviewType.AI.getCode() == interviewType) {
            return "AI智能面试";
        } else if (InterviewEnums.InterviewType.TECHNICAL.getCode() == interviewType) {
            return "技术面试";
        } else if (InterviewEnums.InterviewType.HR.getCode() == interviewType) {
            return "HR面试";
        } else if (InterviewEnums.InterviewType.LEADERSHIP.getCode() == interviewType) {
            return "领导面试";
        }
        return "综合面试";
    }

    private String resolveStatusLabel(Integer status) {
        InterviewEnums.InterviewStatus s = InterviewEnums.InterviewStatus.fromCode(status);
        return s != null ? s.getLabel() : "";
    }

    private String resolveResultLabel(Integer result) {
        InterviewEnums.InterviewResult r = InterviewEnums.InterviewResult.fromCode(result);
        return r != null ? r.getLabel() : "";
    }

    private String resolveTypeLabel(Integer type) {
        InterviewEnums.InterviewType t = InterviewEnums.InterviewType.fromCode(type);
        return t != null ? t.getLabel() : "";
    }

    /** 从 AssessmentRequest 反推六维度评分（1.0-5.0 → 0-100）。 */
    private List<DimensionScore> dimensionsFromAssessRequest(AssessmentRequest req) {
        return List.of(
                new DimensionScore("技术深度", 25, toIntScore(req.getTechnologyDepth())),
                new DimensionScore("沟通表达", 20, toIntScore(req.getCommunication())),
                new DimensionScore("问题解决", 20, toIntScore(req.getProblemSolving())),
                new DimensionScore("团队协作", 15, toIntScore(req.getTeamwork())),
                new DimensionScore("学习能力", 10, toIntScore(req.getLearningAbility())),
                new DimensionScore("抗压能力", 10, toIntScore(req.getTechnologyDepth())) // 抗压能力复用技术评分作为近似
        );
    }

    private int toIntScore(BigDecimal bd) {
        return bd != null ? (int) Math.round(bd.doubleValue() * 20) : 60;
    }

    /** 生成模拟六维度评分。 */
    private List<DimensionScore> generateMockDimensionScores() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return List.of(
                new DimensionScore("技术深度", 25, r.nextInt(55, 96)),
                new DimensionScore("沟通表达", 20, r.nextInt(60, 96)),
                new DimensionScore("问题解决", 20, r.nextInt(55, 96)),
                new DimensionScore("团队协作", 15, r.nextInt(65, 96)),
                new DimensionScore("学习能力", 10, r.nextInt(60, 96)),
                new DimensionScore("抗压能力", 10, r.nextInt(55, 96))
        );
    }

    /**
     * 将 evaluation 字段值转换为 Map（兼容 JacksonTypeHandler 反序列化和原始 JSON 字符串）。
     */
    @SuppressWarnings("unchecked")
    private Map<?, ?> toEvalMap(Object evaluation) {
        if (evaluation instanceof Map<?, ?> m) {
            return m;
        }
        if (evaluation instanceof String s && !s.isBlank()) {
            try {
                return new ObjectMapper().readValue(s, Map.class);
            } catch (Exception e) {
                log.warn("解析 evaluation JSON 字符串失败: {}", s.substring(0, Math.min(100, s.length())), e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<DimensionScore> parseDimensionsFromEvaluation(Map<?, ?> eval) {
        List<DimensionScore> dimensions = new ArrayList<>();
        Object dimsObj = eval.get("dimensions");
        if (dimsObj instanceof List<?> dims) {
            for (Object d : dims) {
                if (d instanceof Map<?, ?> dm) {
                    dimensions.add(new DimensionScore(
                            String.valueOf(dm.get("name")),
                            toInt(dm.get("weight")),
                            toInt(dm.get("score"))
                    ));
                }
            }
        }
        return dimensions;
    }

    private long toLong(Object obj) {
        if (obj instanceof Number n) return n.longValue();
        if (obj instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
        }
        return 0L;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(s -> !s.isBlank()).toList();
        }
        return List.of();
    }

    private String generateStrengths(List<DimensionScore> dimensions) {
        DimensionScore best = dimensions.stream()
                .max((a, b) -> Integer.compare(a.getScore(), b.getScore()))
                .orElse(new DimensionScore("综合能力", 100, 80));
        return "候选人在「" + best.getName() + "」维度表现突出，得分" + best.getScore() + "分，具备良好的专业素养和实践能力。";
    }

    private String generateWeaknesses(List<DimensionScore> dimensions) {
        DimensionScore worst = dimensions.stream()
                .min((a, b) -> Integer.compare(a.getScore(), b.getScore()))
                .orElse(new DimensionScore("综合能力", 100, 60));
        return "建议候选人加强「" + worst.getName() + "」方面的能力，当前得分" + worst.getScore() + "分，可通过相关培训和实践提升。";
    }

    private String resolveStatusTag(Integer status) {
        InterviewEnums.InterviewStatus s = InterviewEnums.InterviewStatus.fromCode(status);
        if (s == null) return "pending";
        return switch (s) {
            case SCHEDULED -> "pending";
            case IN_PROGRESS -> "ongoing";
            case COMPLETED -> "done";
            case CANCELLED, NO_SHOW, RESCHEDULED -> "cancelled";
        };
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        if (obj instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
        }
        return 0.0;
    }

    private int calculateAverageRating(InterviewFeedbackRequest request) {
        int sum = 0;
        int count = 0;
        if (request.getTechRating() != null) { sum += request.getTechRating(); count++; }
        if (request.getCommRating() != null) { sum += request.getCommRating(); count++; }
        if (request.getSolveRating() != null) { sum += request.getSolveRating(); count++; }
        if (request.getLearnRating() != null) { sum += request.getLearnRating(); count++; }
        if (request.getTeamRating() != null) { sum += request.getTeamRating(); count++; }
        return count > 0 ? Math.clamp(sum / count, 1, 5) : 3;
    }

    private Map<String, Object> buildDimensionsMap(InterviewFeedbackRequest request) {
        Map<String, Object> dims = new LinkedHashMap<>();
        if (request.getTechRating() != null) dims.put("tech", request.getTechRating());
        if (request.getCommRating() != null) dims.put("comm", request.getCommRating());
        if (request.getSolveRating() != null) dims.put("solve", request.getSolveRating());
        if (request.getLearnRating() != null) dims.put("learn", request.getLearnRating());
        if (request.getTeamRating() != null) dims.put("team", request.getTeamRating());
        return dims;
    }

    private BigDecimal toBigDecimal(Integer value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }

    private String mapResultToSuggestion(Integer result) {
        if (result == null) return null;
        return switch (result) {
            case 0 -> "ADVANCE";
            case 1 -> "REJECT";
            case 2 -> "RETEST";
            default -> null;
        };
    }

    private CandidateInfo fetchCandidateInfo(Long candidateId) {
        if (candidateId == null || recruitmentClient == null) return null;
        try {
            var resp = recruitmentClient.getCandidate(candidateId);
            if (resp == null || resp.data() == null) return null;
            CandidateDTO c = resp.data();
            return CandidateInfo.builder()
                    .gender(c.getGender())
                    .age(calcAge(c.getBirthDate()))
                    .education(educationLabel(c.getEducation()))
                    .school(c.getSchool())
                    .major(c.getMajor())
                    .city(c.getCity())
                    .phone(c.getPhone())
                    .email(c.getEmail())
                    .currentCompany(c.getCurrentCompany())
                    .currentPosition(c.getCurrentPosition())
                    .yearsOfExperience(c.getYearsOfExperience())
                    .build();
        } catch (Exception e) {
            log.warn("获取候选人信息失败: candidateId={}", candidateId, e);
            return null;
        }
    }

    private Integer calcAge(LocalDate birthDate) {
        if (birthDate == null) return null;
        return DateUtils.calculateAge(birthDate);
    }

    private String educationLabel(Integer code) {
        if (code == null) return null;
        return switch (code) {
            case 0 -> "高中";
            case 1 -> "大专";
            case 2 -> "本科";
            case 3 -> "硕士";
            case 4 -> "博士";
            default -> null;
        };
    }

    /**
     * 站内通知面试官（best-effort，失败不影响主流程）。
     */
    @SuppressWarnings("unchecked")
    private void notifyInterviewers(Interview interview, String title, String content,
                                    String businessType, String actionUrl) {
        Object raw = interview.getInterviewerIds();
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            log.debug("面试官为空，跳过通知: interviewId={}", interview.getId());
            return;
        }
        for (Object item : list) {
            if (!(item instanceof Number n)) {
                continue;
            }
            try {
                SendNotificationRequest request = SendNotificationRequest.builder()
                        .userId(n.longValue())
                        .title(title)
                        .content(content)
                        .type(String.valueOf(NotificationConstants.TYPE_INTERVIEW))
                        .businessType(businessType)
                        .businessId(interview.getId())
                        .actionUrl(actionUrl)
                        .build();
                systemClient.sendNotification(request);
            } catch (Exception e) {
                log.warn("面试通知发送失败（可忽略）: interviewId={}, interviewerId={}, error={}",
                        interview.getId(), item, e.getMessage());
            }
        }
    }

    /**
     * 格式化面试时间（yyyy-MM-dd HH:mm）。
     */
    private String interviewPersonLabel(Interview interview) {
        String name = interview.getCandidateName() != null ? interview.getCandidateName() : "未知";
        String job = interview.getJobTitle() != null && !interview.getJobTitle().isBlank()
                ? interview.getJobTitle() : null;
        return job != null ? name + "（" + job + "）" : name;
    }

    private String formatScheduledTime(LocalDateTime scheduledTime) {
        if (scheduledTime == null) {
            return "待定";
        }
        return DateUtils.formatDateTimeMinute(scheduledTime);
    }

    /**
     * 站内通知管理员（超级管理员 + HR 管理员）——新面试安排、面试反馈等关键操作。
     *
     * <p>best-effort：管理员查询或通知发送失败不影响主流程。</p>
     */
    private void notifyAdmins(Interview interview, String title, String content,
                              String businessType, String actionUrl) {
        if (systemClient == null) {
            return;
        }
        try {
            List<Long> adminIds = systemClient.getAdminUserIds().data();
            if (adminIds == null || adminIds.isEmpty()) {
                log.debug("无启用中的管理员用户，跳过通知: interviewId={}", interview.getId());
                return;
            }
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .userIds(adminIds)
                    .title(title)
                    .content(content)
                    .type(String.valueOf(NotificationConstants.TYPE_INTERVIEW))
                    .businessType(businessType)
                    .businessId(interview.getId())
                    .actionUrl(actionUrl)
                    .build();
            systemClient.sendNotification(request);
        } catch (Exception e) {
            log.warn("管理员通知发送失败（可忽略）: interviewId={}, error={}",
                    interview.getId(), e.getMessage());
        }
    }

    /**
     * 异步调用 AI 出题（内部也走异步任务：提交即返回 taskId，轮询拿结果），
     * 成功后回填面试题目；失败保留内置模板并标记状态。
     */
    private void asyncGenerateQuestions(Interview interview, Integer interviewType, int round,
                                        String jobTitle, String candidateName) {
        if (aiAgentCapabilityClient == null) {
            markAiQuestionFailed(interview.getId());
            return;
        }
        Thread.startVirtualThread(() -> {
            try {
                // 1. 提交异步出题任务，立即拿到 taskId（不等待 LLM）
                ApiResponse<Map<String, String>> submitResp = aiAgentCapabilityClient
                        .submitInterviewQuestions(new AgentQuestionRequest(
                                interviewType, round,
                                jobTitle == null ? "" : jobTitle,
                                candidateName == null ? "" : candidateName));
                if (submitResp == null || !submitResp.ok() || submitResp.data() == null
                        || submitResp.data().get("taskId") == null) {
                    log.warn("AI 出题任务提交失败（保留内置模板）: interviewId={}",
                            interview.getId());
                    markAiQuestionFailed(interview.getId());
                    return;
                }
                String taskId = submitResp.data().get("taskId");

                // 2. 轮询任务结果（每 1.5s 一次，最多 120 次 ≈ 3 分钟）
                List<String> questions = null;
                for (int i = 0; i < 120; i++) {
                    Thread.sleep(1500);
                    ApiResponse<Map<String, Object>> pollResp =
                            aiAgentCapabilityClient.getInterviewQuestionsTask(taskId);
                    if (pollResp == null || !pollResp.ok() || pollResp.data() == null) {
                        continue;
                    }
                    String status = String.valueOf(pollResp.data().get("status"));
                    if ("COMPLETED".equals(status)) {
                        questions = extractQuestions(pollResp.data().get("questions"));
                        break;
                    }
                    if ("FAILED".equals(status)) {
                        log.warn("AI 出题任务失败（保留内置模板）: interviewId={}, taskId={}, message={}",
                                interview.getId(), taskId, pollResp.data().get("message"));
                        break;
                    }
                }

                if (questions != null) {
                    if (!questions.isEmpty()) {
                        // 只更新题目与状态两列；不能用 updateById(部分实体)，
                        // 否则实体的默认字段（如 interviewerIds=[]）会被一并写入
                        interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                                .eq(Interview::getId, interview.getId())
                                .set(Interview::getAiTranscript, String.join("\n", questions))
                                .set(Interview::getAiQuestionStatus, 2));
                        log.info("AI 出题完成: interviewId={}, questions={}",
                                interview.getId(), questions.size());
                        return;
                    }
                }
                markAiQuestionFailed(interview.getId());
            } catch (Exception e) {
                log.warn("AI 出题失败（保留内置模板）: interviewId={}, error={}",
                        interview.getId(), e.getMessage());
                markAiQuestionFailed(interview.getId());
            }
        });
    }

    /** 从任务结果中提取题目文本（兼容 question/content/text 键与纯字符串）。 */
    private List<String> extractQuestions(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(item -> {
            if (item instanceof Map<?, ?> m) {
                Object text = m.get("question");
                if (text == null) {
                    text = m.get("content");
                }
                if (text == null) {
                    text = m.get("text");
                }
                return String.valueOf(text == null ? "" : text);
            }
            return String.valueOf(item == null ? "" : item);
        }).filter(s -> !s.isBlank()).toList();
    }

    /**
     * 标记 AI 出题失败（保留内置模板题目）。
     */
    private void markAiQuestionFailed(Long interviewId) {
        interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, interviewId)
                .set(Interview::getAiQuestionStatus, 3));
    }
}
