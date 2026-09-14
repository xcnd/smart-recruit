package com.smartrecruit.offer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.offer.entity.OnboardingDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入职文档 Mapper，提供 CRUD 操作。
 *
 * @since 1.0.0
 */
@Mapper
public interface OnboardingDocumentMapper extends BaseMapper<OnboardingDocument> {
}
