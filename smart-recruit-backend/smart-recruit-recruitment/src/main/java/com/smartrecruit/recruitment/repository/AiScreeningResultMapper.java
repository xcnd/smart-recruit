package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.recruitment.entity.AiScreeningResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 筛选结果 Mapper。
 *
 * @since 1.1.0
 */
@Mapper
public interface AiScreeningResultMapper extends BaseMapper<AiScreeningResult> {

    /**
     * 查询指定简历的最新筛选结果。
     * <p>使用 LambdaQueryWrapper 以确保 autoResultMap 生效，
     * JacksonTypeHandler 能正确反序列化 JSON 列。</p>
     */
    default AiScreeningResult selectLatestByResumeId(Long resumeId) {
        return selectOne(new LambdaQueryWrapper<AiScreeningResult>()
                .eq(AiScreeningResult::getResumeId, resumeId)
                .orderByDesc(AiScreeningResult::getCreatedAt)
                .last("LIMIT 1"));
    }
}
