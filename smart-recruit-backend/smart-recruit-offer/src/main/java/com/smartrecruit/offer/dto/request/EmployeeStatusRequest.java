package com.smartrecruit.offer.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 更新员工状态请求。
 *
 * @param status 员工状态：0=待入职,1=试用期,2=正式,3=已离职
 * @since 2026-04-09
 */
public record EmployeeStatusRequest(@NotNull(message = "员工状态不能为空") Integer status) {
}
