package com.smartrecruit.referral.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.referral.entity.RefProgramJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * {@code ref_program_job} 表的Mapper。
 *
 * @author xdh
 * @since 2026-05-04
 */
@Mapper
public interface RefProgramJobMapper extends BaseMapper<RefProgramJob> {

    /** 跨库查询职位详情（job + program + jobPosition）。 */
    Map<String, Object> selectJobDetailById(@Param("programJobId") Long programJobId);
}
