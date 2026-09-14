package com.smartrecruit.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.recruitment.entity.Application;
import com.smartrecruit.recruitment.entity.Candidate;
import com.smartrecruit.recruitment.entity.JobPosition;
import com.smartrecruit.recruitment.enums.RecruitmentEnums;
import com.smartrecruit.recruitment.repository.ApplicationMapper;
import com.smartrecruit.recruitment.repository.CandidateMapper;
import com.smartrecruit.recruitment.repository.JobPositionMapper;
import com.smartrecruit.recruitment.service.ActivityFeedService;
import com.smartrecruit.recruitment.service.ApplicationService;
import com.smartrecruit.recruitment.service.WorkbenchTaskService;
import com.smartrecruit.recruitment.util.UserContextUtil;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link ApplicationService} 的实现类。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final CandidateMapper candidateMapper;
    private final JobPositionMapper jobPositionMapper;
    private final ActivityFeedService activityFeedService;
    private final WorkbenchTaskService workbenchTaskService;

    /** 创建记录。 */
    @Override
    @Transactional
    public Application create(Long candidateId, Long jobId) {
        Application app = new Application();
        app.setCandidateId(candidateId);
        app.setJobId(jobId);
        app.setStage(RecruitmentEnums.ApplicationStage.RESUME_SCREEN.getCode());
        app.setApplyAt(DateUtils.now());
        app.setCreatedAt(DateUtils.now());
        applicationMapper.insert(app);
        log.info("Application created: id={}, candidateId={}, jobId={}", app.getId(), candidateId, jobId);

        // 记录活动动态
        try {
            Candidate candidate = candidateMapper.selectById(candidateId);
            JobPosition job = jobPositionMapper.selectById(jobId);
            String candidateName = candidate != null ? candidate.getName() : "未知";
            String jobTitle = job != null ? job.getTitle() : "未知职位";
            activityFeedService.recordActivity(
                    RecruitmentEnums.ActivityType.APPLY,
                    candidateName + " 投递了「" + jobTitle + "」",
                    candidateName + " 投递了职位「" + jobTitle + "」，简历已进入初筛阶段。",
                    0L, "system",
                    "APPLICATION",
                    app.getId());
        } catch (Exception e) {
            log.warn("Failed to record activity feed for application creation: {}", e.getMessage());
        }

        // 生成待办：筛选简历
        try {
            Candidate candidate = candidateMapper.selectById(candidateId);
            JobPosition job = jobPositionMapper.selectById(jobId);
            String candidateName = candidate != null ? candidate.getName() : "未知";
            String jobTitle = job != null ? job.getTitle() : "未知职位";
            workbenchTaskService.createTask(
                    UserContextUtil.getUserIdOrDefault(0L),
                    "筛选简历 - " + candidateName,
                    candidateName + " 投递了职位「" + jobTitle + "」，请尽快筛选简历。",
                    0, 1, "APPLICATION", app.getId(),
                    candidateName,
                    DateUtils.now().plusDays(1));
        } catch (Exception e) {
            log.warn("Failed to create workbench task for application: {}", e.getMessage());
        }

        return app;
    }

    /** 根据候选人与职位查询申请记录。 */
    @Override
    public Application findByCandidateAndJob(Long candidateId, Long jobId) {
        List<Application> list = applicationMapper.selectList(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getCandidateId, candidateId)
                        .eq(Application::getJobId, jobId)
                        .orderByDesc(Application::getCreatedAt)
                        .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询申请记录，不存在则创建。 */
    @Override
    @Transactional
    public Application findOrCreate(Long candidateId, Long jobId) {
        Application existing = findByCandidateAndJob(candidateId, jobId);
        if (existing != null) {
            return existing;
        }
        return create(candidateId, jobId);
    }

    /** 更新申请阶段。 */
    @Override
    @Transactional
    public void updateStage(Long id, Integer stage) {
        applicationMapper.updateStage(id, String.valueOf(stage));
        log.info("Application stage updated: id={}, stage={}", id, stage);
    }

    /** 查询候选人最新一条申请记录。 */
    @Override
    public Application findLatestByCandidate(Long candidateId) {
        List<Application> list = applicationMapper.selectList(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getCandidateId, candidateId)
                        .orderByDesc(Application::getCreatedAt)
                        .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }
}
