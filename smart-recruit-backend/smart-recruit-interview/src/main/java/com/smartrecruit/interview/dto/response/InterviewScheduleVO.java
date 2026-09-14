package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试日程视图 VO，用于 AI 智能面试页面的日程列表。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewScheduleVO {

    /** 面试 ID。 */
    private Long id;

    /** 时间段（如 "09:00 - 09:45"）。 */
    private String timeSlot;

    /** 开始时间（HH:mm）。 */
    private String startTime;

    /** 结束时间（HH:mm）。 */
    private String endTime;

    /** 候选人姓名。 */
    private String candidateName;

    /** 应聘职位。 */
    private String jobTitle;

    /** 面试类型标签。 */
    @JsonProperty("typeLabel")
    private String typeLabel;

    /** 面试官姓名。 */
    @JsonProperty("interviewerName")
    private String interviewerName;

    /** 面试状态：pending/ongoing/done。 */
    private String statusTag;

    /** 状态编码：0=待开始,1=进行中,2=已完成。 */
    private Integer statusCode;

    /** 面试日期（yyyy-MM-dd），用于周视图分组。 */
    @JsonProperty("scheduledDate")
    private String scheduledDate;
}
