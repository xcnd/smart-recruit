package com.smartrecruit.interview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.interview.entity.QuestionBankItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试题库-题目 Mapper。
 *
 * @since 2026-04-10
 */
@Mapper
public interface QuestionBankItemMapper extends BaseMapper<QuestionBankItem> {
}
