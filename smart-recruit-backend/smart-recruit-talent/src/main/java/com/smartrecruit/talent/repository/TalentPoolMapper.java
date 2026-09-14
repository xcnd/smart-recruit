package com.smartrecruit.talent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.talent.entity.TalentPool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 人才库 Mapper，提供 CRUD、搜索与过滤查询。
 *
 * @since 1.0.0
 */
@Mapper
public interface TalentPoolMapper extends BaseMapper<TalentPool> {

    /**
     * 分页查询，支持可选过滤条件。
     */
    IPage<TalentPool> selectPageWithFilters(Page<TalentPool> page, @Param("params") Map<String, Object> params);

    /**
     * 按关键词搜索人才库，范围包括技能、标签与期望职位。通过 JSON 列对 tags 字段进行搜索。
     */
    List<TalentPool> searchByKeyword(@Param("keyword") String keyword);
}
