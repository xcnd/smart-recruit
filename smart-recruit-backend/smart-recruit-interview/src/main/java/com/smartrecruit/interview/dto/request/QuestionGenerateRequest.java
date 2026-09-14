package com.smartrecruit.interview.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI 智能出题请求 DTO。
 *
 * @since 1.0.0
 */
@Data
public class QuestionGenerateRequest {

    /** 岗位类型：同 {@link com.smartrecruit.interview.enums.InterviewEnums.QuestionPositionType} 的 code。 */
    @NotNull(message = "岗位类型不能为空")
    private Integer positionType;

    /** 难度级别：easy=简单, medium=中等, hard=困难, mixed=混合（默认）。 */
    private String difficultyLevel = "mixed";

    /** 需要生成的题目分类列表（tech=技术基础, project=项目经验, behavioral=行为面试）。默认全部。 */
    private List<String> categories;

    /**
     * 每个分类的题目数量映射（分类编码 → 数量，1-50）。
     * 例如：{"tech": 3, "project": 2, "behavioral": 4}。
     * 未指定的分类默认使用 3。
     */
    private Map<String, Integer> categoryQuestionCounts;

    /**
     * 技术基础题中各题型的数量分配（题型编码 → 数量）。
     * 可选题型：single_choice=单选, multiple_choice=多选, true_false=判断, essay=问答。
     * 总和不超 categoryQuestionCounts.tech 的值。
     * 例如：{"single_choice": 2, "multiple_choice": 1, "true_false": 1, "essay": 1}。
     */
    private Map<String, Integer> techQuestionTypes;
}
