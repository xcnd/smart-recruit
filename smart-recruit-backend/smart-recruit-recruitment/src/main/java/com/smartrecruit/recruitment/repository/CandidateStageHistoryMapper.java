package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.recruitment.entity.CandidateStageHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code rec_candidate_stage_history} 表的 Mapper 接口。
 *
 * @since 1.0.0
 */
@Mapper
public interface CandidateStageHistoryMapper extends BaseMapper<CandidateStageHistory> {
}
