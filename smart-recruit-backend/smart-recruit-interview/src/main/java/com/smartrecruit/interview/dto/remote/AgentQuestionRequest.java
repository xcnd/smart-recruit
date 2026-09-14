package com.smartrecruit.interview.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调用 AI Agent 生成面试题请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentQuestionRequest {

    /** 面试类型编码。 */
    private Integer interviewType;

    /** 面试轮次。 */
    private Integer round;

    /** 职位名称。 */
    private String jobTitle;

    /** 候选人姓名。 */
    private String candidateName;
}
