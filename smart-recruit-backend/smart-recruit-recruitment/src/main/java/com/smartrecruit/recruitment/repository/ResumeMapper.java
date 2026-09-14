package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.recruitment.entity.Resume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code rec_resume} 表的 Mapper 接口。
 *
 * @since 1.0.0
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

    /**
     * 查询某候选人拥有的所有简历。
     */
    List<Resume> findByCandidateId(@Param("candidateId") Long candidateId);
}
