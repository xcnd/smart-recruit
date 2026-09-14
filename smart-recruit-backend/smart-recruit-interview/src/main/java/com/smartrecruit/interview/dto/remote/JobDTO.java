package com.smartrecruit.interview.dto.remote;

import lombok.Data;

/**
 * 招聘服务职位 DTO - 对应 smart-recruit-recruitment 的 JobDetailVO 响应。
 *
 * @since 1.0.0
 */
@Data
public class JobDTO {
    private Long id;
    private String title;
}
