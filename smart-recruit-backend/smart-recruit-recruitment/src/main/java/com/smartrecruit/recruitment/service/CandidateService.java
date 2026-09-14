package com.smartrecruit.recruitment.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.request.AiScreenRequest;
import com.smartrecruit.recruitment.dto.request.BatchTransitionRequest;
import com.smartrecruit.recruitment.dto.request.CandidatePageQuery;
import com.smartrecruit.recruitment.dto.request.CreateCandidateRequest;
import com.smartrecruit.recruitment.dto.request.UpdateCandidateRequest;
import com.smartrecruit.recruitment.dto.response.AiScreeningResultVO;
import com.smartrecruit.recruitment.dto.response.CandidateDetailVO;
import com.smartrecruit.recruitment.dto.response.CandidateStatsVO;
import com.smartrecruit.recruitment.dto.response.CandidateVO;
import com.smartrecruit.recruitment.dto.response.StageHistoryVO;

import java.util.List;

/**
 * 候选人管理的服务接口。
 *
 * @since 1.0.0
 */
public interface CandidateService {

    /**
     * 带筛选条件的候选人分页查询。
     */
    PageResult<CandidateVO> pageQuery(CandidatePageQuery query);

    /**
     * 根据ID获取单个候选人。
     */
    CandidateDetailVO getById(Long id);

    /**
     * 创建一个新的候选人档案。
     */
    CandidateVO create(CreateCandidateRequest request);

    /**
     * 更新候选人信息。
     */
    CandidateVO update(Long id, UpdateCandidateRequest request);

    /**
     * 软删除候选人。
     */
    void delete(Long id);

    /**
     * 更新候选人的招聘流程阶段。
     *
     * @param id    候选人ID
     * @param stage 阶段编码
     */
    void updateStage(Long id, Integer stage);

    /**
     * 批量将候选人转移到新的阶段。
     */
    void batchTransition(BatchTransitionRequest request);

    /**
     * 获取候选人的阶段变更历史。
     */
    List<StageHistoryVO> getStageHistory(Long candidateId);

    /**
     * 获取候选人统计看板数据。
     */
    CandidateStatsVO getStats();

    /**
     * 对给定的简历进行模拟AI筛选。
     */
    List<AiScreeningResultVO> aiScreen(AiScreenRequest request);
}
