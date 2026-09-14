package com.smartrecruit.referral.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.referral.dto.request.BatchSaveProgramJobRequest;
import com.smartrecruit.referral.dto.request.BatchSaveSettingsRequest;
import com.smartrecruit.referral.dto.request.CreateProgramRequest;
import com.smartrecruit.referral.dto.request.CreateReferralRequest;
import com.smartrecruit.referral.dto.request.ReferralPageQuery;
import com.smartrecruit.referral.dto.request.ShareTokenRequest;
import com.smartrecruit.referral.dto.request.UpdateProgramRequest;
import com.smartrecruit.referral.dto.request.UpdateBonusRequest;
import com.smartrecruit.referral.dto.request.GeneratePosterRequest;
import com.smartrecruit.referral.dto.response.BonusRecordVO;
import com.smartrecruit.referral.dto.response.ReferralMatchVO;
import com.smartrecruit.referral.dto.response.LeaderboardVO;
import com.smartrecruit.referral.dto.response.ProgramJobDetailVO;
import com.smartrecruit.referral.dto.response.RefProgramJobVO;
import com.smartrecruit.referral.dto.response.ReferralProgramVO;
import com.smartrecruit.referral.dto.response.ReferralRecordVO;
import com.smartrecruit.referral.dto.response.ReferralPolicyVO;
import com.smartrecruit.referral.dto.response.PosterVO;
import com.smartrecruit.referral.service.ReferralService;
import com.smartrecruit.referral.service.ReferralMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 内推管理控制器。
 *
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
@Slf4j
public class ReferralController {

    private final ReferralService referralService;
    private final ReferralMatchService referralMatchService;


    /**
     * 列出所有已启用的内推计划。
     */
    @GetMapping("/programs")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ReferralProgramVO>> listPrograms() {
        return ApiResponse.success(referralService.getPrograms());
    }

    /**
     * 内推计划分页查询。
     */
    @GetMapping("/programs/page")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ReferralProgramVO>> listProgramsPage(@Valid ReferralPageQuery query) {
        return ApiResponse.success(referralService.getProgramPage(query));
    }

    /**
     * 创建内推计划。
     */
    @PostMapping("/programs")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReferralProgramVO> createProgram(
            @Valid @RequestBody CreateProgramRequest request) {
        return ApiResponse.success(referralService.createProgram(request));
    }

    /**
     * 全量更新内推计划。
     */
    @PutMapping("/programs/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReferralProgramVO> updateProgram(
            @PathVariable Long id,
            @RequestBody UpdateProgramRequest request) {
        return ApiResponse.success(referralService.updateProgram(id, request));
    }

    /**
     * 删除内推计划（逻辑删除）。
     */
    @DeleteMapping("/programs/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteProgram(@PathVariable Long id) {
        referralService.deleteProgram(id);
        return ApiResponse.success();
    }

    /**
     * 切换内推计划的启用状态。
     */
    @PutMapping("/programs/{id}/toggle")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> toggleProgram(@PathVariable Long id) {
        referralService.toggleProgram(id);
        return ApiResponse.success();
    }

    /**
     * 更新内推计划的奖金金额。
     */
    @PutMapping("/programs/{id}/bonus")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> updateBonus(@PathVariable Long id,
                                          @RequestBody UpdateBonusRequest request) {
        BigDecimal bonusAmount = request.getBonusAmount();
        referralService.updateBonus(id, bonusAmount);
        return ApiResponse.success();
    }

    /**
     * 批量保存或更新内推计划设置。
     */
    @PostMapping("/programs/batch-save")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> batchSaveSettings(
            @Valid @RequestBody List<BatchSaveSettingsRequest> requests) {
        referralService.batchSaveSettings(requests);
        return ApiResponse.success();
    }


    /**
     * 获取计划下所有关联职位。
     */
    @GetMapping("/programs/{programId}/jobs")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<RefProgramJobVO>> listProgramJobs(
            @PathVariable Long programId) {
        return ApiResponse.success(referralService.getProgramJobs(programId));
    }

    /**
     * 获取职位详情（含计划上下文和完整职位数据）。
     */
    @GetMapping("/positions/{programJobId}/detail")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProgramJobDetailVO> getJobDetail(@PathVariable Long programJobId) {
        return ApiResponse.success(referralService.getProgramJobDetail(programJobId));
    }

