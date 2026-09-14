package com.smartrecruit.offer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.offer.entity.Onboarding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 入职 Mapper，提供 CRUD 与分页筛选查询。
 *
 * @since 1.0.0
 */
@Mapper
public interface OnboardingMapper extends BaseMapper<Onboarding> {

    /**
     * 分页查询，支持可选过滤条件：candidateId、status。
     */
    IPage<Onboarding> selectPageWithFilters(Page<Onboarding> page, @Param("params") Map<String, Object> params);

    /**
     * 按状态分组计数。
     */
    List<Map<String, Object>> countByStatus();

    /**
     * 按风险等级分组计数。
     */
    List<Map<String, Object>> countByRiskLevel();
}
