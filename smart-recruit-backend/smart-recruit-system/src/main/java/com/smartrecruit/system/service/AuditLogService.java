package com.smartrecruit.system.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.system.dto.request.AuditLogPageQuery;
import com.smartrecruit.system.dto.response.AuditLogDetailVO;
import com.smartrecruit.system.dto.response.AuditLogStatsVO;
import com.smartrecruit.system.dto.response.AuditLogVO;

/**
 * 审计日志查询服务。
 *
 * @since 2026-04-08
 */
public interface AuditLogService {

    /**
     * 分页查询审计日志（支持多条件组合筛选）。
     */
    PageResult<AuditLogVO> pageQuery(AuditLogPageQuery query);

    /**
     * 查询审计日志详情。
     */
    AuditLogDetailVO getDetail(Long id);

    /**
     * 查询审计日志统计（总量/今日/成功/失败/成功率/平均耗时）。
     */
    AuditLogStatsVO getStats();
}
