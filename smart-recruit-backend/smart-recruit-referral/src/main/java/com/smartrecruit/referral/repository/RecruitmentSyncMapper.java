package com.smartrecruit.referral.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 跨库同步 Mapper：将内推投递的候选人 + 简历写入招聘模块的
 * {@code rec_candidate} 和 {@code rec_resume} 表。
 *
 * <p>两个模块连接同一 MySQL，通过 schema-prefixed SQL
 * （{@code smart_recruit_recruitment.rec_*}}）实现跨库写入。</p>
 *
 * @author xdh
 * @since 2026-04-01
 */
@Mapper
public interface RecruitmentSyncMapper {

    /** 插入候选人记录。 */
    int insertCandidate(@Param("id") Long id,
                        @Param("name") String name,
                        @Param("email") String email,
                        @Param("phone") String phone,
                        @Param("source") Integer source,
                        @Param("referrerId") Long referrerId,
                        @Param("createdAt") LocalDateTime createdAt);

    /** 插入简历记录（parseStatus=0 PENDING，由 ResumeParseScheduler 自动解析）。 */
    int insertResume(@Param("id") Long id,
                     @Param("candidateId") Long candidateId,
                     @Param("jobPositionId") Long jobPositionId,
                     @Param("fileName") String fileName,
                     @Param("fileUrl") String fileUrl,
                     @Param("fileType") Integer fileType,
                     @Param("fileSize") Long fileSize,
                     @Param("parseStatus") Integer parseStatus,
                     @Param("createdAt") LocalDateTime createdAt);

    /** 根据邮箱查找已有候选人 ID（去重）。 */
    Long findCandidateIdByEmail(@Param("email") String email);

    /** 通过用户 ID 关联 sys_user 的 mobile 查找对应 rec_candidate 记录。 */
    Long findCandidateIdByUserId(@Param("userId") Long userId);

    /** 更新已有候选人姓名和手机号。 */
    int updateCandidate(@Param("id") Long id,
                        @Param("name") String name,
                        @Param("phone") String phone);
}
