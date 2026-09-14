package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.recruitment.entity.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code rec_application} 表的 Mapper 接口。
 *
 * @since 1.0.0
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {

    /**
     * 查询某候选人的所有申请。
     */
    List<Application> findByCandidateId(@Param("candidateId") Long candidateId);

    /**
     * 查询某职位的所有申请。
     */
    List<Application> findByJobId(@Param("jobId") Long jobId);

    /**
     * 更新申请的招聘阶段。
     */
    int updateStage(@Param("id") Long id, @Param("stage") String stage);
}
