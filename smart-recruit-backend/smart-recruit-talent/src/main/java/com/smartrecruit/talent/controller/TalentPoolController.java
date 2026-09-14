package com.smartrecruit.talent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.talent.dto.response.TalentPoolVO;
import com.smartrecruit.talent.dto.response.RecommendationTaskVO;
import com.smartrecruit.talent.dto.request.AddToTalentPoolRequest;
import com.smartrecruit.talent.service.TalentPoolService;
import com.smartrecruit.talent.search.ElasticsearchTalentSearch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人才库管理 REST 控制器。
 *
 * <p>端点：列表、搜索、推荐、添加、联系、更新标签。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/talent-pool")
@RequiredArgsConstructor
@Slf4j
public class TalentPoolController {

    private final TalentPoolService talentPoolService;
    private final ElasticsearchTalentSearch elasticsearchTalentSearch;

    /**
     * 分页查询人才库候选人，支持池类型、技能等级、可用性与关键词筛选。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('talent:view')")
    public ApiResponse<PageResult<TalentPoolVO>> list(
            @RequestParam(required = false) String poolType,
            @RequestParam(required = false) String skillLevel,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> params = new HashMap<>();
        if (poolType != null) params.put("poolType", poolType);
        if (skillLevel != null) params.put("skillLevel", skillLevel);
        if (availability != null) params.put("availability", availability);
        if (keyword != null) params.put("keyword", keyword);

        Page<?> pageQuery = new Page<>(page, size);
        PageResult<TalentPoolVO> result = talentPoolService.pageQuery(pageQuery, params);
        return ApiResponse.success(result);
    }

    /**
     * 按关键词搜索人才库候选人。
     */
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('talent:view')")
    public ApiResponse<List<TalentPoolVO>> search(@RequestParam String keyword) {
        log.info("Talent pool search: keyword='{}'", keyword);
        List<TalentPoolVO> results = talentPoolService.search(keyword);
        return ApiResponse.success(results);
    }

    /**
     * 获取某职位的人才推荐列表。
     */
    @GetMapping("/recommendations")
    @PreAuthorize("hasAuthority('talent:view')")
    public ApiResponse<List<TalentPoolVO>> recommendations(@RequestParam Long jobId) {
        log.info("Talent recommendations for jobId={}", jobId);
        List<TalentPoolVO> results = talentPoolService.getRecommendations(jobId);
        return ApiResponse.success(results);
    }

    /**
     * 启动异步 AI 人才推荐（后台匹配，避免 LLM 超时阻塞），返回任务 ID。
     */
    @PostMapping("/recommendations/async")
    @PreAuthorize("hasAuthority('talent:view')")
    public ApiResponse<Map<String, String>> startAsyncRecommendation(
            @RequestBody Map<String, Long> body) {
        Long jobId = body != null ? body.get("jobId") : null;
        if (jobId == null) {
            return ApiResponse.error(400, "jobId 不能为空");
        }
        String taskId = talentPoolService.startAsyncRecommendation(jobId);
        return ApiResponse.success(Map.of("taskId", taskId));
    }

    /**
     * 查询异步推荐任务状态与结果（前端轮询，支持按批分页）。
     */
    @GetMapping("/recommendations/async/{taskId}")
    @PreAuthorize("hasAuthority('talent:view')")
    public ApiResponse<RecommendationTaskVO> asyncRecommendation(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.success(talentPoolService.getAsyncRecommendation(taskId, page, size));
    }

    /**
     * 手动触发 Elasticsearch 人才索引同步（维护/测试用）。
     */
    @PostMapping("/search/es-sync")
    public ApiResponse<Void> syncEsIndex() {
        elasticsearchTalentSearch.syncIndex();
        return ApiResponse.success();
    }

    @PutMapping("/{id}/tags")
    @PreAuthorize("hasAuthority('talent:edit')")
    public ApiResponse<Void> updateTags(@PathVariable Long id,
                                         @RequestBody List<String> tags) {
        log.info("Updating talent tags: id={}, tags={}", id, tags);
        talentPoolService.updateTags(id, tags);
        return ApiResponse.success();
    }

    /**
     * 将候选人加入人才库。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('talent:create')")
    public ApiResponse<TalentPoolVO> addToPool(@RequestBody AddToTalentPoolRequest request) {
        Long candidateId = request.getCandidateId();
        if (candidateId == null) {
            return ApiResponse.error(40001, "candidateId 不能为空");
        }

        List<String> tags = request.getTags();

        log.info("Adding candidate to talent pool: candidateId={}, tags={}", candidateId, tags);
        TalentPoolVO vo = talentPoolService.addToPool(candidateId, tags);
        return ApiResponse.success(vo);
    }

    /**
     * 标记人才库候选人为已联系。
     */
    @PostMapping("/{id}/contact")
    @PreAuthorize("hasAuthority('talent:edit')")
    public ApiResponse<Void> contactCandidate(@PathVariable Long id) {
        log.info("Contacting talent pool candidate: id={}", id);
        talentPoolService.contactCandidate(id);
        return ApiResponse.success();
    }
}
