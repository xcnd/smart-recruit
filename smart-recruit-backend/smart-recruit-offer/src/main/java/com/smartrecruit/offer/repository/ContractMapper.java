package com.smartrecruit.offer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.offer.entity.Contract;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code ofr_contract} 表 Mapper。
 *
 * @since 2026-04-09
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    /**
     * 查询指定年份合同编号的最大序号。
     *
     * <p>合同编号格式 {@code CT-2026-0249}，取最后一个 {@code -} 后的数字部分
     * 求最大值，用于生成递增且不重复的编号；无记录时返回 0。</p>
     *
     * @param year 年份，如 2026
     * @return 当年最大序号，无记录时为 0
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(contract_no, '-', -1) AS UNSIGNED)), 0) "
            + "FROM ofr_contract "
            + "WHERE contract_no LIKE CONCAT('CT-', #{year}, '-%') AND deleted = 0")
    Long selectMaxContractSeq(@Param("year") int year);
}
