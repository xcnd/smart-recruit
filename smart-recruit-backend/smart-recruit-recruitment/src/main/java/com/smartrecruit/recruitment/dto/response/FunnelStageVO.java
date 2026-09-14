package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 招聘漏斗阶段统计视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunnelStageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 阶段名称 */
    private String name;

    /** 阶段码 */
    private Integer stage;

    /** 该阶段人数 */
    private Long count;
}
