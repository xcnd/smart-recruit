package com.smartrecruit.referral.service.impl;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.DuplicateResourceException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.referral.feign.SystemFileClient;
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
import com.smartrecruit.referral.entity.RefBonusRecord;
import com.smartrecruit.referral.entity.RefProgramJob;
import com.smartrecruit.referral.entity.RefShareToken;
import com.smartrecruit.referral.entity.ReferralProgram;
import com.smartrecruit.referral.entity.ReferralRecord;
import com.smartrecruit.referral.repository.RecruitmentSyncMapper;
import com.smartrecruit.referral.repository.RefBonusRecordMapper;
import com.smartrecruit.referral.repository.RefProgramJobMapper;
import com.smartrecruit.referral.repository.RefShareTokenMapper;
import com.smartrecruit.referral.repository.ReferralProgramMapper;
import com.smartrecruit.referral.repository.ReferralRecordMapper;
import com.smartrecruit.referral.service.ReferralService;
import com.smartrecruit.referral.service.ReferralMatchService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;

/**
 * 内推服务实现。
 * <p>
 * 涵盖内推计划管理、内推记录、计划-职位关联、奖金发放、分享令牌、公开落地页等核心业务逻辑。
 * 所有公开方法均标注 {@link Transactional}（如涉及写操作），确保数据一致性。
 * </p>
 *
 * @author xdh
 * @since 2026-04-26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    /** 排行榜展示人数上限。 */
    private static final int LEADERBOARD_TOP_N = 10;
    /** 分享令牌长度（小写字母+数字）。 */
    private static final int SHARE_TOKEN_LENGTH = 12;
    /** 内推码默认长度（大写字母+数字）。 */
    private static final int REFERRAL_CODE_LENGTH = 6;
    /** 内推码碰撞重试次数上限。 */
    private static final int REFERRAL_CODE_MAX_RETRIES = 5;
    /** 分享令牌可用字符集。 */
    private static final String SHARE_TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    /** 内推码可用字符集。 */
    private static final String REFERRAL_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    /** 安全的随机数生成器。 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /** JSON 序列化/反序列化工具。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SystemFileClient systemFileClient;
    private final ReferralProgramMapper programMapper;
    private final ReferralRecordMapper recordMapper;
    private final RefProgramJobMapper programJobMapper;
    private final RefBonusRecordMapper bonusRecordMapper;
    private final RefShareTokenMapper shareTokenMapper;
    private final RecruitmentSyncMapper recruitmentSyncMapper;
    private final ReferralMatchService referralMatchService;

    // ================================================================
    // 内推计划管理
    // ================================================================

    /**
     * 获取所有启用的内推计划列表。
     *
     * @return 启用的内推计划 VO 列表
     */
    @Override
    public List<ReferralProgramVO> getPrograms() {
        log.info("查询启用的内推计划列表");
        List<ReferralProgram> programs = programMapper.selectEnabledPrograms();
        return programs.stream()
                .map(this::toProgramVO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询内推计划，支持关键词和状态筛选。
     *
     * @param query 分页查询参数（关键词、状态、页码、每页大小）
     * @return 内推计划分页结果
     */
    @Override
    public PageResult<ReferralProgramVO> getProgramPage(ReferralPageQuery query) {
        log.info("分页查询内推计划: keyword={}, status={}, page={}, size={}",
                query.getKeyword(), query.getStatus(), query.getPage(), query.getSize());
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<ReferralProgram>(
                query.getPage(), query.getSize());
        var ipage = programMapper.selectPageWithQuery(page, query);
        List<ReferralProgramVO> vos = ipage.getRecords().stream()
                .map(this::toProgramVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, ipage.getTotal(), ipage.getSize(), ipage.getCurrent());
    }

    /**
     * 创建内推计划。
     * <p>
     * 自动填充创建人信息、创建时间和默认启用状态。
     * </p>
     *
     * @param request 计划创建请求
     * @return 创建成功的计划 VO
     */
    @Override
    @Transactional
    public ReferralProgramVO createProgram(CreateProgramRequest request) {
        log.info("创建内推计划: title={}", request.getTitle());
        LocalDateTime now = DateUtils.now();
        ReferralProgram program = new ReferralProgram();
        program.setTitle(request.getTitle());
        program.setDescription(request.getDescription());
        program.setBonusAmount(request.getBonusAmount());
        program.setBonusStructure(request.getBonusStructure());
        program.setStartDate(request.getStartDate());
        program.setEndDate(request.getEndDate());
        program.setEligibleDeptIds(request.getEligibleDeptIds());
        program.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        program.setCreateBy(getCurrentUserName());
        program.setCreateUserId(getCurrentUserId());
        program.setUpdateBy(getCurrentUserName());
        program.setUpdateUserId(getCurrentUserId());
        program.setCreateTime(now);
        program.setUpdateTime(now);
        programMapper.insert(program);
        log.info("内推计划创建成功: id={}, createBy={}", program.getId(), program.getCreateBy());
        return toProgramVO(program);
    }

    /**
     * 更新内推计划。
     * <p>
     * 仅更新请求中非空的字段，自动记录更新人和更新时间。
     * </p>
     *
     * @param programId 计划 ID
     * @param request   计划更新请求
     * @return 更新后的计划 VO
     * @throws ResourceNotFoundException 如果计划不存在
     */
    @Override
    @Transactional
    public ReferralProgramVO updateProgram(Long programId, UpdateProgramRequest request) {
        log.info("更新内推计划: id={}", programId);
        ReferralProgram program = programMapper.selectById(programId);
        if (program == null) {
            throw new ResourceNotFoundException("内推计划不存在: id=" + programId);
        }
        if (request.getTitle() != null) {
            program.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            program.setDescription(request.getDescription());
        }
        if (request.getBonusAmount() != null) {
            program.setBonusAmount(request.getBonusAmount());
        }
        if (request.getBonusStructure() != null) {
            program.setBonusStructure(request.getBonusStructure());
        }
        if (request.getStartDate() != null) {
            program.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            program.setEndDate(request.getEndDate());
        }
        if (request.getEligibleDeptIds() != null) {
            program.setEligibleDeptIds(request.getEligibleDeptIds());
        }
        if (request.getStatus() != null) {
            program.setStatus(request.getStatus());
        }
        program.setUpdateBy(getCurrentUserName());
        program.setUpdateUserId(getCurrentUserId());
        program.setUpdateTime(DateUtils.now());
        programMapper.updateById(program);
        log.info("内推计划更新成功: id={}, updateBy={}", programId, program.getUpdateBy());
        return toProgramVO(program);
    }

    /**
     * 删除内推计划（物理删除）。
     *
     * @param programId 计划 ID
     * @throws ResourceNotFoundException 如果计划不存在
     */
    @Override
    @Transactional
    public void deleteProgram(Long programId) {
        log.info("删除内推计划: id={}", programId);
        ReferralProgram program = programMapper.selectById(programId);
        if (program == null) {
            throw new ResourceNotFoundException("内推计划不存在: id=" + programId);
        }
        programMapper.deleteById(programId);
        log.info("内推计划删除成功: id={}", programId);
    }

    /**
     * 切换内推计划的启用/停用状态。
     * <p>
     * 启用 → 停用，停用 → 启用。
     * </p>
     *
     * @param programId 计划 ID
     * @throws ResourceNotFoundException 如果计划不存在
     */
    @Override
    @Transactional
    public void toggleProgram(Long programId) {
        log.info("切换内推计划启用状态: id={}", programId);
        ReferralProgram program = programMapper.selectById(programId);
        if (program == null) {
            throw new ResourceNotFoundException("内推计划不存在: id=" + programId);
        }
        int newStatus = (program.getStatus() != null && program.getStatus() == 1) ? 0 : 1;
        program.setStatus(newStatus);
        programMapper.updateById(program);
        log.info("内推计划状态已切换: id={}, status={}", programId, newStatus);
    }

    /**
     * 更新计划的奖金金额。
     *
     * @param programId   计划 ID
     * @param bonusAmount 新的奖金金额
     * @throws ResourceNotFoundException 如果计划不存在
     */
    @Override
    @Transactional
    public void updateBonus(Long programId, BigDecimal bonusAmount) {
        log.info("更新内推计划奖金: id={}, bonus={}", programId, bonusAmount);
        ReferralProgram program = programMapper.selectById(programId);
        if (program == null) {
            throw new ResourceNotFoundException("内推计划不存在: id=" + programId);
        }
        program.setBonusAmount(bonusAmount);
        programMapper.updateById(program);
        log.info("内推计划奖金更新成功: id={}", programId);
    }

    /**
     * 批量保存内推计划设置（创建或更新）。
     * <p>
     * 请求中包含 id 则为更新，否则为新建。
     * </p>
     *
     * @param requests 批量设置请求列表
     * @throws ResourceNotFoundException 如果指定的计划不存在
     */
    @Override
    @Transactional
    public void batchSaveSettings(List<BatchSaveSettingsRequest> requests) {
        log.info("批量保存内推计划设置: {} 条", requests.size());
        for (BatchSaveSettingsRequest req : requests) {
            if (req.getId() != null) {
                ReferralProgram existing = programMapper.selectById(req.getId());
                if (existing == null) {
                    throw new ResourceNotFoundException("计划职位不存在: id=" + req.getId());
                }
                existing.setTitle(req.getJobTitle());
                existing.setStatus(req.getEnabled());
                existing.setBonusAmount(req.getBonusAmount());
                existing.setDescription(req.getDescription());
                existing.setUpdateBy(getCurrentUserName());
                existing.setUpdateUserId(getCurrentUserId());
                existing.setUpdateTime(DateUtils.now());
                programMapper.updateById(existing);
                log.debug("已更新计划: id={}", req.getId());
            } else {
                ReferralProgram program = new ReferralProgram();
                program.setTitle(req.getJobTitle());
                program.setStatus(req.getEnabled() != null ? req.getEnabled() : 1);
                program.setBonusAmount(req.getBonusAmount());
                program.setDescription(req.getDescription());
                program.setStartDate(DateUtils.today());
                program.setCreateBy(getCurrentUserName());
                program.setCreateUserId(getCurrentUserId());
                program.setUpdateBy(getCurrentUserName());
                program.setUpdateUserId(getCurrentUserId());
                programMapper.insert(program);
                log.debug("已创建计划: jobTitle={}", req.getJobTitle());
            }
        }
        log.info("批量保存完成: 处理了 {} 条设置", requests.size());
    }

    // ================================================================
    // 内推记录管理
    // ================================================================

    /**
     * 分页查询内推记录。
     * <p>
     * 支持按关键词（候选人姓名/邮箱/手机号/职位名称）、状态、推荐人、计划进行筛选。
     * SQL 使用 LEFT JOIN 关联候选人、职位、部门、计划职位表，直接返回含展示字段的 VO。
     * </p>
     *
     * @param query 分页查询参数
     * @return 内推记录分页结果（含候选人姓名、职位、部门等展示字段）
     */
    @Override
    public PageResult<ReferralRecordVO> getRecords(ReferralPageQuery query) {
        log.info("分页查询内推记录: keyword={}, status={}, referrerId={}, programId={}, page={}, size={}",
                query.getKeyword(), query.getStatus(), query.getReferrerId(), query.getProgramId(),
                query.getPage(), query.getSize());
        long total = recordMapper.countRecords(query);
        long offset = (long) (query.getPage() - 1) * query.getSize();
        List<ReferralRecordVO> records = recordMapper.queryRecords(query, offset, (long) query.getSize());
        return PageResult.of(records, total, query.getSize(), query.getPage());
    }

    /**
     * 创建内推记录（内部员工推荐）。
     * <p>
     * 插入记录后自动根据计划奖金结构生成分阶段奖金发放记录。
     * </p>
     *
     * @param request 创建内推请求
     * @return 创建成功的内推记录 VO
     */
    @Override
    @Transactional
    public ReferralRecordVO createRecord(CreateReferralRequest request) {
        log.info("创建内推记录: referrerId={}, candidateName={}",
                request.getReferrerId(), request.getCandidateName());
        LocalDateTime now = DateUtils.now();
        ReferralRecord record = new ReferralRecord();
        record.setProgramId(request.getProgramId());
        record.setProgramJobId(request.getProgramJobId());
        record.setReferrerId(request.getReferrerId());
        record.setCandidateId(request.getCandidateId());
        record.setJobPositionId(request.getJobId());
        record.setRelationship(request.getRelationship());
        record.setReferralNote(request.getReferralNote());
        record.setStatus(0); // PENDING
        record.setBonusAmount(request.getBonus() != null ? request.getBonus() : BigDecimal.ZERO);
        record.setBonusPaid(BigDecimal.ZERO);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        recordMapper.insert(record);
        log.info("内推记录创建成功: id={}", record.getId());

        // 根据计划奖金结构快照生成分阶段奖金记录
        snapshotBonusRecords(record);

        ReferralRecordVO vo = toRecordVO(record);
        vo.setBonusRecords(bonusRecordMapper.selectByRecordId(record.getId()));

        // 内推记录创建后自动触发智能匹配（best-effort，失败不影响创建）
        try {
            referralMatchService.refreshMatches(record.getId());
        } catch (Exception e) {
            log.warn("内推智能匹配自动触发失败（可稍后手动重新匹配）: recordId={}, error={}",
                    record.getId(), e.getMessage());
        }
        return vo;
    }

    /**
     * 查询指定推荐人的所有内推记录（含候选人、职位、部门等展示字段）。
     *
     * @param referrerId 推荐人用户 ID
     * @return 内推记录列表
     */
    @Override
    public List<ReferralRecordVO> getMyReferrals(Long referrerId) {
        log.info("查询推荐人的内推记录: referrerId={}", referrerId);
        List<ReferralRecordVO> vos = recordMapper.findByReferrerIdWithDetails(referrerId);
        vos.forEach(vo -> vo.setBonusRecords(bonusRecordMapper.selectByRecordId(vo.getId())));
        return vos;
    }

    /**
     * 获取当前登录用户的投递记录。
     * <p>
     * 仅返回已认证用户以候选人身份投递的记录。未认证时返回空列表。
     * </p>
     *
     * @return 我的投递记录列表
     */
    @Override
    public List<ReferralRecordVO> getMyApplications() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            log.warn("获取我的投递记录: 当前未认证用户");
            return List.of();
        }
        log.info("查询用户的投递记录: userId={}", userId);
        List<ReferralRecordVO> vos = recordMapper.findByCandidateIdWithDetails(userId);
        vos.forEach(vo -> vo.setBonusRecords(bonusRecordMapper.selectByRecordId(vo.getId())));
        return vos;
    }

    /**
     * 获取内推排行榜。
     * <p>
     * 按成功推荐数量降序排列，取前 N 名。同时填充排名序号。
     * </p>
     *
     * @return 排行榜 VO 列表
     */
    @Override
    public List<LeaderboardVO> getLeaderboard() {
        log.info("查询内推排行榜: 前 {} 名", LEADERBOARD_TOP_N);
        List<LeaderboardVO> list = recordMapper.selectLeaderboard(LEADERBOARD_TOP_N);
        AtomicInteger rank = new AtomicInteger(1);
        list.forEach(vo -> vo.setRank(rank.getAndIncrement()));
        return list;
    }

    // ================================================================
    // 政策与海报
    // ================================================================

    /**
     * 获取内推政策数据，包含奖金阶梯、规则、流程、常见问题和联系方式。
     *
     * @return 内推政策数据结构
     */
    @Override
    public ReferralPolicyVO getPolicy() {
        log.info("获取内推政策数据");

        // 分阶段奖金阶梯
        List<ReferralPolicyVO.BonusTierVO> bonusTiers = List.of(
                new ReferralPolicyVO.BonusTierVO(
                        "第一期", "候选人完成面试并通过录用审批", "30%", "候选人接受Offer并入职"),
                new ReferralPolicyVO.BonusTierVO(
                        "第二期", "候选人顺利通过试用期考核", "40%", "入职满3个月并通过试用期"),
                new ReferralPolicyVO.BonusTierVO(
                        "第三期", "候选人在公司稳定工作满一年", "30%", "入职满12个月")
        );

        // 内推规则
        List<String> rules = List.of(
                "所有正式员工均可参与内推，每人推荐次数不限",
                "被推荐人在过去6个月内不曾投递过同一职位",
                "奖金按阶段发放，推荐人需在发奖时仍在职",
                "推荐人不得为被推荐人的面试官或直属上级",
                "同一候选人被多人推荐，以最早提交的推荐记录为准",
                "HR 保留对内推政策的最终解释权和调整权"
        );

        // 内推流程
        List<ReferralPolicyVO.PolicyStepVO> process = List.of(
                new ReferralPolicyVO.PolicyStepVO("1", "提交推荐", "填写候选人信息并选择目标职位"),
                new ReferralPolicyVO.PolicyStepVO("2", "HR 筛选", "HR 团队审核候选人简历与岗位匹配度"),
                new ReferralPolicyVO.PolicyStepVO("3", "安排面试", "候选人进入面试流程，推进各轮考核"),
                new ReferralPolicyVO.PolicyStepVO("4", "录用入职", "候选人接受Offer并办理入职手续"),
                new ReferralPolicyVO.PolicyStepVO("5", "奖金发放", "按阶段发放内推奖金至推荐人")
        );

        // 常见问题
        List<ReferralPolicyVO.PolicyFaqVO> faqs = List.of(
                new ReferralPolicyVO.PolicyFaqVO("哪些人可以参与内推？",
                        "公司所有正式员工都可以参与内推计划，每人推荐次数不限。实习生和外包人员暂不参与。"),
                new ReferralPolicyVO.PolicyFaqVO("内推奖金有多少？",
                        "奖金金额根据具体内推计划而定，一般在3,000元至50,000元之间。高管岗位和紧急岗位的奖金会更高。具体金额请查看各内推计划详情。"),
                new ReferralPolicyVO.PolicyFaqVO("奖金什么时候发放？",
                        "奖金分阶段发放：入职发放30%，通过试用期发放40%，满一年发放30%。推荐人需在发奖时仍在职。"),
                new ReferralPolicyVO.PolicyFaqVO("可以推荐已经投递过的人吗？",
                        "不可以。如果候选人在过去6个月内曾主动投递过同一职位，该次推荐将不计入内推奖励。"),
                new ReferralPolicyVO.PolicyFaqVO("推荐后多久能知道结果？",
                        "HR 通常在3-5个工作日内完成简历筛选并反馈。你可以通过'内推记录'页面随时查看推荐进度。"),
                new ReferralPolicyVO.PolicyFaqVO("奖金需要缴税吗？",
                        "内推奖金属于个人劳动报酬，根据国家相关税法规定需要缴纳个人所得税，税款由公司在发放时代扣代缴。")
        );

        // 联系方式
        ReferralPolicyVO.ContactVO contact =
                new ReferralPolicyVO.ContactVO("referral@smartrecruit.com", "400-888-0000", "SmartRecruit_HR");
        return new ReferralPolicyVO(bonusTiers, rules, process, faqs, contact);
    }

    /**
     * 生成内推海报数据。
     * <p>
     * 包含计划信息、海报图片 URL、分享链接和二维码 URL（均为占位地址，后续对接海报渲染服务）。
     * </p>
     *
     * @param programId 计划 ID
     * @return 海报数据 Map
     * @throws ResourceNotFoundException 如果计划不存在
     */
    @Override
    public PosterVO generatePoster(Long programId) {
        log.info("生成内推海报: programId={}", programId);
        ReferralProgram program = programMapper.selectById(programId);
        if (program == null) {
            throw new ResourceNotFoundException("内推计划不存在: id=" + programId);
        }
        PosterVO poster = new PosterVO(
                String.valueOf(programId),
                program.getTitle(),
                program.getBonusAmount() != null ? program.getBonusAmount().toString() : "0",
                "https://cdn.smartrecruit.com/posters/"
                        + programId + "_" + DateUtils.currentEpochMillis() + ".png",
                "https://smartrecruit.com/referral/apply?programId=" + programId,
                "https://cdn.smartrecruit.com/qrcodes/program_" + programId + ".png",
                DateUtils.now().toString()
        );
        log.info("内推海报生成成功: programId={}", programId);
        return poster;
    }

    // ================================================================
    // 计划-职位关联管理
    // ================================================================

    /**
     * 获取计划下所有关联的职位列表。
     *
     * @param programId 计划 ID
     * @return 计划关联职位 VO 列表
     */
    @Override
    public List<RefProgramJobVO> getProgramJobs(Long programId) {
        log.info("查询计划关联职位: programId={}", programId);
        List<RefProgramJob> jobs = programJobMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RefProgramJob>()
                        .eq(RefProgramJob::getProgramId, programId)
                        .orderByDesc(RefProgramJob::getCreateTime));
        return jobs.stream().map(this::toProgramJobVO).collect(Collectors.toList());
    }

    /**
     * 获取计划职位的详细信息（含计划概要、职位详情）。
     * <p>
     * 通过关联查询聚合 plan_job、program、job_position 三张表的数据。
     * </p>
     *
     * @param programJobId 计划职位关联 ID
     * @return 计划职位详情 VO
     * @throws ResourceNotFoundException 如果计划职位不存在
     */
    @Override
    public ProgramJobDetailVO getProgramJobDetail(Long programJobId) {
        log.info("查询计划职位详情: programJobId={}", programJobId);
        Map<String, Object> row = programJobMapper.selectJobDetailById(programJobId);
        if (row == null || row.isEmpty()) {
            throw new ResourceNotFoundException("计划职位不存在: id=" + programJobId);
        }

        RefProgramJobVO programJob = new RefProgramJobVO();
        programJob.setId(toLong(row.get("pj_id")));
        programJob.setProgramId(toLong(row.get("pj_program_id")));
        programJob.setJobPositionId(toLong(row.get("pj_job_position_id")));
        programJob.setIsEnabled(toInt(row.get("pj_is_enabled")));
        programJob.setBonusAmount(toBigDecimal(row.get("pj_bonus_amount")));
        programJob.setTag(toInt(row.get("pj_tag")));
        programJob.setJobTitle((String) row.get("pj_job_title"));
        programJob.setMinSalary(toInt(row.get("pj_min_salary")));
        programJob.setMaxSalary(toInt(row.get("pj_max_salary")));
        programJob.setHeadCount(toInt(row.get("pj_head_count")));

        ProgramJobDetailVO.ProgramBrief program = new ProgramJobDetailVO.ProgramBrief();
        program.setId(toLong(row.get("pj_program_id")));
        program.setTitle((String) row.get("program_title"));
        program.setDescription((String) row.get("program_description"));
        program.setBonusAmount(toBigDecimal(row.get("program_bonus_amount")));
        program.setBonusStructure((String) row.get("program_bonus_structure"));
        Object startDate = row.get("program_start_date");
        program.setStartDate(startDate != null ? startDate.toString() : null);
        Object endDate = row.get("program_end_date");
        program.setEndDate(endDate != null ? endDate.toString() : null);
        program.setStatus(toInt(row.get("program_status")));
        program.setCreateBy((String) row.get("program_create_by"));

        ProgramJobDetailVO.JobBrief jobPosition = new ProgramJobDetailVO.JobBrief();
        jobPosition.setId(toLong(row.get("job_id")));
        jobPosition.setTitle((String) row.get("job_title"));
        jobPosition.setDescription((String) row.get("job_description"));
        jobPosition.setResponsibilities((String) row.get("job_responsibilities"));
        jobPosition.setRequirements((String) row.get("job_requirements"));
        jobPosition.setSkills((String) row.get("job_skills"));
        jobPosition.setMinSalary(toInt(row.get("job_min_salary")));
        jobPosition.setMaxSalary(toInt(row.get("job_max_salary")));
        jobPosition.setLocation((String) row.get("job_location"));
        jobPosition.setPositionType(toInt(row.get("job_position_type")));
        jobPosition.setExperienceLevel(toInt(row.get("job_experience_level")));
        jobPosition.setEducationLevel(toInt(row.get("job_education_level")));
        jobPosition.setStatus(toInt(row.get("job_status")));
        jobPosition.setDepartmentId(toLong(row.get("job_department_id")));
        jobPosition.setDepartmentName((String) row.get("job_department_name"));

        ProgramJobDetailVO vo = new ProgramJobDetailVO();
        vo.setProgramJob(programJob);
        vo.setProgram(program);
        vo.setJobPosition(jobPosition);
        return vo;
    }

    /**
     * 批量保存计划-职位关联（创建或更新）。
     * <p>
     * 如果请求中包含 id 且非空则为更新，否则为新建。
     * </p>
     *
     * @param programId 计划 ID
     * @param requests  批量保存请求列表
     * @throws ResourceNotFoundException 如果指定的计划职位不存在
     */
    @Override
    @Transactional
    public void batchSaveProgramJobs(Long programId, List<BatchSaveProgramJobRequest> requests) {
        log.info("批量保存计划职位: programId={}, 数量={}", programId, requests.size());
        for (BatchSaveProgramJobRequest req : requests) {
            if (req.getId() != null && !req.getId().isEmpty()) {
                Long existingId = Long.valueOf(req.getId());
                RefProgramJob existing = programJobMapper.selectById(existingId);
                if (existing == null) {
                    throw new ResourceNotFoundException("计划职位不存在: id=" + existingId);
                }
                existing.setIsEnabled(req.getIsEnabled());
                existing.setBonusAmount(req.getBonusAmount());
                existing.setTag(req.getTag());
                existing.setJobTitle(req.getJobTitle());
                existing.setMinSalary(req.getMinSalary());
                existing.setMaxSalary(req.getMaxSalary());
                existing.setHeadCount(req.getHeadCount());
                existing.setUpdateTime(DateUtils.now());
                programJobMapper.updateById(existing);
            } else {
                RefProgramJob job = new RefProgramJob();
                job.setProgramId(programId);
                job.setJobPositionId(req.getJobPositionId());
                job.setIsEnabled(req.getIsEnabled() != null ? req.getIsEnabled() : 1);
                job.setBonusAmount(req.getBonusAmount());
                job.setTag(req.getTag());
                job.setJobTitle(req.getJobTitle());
                job.setMinSalary(req.getMinSalary());
                job.setMaxSalary(req.getMaxSalary());
                job.setHeadCount(req.getHeadCount());
                job.setCreateTime(DateUtils.now());
                job.setUpdateTime(DateUtils.now());
                programJobMapper.insert(job);
            }
        }
        log.info("计划职位批量保存完成: programId={}", programId);
    }

    /**
     * 删除计划职位关联。
     *
     * @param jobId 计划职位 ID
     * @throws ResourceNotFoundException 如果计划职位不存在
     */
    @Override
    @Transactional
    public void deleteProgramJob(Long jobId) {
        log.info("删除计划职位: id={}", jobId);
        RefProgramJob job = programJobMapper.selectById(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("计划职位不存在: id=" + jobId);
        }
        programJobMapper.deleteById(jobId);
        log.info("计划职位删除成功: id={}", jobId);
    }

    /**
     * 切换计划职位的启用/停用状态。
     *
     * @param jobId 计划职位 ID
     * @throws ResourceNotFoundException 如果计划职位不存在
     */
    @Override
    @Transactional
    public void toggleProgramJob(Long jobId) {
        log.info("切换计划职位启用状态: id={}", jobId);
        RefProgramJob job = programJobMapper.selectById(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("计划职位不存在: id=" + jobId);
        }
        job.setIsEnabled(job.getIsEnabled() != null && job.getIsEnabled() == 1 ? 0 : 1);
        job.setUpdateTime(DateUtils.now());
        programJobMapper.updateById(job);
        log.info("计划职位状态已切换: id={}, isEnabled={}", jobId, job.getIsEnabled());
    }

    /**
     * 更新计划职位的奖金金额。
     *
     * @param jobId       计划职位 ID
     * @param bonusAmount 新的奖金金额
     * @throws ResourceNotFoundException 如果计划职位不存在
     */
    @Override
    @Transactional
    public void updateProgramJobBonus(Long jobId, BigDecimal bonusAmount) {
        log.info("更新计划职位奖金: id={}, bonus={}", jobId, bonusAmount);
        RefProgramJob job = programJobMapper.selectById(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("计划职位不存在: id=" + jobId);
        }
        job.setBonusAmount(bonusAmount);
        job.setUpdateTime(DateUtils.now());
        programJobMapper.updateById(job);
        log.info("计划职位奖金更新成功: id={}", jobId);
    }

    // ================================================================
    // 奖金发放记录管理
    // ================================================================

    /**
     * 获取指定内推记录的分阶段奖金发放明细。
     *
     * @param recordId 内推记录 ID
     * @return 奖金发放记录列表
     */
    @Override
    public List<BonusRecordVO> getBonusRecords(Long recordId) {
        log.info("查询内推记录奖金发放明细: recordId={}", recordId);
        return bonusRecordMapper.selectByRecordId(recordId);
    }

    /**
     * 确认发放单条分阶段奖金。
     * <p>
     * 标记该阶段为已发放，记录发放时间，并同步更新关联内推记录的已发放累计金额和发放状态。
     * 如果已发放则跳过（幂等）。
     * </p>
     *
     * @param bonusRecordId 奖金发放记录 ID
     * @throws ResourceNotFoundException 如果奖金记录不存在
     */
    @Override
    @Transactional
    public void payBonusStage(Long bonusRecordId) {
        log.info("确认发放奖金: bonusRecordId={}", bonusRecordId);
        RefBonusRecord bonus = bonusRecordMapper.selectById(bonusRecordId);
        if (bonus == null) {
            throw new ResourceNotFoundException("奖金发放记录不存在: id=" + bonusRecordId);
        }
        if (bonus.getStatus() != null && bonus.getStatus() == 1) {
            log.warn("奖金记录已发放，跳过: id={}", bonusRecordId);
            return;
        }
        LocalDateTime now = DateUtils.now();
        bonus.setStatus(1); // PAID
        bonus.setPaidTime(now);
        bonus.setUpdateTime(now);
        bonusRecordMapper.updateById(bonus);

        // 同步更新内推记录的已发放金额
        ReferralRecord record = recordMapper.selectById(bonus.getRefRecordId());
        if (record != null) {
            BigDecimal newPaid = (record.getBonusPaid() != null ? record.getBonusPaid() : BigDecimal.ZERO)
                    .add(bonus.getAmount());
            record.setBonusPaid(newPaid);
            record.setBonusStatus(1); // PARTIAL_PAID
            if (record.getBonusAmount() != null && newPaid.compareTo(record.getBonusAmount()) >= 0) {
                record.setBonusStatus(2); // FULL_PAID
            }
            record.setUpdateTime(now);
            recordMapper.updateById(record);
        }

        log.info("奖金发放成功: id={}, amount={}, refRecordId={}",
                bonusRecordId, bonus.getAmount(), bonus.getRefRecordId());
    }

    // ================================================================
    // 公开落地页（分享令牌 & 投递）
    // ================================================================

    /**
     * 生成分享令牌。
     * <p>
     * 为指定计划生成一个随机分享令牌（小写字母+数字，12位）和人类友好的内推码（大写字母+数字，6位）。
     * 推荐人信息自动从当前认证用户获取。分享链接格式为 {@code /p/{token}}。
     * </p>
     *
     * @param request 分享令牌生成请求
     * @return 分享令牌响应（含 token、链接、内推码）
     * @throws ResourceNotFoundException 如果计划不存在
     */
    @Override
    public ShareTokenResponse generateShareToken(ShareTokenRequest request) {
        log.info("生成分享令牌: programId={}, referrerName={}",
                request.getProgramId(), request.getReferrerName());
        ReferralProgram program = programMapper.selectById(request.getProgramId());
        if (program == null) {
            throw new ResourceNotFoundException("内推计划不存在: id=" + request.getProgramId());
        }

        // 从当前认证用户获取推荐人信息
        Long referrerId = getCurrentUserId();
        String referrerName = request.getReferrerName();
        if (referrerName == null || referrerName.isBlank()) {
            referrerName = getCurrentUserName();
        }

        // 生成随机分享令牌和内推码
        String token = generateRandomToken();
        String referralCode = generateReferralCode();
        LocalDateTime now = DateUtils.now();

        RefShareToken shareToken = new RefShareToken();
        shareToken.setToken(token);
        shareToken.setReferralCode(referralCode);
        shareToken.setReferrerId(referrerId);
        shareToken.setProgramId(request.getProgramId());
        shareToken.setReferrerName(referrerName);
        shareToken.setSource(request.getSource());
        shareToken.setCreateTime(now);
        shareTokenMapper.insert(shareToken);

        String url = "/p/" + token;
        log.info("分享令牌生成成功: token={}, referralCode={}, referrerId={}, programId={}",
                token, referralCode, referrerId, request.getProgramId());
        return ShareTokenResponse.of(token, url, referralCode, referrerName, request.getSource());
    }

    /**
     * 获取所有启用的公开内推计划及关联职位、分享令牌。
     * <p>
     * 用于内推广场等公开页面的列表展示。
     * </p>
     *
     * @return 公开计划列表（含 program、jobs、shareToken）
     */
    @Override
    public List<PublicProgramVO> getPublicPrograms() {
        log.info("查询所有启用的公开内推计划及关联职位");
        List<ReferralProgram> programs = programMapper.selectEnabledPrograms();
        List<PublicProgramVO> result = new ArrayList<>();
        for (ReferralProgram program : programs) {
            List<RefProgramJob> jobs = programJobMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RefProgramJob>()
                            .eq(RefProgramJob::getProgramId, program.getId())
                            .eq(RefProgramJob::getIsEnabled, 1)
                            .orderByDesc(RefProgramJob::getCreateTime));
            List<RefProgramJobVO> jobVOs = jobs.stream()
                    .map(this::toProgramJobVO)
                    .collect(Collectors.toList());

            // 获取该计划最新的分享令牌
            RefShareToken latestToken = shareTokenMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RefShareToken>()
                            .eq(RefShareToken::getProgramId, program.getId())
                            .orderByDesc(RefShareToken::getCreateTime)
                            .last("LIMIT 1"));

            PublicProgramVO item = new PublicProgramVO(
                    toProgramVO(program), jobVOs,
                    latestToken != null ? latestToken.getToken() : null);
            result.add(item);
        }
        log.info("公开计划查询完成: {} 个计划", result.size());
        return result;
    }

    /**
     * 通过分享令牌获取公开计划详情。
     *
     * @param token 分享令牌
     * @return 计划详情（含 program、jobs、推荐人、内推码）
     * @throws ResourceNotFoundException 如果令牌无效或计划不存在
     */
    @Override
    public PublicProgramDetailVO getPublicProgramByToken(String token) {
        log.info("通过分享令牌查询计划: token={}", token);
        RefShareToken shareToken = shareTokenMapper.selectByToken(token);
        if (shareToken == null) {
            throw new ResourceNotFoundException("分享令牌无效: " + token);
        }
        Long programId = shareToken.getProgramId();
        ReferralProgram program = programMapper.selectById(programId);
        if (program == null) {
            throw new ResourceNotFoundException("内推计划不存在: id=" + programId);
        }
        // 仅返回启用的计划职位
        List<RefProgramJob> jobs = programJobMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RefProgramJob>()
                        .eq(RefProgramJob::getProgramId, programId)
                        .eq(RefProgramJob::getIsEnabled, 1)
                        .orderByDesc(RefProgramJob::getCreateTime));
        List<RefProgramJobVO> jobVOs = jobs.stream()
                .map(this::toProgramJobVO)
                .collect(Collectors.toList());

        return new PublicProgramDetailVO(
                toProgramVO(program), jobVOs,
                shareToken.getReferrerName(), shareToken.getSource(), shareToken.getReferralCode());
    }

    /**
     * 检查候选人是否已投递过指定计划职位。
     *
     * @param candidateId  候选人 ID
     * @param programJobId 计划职位关联 ID
     * @return true 已投递，false 未投递
     */
    @Override
    public boolean hasApplied(Long candidateId, Long programJobId) {
        int count = recordMapper.countByCandidateIdAndProgramJobId(candidateId, programJobId);
        return count > 0;
    }

    /**
     * 上传简历至文件服务，返回文件访问 URL。
     *
     * @param file 上传的简历文件
     * @return 简历文件 URL
     * @throws RuntimeException 如果上传失败
     */
    @Override
    public String uploadResume(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            String originalName = file.getOriginalFilename();
            String ext = ".pdf";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String keyName = UUID.randomUUID().toString().substring(0, 8) + ext.toLowerCase();
            String relativePath = "referrals/"
                    + DateUtils.formatSlashMonth(DateUtils.now())
                    + "/" + keyName;
            ApiResponse<String> resp = systemFileClient.upload(
                    fileBytes, originalName, file.getContentType(), relativePath);
            if (resp != null && resp.code() == 0 && resp.data() != null) {
                String fullUrl = resp.data();
                log.info("简历上传至文件服务成功: url={}", fullUrl);
                return fullUrl;
            }
            throw new RuntimeException("文件上传失败: " + (resp != null ? resp.message() : "null response"));
        } catch (java.io.IOException e) {
            log.error("读取上传文件字节失败", e);
            throw new RuntimeException("无法读取上传文件", e);
        }
    }

    /**
     * 创建公开投递记录（候选人通过分享链接投递）。
     * <p>
     * 核心流程：
     * <ol>
     *   <li>通过分享令牌解析推荐人信息</li>
     *   <li>同步候选人信息到招聘模块（已存在的更新姓名/手机号，不存在的新建）</li>
     *   <li>同步简历信息到招聘模块</li>
     *   <li>检查是否重复投递（唯一索引防重）</li>
     *   <li>创建内推记录并快照奖金结构</li>
     * </ol>
     * </p>
     *
     * @param request 公开投递请求
     * @return 创建成功的投递记录 VO
     * @throws DuplicateResourceException 如果已投递过该职位
     * @throws IllegalArgumentException  如果缺少分享令牌参数
     */
    @Override
    @Transactional
    public ReferralRecordVO createPublicRecord(PublicReferralRequest request) {
        log.info("创建公开投递记录: candidateName={}, programId={}, shareToken={}",
                request.getCandidateName(), request.getProgramId(), request.getShareToken());
        LocalDateTime now = DateUtils.now();
        ReferralRecord record = new ReferralRecord();
        record.setProgramId(request.getProgramId());
        record.setProgramJobId(request.getProgramJobId());

        // 从分享令牌解析推荐人
        String shareTokenVal = request.getShareToken();
        if (shareTokenVal != null && !shareTokenVal.isBlank()) {
            RefShareToken shareTokenRecord = shareTokenMapper.selectByToken(shareTokenVal);
            if (shareTokenRecord != null) {
                record.setShareToken(shareTokenVal);
                record.setReferralCode(shareTokenRecord.getReferralCode());
                if (shareTokenRecord.getReferrerId() != null) {
                    record.setReferrerId(shareTokenRecord.getReferrerId());
                    log.info("公开投递关联推荐人成功: referrerId={}, referralCode={}",
                            shareTokenRecord.getReferrerId(), shareTokenRecord.getReferralCode());
                } else {
                    log.warn("分享令牌推荐人为空，按自投递处理: token={}", shareTokenVal);
                }
            } else {
                log.warn("分享令牌未找到，按自投递处理: token={}", shareTokenVal);
            }
        } else {
            throw new IllegalArgumentException("缺少分享令牌参数");
        }

        // 自投递（无推荐人）时 referrer_id 设为 0，满足数据库 NOT NULL 约束
        if (record.getReferrerId() == null) {
            record.setReferrerId(0L);
        }

        // ========== 同步候选人 + 简历到招聘模块 ==========
        Long syncCandidateId = null;
        String candidateEmail = request.getCandidateEmail();
        if (candidateEmail != null && !candidateEmail.isBlank()) {
            Long existingCandidateId = recruitmentSyncMapper.findCandidateIdByEmail(candidateEmail);
            if (existingCandidateId != null) {
                syncCandidateId = existingCandidateId;
                // 更新已有候选人的姓名和手机号，避免展示旧数据
                recruitmentSyncMapper.updateCandidate(existingCandidateId,
                        request.getCandidateName(), request.getCandidatePhone());
                log.info("公开投递: 候选人邮箱已存在，已更新姓名和手机号: candidateId={}", syncCandidateId);
            }
        }
        if (syncCandidateId == null) {
            syncCandidateId = generateId();
            recruitmentSyncMapper.insertCandidate(
                    syncCandidateId,
                    request.getCandidateName(),
                    request.getCandidateEmail(),
                    request.getCandidatePhone(),
                    1, // source = REFERRAL
                    record.getReferrerId() != null && record.getReferrerId() > 0 ? record.getReferrerId() : null,
                    now);
            log.info("公开投递: 候选人创建成功: candidateId={}", syncCandidateId);
        }

        if (request.getResumeUrl() != null && !request.getResumeUrl().isBlank()) {
            String fileName = extractFileName(request.getResumeUrl());
            int fileType = detectFileType(request.getResumeUrl());
            Long resumeId = generateId();
            recruitmentSyncMapper.insertResume(
                    resumeId,
                    syncCandidateId,
                    request.getJobId(),
                    fileName,
                    request.getResumeUrl(),
                    fileType,
                    0L, // fileSize 暂未知
                    0,  // parseStatus = PENDING（由 ResumeParseScheduler 自动解析）
                    now);
            log.info("公开投递: 简历创建成功: resumeId={}, fileName={}, fileType={}",
                    resumeId, fileName, fileType);
        }
        // =================================================

        // 自投递：候选人即注册用户本人，candidate_id = sys_user.id
        // 匿名投递：使用临时时间戳 ID
        Long loginUserId = getCurrentUserId();
        if (loginUserId != null) {
            record.setCandidateId(loginUserId);
            log.info("自投递: 使用已登录用户作为候选人 candidateId={}", loginUserId);
        } else {
            record.setCandidateId(DateUtils.currentEpochMillis()); // 匿名临时 ID
        }
        record.setJobPositionId(request.getJobId());

        // 检查是否已投递过该职位（按唯一索引 uk_referrer_candidate_job 列匹配）
        int existingCount = recordMapper.countByReferrerCandidateJob(
                record.getReferrerId(), record.getCandidateId(), request.getJobId());
        if (existingCount > 0) {
            throw new DuplicateResourceException("您已经投递过该职位了");
        }
        record.setRelationship(request.getRelationship());
        record.setReferralNote(request.getReferralNote());
        record.setResumeUrl(request.getResumeUrl());
        record.setStatus(0); // PENDING
        record.setBonusAmount(request.getBonus() != null ? request.getBonus() : BigDecimal.ZERO);
        record.setBonusPaid(BigDecimal.ZERO);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        try {
            recordMapper.insert(record);
        } catch (DataIntegrityViolationException e) {
            log.warn("重复投递（唯一约束冲突）: referrerId={}, candidateId={}, jobPositionId={}",
                    record.getReferrerId(), record.getCandidateId(), record.getJobPositionId());
            throw new DuplicateResourceException("您已经投递过该职位了");
        }
        log.info("公开投递记录创建成功: id={}", record.getId());

        // 快照奖金结构
        snapshotBonusRecords(record);

        ReferralRecordVO vo = toRecordVO(record);
        // 从请求中设置候选人姓名（因为数据库表中不存储）
        vo.setCandidateName(request.getCandidateName());
        vo.setBonusRecords(bonusRecordMapper.selectByRecordId(record.getId()));
        return vo;
    }

    // ================================================================
    // 私有辅助方法 — 业务逻辑
    // ================================================================

    /**
     * 根据计划奖金结构快照，为新建的内推记录生成分阶段奖金发放记录。
     * <p>
     * 读取计划配置的奖金结构（Map&lt;阶段名称, 金额&gt;），按比例计算各阶段金额并写入 {@code ref_bonus_record}。
     * 解析失败或结构为空时静默跳过，不中断主流程。
     * </p>
     *
     * @param record 新建的内推记录（已写入数据库，有 id）
     */
    private void snapshotBonusRecords(ReferralRecord record) {
        if (record.getBonusAmount() == null || record.getBonusAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ReferralProgram program = programMapper.selectById(record.getProgramId());
        if (program == null || program.getBonusStructure() == null) {
            return;
        }
        try {
            Map<String, Object> structure = parseBonusStructure(program.getBonusStructure());
            if (structure == null || structure.isEmpty()) {
                return;
            }
            BigDecimal programBonusTotal = program.getBonusAmount() != null
                    ? program.getBonusAmount() : record.getBonusAmount();
            BigDecimal ratio = programBonusTotal.compareTo(BigDecimal.ZERO) > 0
                    ? record.getBonusAmount().divide(programBonusTotal, 6, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ONE;
            LocalDateTime now = DateUtils.now();
            int stageIndex = 0;
            for (Map.Entry<String, Object> entry : structure.entrySet()) {
                String stageName = entry.getKey();
                BigDecimal stageAmount;
                if (entry.getValue() instanceof Number) {
                    stageAmount = BigDecimal.valueOf(((Number) entry.getValue()).doubleValue())
                            .multiply(ratio).setScale(2, java.math.RoundingMode.HALF_UP);
                } else {
                    continue;
                }
                RefBonusRecord bonus = new RefBonusRecord();
                bonus.setRefRecordId(record.getId());
                bonus.setReferrerId(record.getReferrerId());
                bonus.setStage(stageIndex++);
                bonus.setStageName(stageName);
                bonus.setAmount(stageAmount);
                bonus.setStatus(0); // PENDING
                bonus.setCreateTime(now);
                bonus.setUpdateTime(now);
                bonusRecordMapper.insert(bonus);
                log.debug("奖金快照创建成功: refRecordId={}, stage={}, amount={}",
                        record.getId(), stageName, stageAmount);
            }
        } catch (Exception e) {
            log.error("奖金快照创建失败: recordId={}", record.getId(), e);
        }
    }

    /**
     * 解析 bonus_structure Object 为 Map。
     * <p>
     * 兼容 Jackson 返回的 LinkedHashMap 和 JSON 字符串两种格式。
     * </p>
     *
     * @param source bonus_structure 原始值
     * @return 解析后的奖金结构 Map，解析失败返回 null
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBonusStructure(Object source) {
        if (source == null) return null;
        if (source instanceof Map) {
            return (Map<String, Object>) source;
        }
        if (source instanceof String) {
            try {
                return OBJECT_MAPPER.readValue((String) source,
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("奖金结构解析失败: {}", source, e);
                return null;
            }
        }
        return null;
    }

    // ================================================================
    // 私有辅助方法 — 令牌 & ID 生成
    // ================================================================

    /**
     * 生成随机分享令牌（小写字母 + 数字，{@value #SHARE_TOKEN_LENGTH} 位）。
     *
     * @return 随机分享令牌字符串
     */
    private String generateRandomToken() {
        StringBuilder sb = new StringBuilder(SHARE_TOKEN_LENGTH);
        for (int i = 0; i < SHARE_TOKEN_LENGTH; i++) {
            sb.append(SHARE_TOKEN_CHARS.charAt(SECURE_RANDOM.nextInt(SHARE_TOKEN_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成人类友好的内推码（大写字母 + 数字，{@value #REFERRAL_CODE_LENGTH} 位）。
     * <p>
     * 包含碰撞重试机制：最多重试 {@value #REFERRAL_CODE_MAX_RETRIES} 次，
     * 全部碰撞后扩大为 8 位。极端情况下抛出异常。
     * </p>
     *
     * @return 唯一的内推码
     * @throws RuntimeException 如果多次重试后仍碰撞
     */
    private String generateReferralCode() {
        for (int length = REFERRAL_CODE_LENGTH; length <= REFERRAL_CODE_LENGTH + 2; length += 2) {
            for (int attempt = 0; attempt < REFERRAL_CODE_MAX_RETRIES; attempt++) {
                StringBuilder sb = new StringBuilder(length);
                for (int i = 0; i < length; i++) {
                    sb.append(REFERRAL_CODE_CHARS.charAt(
                            SECURE_RANDOM.nextInt(REFERRAL_CODE_CHARS.length())));
                }
                String code = sb.toString();
                if (shareTokenMapper.selectByReferralCode(code) == null) {
                    return code;
                }
                log.warn("内推码冲突: code={}, attempt={}", code, attempt + 1);
            }
        }
        // 极端情况：多次重试后仍无法生成唯一内推码
        throw new RuntimeException("已重试最大次数，仍无法生成唯一的内推码");
    }

    /**
     * 生成跨模块同步用的唯一 ID。
     * <p>
     * 使用 MyBatis-Plus IdWorker，与模块内实体 ID 生成策略一致。
     * </p>
     *
     * @return 全局唯一 ID
     */
    private Long generateId() {
        return com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
    }

    // ================================================================
    // 私有辅助方法 — 文件处理
    // ================================================================

    /**
     * 从简历文件 URL 路径中提取文件名。
     * <p>
     * 例如 "referrals/2026/08/abc12345.pdf" → "abc12345.pdf"
     * </p>
     *
     * @param fileUrl 文件 URL
     * @return 文件名
     */
    private String extractFileName(String fileUrl) {
        if (fileUrl == null) return "resume.pdf";
        int lastSlash = fileUrl.lastIndexOf('/');
        return lastSlash >= 0 ? fileUrl.substring(lastSlash + 1) : fileUrl;
    }

    /**
     * 根据文件扩展名检测文件类型。
     *
     * @param fileUrl 文件 URL
     * @return 文件类型：0=Other, 1=PDF, 2=Word
     */
    private int detectFileType(String fileUrl) {
        if (fileUrl == null) return 0;
        String lower = fileUrl.toLowerCase();
        if (lower.endsWith(".pdf")) return 1;
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return 2;
        return 0;
    }

    // ================================================================
    // 私有辅助方法 — 认证用户信息
    // ================================================================

    /**
     * 从 Spring Security 上下文获取当前登录用户名。
     * <p>
     * 如果未认证或为匿名用户，返回 {@code "system"} 作为回退值。
     * </p>
     *
     * @return 当前用户名
     */
    private String getCurrentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "system";
    }

    /**
     * 从 Spring Security 上下文获取当前登录用户 ID。
     * <p>
     * 用户 ID 存储在认证凭证（credentials）中，为字符串形式的数字。
     * 如果未认证或解析失败，返回 {@code null}。
     * </p>
     *
     * @return 当前用户 ID，未认证时返回 null
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String) {
            try {
                return Long.valueOf((String) auth.getCredentials());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // ================================================================
    // 私有辅助方法 — VO 转换
    // ================================================================

    /**
     * 将 {@link ReferralProgram} 实体转换为 {@link ReferralProgramVO}。
     *
     * @param entity 计划实体
     * @return 计划 VO
     */
    private ReferralProgramVO toProgramVO(ReferralProgram entity) {
        ReferralProgramVO vo = new ReferralProgramVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setBonusAmount(entity.getBonusAmount());
        vo.setBonusStructure(entity.getBonusStructure());
        vo.setStartDate(entity.getStartDate());
        vo.setEndDate(entity.getEndDate());
        vo.setEligibleDeptIds(entity.getEligibleDeptIds());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setCreateBy(entity.getCreateBy());
        vo.setUpdateBy(entity.getUpdateBy());
        return vo;
    }

    /**
     * 将 {@link ReferralRecord} 实体转换为 {@link ReferralRecordVO}。
     * <p>
     * 注意：此方法不填充候选人姓名、职位、部门等展示字段。
     * 需要这些字段的场景应使用 SQL JOIN 查询直接返回 VO（参见
     * {@link ReferralRecordMapper#findByReferrerIdWithDetails} 和
     * {@link ReferralRecordMapper#selectPageWithQuery}）。
     * </p>
     *
     * @param entity 内推记录实体
     * @return 内推记录 VO
     */
    private ReferralRecordVO toRecordVO(ReferralRecord entity) {
        ReferralRecordVO vo = new ReferralRecordVO();
        vo.setId(entity.getId());
        vo.setProgramId(entity.getProgramId());
        vo.setProgramJobId(entity.getProgramJobId());
        vo.setReferrerId(entity.getReferrerId());
        vo.setCandidateId(entity.getCandidateId());
        vo.setJobPositionId(entity.getJobPositionId());
        vo.setRelationship(entity.getRelationship());
        vo.setReferralNote(entity.getReferralNote());
        vo.setStatus(entity.getStatus());
        vo.setBonusStatus(entity.getBonusStatus());
        vo.setBonusAmount(entity.getBonusAmount());
        vo.setBonusPaid(entity.getBonusPaid());
        vo.setHiredTime(entity.getHiredTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * 将 {@link RefProgramJob} 实体转换为 {@link RefProgramJobVO}。
     *
     * @param entity 计划职位实体
     * @return 计划职位 VO
     */
    private RefProgramJobVO toProgramJobVO(RefProgramJob entity) {
        RefProgramJobVO vo = new RefProgramJobVO();
        vo.setId(entity.getId());
        vo.setProgramId(entity.getProgramId());
        vo.setJobPositionId(entity.getJobPositionId());
        vo.setIsEnabled(entity.getIsEnabled());
        vo.setBonusAmount(entity.getBonusAmount());
        vo.setTag(entity.getTag());
        vo.setJobTitle(entity.getJobTitle());
        vo.setMinSalary(entity.getMinSalary());
        vo.setMaxSalary(entity.getMaxSalary());
        vo.setHeadCount(entity.getHeadCount());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    // ================================================================
    // 私有辅助方法 — 类型安全转换
    // ================================================================

    /**
     * Map 值转 Long 的安全辅助方法。
     *
     * @param val Map 中取出的值
     * @return Long 值，null 输入返回 null
     */
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.valueOf(val.toString());
    }

    /**
     * Map 值转 Integer 的安全辅助方法。
     *
     * @param val Map 中取出的值
     * @return Integer 值，null 输入返回 null
     */
    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.valueOf(val.toString());
    }

    /**
     * Map 值转 BigDecimal 的安全辅助方法。
     *
     * @param val Map 中取出的值
     * @return BigDecimal 值，null 输入返回 null
     */
    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }
}
