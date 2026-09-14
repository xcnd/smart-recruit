package com.smartrecruit.interview.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建新面试的请求DTO。
 *
 * @since 1.0.0
 */
@Data
public class CreateInterviewRequest {

    /** 候选人 ID。 */
    @NotNull(message = "候选人ID不能为空")
    private Long candidateId;

    /** 候选人姓名（前端传入，存入冗余字段）。 */
    @JsonProperty("candidateName")
    private String candidateName;

    /** 职位 ID。 */
    @NotNull(message = "职位ID不能为空")
    private Long jobId;

    /** 职位名称（前端传入，存入冗余字段）。 */
    @JsonProperty("jobTitle")
    private String jobTitle;

    /** 面试类型：前端传字符串（"AI"/"TECH"/"BEHAVIOR"），通过 {@link #setType(String)} 转换为整数编码。 */
    @NotNull(message = "面试类型不能为空")
    private Integer interviewType;

    /** 计划面试时间。 */
    @NotNull(message = "面试安排时间不能为空")
    private LocalDateTime scheduledAt;

    /** 预计时长（分钟）。 */
    @NotNull(message = "预计面试时长不能为空")
    @Min(value = 10, message = "面试时长至少10分钟")
    @JsonProperty("duration")
    private Integer durationMin;

    /** 面试轮次，前端传入。未传入时由后端默认为 1。 */
    @Min(value = 1, message = "面试轮次最小值为1")
    @JsonProperty("round")
    private Integer round;

    /** 面试官 ID，前端传入。未传入时由后端使用默认面试官。 */
    @JsonProperty("interviewerId")
    private Long interviewerId;

    /** 是否为终面：0=否, 1=是。终面完成后该候选人的面试流程即告结束。 */
    @JsonProperty("isFinalRound")
    private Integer isFinalRound;

    /**
     * 接受前端传来的字符串类型（如 "AI", "TECH", "BEHAVIOR"），转换为对应整数编码。
     */
    public void setType(String type) {
        if (type == null) {
            this.interviewType = null;
            return;
        }
        this.interviewType = switch (type.toUpperCase()) {
            case "PHONE" -> 0;
            case "VIDEO" -> 1;
            case "ONSITE" -> 2;
            case "AI" -> 3;
            case "TECH", "TECHNICAL" -> 4;
            case "HR", "BEHAVIOR" -> 5;
            case "LEADERSHIP", "EXECUTIVE" -> 6;
            default -> throw new IllegalArgumentException("不支持的面试类型: " + type);
        };
    }
}
