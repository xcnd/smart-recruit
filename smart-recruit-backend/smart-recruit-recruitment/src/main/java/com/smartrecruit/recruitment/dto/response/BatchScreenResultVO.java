package com.smartrecruit.recruitment.dto.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 批量 AI 筛选提交结果的视图对象。
 *
 * @since 1.0.0
 */
@Data
public class BatchScreenResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 异步任务 ID，用于进度轮询。 */
    private String taskId;
    /** 提交筛选的简历数量。 */
    private Integer totalCount;
}
