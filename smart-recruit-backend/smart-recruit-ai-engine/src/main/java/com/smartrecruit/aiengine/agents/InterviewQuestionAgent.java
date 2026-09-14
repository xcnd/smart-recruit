package com.smartrecruit.aiengine.agents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试题生成智能体。
 *
 * <p>根据面试类型与轮次生成个性化面试题（技术 / 行为 / 综合）。
 * LLM 不可用时作为降级实现，也可作为 Agent 能力网关的兜底。</p>
 *
 * @since 2026-04-06
 */
@Component
@Slf4j
public class InterviewQuestionAgent {

    /** 技术面试题模板。 */
    private static final List<String> TECHNICAL_QUESTIONS = List.of(
            "请介绍你过去项目中使用的技术栈及核心架构设计",
            "简述你对高并发/分布式系统的理解和实际落地经验",
            "你如何保证代码质量和系统稳定性？",
            "描述你常用的设计模式及其应用场景",
            "遇到线上故障时，你的排查思路是什么？");

    /** HR/行为面试题模板。 */
    private static final List<String> HR_QUESTIONS = List.of(
            "请分享一个你带领团队完成挑战性目标的案例",
            "描述一次你与同事发生分歧的经历，你是如何处理的？",
            "你如何设定个人成长目标并确保达成？",
            "面对紧迫的截止日期，你如何排定优先级？",
            "谈谈你对公司价值观的理解以及你的契合点");

    /** 综合/常规面试题模板。 */
    private static final List<String> GENERAL_QUESTIONS = List.of(
            "请做一个 2 分钟自我介绍，突出你的核心优势",
            "为什么选择应聘这个岗位？",
            "你过去 1-2 年最有成就感的一件事是什么？",
            "如果录用你，你计划如何快速融入团队并产生价值？",
            "你对未来 3-5 年的职业规划是什么？");

    /**
     * 生成面试题列表。
     *
     * @param interviewType 面试类型：0=PHONE,1=VIDEO,2=ONSITE,3=AI,4=TECHNICAL,5=HR,6=EXECUTIVE
     * @param round         面试轮次（1 起）
     * @param jobTitle      职位名称
     * @return 面试题列表（Map 含 question/type/scoreStandard）
     */
    public List<Map<String, Object>> generate(Integer interviewType, int round, String jobTitle) {
        log.info("InterviewQuestionAgent 生成面试题: type={}, round={}, job={}",
                interviewType, round, jobTitle);

        List<String> pool = switch (interviewType == null ? 1 : interviewType) {
            case 4 -> TECHNICAL_QUESTIONS;
            case 5 -> HR_QUESTIONS;
            case 3 -> GENERAL_QUESTIONS;
            default -> TECHNICAL_QUESTIONS;
        };

        List<Map<String, Object>> questions = new ArrayList<>();
        for (String text : pool) {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("question", text);
            q.put("type", interviewType);
            q.put("round", round);
            q.put("jobTitle", jobTitle);
            q.put("scoreStandard", "0-100 分，关注逻辑性、深度与实践经验");
            questions.add(q);
        }
        return questions;
    }
}
