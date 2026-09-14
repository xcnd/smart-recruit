package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 测评题目的单个项目，包含题干、选项和答案信息。
 *
 * <p>correctAnswer 仅用于后端评分，前端不应展示给候选人。</p>
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssessmentQuestionItem {

    @JsonProperty("questionId")
    private Integer questionId;

    @JsonProperty("type")
    private Integer type;

    @JsonProperty("questionText")
    private String questionText;

    /** 难度：简单、中等、困难 */
    @JsonProperty("difficulty")
    private String difficulty;

    /** single_choice | multiple_choice | true_false | essay | likert */
    @JsonProperty("questionType")
    private String questionType;

    /** [{key: "A", value: "..."}, ...] 或 [{key: "1", value: "非常同意"}, ...] */
    @JsonProperty("options")
    private List<Map<String, String>> options;

    /** 仅用于自动评分（type 0 和 type 2），不展示给候选人 */
    @JsonProperty("correctAnswer")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String correctAnswer;

    @JsonProperty("score")
    private Integer score;
}
