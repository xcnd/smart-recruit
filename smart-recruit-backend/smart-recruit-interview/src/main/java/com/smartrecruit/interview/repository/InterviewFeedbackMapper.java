package com.smartrecruit.interview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.interview.entity.InterviewFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 面试反馈 Mapper，提供 CRUD 与按面试查询反馈列表。
 *
 * @since 1.0.0
 */
@Mapper
public interface InterviewFeedbackMapper extends BaseMapper<InterviewFeedback> {

    /**
     * 查询指定面试的所有反馈记录。
     */
    List<InterviewFeedback> selectByInterviewId(@Param("interviewId") Long interviewId);
}
