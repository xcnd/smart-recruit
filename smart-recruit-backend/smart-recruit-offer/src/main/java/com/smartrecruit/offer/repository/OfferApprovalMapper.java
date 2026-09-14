package com.smartrecruit.offer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.offer.entity.OfferApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Offer 审批 Mapper，提供 CRUD 与按 Offer 查询审批列表。
 *
 * @since 1.0.0
 */
@Mapper
public interface OfferApprovalMapper extends BaseMapper<OfferApproval> {

    /**
     * 查询指定 Offer 的所有审批记录。
     */
    List<OfferApproval> selectByOfferId(@Param("offerId") Long offerId);

    /**
     * 查询指定 Offer 的最新一条审批记录。
     */
    OfferApproval selectLatestByOfferId(@Param("offerId") Long offerId);
}
