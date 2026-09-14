package com.smartrecruit.referral.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.referral.dto.response.BonusRecordVO;
import com.smartrecruit.referral.entity.RefBonusRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code ref_bonus_record} 表的Mapper。
 *
 * @author xdh
 * @since 2026-05-04
 */
@Mapper
public interface RefBonusRecordMapper extends BaseMapper<RefBonusRecord> {

    /**
     * 查询指定内推记录的所有奖金发放记录。
     */
    List<BonusRecordVO> selectByRecordId(@Param("refRecordId") Long refRecordId);

    /**
     * 查询指定推荐人的所有奖金发放记录。
     */
    List<BonusRecordVO> selectByReferrerId(@Param("referrerId") Long referrerId);
}
