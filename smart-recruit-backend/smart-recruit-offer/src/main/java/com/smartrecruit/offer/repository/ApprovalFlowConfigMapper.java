package com.smartrecruit.offer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.offer.entity.ApprovalFlowConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批流程配置 Mapper。
 *
 * @since 1.0.0
 */
@Mapper
public interface ApprovalFlowConfigMapper extends BaseMapper<ApprovalFlowConfig> {
}
