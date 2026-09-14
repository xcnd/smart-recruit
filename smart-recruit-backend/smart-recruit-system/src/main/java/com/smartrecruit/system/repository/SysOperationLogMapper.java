package com.smartrecruit.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.system.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 操作日志 Mapper。
 *
 * @since 2026-04-26
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 统计全部日志的平均耗时（毫秒）。
     */
    @Select("SELECT COALESCE(AVG(duration_ms), 0) FROM sys_operation_log")
    double avgDurationMs();
}
