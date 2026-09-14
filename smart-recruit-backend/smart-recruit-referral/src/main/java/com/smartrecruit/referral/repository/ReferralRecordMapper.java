package com.smartrecruit.referral.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.referral.dto.request.ReferralPageQuery;
import com.smartrecruit.referral.dto.response.LeaderboardVO;
import com.smartrecruit.referral.dto.response.ReferralRecordVO;
import com.smartrecruit.referral.entity.ReferralRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code ref_record} 表的Mapper。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Mapper
public interface ReferralRecordMapper extends BaseMapper<ReferralRecord> {

    /**
     * 查询指定推荐人的所有内推记录。
     */
    List<ReferralRecord> findByReferrerId(@Param("referrerId") Long referrerId);

    /**
     * 查询指定推荐人的内推记录（含候选人、职位、部门名称）。
     */
    List<ReferralRecordVO> findByReferrerIdWithDetails(@Param("referrerId") Long referrerId);

    /**
     * 统计符合查询条件的记录总数。
     */
    long countRecords(@Param("query") ReferralPageQuery query);

    /**
     * 分页查询内推记录（含候选人姓名、职位、部门名称）。
     */
    List<ReferralRecordVO> queryRecords(@Param("query") ReferralPageQuery query,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit);

    /**
     * 按内推数量和总奖金金额排名的顶级推荐人。
     */
    List<LeaderboardVO> selectLeaderboard(@Param("limit") int limit);

    /**
     * 检查候选人是否已投递某个计划职位。
     * @return 匹配的记录数（0 表示未投递）
     */
    int countByCandidateIdAndProgramJobId(@Param("candidateId") Long candidateId,
                                          @Param("programJobId") Long programJobId);

    /**
     * 检查是否已存在相同 (推荐人, 候选人, 职位) 的记录，匹配表唯一索引 {@code uk_referrer_candidate_job}。
     * @return 匹配的记录数（0 表示未投递）
     */
    int countByReferrerCandidateJob(@Param("referrerId") Long referrerId,
                                    @Param("candidateId") Long candidateId,
                                    @Param("jobPositionId") Long jobPositionId);

    /**
     * 查询指定候选人的投递记录（含候选人、职位、部门名称）。
     */
    List<ReferralRecordVO> findByCandidateIdWithDetails(@Param("candidateId") Long candidateId);
}
