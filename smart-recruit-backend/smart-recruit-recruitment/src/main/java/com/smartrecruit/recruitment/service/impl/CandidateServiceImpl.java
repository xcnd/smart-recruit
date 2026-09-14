package com.smartrecruit.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.enums.RecruitmentEnums;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.exception.ValidationException;
import com.smartrecruit.recruitment.converter.CandidateConverter;
import com.smartrecruit.recruitment.dto.request.AiScreenRequest;
import com.smartrecruit.recruitment.dto.request.BatchTransitionRequest;
import com.smartrecruit.recruitment.dto.request.CandidatePageQuery;
import com.smartrecruit.recruitment.dto.request.CreateCandidateRequest;
import com.smartrecruit.recruitment.dto.request.NotificationRequest;
import com.smartrecruit.recruitment.dto.request.UpdateCandidateRequest;
import com.smartrecruit.recruitment.dto.response.AiScreeningResultVO;
import com.smartrecruit.recruitment.dto.response.CandidateDetailVO;
import com.smartrecruit.recruitment.dto.response.CandidateStatsVO;
import com.smartrecruit.recruitment.dto.response.CandidateVO;
import com.smartrecruit.recruitment.dto.response.ResumeVO;
import com.smartrecruit.recruitment.dto.response.StageHistoryVO;
import com.smartrecruit.recruitment.repository.CandidateMapper;
import com.smartrecruit.recruitment.repository.CandidateStageHistoryMapper;
import com.smartrecruit.recruitment.repository.ResumeMapper;
import com.smartrecruit.recruitment.repository.JobPositionMapper;
import com.smartrecruit.recruitment.service.ApplicationService;
import com.smartrecruit.recruitment.domain.ParsedResume;
import com.smartrecruit.recruitment.entity.Candidate;
import com.smartrecruit.recruitment.entity.CandidateStageHistory;
import com.smartrecruit.recruitment.entity.Application;
import com.smartrecruit.recruitment.entity.JobPosition;
import com.smartrecruit.recruitment.entity.Resume;
import com.smartrecruit.common.constant.NotificationConstants;
import com.smartrecruit.recruitment.converter.ResumeConverter;
import com.smartrecruit.recruitment.service.ActivityFeedService;
import com.smartrecruit.recruitment.service.CandidateService;
import com.smartrecruit.recruitment.service.WorkbenchTaskService;
import com.smartrecruit.recruitment.feign.SystemNotificationClient;
import com.smartrecruit.recruitment.util.UserContextUtil;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@link CandidateService}的实现类，负责候选人管理和AI筛选。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    // 0=新候选人,1=筛选中,2=筛选通过,3=面试中,4=已发Offer,5=已录用,6=已拒绝,7=已撤回
    private static final Set<Integer> VALID_STAGES = Set.of(
            RecruitmentEnums.CandidateStatus.NEW.getCode(),
            RecruitmentEnums.CandidateStatus.SCREENING.getCode(),
            RecruitmentEnums.CandidateStatus.SCREEN_PASSED.getCode(),
            RecruitmentEnums.CandidateStatus.INTERVIEWING.getCode(),
            RecruitmentEnums.CandidateStatus.OFFERED.getCode(),
            RecruitmentEnums.CandidateStatus.HIRED.getCode(),
            RecruitmentEnums.CandidateStatus.REJECTED.getCode(),
            RecruitmentEnums.CandidateStatus.WITHDRAWN.getCode());

    private final CandidateMapper candidateMapper;
    private final ResumeMapper resumeMapper;
    private final JobPositionMapper jobPositionMapper;
    private final CandidateStageHistoryMapper stageHistoryMapper;
    private final CandidateConverter candidateConverter;
    private final ResumeConverter resumeConverter;
    private final ApplicationService applicationService;
    private final ActivityFeedService activityFeedService;
    private final WorkbenchTaskService workbenchTaskService;
    private final SystemNotificationClient systemNotificationClient;

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<CandidateVO> pageQuery(CandidatePageQuery query) {
        Page<Candidate> page = new Page<>(query.getPage(), query.getSize());
        IPage<Candidate> result = candidateMapper.pageQuery(
                page,
                query.getName(),
                query.getKeyword(),
                query.getStage(),
                query.getSource(),
                query.getEducation(),
                query.getAiMatchScoreMin(),
                query.getAiMatchScoreMax(),
                query.getExperienceMin(),
                query.getExperienceMax(),
                query.getCity(),
                query.getApplyDateStart(),
                query.getApplyDateEnd(),
                query.getScreeningStatus());

        // Batch-load resume -> job info for each candidate
        List<Long> candidateIds = result.getRecords().stream()
                .map(Candidate::getId)
                .toList();
        Map<Long, List<Resume>> resumeMap = candidateIds.isEmpty() ? Map.of() :
                resumeMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Resume>()
                        .in(Resume::getCandidateId, candidateIds))
                .stream()
                .collect(Collectors.groupingBy(Resume::getCandidateId));

        List<Long> jobIds = resumeMap.values().stream()
                .flatMap(List::stream)
                .map(Resume::getJobPositionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, JobPosition> jobMap = jobIds.isEmpty() ? Map.of() :
                jobPositionMapper.selectBatchIds(jobIds).stream()
                        .collect(Collectors.toMap(JobPosition::getId, j -> j));

        return PageResult.from(result).map(entity -> {
            CandidateVO vo = candidateConverter.toVO(entity);
            List<Resume> resumes = resumeMap.getOrDefault(entity.getId(), List.of());
            if (!resumes.isEmpty()) {
                Resume firstResume = resumes.get(0);
                if (firstResume.getJobPositionId() != null) {
                    vo.setJobId(firstResume.getJobPositionId());
                    JobPosition job = jobMap.get(firstResume.getJobPositionId());
                    if (job != null) {
                        vo.setJobTitle(job.getTitle());
                    }
                }
                if (firstResume.getAiMatchScore() != null) {
                    vo.setAiMatchScore(firstResume.getAiMatchScore().intValue());
                }
                vo.setResumeUrl(firstResume.getFileUrl());
                // 如果候选人表中的邮箱/手机为空或占位符，从 ParsedResume 中补全
                fillContactFromParsedResume(vo, firstResume);
            }
            return vo;
        });
    }

    /**
     * 如果候选人表中的邮箱或手机为空/占位符，从简历的 ParsedResume 解析结果中补全。
     * 这样即使 enrichment pipeline 写入失败，候选人中心也能正确显示联系信息。
     */
    private void fillContactFromParsedResume(CandidateVO vo, Resume resume) {
        ParsedResume parsed = resume.getParsedContent();
        if (parsed == null) return;
        if (isPlaceholderContact(vo.getEmail()) && parsed.getEmail() != null
                && !parsed.getEmail().isEmpty()) {
            vo.setEmail(parsed.getEmail());
        }
        if (isPlaceholderContact(vo.getPhone()) && parsed.getPhone() != null
                && !parsed.getPhone().isEmpty()) {
            vo.setPhone(parsed.getPhone());
        }
    }

    private boolean isPlaceholderContact(String value) {
        return value == null || value.isEmpty()
                || value.contains("@placeholder.local");
    }

    /** 根据主键查询详情。 */
    @Override
    public CandidateDetailVO getById(Long id) {
        Candidate entity = candidateMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Candidate", id);
        }
        CandidateDetailVO detailVO = candidateConverter.toDetailVO(entity);

        // 关联简历信息
        List<Resume> resumes = resumeMapper.findByCandidateId(id);
        List<ResumeVO> resumeVOs = resumeConverter.toVOList(resumes);
        // 将 aiMatchScore 映射到 VO 的 matchScore（字段名不同，MapStruct 无法自动映射）
        for (int i = 0; i < resumes.size(); i++) {
            Resume r = resumes.get(i);
            ResumeVO vo = resumeVOs.get(i);
            if (r.getAiMatchScore() != null) {
                vo.setMatchScore(r.getAiMatchScore());
            }
        }
        detailVO.setResumes(resumeVOs);

        // 设置最新的简历URL和职位信息
        if (!resumes.isEmpty()) {
            Resume firstResume = resumes.get(0);
            detailVO.setResumeUrl(firstResume.getFileUrl());
            if (firstResume.getJobPositionId() != null) {
                detailVO.setJobId(firstResume.getJobPositionId());
                JobPosition job = jobPositionMapper.selectById(firstResume.getJobPositionId());
                if (job != null) {
                    detailVO.setJobTitle(job.getTitle());
                }
            }
            if (firstResume.getAiMatchScore() != null) {
                detailVO.setAiMatchScore(firstResume.getAiMatchScore().intValue());
            }
            // 如果候选人表中的邮箱/手机为空或占位符，从 ParsedResume 补全
            ParsedResume parsed = firstResume.getParsedContent();
            if (parsed != null) {
                if (isPlaceholderContact(detailVO.getEmail()) && parsed.getEmail() != null
                        && !parsed.getEmail().isEmpty()) {
                    detailVO.setEmail(parsed.getEmail());
                }
                if (isPlaceholderContact(detailVO.getPhone()) && parsed.getPhone() != null
                        && !parsed.getPhone().isEmpty()) {
                    detailVO.setPhone(parsed.getPhone());
                }
                // 计算历史跳槽次数与平均在职时长（供 AI 留任预测综合评估）
                if (parsed.getExperience() != null && !parsed.getExperience().isEmpty()) {
                    int expCount = parsed.getExperience().size();
                    detailVO.setPreviousJobCount(Math.max(0, expCount - 1)); // 排除当前任职
                    double totalMonths = 0;
                    int counted = 0;
                    for (ParsedResume.ExperienceEntry entry : parsed.getExperience()) {
                        Double months = monthsBetween(entry.getStart(), entry.getEnd());
                        if (months != null) {
                            totalMonths += months;
                            counted++;
                        }
                    }
                    if (counted > 0) {
                        detailVO.setAvgTenureMonths(Math.round(totalMonths / counted * 10.0) / 10.0);
                    }
                }
            }
        }

        return detailVO;
    }

    /** 计算两段工作经历之间的月数；结束时间为"至今"或缺失时以当前时间为准。 */
    private Double monthsBetween(String start, String end) {
        YearMonth s = parseYearMonth(start);
        if (s == null) {
            return null;
        }
        YearMonth e = parseYearMonth(end);
        if (e == null) {
            e = YearMonth.now();
        }
        long months = ChronoUnit.MONTHS.between(s, e);
        return months > 0 ? (double) months : null;
    }

    /** 解析 "2020.03" / "2020-03" / "2020年3月" / "2020" 形式的年月；"至今"返回 null。 */
    private YearMonth parseYearMonth(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim().toLowerCase();
        if (t.contains("至今") || t.contains("present") || t.contains("now")) {
            return null;
        }
        Matcher m = Pattern.compile("(\\d{4})[.\\-–—/年]?\\s*(\\d{1,2})?").matcher(t);
        if (m.find()) {
            int year = Integer.parseInt(m.group(1));
            int month = 1;
            if (m.group(2) != null) {
                month = Integer.parseInt(m.group(2));
            }
            if (month < 1 || month > 12) {
                month = 1;
            }
            return YearMonth.of(year, month);
        }
        return null;
    }

    /** 创建记录。 */
    @Override
    @Transactional
    public CandidateVO create(CreateCandidateRequest request) {
        Candidate entity = candidateConverter.toEntity(request);
        entity.setCurrentStage(RecruitmentEnums.CandidateStatus.NEW.getCode());
        candidateMapper.insert(entity);
        log.info("Candidate created: id={}, name={}", entity.getId(), entity.getName());
        return candidateConverter.toVO(entity);
    }

    /** 更新记录。 */
    @Override
    @Transactional
    public CandidateVO update(Long id, UpdateCandidateRequest request) {
        Candidate entity = candidateMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Candidate", id);
        }
        candidateConverter.updateEntity(entity, request);
        entity.setLastActiveTime(DateUtils.now());
        candidateMapper.updateById(entity);
        log.info("Candidate updated: id={}, name={}", entity.getId(), entity.getName());
        return candidateConverter.toVO(entity);
    }

    /** 根据主键删除记录。 */
    @Override
    @Transactional
    public void delete(Long id) {
        Candidate entity = candidateMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Candidate", id);
        }
        candidateMapper.deleteById(id);
        log.info("Candidate deleted: id={}, name={}", entity.getId(), entity.getName());
    }

    /** 更新候选人阶段并记录阶段流转历史。 */
    @Override
    @Transactional
    public void updateStage(Long id, Integer stage) {
        if (!VALID_STAGES.contains(stage)) {
            throw new ValidationException(
                    "Invalid stage '" + stage + "'. Valid values: " + VALID_STAGES);
        }
        Candidate entity = candidateMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Candidate", id);
        }
        Integer oldStage = entity.getCurrentStage();
        entity.setCurrentStage(stage);
        entity.setLastActiveTime(DateUtils.now());
        candidateMapper.updateById(entity);

        // 同步更新关联的求职申请阶段
        Long applicationId = syncApplicationStage(id, stage);

        // Record stage history
        CandidateStageHistory history = new CandidateStageHistory();
        history.setCandidateId(id);
        history.setApplicationId(applicationId != null ? applicationId : 0L);
        history.setFromStage(oldStage);
        history.setToStage(stage);
        history.setOperatorName("system");
        history.setOperatorId(0L);
        stageHistoryMapper.insert(history);

        // 记录活动动态
        recordStageChangeActivity(entity, oldStage, stage);

        // 生成待办任务
        maybeCreateTaskForStage(entity, stage);

        // 发送阶段变更通知（关键里程碑，接收人为当前操作人）
        maybeNotifyStageChange(entity, stage);

        log.info("Candidate stage updated: id={}, {} -> {}", id, oldStage, stage);
    }

    /**
     * 记录候选人阶段变更的活动动态。
     */
    private void recordStageChangeActivity(Candidate entity, Integer oldStage, Integer newStage) {
        String stageLabel = getStageLabel(newStage);
        String oldLabel = getStageLabel(oldStage);
        String title = entity.getName() + " 从「" + oldLabel + "」转入「" + stageLabel + "」";
        String description = "候选人 " + entity.getName() + " 的阶段已更新。";
        try {
            activityFeedService.recordActivity(
                    mapStageToActivityType(newStage),
                    title,
                    description,
                    0L, "system",
                    "CANDIDATE",
                    entity.getId());
        } catch (Exception e) {
            log.warn("Failed to record activity feed for candidate stage change: {}", e.getMessage());
        }
    }

    /**
     * 根据阶段变更生成待办任务。
     */
    private void maybeCreateTaskForStage(Candidate entity, Integer newStage) {
        try {
            if (newStage == RecruitmentEnums.CandidateStatus.SCREENING.getCode()) {
                workbenchTaskService.createTask(
                        UserContextUtil.getUserIdOrDefault(0L),
                        "筛选简历 - " + entity.getName(),
                        "候选人 " + entity.getName() + " 已进入简历筛选阶段，请尽快筛选。",
                        0, 1, "CANDIDATE", entity.getId(),
                        entity.getName(),
                        DateUtils.now().plusDays(1));
            } else if (newStage == RecruitmentEnums.CandidateStatus.SCREEN_PASSED.getCode()) {
                workbenchTaskService.createTask(
                        UserContextUtil.getUserIdOrDefault(0L),
                        "安排面试 - " + entity.getName(),
                        "候选人 " + entity.getName() + " 简历筛选已通过，请尽快安排面试。",
                        1, 0, "CANDIDATE", entity.getId(),
                        entity.getName(),
                        DateUtils.now().plusDays(3));
            } else if (newStage == RecruitmentEnums.CandidateStatus.INTERVIEWING.getCode()) {
                workbenchTaskService.createTask(
                        UserContextUtil.getUserIdOrDefault(0L),
                        "安排面试 - " + entity.getName(),
                        "候选人 " + entity.getName() + " 已进入面试阶段，请尽快安排面试。",
                        1, 0, "CANDIDATE", entity.getId(),
                        entity.getName(),
                        DateUtils.now().plusDays(3));
            } else if (newStage == RecruitmentEnums.CandidateStatus.OFFERED.getCode()) {
                workbenchTaskService.createTask(
                        UserContextUtil.getUserIdOrDefault(0L),
                        "审批Offer - " + entity.getName(),
                        "候选人 " + entity.getName() + " 已收到Offer，请尽快完成审批。",
                        2, 0, "CANDIDATE", entity.getId(),
                        entity.getName(),
                        DateUtils.now().plusDays(5));
            } else if (newStage == RecruitmentEnums.CandidateStatus.HIRED.getCode()) {
                // 入职时需要办理入职手续
                workbenchTaskService.createTask(
                        UserContextUtil.getUserIdOrDefault(0L),
                        "办理入职 - " + entity.getName(),
                        "候选人 " + entity.getName() + " 已通过，请尽快办理入职手续。",
                        3, 0, "CANDIDATE", entity.getId(),
                        entity.getName(),
                        DateUtils.now().plusDays(7));
            }
        } catch (Exception e) {
            log.warn("Failed to create workbench task for candidate stage: {}", e.getMessage());
        }
    }

    /**
     * 候选人关键里程碑阶段变更时发送站内通知（best-effort，失败不影响主流程）。
     *
     * <p>接收人为当前操作人，保证执行筛选/流转的管理账号一定能收到消息。</p>
     */
    private void maybeNotifyStageChange(Candidate entity, Integer newStage) {
        try {
            if (newStage == null) {
                return;
            }
            String title;
            String content;
            // 阶段码：2=筛选通过, 3=面试中, 4=已发Offer, 5=已录用
            switch (newStage) {
                case 2 -> {
                    title = "简历筛选通过 - " + entity.getName();
                    content = "候选人 " + entity.getName() + " 简历筛选已通过，请尽快安排面试。";
                }
                case 3 -> {
                    title = "进入面试阶段 - " + entity.getName();
                    content = "候选人 " + entity.getName() + " 已进入面试阶段，请尽快安排面试。";
                }
                case 4 -> {
                    title = "已发 Offer - " + entity.getName();
                    content = "候选人 " + entity.getName() + " 已收到 Offer，请尽快完成审批。";
                }
                case 5 -> {
                    title = "候选人已入职 - " + entity.getName();
                    content = "候选人 " + entity.getName() + " 已完成入职，请尽快办理入职手续。";
                }
                default -> {
                    return;
                }
            }
            sendNotification(UserContextUtil.getUserIdOrDefault(0L), title, content,
                    NotificationConstants.TYPE_RECRUITMENT, "CANDIDATE_STAGE",
                    entity.getId(), "/candidates/" + entity.getId());
        } catch (Exception e) {
            log.warn("候选人阶段变更通知发送失败（可忽略）: candidateId={}, stage={}, error={}",
                    entity.getId(), newStage, e.getMessage());
        }
    }

    /**
     * 发送站内通知（best-effort，失败不影响主流程）。
     */
    private void sendNotification(Long userId, String title, String content, int type,
                                  String businessType, Long businessId, String actionUrl) {
        if (userId == null || userId == 0L) {
            log.debug("通知接收人为空，跳过: title={}", title);
            return;
        }
        try {
            NotificationRequest request = new NotificationRequest();
            request.setUserId(userId);
            request.setTitle(title);
            request.setContent(content);
            request.setType(type);
            request.setBusinessType(businessType);
            request.setBusinessId(businessId);
            request.setActionUrl(actionUrl);
            systemNotificationClient.send(request);
        } catch (Exception e) {
            log.warn("站内通知发送失败（可忽略）: userId={}, title={}, error={}",
                    userId, title, e.getMessage());
        }
    }

    private static String getStageLabel(Integer stage) {
        if (stage == null) return "未知";
        RecruitmentEnums.CandidateStatus status = RecruitmentEnums.CandidateStatus.fromCode(stage);
        return status != null ? status.getLabel() : String.valueOf(stage);
    }

    private static RecruitmentEnums.ActivityType mapStageToActivityType(Integer stage) {
        if (stage == null) return RecruitmentEnums.ActivityType.SYSTEM;
        return switch (stage) {
            case 0, 1, 2 -> RecruitmentEnums.ActivityType.SCREEN;
            case 3 -> RecruitmentEnums.ActivityType.INTERVIEW;
            case 4 -> RecruitmentEnums.ActivityType.OFFER;
            case 5 -> RecruitmentEnums.ActivityType.HIRE;
            default -> RecruitmentEnums.ActivityType.SYSTEM;
        };
    }

    /** 批量流转阶段。 */
    @Override
    @Transactional
    public void batchTransition(BatchTransitionRequest request) {
        Integer targetStage = request.getTargetStage();
        if (!VALID_STAGES.contains(targetStage)) {
            throw new ValidationException(
                    "Invalid target stage '" + targetStage + "'. Valid values: " + VALID_STAGES);
        }

        List<Long> candidateIds = request.getCandidateIds();
        if (candidateIds == null || candidateIds.isEmpty()) {
            return;
        }

        // Capture old stages before bulk update
        List<Candidate> candidates = candidateMapper.selectBatchIds(candidateIds);
        Map<Long, Integer> oldStageMap = new HashMap<>();
        for (Candidate c : candidates) {
            oldStageMap.put(c.getId(), c.getCurrentStage());
        }

        // Bulk update using LambdaUpdateWrapper
        LambdaUpdateWrapper<Candidate> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Candidate::getCurrentStage, targetStage)
                .set(Candidate::getLastActiveTime, DateUtils.now())
                .in(Candidate::getId, candidateIds);
        candidateMapper.update(updateWrapper);

        // Write stage history for each candidate
        for (Candidate candidate : candidates) {
            Long applicationId = syncApplicationStage(candidate.getId(), targetStage);
            CandidateStageHistory history = new CandidateStageHistory();
            history.setCandidateId(candidate.getId());
            history.setApplicationId(applicationId != null ? applicationId : 0L);
            history.setFromStage(oldStageMap.get(candidate.getId()));
            history.setToStage(targetStage);
            history.setOperatorName("system");
            history.setOperatorId(0L);
            stageHistoryMapper.insert(history);

            // Record activity feed for batch transition
            try {
                String name = candidate.getName();
                String oldLabel = getStageLabel(oldStageMap.get(candidate.getId()));
                String newLabel = getStageLabel(targetStage);
                activityFeedService.recordActivity(
                        mapStageToActivityType(targetStage),
                        name + " 从「" + oldLabel + "」转入「" + newLabel + "」",
                        "候选人 " + name + " 的阶段已通过批量操作更新。",
                        0L, "system", "CANDIDATE", candidate.getId());
                maybeCreateTaskForStage(candidate, targetStage);
                maybeNotifyStageChange(candidate, targetStage);
            } catch (Exception e) {
                log.warn("Failed to record activity/task for batch candidate {}: {}", candidate.getId(), e.getMessage());
            }
        }

        log.info("Batch transition: {} candidates to stage={}", candidates.size(), targetStage);
    }

    /** 查询阶段流转历史。 */
    @Override
    public List<StageHistoryVO> getStageHistory(Long candidateId) {
        List<CandidateStageHistory> historyList = stageHistoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CandidateStageHistory>()
                        .eq(CandidateStageHistory::getCandidateId, candidateId)
                        .orderByDesc(CandidateStageHistory::getCreateTime));
        return candidateConverter.toStageHistoryVOList(historyList);
    }

    /** 查询统计信息。 */
    @Override
    public CandidateStatsVO getStats() {
        int newCount = 0, screeningCount = 0, screenPassedCount = 0, interviewingCount = 0;
        int offeredCount = 0, hiredCount = 0, rejectedCount = 0, withdrawnCount = 0;

        // Stage distribution
        List<Map<String, Object>> stageDist = candidateMapper.stageDistribution();
        for (Map<String, Object> row : stageDist) {
            Object stageObj = row.get("stage");
            Object countObj = row.get("count");
            if (stageObj == null || countObj == null) continue;
            int stage = Integer.parseInt(stageObj.toString());
            int count = Integer.parseInt(countObj.toString());
            switch (stage) {
                case 0 -> newCount = count;
                case 1 -> screeningCount = count;
                case 2 -> screenPassedCount = count;
                case 3 -> interviewingCount = count;
                case 4 -> offeredCount = count;
                case 5 -> hiredCount = count;
                case 6 -> rejectedCount = count;
                case 7 -> withdrawnCount = count;
            }
        }
        int totalCount = newCount + screeningCount + screenPassedCount + interviewingCount
                + offeredCount + hiredCount + rejectedCount + withdrawnCount;

        // Source distribution
        Map<String, Long> sourceDistMap = new LinkedHashMap<>();
        List<Map<String, Object>> sourceDist = candidateMapper.sourceDistribution();
        for (Map<String, Object> row : sourceDist) {
            Object srcObj = row.get("source");
            Object countObj = row.get("count");
            if (srcObj == null || countObj == null) continue;
            sourceDistMap.put(srcObj.toString(), Long.parseLong(countObj.toString()));
        }

        // Monthly trend
        List<CandidateStatsVO.MonthlyTrend> trends = new ArrayList<>();
        List<Map<String, Object>> monthlyData = candidateMapper.monthlyTrend(12);
        for (Map<String, Object> row : monthlyData) {
            Object monthObj = row.get("month");
            Object countObj = row.get("count");
            if (monthObj == null || countObj == null) continue;
            trends.add(CandidateStatsVO.MonthlyTrend.builder()
                    .month(monthObj.toString())
                    .count(Long.parseLong(countObj.toString()))
                    .build());
        }

        return CandidateStatsVO.builder()
                .totalCount(totalCount)
                .newCount(newCount)
                .screeningCount(screeningCount)
                .screenPassedCount(screenPassedCount)
                .interviewingCount(interviewingCount)
                .offeredCount(offeredCount)
                .hiredCount(hiredCount)
                .rejectedCount(rejectedCount)
                .withdrawnCount(withdrawnCount)
                .sourceDistribution(sourceDistMap)
                .monthlyTrend(trends)
                .build();
    }

    /** 执行 AI 智能筛选。 */
    @Override
    public List<AiScreeningResultVO> aiScreen(AiScreenRequest request) {
        log.info("AI screening requested: resumeCount={}, jobId={}",
                request.getResumeIds().size(), request.getJobId());

        return request.getResumeIds().stream()
                .map(resumeId -> simulateAiScreen(resumeId, request.getKeywords()))
                .collect(Collectors.toList());
    }

    /**
     * 同步更新候选人关联的求职申请阶段。
     *
     * @param candidateId    候选人 ID
     * @param candidateStage 候选人新阶段码
     * @return 申请记录 ID，如果该候选人尚无申请记录则返回 {@code null}
     */
    private Long syncApplicationStage(Long candidateId, Integer candidateStage) {
        Integer appStage = mapToApplicationStage(candidateStage);
        if (appStage == null) {
            return null;
        }
        Application app = applicationService.findLatestByCandidate(candidateId);
        if (app == null) {
            return null;
        }
        applicationService.updateStage(app.getId(), appStage);
        return app.getId();
    }

    /**
     * 将候选人阶段码（CandidateStatus）映射为求职申请阶段码（ApplicationStage）。
     */
    private static Integer mapToApplicationStage(Integer candidateStage) {
        if (candidateStage == null) return null;
        if (candidateStage == RecruitmentEnums.CandidateStatus.NEW.getCode()
                || candidateStage == RecruitmentEnums.CandidateStatus.SCREENING.getCode()
                || candidateStage == RecruitmentEnums.CandidateStatus.SCREEN_PASSED.getCode()) {
            return RecruitmentEnums.ApplicationStage.RESUME_SCREEN.getCode();
        }
        if (candidateStage == RecruitmentEnums.CandidateStatus.INTERVIEWING.getCode()) {
            return RecruitmentEnums.ApplicationStage.INTERVIEW.getCode();
        }
        if (candidateStage == RecruitmentEnums.CandidateStatus.OFFERED.getCode()) {
            return RecruitmentEnums.ApplicationStage.OFFER.getCode();
        }
        if (candidateStage == RecruitmentEnums.CandidateStatus.HIRED.getCode()) {
            return RecruitmentEnums.ApplicationStage.ONBOARDING.getCode();
        }
        return null; // REJECTED, WITHDRAWN 不映射
    }

    /**
     * 模拟AI筛选，通过生成随机分数并结合简历内容进行简单的关键词匹配。
     */
    private AiScreeningResultVO simulateAiScreen(Long resumeId, List<String> keywords) {
        Resume resume = resumeMapper.selectById(resumeId);
        String candidateName = "Unknown";

        if (resume != null && resume.getCandidateId() != null) {
            Candidate candidate = candidateMapper.selectById(resume.getCandidateId());
            if (candidate != null) {
                candidateName = candidate.getName();
            }
        }

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int overallScore = rand.nextInt(50, 96);

        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("skillMatch", rand.nextInt(40, 96));
        dimensionScores.put("experienceMatch", rand.nextInt(40, 96));
        dimensionScores.put("educationMatch", rand.nextInt(40, 96));
        dimensionScores.put("stabilityScore", rand.nextInt(40, 96));
        dimensionScores.put("overallFit", overallScore);

        List<String> matchedKeywords = new ArrayList<>();
        List<String> missingKeywords = new ArrayList<>();

        if (keywords != null && !keywords.isEmpty() && resume != null
                && resume.getParsedContent() != null) {
            String contentStr = resume.getParsedContent().toString().toLowerCase();
            for (String kw : keywords) {
                if (contentStr.contains(kw.toLowerCase())) {
                    matchedKeywords.add(kw);
                } else {
                    missingKeywords.add(kw);
                }
            }
        }

        String recommendation;
        if (overallScore >= 80) {
            recommendation = "STRONG_MATCH";
        } else if (overallScore >= 65) {
            recommendation = "MATCH";
        } else if (overallScore >= 50) {
            recommendation = "WEAK_MATCH";
        } else {
            recommendation = "NO_MATCH";
        }

        return AiScreeningResultVO.builder()
                .resumeId(resumeId)
                .candidateId(resume != null ? resume.getCandidateId() : null)
                .candidateName(candidateName)
                .overallScore(overallScore)
                .dimensionScores(dimensionScores)
                .matchedKeywords(matchedKeywords)
                .missingKeywords(missingKeywords)
                .recommendation(1)
                .build();
    }
}
