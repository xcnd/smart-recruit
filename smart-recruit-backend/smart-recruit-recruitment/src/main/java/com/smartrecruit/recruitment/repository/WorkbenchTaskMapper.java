package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.recruitment.entity.WorkbenchTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code rec_workbench_task} 表的 Mapper 接口。
 *
 * @since 1.0.0
 */
@Mapper
public interface WorkbenchTaskMapper extends BaseMapper<WorkbenchTask> {
}
