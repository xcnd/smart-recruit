package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.recruitment.entity.CareersJobPosition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * {@code careers_job_position} 表的 Mapper 接口。
 *
 * @since 1.0.0
 */
@Mapper
public interface CareersJobPositionMapper extends BaseMapper<CareersJobPosition> {

    /**
     * 分页查询，支持按类型、关键词、分类筛选。
     */
    IPage<CareersJobPosition> pageQueryByType(Page<CareersJobPosition> page,
                                               @Param("recType") String recType,
                                               @Param("keyword") String keyword,
                                               @Param("category") String category);
}
