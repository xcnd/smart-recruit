package com.smartrecruit.offer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.offer.entity.Offer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * Offer Mapper，提供 CRUD 与分页筛选查询。
 *
 * @since 1.0.0
 */
@Mapper
public interface OfferMapper extends BaseMapper<Offer> {

    /**
     * 分页查询，支持可选过滤条件：candidateId、status、departmentName。
     */
    IPage<Offer> selectPageWithFilters(Page<Offer> page, @Param("params") Map<String, Object> params);
}
