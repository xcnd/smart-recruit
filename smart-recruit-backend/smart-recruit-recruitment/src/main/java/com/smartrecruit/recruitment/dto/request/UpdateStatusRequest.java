package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 更新启用/发布状态请求。
 *
 * @since 2026-04-07
 */
@Data
public class UpdateStatusRequest {

    /** 目标状态编码（由各业务枚举定义）。 */
    private Integer status;
}
