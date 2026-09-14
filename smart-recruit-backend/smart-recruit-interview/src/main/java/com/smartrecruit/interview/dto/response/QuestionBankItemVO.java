package com.smartrecruit.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 面试题库-题目视图对象。
 *
 * @since 2026-04-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankItemVO {

    private Long id;

    /** 题型：0=单选,1=多选,2=问答。 */
    private Integer questionType;

    /** 题目内容。 */
    private String question;

    /** 选项列表（问答题为空）。 */
    private List<String> options;

    /** 答案。 */
    private String answer;

    /** 题目解读。 */
    private String explanation;

    /** 难度：1=简单,2=中等,3=困难。 */
    private Integer difficulty;

    /** 排序。 */
    private Integer sortOrder;
}
