package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 用于评估的面试会话数据。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession {

    /**
     * 会话唯一标识。
     */
    private Long sessionId;

    /**
     * 候选人 ID。
     */
    private Long candidateId;

    /**
     * 职位 ID。
     */
    private Long jobId;

    /**
     * 面试类型：TECHNICAL、BEHAVIORAL、CODING 等。
     */
    private String interviewType;

    /**
     * 面试轮次，从 1 开始。
     */
    private Integer round;

    /**
     * 问答对列表，包含面试官提问与候选人回答。
     */
    private List<Map<String, Object>> qaPairs;

    /**
     * 面试完整文字记录。
     */
    private String transcript;

    /**
     * 面试持续时长（分钟）。
     */
    private Integer durationMinutes;
}
