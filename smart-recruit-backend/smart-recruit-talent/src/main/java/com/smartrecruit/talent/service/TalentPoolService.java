package com.smartrecruit.talent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.talent.dto.response.TalentPoolVO;
import com.smartrecruit.talent.dto.response.RecommendationTaskVO;

import java.util.List;
import java.util.Map;

/**
 * 人才库管理服务接口。
 *
 * @since 1.0.0
 */
public interface TalentPoolService {

    /**
     * 支持可选过滤参数的分页查询。
     */
    PageResult<TalentPoolVO> pageQuery(Page<?> page, Map<String, Object> params);

    /**
     * 按关键字跨技能、标签和职位搜索人才库。
     */
    List<TalentPoolVO> search(String keyword);

    /**
     * 基于指定岗位要求实时匹配人才库候选人。
     *
     * @param jobId 招聘岗位 ID（必填）
     * @return 按匹配度降序排列的候选人列表
     */
    List<TalentPoolVO> getRecommendations(Long jobId);

    /**
     * 启动异步 AI 人才推荐（后台匹配，避免 LLM 超时阻塞），返回任务 ID。
     */
    String startAsyncRecommendation(Long jobId);

    /**
     * 查询异步推荐任务状态与结果（分页，默认每批 8 人）。
     *
     * @param taskId 任务 ID
     * @param page   页码，从 1 开始
     * @param size   每批人数
     */
    RecommendationTaskVO getAsyncRecommendation(String taskId, int page, int size);

    /**
     * 更新人才库条目的标签。
     */
    void updateTags(Long id, List<String> tags);

    /**
     * 将候选人加入人才库。
     */
    TalentPoolVO addToPool(Long candidateId, List<String> tags);

    /**
     * 联系人才库中的候选人（更新联系时间和状态）。
     */
    void contactCandidate(Long id);
}
