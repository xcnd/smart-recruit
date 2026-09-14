package com.smartrecruit.recruitment.dto.response;

import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 候选人阶段变更历史的视图对象。
 *
 * @since 1.0.0
 */
@Data
public class StageHistoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 变更前阶段。 */
    private Integer fromStage;

    /** 变更后阶段。 */
    private Integer toStage;

    /** 操作人员姓名。 */
    private String operatorName;

    /** 操作人员 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;

    /** 变更备注。 */
    private String remark;

    /** 变更时间。 */
    private LocalDateTime createTime;
}
