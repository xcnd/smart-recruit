package com.smartrecruit.offer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.offer.entity.OnboardingEquipment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入职设备 Mapper，提供 CRUD 操作。
 *
 * @since 1.0.0
 */
@Mapper
public interface OnboardingEquipmentMapper extends BaseMapper<OnboardingEquipment> {
}
