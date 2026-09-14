package com.smartrecruit.referral.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.referral.entity.RefShareToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 分享令牌 Mapper。
 *
 * @author xdh
 * @since 2026-04-01
 */
@Mapper
public interface RefShareTokenMapper extends BaseMapper<RefShareToken> {

    /**
     * 根据令牌字符串查询分享令牌记录。
     */
    RefShareToken selectByToken(@Param("token") String token);

    /**
     * 根据内推码查询分享令牌记录。
     */
    RefShareToken selectByReferralCode(@Param("referralCode") String referralCode);
}
