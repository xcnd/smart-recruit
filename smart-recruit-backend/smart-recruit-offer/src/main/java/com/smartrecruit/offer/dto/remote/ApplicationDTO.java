package com.smartrecruit.offer.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 求职申请信息 DTO（来自招聘服务的远程调用结果）。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDTO {

    /** 申请 ID。 */
    private Long id;

    /** 候选人 ID。 */
    private Long candidateId;

    /** 职位 ID。 */
    private Long jobId;

    /** 当前阶段码。 */
    private Integer stage;

    /** 投递时间。 */
    private LocalDateTime applyAt;
}
