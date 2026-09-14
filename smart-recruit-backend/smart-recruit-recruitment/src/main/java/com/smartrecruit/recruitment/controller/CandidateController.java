package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.request.*;
import com.smartrecruit.recruitment.dto.response.AiScreeningResultVO;
import com.smartrecruit.recruitment.dto.response.CandidateDetailVO;
import com.smartrecruit.recruitment.dto.response.CandidateStatsVO;
import com.smartrecruit.recruitment.dto.response.CandidateVO;
import com.smartrecruit.recruitment.dto.response.StageHistoryVO;
import com.smartrecruit.recruitment.enums.RecruitmentEnums;
import com.smartrecruit.recruitment.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 候选人管理的REST控制器。
 *
 * <p>提供候选人查询、CRUD、阶段流转和AI筛选接口。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
@Slf4j
public class CandidateController {

    private final CandidateService candidateService;

    /**
     * 分页查询候选人列表，支持多条件筛选。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('candidate:view')")
    public ApiResponse<PageResult<CandidateVO>> list(@Valid CandidatePageQuery query) {
        return ApiResponse.success(candidateService.pageQuery(query));
    }

    /**
     * 查询候选人整体统计（总数、各阶段人数等）。
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('candidate:view')")
    public ApiResponse<CandidateStatsVO> stats() {
        return ApiResponse.success(candidateService.getStats());
    }

    /**
     * 查询候选人详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('candidate:view')")
    public ApiResponse<CandidateDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(candidateService.getById(id));
    }

    /**
     * 查询候选人阶段流转历史。
     */
    @GetMapping("/{id}/stage-history")
    @PreAuthorize("hasAuthority('candidate:view')")
    public ApiResponse<List<StageHistoryVO>> stageHistory(@PathVariable Long id) {
        return ApiResponse.success(candidateService.getStageHistory(id));
    }

    /**
     * 创建候选人。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('candidate:create')")
    public ApiResponse<CandidateVO> create(@Valid @RequestBody CreateCandidateRequest request) {
        return ApiResponse.success(candidateService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('candidate:edit')")
    public ApiResponse<CandidateVO> update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateCandidateRequest request) {
        return ApiResponse.success(candidateService.update(id, request));
    }

    /**
     * 删除候选人。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('candidate:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        candidateService.delete(id);
        return ApiResponse.success();
    }

    /** 字符串阶段名 -> 整数阶段码的映射（兼容前端传字符串）。 */
    private static final Map<String, Integer> STAGE_NAME_MAP = Map.ofEntries(
        Map.entry("NEW", RecruitmentEnums.CandidateStatus.NEW.getCode()),
        Map.entry("SCREENING", RecruitmentEnums.CandidateStatus.SCREENING.getCode()),
        Map.entry("SCREEN_PASSED", RecruitmentEnums.CandidateStatus.SCREEN_PASSED.getCode()),
        Map.entry("INTERVIEW", RecruitmentEnums.CandidateStatus.INTERVIEWING.getCode()),
        Map.entry("OFFER", RecruitmentEnums.CandidateStatus.OFFERED.getCode()),
        Map.entry("HIRED", RecruitmentEnums.CandidateStatus.HIRED.getCode()),
        Map.entry("REJECTED", RecruitmentEnums.CandidateStatus.REJECTED.getCode()),
        Map.entry("WITHDRAWN", RecruitmentEnums.CandidateStatus.WITHDRAWN.getCode())
    );

    @PutMapping("/{id}/stage")
    @PreAuthorize("hasAuthority('candidate:edit')")
    public ApiResponse<Void> updateStage(@PathVariable Long id,
                                         @RequestBody UpdateCandidateStageRequest request) {
        String rawStage = request.getStage();
        Integer stageCode;
        if (rawStage != null && STAGE_NAME_MAP.containsKey(rawStage)) {
            stageCode = STAGE_NAME_MAP.get(rawStage);
        } else {
            stageCode = Integer.parseInt(rawStage);
        }
        candidateService.updateStage(id, stageCode);
        return ApiResponse.success();
    }

    /**
     * 批量流转候选人阶段。
     */
    @PostMapping("/batch-transition")
    @PreAuthorize("hasAuthority('candidate:edit')")
    public ApiResponse<Void> batchTransition(@Valid @RequestBody BatchTransitionRequest request) {
        candidateService.batchTransition(request);
        return ApiResponse.success();
    }

    /**
     * 对候选人批量执行 AI 智能筛选。
     */
    @PostMapping("/ai-screen")
    @PreAuthorize("hasAuthority('candidate:view')")
    public ApiResponse<List<AiScreeningResultVO>> aiScreen(@Valid @RequestBody AiScreenRequest request) {
        return ApiResponse.success(candidateService.aiScreen(request));
    }
}
