package com.smartrecruit.interview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.interview.entity.OnlineAssessment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 在线测评 Mapper，提供 CRUD 与分页查询。
 *
 * @since 1.0.0
 */
@Mapper
public interface OnlineAssessmentMapper extends BaseMapper<OnlineAssessment> {

    /**
     * 分页查询在线测评，支持按候选人和状态筛选。
     */
    IPage<OnlineAssessment> selectPageWithFilters(Page<OnlineAssessment> page,
                                                   @Param("params") Map<String, Object> params);
}
