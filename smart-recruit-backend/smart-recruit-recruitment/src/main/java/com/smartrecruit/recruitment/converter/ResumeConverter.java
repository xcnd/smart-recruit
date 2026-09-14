package com.smartrecruit.recruitment.converter;

import com.smartrecruit.recruitment.dto.response.ResumeDetailVO;
import com.smartrecruit.recruitment.dto.response.ResumeVO;
import com.smartrecruit.recruitment.entity.Resume;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Resume实体与相关DTO之间的MapStruct转换器。
 *
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ResumeConverter {

    ResumeVO toVO(Resume entity);

    List<ResumeVO> toVOList(List<Resume> entities);

    ResumeDetailVO toDetailVO(Resume entity);
}
