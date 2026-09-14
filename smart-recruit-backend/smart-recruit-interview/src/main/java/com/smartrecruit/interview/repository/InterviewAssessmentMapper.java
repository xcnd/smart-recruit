package com.smartrecruit.interview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.interview.entity.InterviewAssessment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试评估 Mapper，提供 CRUD 操作。
 *
 * @since 1.0.0
 */
@Mapper
public interface InterviewAssessmentMapper extends BaseMapper<InterviewAssessment> {
}
