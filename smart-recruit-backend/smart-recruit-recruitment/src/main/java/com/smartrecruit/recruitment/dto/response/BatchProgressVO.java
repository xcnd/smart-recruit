package com.smartrecruit.recruitment.dto.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量 AI 筛选进度查询的视图对象。
 *
 * @since 1.0.0
 */
@Data
public class BatchProgressVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 异步任务 ID。 */
    private String taskId;
    /** 简历总数。 */
    private Integer total;
    /** 已完成数量。 */
    private Integer completed;
    /** 失败数量。 */
    private Integer failed;
    /** 任务状态：PROCESSING / COMPLETED / FAILED / NOT_FOUND。 */
    private String status;
    /** 失败详情列表（每条包含简历ID、文件名和失败原因）。 */
    private List<String> failedDetails = new ArrayList<>();
}
