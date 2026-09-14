package com.smartrecruit.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.interview.dto.request.AssessmentRequest;
import com.smartrecruit.interview.dto.response.AssessmentResponse;
import com.smartrecruit.interview.dto.response.AssessmentResponse.KeyMomentItem;
import com.smartrecruit.interview.entity.Interview;
import com.smartrecruit.interview.entity.InterviewAssessment;
import com.smartrecruit.interview.enums.InterviewEnums.AssessmentSuggestion;
import com.smartrecruit.interview.repository.InterviewAssessmentMapper;
import com.smartrecruit.interview.repository.InterviewMapper;
import com.smartrecruit.interview.service.AiEvaluationService;
import com.smartrecruit.interview.service.AssessmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 评估报告服务实现。
 *
 * <p>对 {@code rec_interview_assessment} 表进行 CRUD 操作，
 * 包含五维评分、关键时刻、综合评价和录用建议。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class AssessmentServiceImpl implements AssessmentService {

    private final InterviewAssessmentMapper assessmentMapper;
    private final InterviewMapper interviewMapper;
    private final AiEvaluationService aiEvaluationService;

    public AssessmentServiceImpl(InterviewAssessmentMapper assessmentMapper,
                                 InterviewMapper interviewMapper,
                                 AiEvaluationService aiEvaluationService) {
        this.assessmentMapper = assessmentMapper;
        this.interviewMapper = interviewMapper;
        this.aiEvaluationService = aiEvaluationService;
    }

    /** 根据面试 ID 查询评估记录。 */
    @Override
    public AssessmentResponse getByInterviewId(Long interviewId) {
        LambdaQueryWrapper<InterviewAssessment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewAssessment::getInterviewId, interviewId)
               .orderByDesc(InterviewAssessment::getCreateTime)
               .last("LIMIT 1");
        InterviewAssessment entity = assessmentMapper.selectOne(wrapper);
        if (entity != null) {
            return toResponse(entity);
        }

        // 无评估记录时，调用 AI 评估服务生成并入库
        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            return AssessmentResponse.builder()
                    .interviewId(interviewId).candidateName("未知").jobTitle("")
                    .overallComment("面试记录不存在").aiGenerated(false).build();
        }
        AssessmentRequest assessReq = aiEvaluationService.evaluate(interview);
        // 入库后下次查询直接命中数据库记录
        AssessmentResponse saved = save(interviewId, assessReq);
        saved.setCandidateName(interview.getCandidateName());
        saved.setJobTitle(interview.getJobTitle());
        return saved;
    }

    /** 保存记录。 */
    @Override
    @Transactional
    public AssessmentResponse save(Long interviewId, AssessmentRequest request) {
        // 检查是否已存在评估记录
        LambdaQueryWrapper<InterviewAssessment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewAssessment::getInterviewId, interviewId)
               .last("LIMIT 1");
        InterviewAssessment existing = assessmentMapper.selectOne(wrapper);

        InterviewAssessment entity = existing != null ? existing : new InterviewAssessment();
        entity.setInterviewId(interviewId);
        entity.setCandidateId(request.getCandidateId());
        entity.setTechnologyDepth(request.getTechnologyDepth());
        entity.setCommunication(request.getCommunication());
        entity.setProblemSolving(request.getProblemSolving());
        entity.setLearningAbility(request.getLearningAbility());
        entity.setTeamwork(request.getTeamwork());
        entity.setOverallScore(request.getOverallScore());
        entity.setOverallComment(request.getOverallComment());
        entity.setStrengths(request.getStrengths());
        entity.setWeaknesses(request.getWeaknesses());
        entity.setKeyMoments(request.getKeyMoments());
        entity.setSuggestion(request.getSuggestion());
        entity.setAiGenerated(1);
        entity.setAssessorId(0L); // 0 = AI/系统生成

        if (existing != null) {
            assessmentMapper.updateById(entity);
            log.info("更新 AI 评估: interviewId={}, id={}", interviewId, entity.getId());
        } else {
            assessmentMapper.insert(entity);
            log.info("创建 AI 评估: interviewId={}, id={}", interviewId, entity.getId());
        }

        return toResponse(entity);
    }

    @SuppressWarnings("unchecked")
    private AssessmentResponse toResponse(InterviewAssessment entity) {
        AssessmentSuggestion suggestion = AssessmentSuggestion.fromCode(entity.getSuggestion());
        return AssessmentResponse.builder()
                .id(entity.getId())
                .interviewId(entity.getInterviewId())
                .candidateId(entity.getCandidateId())
                .technologyDepth(entity.getTechnologyDepth())
                .communication(entity.getCommunication())
                .problemSolving(entity.getProblemSolving())
                .learningAbility(entity.getLearningAbility())
                .teamwork(entity.getTeamwork())
                .overallScore(entity.getOverallScore())
                .overallComment(entity.getOverallComment())
                .strengths(castToStringList(entity.getStrengths()))
                .weaknesses(castToStringList(entity.getWeaknesses()))
                .keyMoments(castToKeyMoments(entity.getKeyMoments()))
                .suggestion(entity.getSuggestion())
                .suggestionLabel(suggestion != null ? suggestion.getLabel() : null)
                .aiGenerated(entity.getAiGenerated() != null && entity.getAiGenerated() == 1)
                .assessorId(entity.getAssessorId())
                .createTime(entity.getCreateTime())
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> castToStringList(Object obj) {
        if (obj instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                result.add(item != null ? item.toString() : "");
            }
            return result;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<KeyMomentItem> castToKeyMoments(Object obj) {
        if (obj instanceof List<?> list) {
            List<KeyMomentItem> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Object timeVal = m.get("time");
                    Object textVal = m.get("text");
                    result.add(KeyMomentItem.builder()
                            .time(timeVal != null ? timeVal.toString() : "")
                            .text(textVal != null ? textVal.toString() : "")
                            .build());
                } else if (item instanceof KeyMomentItem kmi) {
                    result.add(kmi);
                }
            }
            return result;
        }
        return List.of();
    }
}
