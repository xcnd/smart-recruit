package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

/**
 * 求职申请响应 DTO。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDTO {

    /** 申请 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 候选人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateId;

    /** 职位 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobId;

    /** 当前阶段码。 */
    private Integer stage;

    /** 投递时间。 */
    private LocalDateTime applyAt;
}
