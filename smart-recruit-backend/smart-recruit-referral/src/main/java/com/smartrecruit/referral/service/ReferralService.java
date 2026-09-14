package com.smartrecruit.referral.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.referral.dto.request.BatchSaveProgramJobRequest;
import com.smartrecruit.referral.dto.request.BatchSaveSettingsRequest;
import com.smartrecruit.referral.dto.request.CreateProgramRequest;
import com.smartrecruit.referral.dto.request.CreateReferralRequest;
import com.smartrecruit.referral.dto.request.PublicReferralRequest;
import com.smartrecruit.referral.dto.request.ReferralPageQuery;
import com.smartrecruit.referral.dto.request.ShareTokenRequest;
import com.smartrecruit.referral.dto.request.UpdateProgramRequest;
import com.smartrecruit.referral.dto.response.BonusRecordVO;
import com.smartrecruit.referral.dto.response.LeaderboardVO;
import com.smartrecruit.referral.dto.response.ProgramJobDetailVO;
import com.smartrecruit.referral.dto.response.RefProgramJobVO;
import com.smartrecruit.referral.dto.response.ReferralProgramVO;
import com.smartrecruit.referral.dto.response.ReferralRecordVO;
import com.smartrecruit.referral.dto.response.ShareTokenResponse;
import com.smartrecruit.referral.dto.response.ReferralPolicyVO;
import com.smartrecruit.referral.dto.response.PosterVO;
import com.smartrecruit.referral.dto.response.PublicProgramVO;
import com.smartrecruit.referral.dto.response.PublicProgramDetailVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

/**
 * 员工内推服务接口。
 *
 * @author xdh
 * @since 2026-04-26
 */
public interface ReferralService {

    /**
     * 列出所有已启用的内推计划。
     */
    List<ReferralProgramVO> getPrograms();

    /**
     * 内推计划分页查询。
     */
    PageResult<ReferralProgramVO> getProgramPage(ReferralPageQuery query);

    /**
     * 切换内推计划的启用状态。
     */
    void toggleProgram(Long programId);

    /**
     * 更新内推计划的奖金金额。
     */
    void updateBonus(Long programId, BigDecimal bonusAmount);

    /**
     * 创建内推计划。
     */
    ReferralProgramVO createProgram(CreateProgramRequest request);

    /**
     * 更新内推计划。
     */
    ReferralProgramVO updateProgram(Long programId, UpdateProgramRequest request);

    /**
     * 删除内推计划（逻辑删除）。
     */
    void deleteProgram(Long programId);

    /**
     * 批量保存或更新内推计划设置。
     */
    void batchSaveSettings(List<BatchSaveSettingsRequest> requests);

    /**
     * 内推记录分页查询。
     */
    PageResult<ReferralRecordVO> getRecords(ReferralPageQuery query);

    /**
     * 提交一条新的内推记录。
     */
    ReferralRecordVO createRecord(CreateReferralRequest request);

    /**
     * 获取指定推荐人的所有内推记录。
     */
    List<ReferralRecordVO> getMyReferrals(Long referrerId);

    /**
     * 获取按内推数量和奖金排名的前10名排行榜。
     */
    List<LeaderboardVO> getLeaderboard();

    /**
     * 获取内推政策数据。
     */
    ReferralPolicyVO getPolicy();

    /**
     * 为特定计划生成可分享的海报图片。
     */
    PosterVO generatePoster(Long programId);

    // ================================================================
    // 计划-职位关联
    // ================================================================

    /**
     * 获取计划下的所有关联职位。
     */
    List<RefProgramJobVO> getProgramJobs(Long programId);

    /**
     * 批量保存计划-职位关联。
     */
    void batchSaveProgramJobs(Long programId, List<BatchSaveProgramJobRequest> requests);

    /**
     * 获取职位详情（含计划上下文 + 完整职位数据）。
     */
    ProgramJobDetailVO getProgramJobDetail(Long programJobId);

    /**
     * 删除计划下的某个职位关联。
     */
    void deleteProgramJob(Long jobId);

    /**
     * 切换计划职位的启用状态。
     */
    void toggleProgramJob(Long jobId);

    /**
     * 更新职位级别的内推奖金。
     */
    void updateProgramJobBonus(Long jobId, BigDecimal bonusAmount);

    // ================================================================
    // 奖金发放记录
    // ================================================================

    /**
     * 查询指定内推记录的奖金发放明细。
     */
    List<BonusRecordVO> getBonusRecords(Long recordId);

    /**
     * 确认发放单条奖金记录，同步更新内推记录的已发放金额。
     */
    void payBonusStage(Long bonusRecordId);

    // ================================================================
    // 公开落地页
    // ================================================================

    /**
     * 获取所有已启用内推计划及其启用职位的公开列表（无需认证）。
     */
    List<PublicProgramVO> getPublicPrograms();

    /**
     * 通过令牌获取公开落地页数据：计划详情 + 启用职位列表 + 分享人信息。
     */
    PublicProgramDetailVO getPublicProgramByToken(String token);

    /**
     * 上传候选人简历并返回 RustFS 文件路径。
     */
    String uploadResume(MultipartFile file);

    /**
     * 通过公开页面提交匿名内推。
     */
    ReferralRecordVO createPublicRecord(PublicReferralRequest request);

    /**
     * 生成分享令牌和链接。
     */
    ShareTokenResponse generateShareToken(ShareTokenRequest request);

    /**
     * 检查当前登录候选人是否已投递过某个计划职位。
     */
    boolean hasApplied(Long candidateId, Long programJobId);

    /**
     * 获取当前登录用户的投递记录列表。
     */
    List<ReferralRecordVO> getMyApplications();
}
