package com.smartrecruit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.system.dto.request.AuditLogPageQuery;
import com.smartrecruit.system.dto.response.AuditLogDetailVO;
import com.smartrecruit.system.dto.response.AuditLogStatsVO;
import com.smartrecruit.system.dto.response.AuditLogVO;
import com.smartrecruit.system.entity.SysOperationLog;
import com.smartrecruit.system.repository.SysOperationLogMapper;
import com.smartrecruit.system.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志查询服务实现。
 *
 * <p>基于 {@code sys_operation_log} 表提供多条件分页查询、
 * 详情查询与统计查询。日志写入由 {@code AuditLogAspect} 自动完成。</p>
 *
 * @since 2026-04-08
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final SysOperationLogMapper operationLogMapper;

    /** 模块编码 → 名称。 */
    private static final Map<Integer, String> MODULE_LABELS = Map.of(
            0, "系统管理",
            1, "职位管理",
            2, "候选人管理",
            3, "面试管理",
            4, "Offer管理",
            5, "入职管理",
            6, "人才库",
            7, "内推管理",
            8, "AI引擎");

    /** 动作编码 → 名称。 */
    private static final Map<Integer, String> ACTION_LABELS = Map.of(
            0, "创建",
            1, "更新",
            2, "删除",
            3, "导出",
            4, "导入",
            5, "登录");

    /** 分页查询审计日志。 */
    @Override
    public PageResult<AuditLogVO> pageQuery(AuditLogPageQuery query) {
        LambdaQueryWrapper<SysOperationLog> wrapper = buildWrapper(query);
        wrapper.orderByDesc(SysOperationLog::getCreateTime)
                .orderByDesc(SysOperationLog::getId);

        IPage<SysOperationLog> page = operationLogMapper.selectPage(
                new Page<>(query.page(), query.size()), wrapper);

        List<AuditLogVO> records = page.getRecords().stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getSize(),
                page.getCurrent(), page.getPages());
    }

    /** 查询审计日志详情。 */
    @Override
    public AuditLogDetailVO getDetail(Long id) {
        SysOperationLog log = operationLogMapper.selectById(id);
        if (log == null) {
            throw new ResourceNotFoundException("审计日志不存在: id=" + id);
        }
        return new AuditLogDetailVO(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getModule(),
                moduleLabel(log.getModule()),
                log.getAction(),
                actionLabel(log.getAction()),
                log.getTargetType(),
                log.getTargetId(),
                log.getDescription(),
                log.getRequestMethod(),
                log.getRequestUri(),
                log.getRequestParams(),
                log.getResponseStatus(),
                log.getClientIp(),
                log.getUserAgent(),
                log.getDurationMs(),
                log.getErrorMsg(),
                log.getTraceId(),
                formatTime(log.getCreateTime()));
    }

    /** 查询审计日志统计。 */
    @Override
    public AuditLogStatsVO getStats() {
        long total = operationLogMapper.selectCount(null);

        LocalDateTime todayStart = DateUtils.beginOfDay();
        long todayCount = operationLogMapper.selectCount(
                new LambdaQueryWrapper<SysOperationLog>()
                        .ge(SysOperationLog::getCreateTime, todayStart));

        long successCount = operationLogMapper.selectCount(
                new LambdaQueryWrapper<SysOperationLog>()
                        .isNull(SysOperationLog::getErrorMsg)
                        .and(w -> w.isNull(SysOperationLog::getResponseStatus)
                                .or().lt(SysOperationLog::getResponseStatus, 400)));
        long failCount = total - successCount;
        double successRate = total > 0 ? Math.round(successCount * 10000.0 / total) / 100.0 : 0.0;
        double avgDurationMs = Math.round(operationLogMapper.avgDurationMs() * 10.0) / 10.0;

        return new AuditLogStatsVO(total, todayCount, successCount, failCount,
                successRate, avgDurationMs);
    }

    // ================================================================
    // 私有辅助方法
    // ================================================================

    /** 构建多条件筛选 Wrapper。 */
    private LambdaQueryWrapper<SysOperationLog> buildWrapper(AuditLogPageQuery query) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.username())) {
            wrapper.like(SysOperationLog::getUsername, query.username().trim());
        }
        if (query.module() != null) {
            wrapper.eq(SysOperationLog::getModule, query.module());
        }
        if (query.action() != null) {
            wrapper.eq(SysOperationLog::getAction, query.action());
        }
        if (StringUtils.hasText(query.targetType())) {
            wrapper.like(SysOperationLog::getTargetType, query.targetType().trim());
        }
        if (StringUtils.hasText(query.requestMethod())) {
            wrapper.eq(SysOperationLog::getRequestMethod,
                    query.requestMethod().trim().toUpperCase());
        }
        if (query.result() != null) {
            if (query.result() == 1) {
                wrapper.isNull(SysOperationLog::getErrorMsg)
                        .and(w -> w.isNull(SysOperationLog::getResponseStatus)
                                .or().lt(SysOperationLog::getResponseStatus, 400));
            } else {
                wrapper.and(w -> w.isNotNull(SysOperationLog::getErrorMsg)
                        .or().ge(SysOperationLog::getResponseStatus, 400));
            }
        }
        if (StringUtils.hasText(query.startDate())) {
            LocalDateTime start = DateUtils.parseDate(query.startDate().trim())
                    .atStartOfDay();
            wrapper.ge(SysOperationLog::getCreateTime, start);
        }
        if (StringUtils.hasText(query.endDate())) {
            LocalDateTime end = DateUtils.parseDate(query.endDate().trim())
                    .atTime(23, 59, 59);
            wrapper.le(SysOperationLog::getCreateTime, end);
        }
        if (StringUtils.hasText(query.keyword())) {
            String keyword = query.keyword().trim();
            wrapper.and(w -> w.like(SysOperationLog::getUsername, keyword)
                    .or().like(SysOperationLog::getRequestUri, keyword)
                    .or().like(SysOperationLog::getDescription, keyword));
        }
        return wrapper;
    }

    private AuditLogVO toVO(SysOperationLog log) {
        return new AuditLogVO(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getModule(),
                moduleLabel(log.getModule()),
                log.getAction(),
                actionLabel(log.getAction()),
                log.getTargetType(),
                log.getTargetId(),
                log.getDescription(),
                log.getRequestMethod(),
                log.getRequestUri(),
                log.getResponseStatus(),
                log.getClientIp(),
                log.getDurationMs(),
                log.getErrorMsg(),
                formatTime(log.getCreateTime()));
    }

    private static String moduleLabel(Integer module) {
        return module != null ? MODULE_LABELS.getOrDefault(module, "其他") : "其他";
    }

    private static String actionLabel(Integer action) {
        return action != null ? ACTION_LABELS.getOrDefault(action, "其他") : "其他";
    }

    private static String formatTime(LocalDateTime time) {
        return time != null ? DateUtils.format(time) : null;
    }
}
