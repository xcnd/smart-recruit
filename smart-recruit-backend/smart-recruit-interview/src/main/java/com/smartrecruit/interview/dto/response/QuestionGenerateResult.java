package com.smartrecruit.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 智能出题结果 VO，按分类分组返回题目列表。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGenerateResult {

    /** 岗位类型编码。 */
    private Integer positionType;

    /** 岗位类型标签。 */
    private String positionLabel;

    /** 技术基础题。 */
    private List<QuestionItem> techQuestions;

    /** 项目经验题。 */
    private List<QuestionItem> projectQuestions;

    /** 行为面试题。 */
    private List<QuestionItem> behavioralQuestions;

    /** 是否包含 AI 增强生成的补充题。 */
    private boolean aiEnhanced;

    /**
     * 单道面试题。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionItem {
        /** 题目编号。 */
        private Integer number;
        /** 题目内容。 */
        private String question;
        /** 题型：single_choice=单选, multiple_choice=多选, true_false=判断, essay=问答。 */
        private String questionType;
        /** 选择题的选项列表（单选/多选时有值）。 */
        private List<String> options;
        /** 难度：简单、中等、困难。 */
        private String difficulty;
        /** 难度编码：0=简单,1=中等,2=困难。 */
        private Integer difficultyCode;
        /** 参考答案要点。 */
        private String referenceAnswer;
    }
}
