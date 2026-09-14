package com.smartrecruit.aiengine.agents;

import com.smartrecruit.aiengine.domain.InterviewReport;
import com.smartrecruit.aiengine.domain.InterviewSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 面试评估智能体——模拟AI驱动的面试评估。
 *
 * <p>分析面试会话并生成涵盖沟通、技术和行为维度的结构化评估报告。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Component
@Slf4j
public class InterviewEvaluatorAgent {

    private static final String[] COMMUNICATION_NOTES = {
            "Articulate and clear in expressing ideas",
            "Good at breaking down complex concepts",
            "Could improve on structured communication",
            "Excellent listener, responds thoughtfully",
            "Demonstrates strong presentation skills"
    };

    private static final String[] TECHNICAL_NOTES = {
            "Strong foundational knowledge demonstrated",
            "Good problem decomposition ability",
            "Excellent system design thinking",
            "Solid coding and algorithm skills",
            "Needs more depth in distributed systems"
    };

    private static final String[] BEHAVIORAL_NOTES = {
            "Demonstrates ownership and accountability",
            "Strong team collaboration mindset",
            "Good conflict resolution examples",
            "Shows leadership potential",
            "Growth mindset and eager to learn"
    };

    /**
     * 评估面试会话并生成报告。
     *
     * @param session 面试会话数据
     * @return 结构化的面试评估报告
     */
    public InterviewReport evaluate(InterviewSession session) {
        log.info("InterviewEvaluatorAgent: evaluating session={}, candidate={}",
                session.getSessionId(), session.getCandidateId());
        simulateLatency(150, 600);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("communication", Math.round(rnd.nextDouble(60.0, 95.0) * 10.0) / 10.0);
        dimensionScores.put("technical", Math.round(rnd.nextDouble(55.0, 95.0) * 10.0) / 10.0);
        dimensionScores.put("behavioral", Math.round(rnd.nextDouble(60.0, 95.0) * 10.0) / 10.0);
        dimensionScores.put("problemSolving", Math.round(rnd.nextDouble(50.0, 95.0) * 10.0) / 10.0);
        dimensionScores.put("cultureFit", Math.round(rnd.nextDouble(55.0, 95.0) * 10.0) / 10.0);

        double overallScore = dimensionScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(70.0);
        overallScore = Math.round(overallScore * 10.0) / 10.0;

        String hireRecommendation;
        if (overallScore >= 85.0) {
            hireRecommendation = "STRONG_HIRE";
        } else if (overallScore >= 70.0) {
            hireRecommendation = "HIRE";
        } else if (overallScore >= 60.0) {
            hireRecommendation = "HOLD";
        } else {
            hireRecommendation = "NO_HIRE";
        }

        InterviewReport report = InterviewReport.builder()
                .sessionId(session.getSessionId())
                .overallScore(overallScore)
                .dimensionScores(dimensionScores)
                .communicationAssessment(COMMUNICATION_NOTES[rnd.nextInt(COMMUNICATION_NOTES.length)])
                .technicalAssessment(TECHNICAL_NOTES[rnd.nextInt(TECHNICAL_NOTES.length)])
                .behavioralAssessment(BEHAVIORAL_NOTES[rnd.nextInt(BEHAVIORAL_NOTES.length)])
                .strengths("Strong technical foundation, good communication, team player")
                .weaknesses("Could benefit from more experience in large-scale system design")
                .hireRecommendation(hireRecommendation)
                .confidenceLevel(Math.round(rnd.nextDouble(75.0, 98.0) * 10.0) / 10.0)
                .build();

        log.info("InterviewEvaluatorAgent: evaluation complete, overallScore={}, recommendation={}",
                overallScore, hireRecommendation);
        return report;
    }

    private void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(minMs, maxMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