    /**
     * 批量保存计划-职位关联。
     */
    @PostMapping("/programs/{programId}/jobs/batch")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> batchSaveProgramJobs(
            @PathVariable Long programId,
            @Valid @RequestBody List<BatchSaveProgramJobRequest> requests) {
        referralService.batchSaveProgramJobs(programId, requests);
        return ApiResponse.success();
    }

    /**
     * 删除计划下的职位关联。
     */
    @DeleteMapping("/programs/jobs/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteProgramJob(@PathVariable Long id) {
        referralService.deleteProgramJob(id);
        return ApiResponse.success();
    }

    /**
     * 切换职位内推启用状态。
     */
    @PutMapping("/programs/jobs/{id}/toggle")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> toggleProgramJob(@PathVariable Long id) {
        log.info("PUT /api/v1/referrals/programs/jobs/{}/toggle", id);
        referralService.toggleProgramJob(id);
        return ApiResponse.success();
    }

    /**
     * 更新职位级别内推奖金。
     */
    @PutMapping("/programs/jobs/{id}/bonus")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> updateProgramJobBonus(
            @PathVariable Long id,
            @RequestBody UpdateBonusRequest request) {
        BigDecimal bonusAmount = request.getBonusAmount();
        referralService.updateProgramJobBonus(id, bonusAmount);
        return ApiResponse.success();
    }

    /**
     * 内推记录分页查询。
     */
    @GetMapping("/records")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ReferralRecordVO>> listRecords(
            @Valid ReferralPageQuery query) {
        return ApiResponse.success(referralService.getRecords(query));
    }

    /**
     * 提交一条新的内推记录。
     */
    @PostMapping("/records")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReferralRecordVO> createRecord(
            @Valid @RequestBody CreateReferralRequest request) {
        return ApiResponse.success(referralService.createRecord(request));
    }

    /**
     * 获取内推排行榜（前10名）。
     */
    @GetMapping("/leaderboard")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<LeaderboardVO>> getLeaderboard() {
        return ApiResponse.success(referralService.getLeaderboard());
    }

    /**
     * 获取当前推荐人的内推记录。
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ReferralRecordVO>> getMyReferrals(
            @RequestParam Long referrerId) {
        return ApiResponse.success(referralService.getMyReferrals(referrerId));
    }

    /**
     * 获取当前登录用户的投递记录（以候选人身份）。
     */
    @GetMapping("/my-applications")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ReferralRecordVO>> getMyApplications() {
        return ApiResponse.success(referralService.getMyApplications());
    }

    /**
     * 获取内推政策数据。
     */
    @GetMapping("/policy")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReferralPolicyVO> getPolicy() {
        return ApiResponse.success(referralService.getPolicy());
    }

    /**
     * 生成分享令牌和链接（需要登录，用于追溯推荐人）。
     */
    @PostMapping("/share-token")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> generateShareToken(@Valid @RequestBody ShareTokenRequest request) {
        return ApiResponse.success(referralService.generateShareToken(request));
    }

    /**
     * 为特定计划生成可分享的海报。
     */
    @PostMapping("/poster/generate")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PosterVO> generatePoster(
            @RequestBody GeneratePosterRequest request) {
        Long programId = request.getProgramId();
        return ApiResponse.success(referralService.generatePoster(programId));
    }


    /**
     * 获取指定内推记录的分阶段奖金发放明细。
     */
    @GetMapping("/records/{recordId}/bonus")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<BonusRecordVO>> getBonusRecords(@PathVariable Long recordId) {
        return ApiResponse.success(referralService.getBonusRecords(recordId));
    }

    /**
     * 确认发放单条奖金记录（HR/管理员操作）。
     */
    @PutMapping("/bonus-records/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> payBonusStage(@PathVariable Long id) {
        referralService.payBonusStage(id);
        return ApiResponse.success();
    }

    /**
     * 查询内推记录的智能匹配推荐（AI Agent 能力网关结果）。
     */
    @GetMapping("/records/{recordId}/matches")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ReferralMatchVO>> referralMatches(@PathVariable Long recordId) {
        return ApiResponse.success(referralMatchService.listMatches(recordId));
    }

    /**
     * 重新触发内推智能匹配（AI 引擎优先，失败降级本地启发式）。
     */
    @PostMapping("/records/{recordId}/matches/refresh")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ReferralMatchVO>> refreshReferralMatches(@PathVariable Long recordId) {
        return ApiResponse.success(referralMatchService.refreshMatches(recordId));
    }
}
