package com.smartrecruit.recruitment.converter;

import com.smartrecruit.recruitment.dto.request.CreateCandidateRequest;
import com.smartrecruit.recruitment.dto.request.UpdateCandidateRequest;
import com.smartrecruit.recruitment.dto.response.CandidateDetailVO;
import com.smartrecruit.recruitment.dto.response.CandidateVO;
import com.smartrecruit.recruitment.dto.response.StageHistoryVO;
import com.smartrecruit.recruitment.entity.Candidate;
import com.smartrecruit.recruitment.entity.CandidateStageHistory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Candidate实体与相关DTO之间的MapStruct转换器。
 *
 * @since 1.0.0
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateConverter {

    CandidateVO toVO(Candidate entity);

    List<CandidateVO> toVOList(List<Candidate> entities);

    CandidateDetailVO toDetailVO(Candidate entity);

    Candidate toEntity(CreateCandidateRequest request);

    void updateEntity(@MappingTarget Candidate entity, UpdateCandidateRequest request);

    StageHistoryVO toStageHistoryVO(CandidateStageHistory entity);

    List<StageHistoryVO> toStageHistoryVOList(List<CandidateStageHistory> entities);
}
