package com.smartrecruit.interview.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 候选人提交测评答案的请求体。
 *
 * @since 1.0.0
 */
@Data
public class SubmitAnswersRequest {

    @JsonProperty("token")
    private String token;

    @JsonProperty("answers")
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {

        @JsonProperty("questionId")
        private Integer questionId;

        @JsonProperty("selectedAnswer")
        private String selectedAnswer;
    }
}
