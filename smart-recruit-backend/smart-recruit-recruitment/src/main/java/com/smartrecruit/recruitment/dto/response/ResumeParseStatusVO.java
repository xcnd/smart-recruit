package com.smartrecruit.recruitment.dto.response;

import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.io.Serializable;

/**
 * 简历解析状态查询的视图对象。
 *
 * @since 1.0.0
 */
@Data
public class ResumeParseStatusVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 简历 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long resumeId;
    /** 解析状态：0=PENDING, 1=PARSING, 2=SUCCESS, 3=FAILED。 */
    private Integer parseStatus;
    /** 解析失败原因（仅在 FAILED 状态时有值）。 */
    private String parseError;
}
