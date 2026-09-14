package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 审计日志分页查询条件。
 *
 * @param page         页码（默认 1）
 * @param size         每页条数（默认 20，最大 100）
 * @param username     操作人用户名（模糊匹配）
 * @param module       操作模块（0=SYSTEM,1=JOB,2=CANDIDATE,3=INTERVIEW,4=OFFER,5=ONBOARD,6=TALENT,7=REFERRAL,8=AI）
 * @param action       操作动作（0=CREATE,1=UPDATE,2=DELETE,3=EXPORT,4=IMPORT,5=LOGIN）
 * @param targetType   目标资源类型（如 USER、ROLE、rec_job_position）
 * @param requestMethod HTTP 方法（GET/POST/PUT/DELETE）
 * @param result       执行结果：1=成功，0=失败
 * @param startDate    起始日期 yyyy-MM-dd（含当天 00:00:00）
 * @param endDate      结束日期 yyyy-MM-dd（含当天 23:59:59）
 * @param keyword      关键字（匹配用户名/请求路径/操作描述）
 * @since 2026-04-08
 */
public record AuditLogPageQuery(
        @Min(value = 1, message = "页码最小为1")
        Integer page,

        @Min(value = 1, message = "每页条数最小为1")
        @Max(value = 100, message = "每页条数最大为100")
        Integer size,

        String username,
        Integer module,
        Integer action,
        String targetType,
        String requestMethod,
        Integer result,
        String startDate,
        String endDate,
        String keyword
) {
    public AuditLogPageQuery {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
    }
}
