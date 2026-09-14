package com.smartrecruit.aiengine.service;

import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.aiengine.domain.JobMatchTarget;
import com.smartrecruit.aiengine.domain.JobRequirement;
import com.smartrecruit.aiengine.domain.OfferDetail;
import com.smartrecruit.aiengine.domain.OnboardingCandidate;
import com.smartrecruit.aiengine.dto.response.JdVO;
import com.smartrecruit.aiengine.dto.response.InterviewQuestionVO;
import com.smartrecruit.aiengine.dto.response.ResumeParseVO;
import com.smartrecruit.aiengine.dto.response.ResumeImageParseVO;
import com.smartrecruit.aiengine.dto.response.ScreenResultVO;
import com.smartrecruit.aiengine.dto.response.PredictOfferVO;
import com.smartrecruit.aiengine.dto.response.MatchResultVO;
import com.smartrecruit.aiengine.dto.response.TalentRecommendVO;
import com.smartrecruit.aiengine.dto.request.TalentRecommendCandidate;
import com.smartrecruit.aiengine.dto.request.AnalyticsInsightRequest;
import com.smartrecruit.aiengine.dto.response.AiInsightVO;
import com.smartrecruit.aiengine.dto.response.RetentionPredictVO;

import java.util.List;
import java.util.Map;

/**
 * Agent 能力网关服务。
 *
 * <p>对外提供统一的智能体能力入口：LLM 可用时调用大模型，
 * 否则降级到各 Agent 的启发式实现。业务模块通过该网关接入 AI 能力。</p>
 *
 * @since 2026-04-06
 */
public interface AgentCapabilityService {

    /** 生成结构化 JD。 */
    JdVO generateJd(String jobTitle, String department, String experienceLevel);

    /** 生成面试题列表。 */
    List<InterviewQuestionVO> generateInterviewQuestions(
            Integer interviewType, int round, String jobTitle, String candidateName);

    /** 解析简历，返回结构化候选人画像。 */
    ResumeParseVO parseResume(String fileName, String contentText);

    /**
     * 解析图片/扫描件简历（AI 视觉模型，如 qwen-vl-max）。
     *
     * @param fileName   文件名
     * @param base64Images 图片列表（每项为 base64 Data URL；PDF 多页渲染时传多张）
     * @return 解析结果；识别不完整时 {@code reviewRequired=true}
     */
    ResumeImageParseVO parseResumeImage(String fileName, List<String> base64Images);

    /** 人岗匹配评分。 */
    ScreenResultVO screenCandidate(CandidateProfile candidate, JobRequirement requirement);

    /** Offer 接受概率预测。 */
    PredictOfferVO predictOffer(CandidateProfile candidate, OfferDetail offer);

    /** 新员工留存风险预测。 */
    RetentionPredictVO predictRetention(OnboardingCandidate candidate);

    /** 内推职位匹配推荐。 */
    MatchResultVO matchReferral(CandidateProfile candidate, List<JobMatchTarget> jobs);

    /** AI 人才推荐：为一批候选人匹配目标职位并排序。 */
    List<TalentRecommendVO> recommendTalent(JobRequirement job,
                                            List<TalentRecommendCandidate> candidates);

    /** AI 数据分析洞察：基于统计数据生成自然语言洞察与建议。 */
    List<AiInsightVO> generateAnalyticsInsights(AnalyticsInsightRequest request);
}
