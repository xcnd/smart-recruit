package com.smartrecruit.referral.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.referral.dto.request.ReferralPageQuery;
import com.smartrecruit.referral.dto.response.ReferralRecordVO;
import com.smartrecruit.referral.dto.response.LeaderboardVO;
import com.smartrecruit.referral.entity.ReferralProgram;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code ref_program} 表的Mapper。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Mapper
public interface ReferralProgramMapper extends BaseMapper<ReferralProgram> {

    /**
     * 列出所有已启用的内推计划。
     *
     * @return 已启用计划的列表
     */
    List<ReferralProgram> selectEnabledPrograms();

    /**
     * 带可选关键词筛选的分页查询。
     */
    IPage<ReferralProgram> selectPageWithQuery(Page<ReferralProgram> page,
                                               @Param("query") ReferralPageQuery query);
}
