package com.smartrecruit.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.recruitment.enums.RecruitmentEnums;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.exception.ValidationException;
import com.smartrecruit.recruitment.converter.JobConverter;
import com.smartrecruit.recruitment.dto.request.AiGenerateJdRequest;
import com.smartrecruit.recruitment.dto.request.CreateJobRequest;
import com.smartrecruit.recruitment.dto.request.JobPageQuery;
import com.smartrecruit.recruitment.dto.request.UpdateJobRequest;
import com.smartrecruit.recruitment.dto.response.DepartmentJobStatVO;
import com.smartrecruit.recruitment.dto.response.JobDetailVO;
import com.smartrecruit.recruitment.dto.response.JobStatsVO;
import com.smartrecruit.recruitment.dto.response.JobVO;
import com.smartrecruit.recruitment.dto.response.JdGenerateVO;
import com.smartrecruit.recruitment.dto.response.JdGenerateTaskVO;
import com.smartrecruit.recruitment.dto.response.LevelJobStatVO;
import com.smartrecruit.recruitment.repository.JobPositionMapper;
import com.smartrecruit.recruitment.dto.request.JdGenerateRequest;
import com.smartrecruit.recruitment.entity.JobPosition;
import com.smartrecruit.recruitment.service.JobService;
import com.smartrecruit.recruitment.feign.AiAgentCapabilityClient;
import com.smartrecruit.recruitment.util.UserContextUtil;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * {@link JobService}的实现类，负责职位的CRUD操作和统计数据。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    /** AI 引擎执行器（限制并发，避免 LLM API 过载）。 */
    @Autowired
    @Qualifier("aiEngineExecutor")
    private Executor aiEngineExecutor;

    /** 异步 AI JD 生成任务存储：taskId → 任务。 */
    private final ConcurrentHashMap<String, JdGenerateTask> jdGenerateTasks = new ConcurrentHashMap<>();

    private final JobPositionMapper jobPositionMapper;
    private final JobConverter jobConverter;
    private final AiAgentCapabilityClient aiAgentCapabilityClient;

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<JobVO> pageQuery(JobPageQuery query) {
        Page<JobPosition> page = new Page<>(query.getPage(), query.getSize());
        IPage<JobPosition> result = jobPositionMapper.pageQuery(
                page, query.getTitle(), query.getDepartmentId(),
                query.getStatus() != null ? String.valueOf(query.getStatus()) : null,
                query.getType() != null ? String.valueOf(query.getType()) : null,
                query.getLocation(), query.getStartDate(), query.getEndDate());
        return PageResult.from(result).map(jobConverter::toVO);
    }

    /** 根据主键查询详情。 */
    @Override
    public JobDetailVO getById(Long id) {
        JobPosition entity = jobPositionMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("JobPosition", id);
        }
        return jobConverter.toDetailVO(entity);
    }

    /** 创建记录。 */
    @Override
    @Transactional
    public JobVO create(CreateJobRequest request) {
        JobPosition entity = jobConverter.toEntity(request);
        entity.setStatus(RecruitmentEnums.JobStatus.DRAFT.getCode());
        entity.setPublishedAt(DateUtils.now());
        // 记录创建人（发布人字段展示）
        entity.setCreateUserId(UserContextUtil.getUserId());
        entity.setCreatedBy(UserContextUtil.getUsername());
        // 职位描述为空时，调用 AI 引擎 JD 生成 Agent 能力（失败保持空，可手动编辑）
        if (entity.getDescription() == null || entity.getDescription().isBlank()) {
            String generated = generateJdDescription(
                    entity.getTitle(), "", levelLabel(entity.getLevel()), entity.getLocation());
            if (generated != null) {
                entity.setDescription(generated);
            }
        }
        jobPositionMapper.insert(entity);
        log.info("Job position created: id={}, title={}", entity.getId(), entity.getTitle());
        return jobConverter.toVO(entity);
    }

    /**
     * AI 生成职位描述（LLM 优先，模板兜底）。
     */
    @Override
    public JdGenerateVO generateJd(AiGenerateJdRequest request) {
        String title = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle() : "高级Java开发工程师";
        String department = request.getDepartment() == null ? "" : request.getDepartment();
        String experience = request.getExperience() != null && !request.getExperience().isBlank()
                ? request.getExperience() : "3-5年";

        // LLM 优先：调用 AI 引擎 JD 生成能力
        String generated = generateJdDescription(
                title, department, experience, request.getLocation());
        if (generated != null) {
            log.info("AI JD 生成完成(LLM): title={}", title);
            return new JdGenerateVO(title, generated);
        }

        // 兜底：本地模板生成
        log.info("AI JD 生成失败，使用本地模板: title={}", title);
        return new JdGenerateVO(title, buildJdTemplate(title, request.getSkills()));
    }

    /**
     * 异步启动 AI 职位描述生成，立即返回任务 ID。
     *
     * <p>LLM 生成耗时较长（可能数十秒），在后台线程池执行，
     * 前端通过 {@link #getJdGenerateTask(String)} 轮询结果，避免请求超时。</p>
     */
    @Override
    public String generateJdAsync(AiGenerateJdRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JdGenerateTask task = new JdGenerateTask();
        jdGenerateTasks.put(taskId, task);
        log.info("异步 AI JD 生成已提交: taskId={}, title={}", taskId, request.getTitle());

        if (aiEngineExecutor != null) {
            aiEngineExecutor.execute(() -> executeJdGenerate(taskId, request));
        } else {
            // 无线程池时同步执行（测试环境）
            executeJdGenerate(taskId, request);
        }
        return taskId;
    }

    /** 执行异步 JD 生成任务并回写状态。 */
    private void executeJdGenerate(String taskId, AiGenerateJdRequest request) {
        JdGenerateTask task = jdGenerateTasks.get(taskId);
        if (task == null) {
            return;
        }
        task.status = "PROCESSING";
        try {
            JdGenerateVO vo = generateJd(request);
            task.status = "COMPLETED";
            task.title = vo.title();
            task.description = vo.description();
            log.info("异步 AI JD 生成完成: taskId={}, title={}, descLength={}",
                    taskId, vo.title(), vo.description() == null ? 0 : vo.description().length());
            scheduleTaskCleanup(taskId);
        } catch (Exception e) {
            task.status = "FAILED";
            task.message = e.getMessage() == null ? "AI JD 生成失败" : e.getMessage();
            log.error("异步 AI JD 生成失败: taskId={}, title={}", taskId, request.getTitle(), e);
            scheduleTaskCleanup(taskId);
        }
    }

    /** 任务完成后延迟清理内存中的任务（保留 60 秒供前端轮询兜底）。 */
    private void scheduleTaskCleanup(String taskId) {
        Thread cleaner = new Thread(() -> {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            jdGenerateTasks.remove(taskId);
        }, "jd-generate-task-cleaner");
        cleaner.setDaemon(true);
        cleaner.start();
    }

    /** 查询异步 AI JD 生成任务状态与结果。 */
    @Override
    public JdGenerateTaskVO getJdGenerateTask(String taskId) {
        JdGenerateTask task = jdGenerateTasks.get(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("JD 生成任务不存在: taskId=" + taskId);
        }
        return JdGenerateTaskVO.builder()
                .taskId(taskId)
                .status(task.status)
                .message(task.message)
                .title(task.title)
                .description(task.description)
                .build();
    }

    /** 异步 JD 生成任务内部对象。 */
    private static class JdGenerateTask {
        private volatile String status = "PENDING";
        private volatile String message = "";
        private volatile String title = "";
        private volatile String description = "";
    }

    /**
     * 调用 AI 引擎 JD 生成能力（best-effort，失败返回 null）。
     */
    private String generateJdDescription(String title, String department,
                                         String experienceLevel, String location) {
        try {
            JdGenerateRequest jdRequest = new JdGenerateRequest();
            jdRequest.setJobTitle(title == null ? "" : title);
            jdRequest.setDepartment(department == null ? "" : department);
            jdRequest.setExperienceLevel(experienceLevel == null ? "3-5年" : experienceLevel);
            ApiResponse<Map<String, Object>> resp =
                    aiAgentCapabilityClient.generateJd(jdRequest);
            if (resp == null || !resp.ok() || resp.data() == null) {
                return null;
            }
            Map<String, Object> jd = resp.data();
            StringBuilder sb = new StringBuilder();
            appendSection(sb, "岗位职责", jd.get("responsibilities"));
            appendSection(sb, "任职要求", jd.get("requirements"));
            appendSection(sb, "加分项", jd.get("plusPoints"));
            if (location != null && !location.isBlank()) {
                sb.append("【工作地点】").append(location).append('\n');
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            log.warn("AI JD 生成失败，保持职位描述为空: title={}, error={}", title, e.getMessage());
            return null;
        }
    }

    /** 本地兜底 JD 模板（AI 引擎不可用时使用）。 */
    private String buildJdTemplate(String title, List<String> skills) {
        StringBuilder sb = new StringBuilder();
        sb.append("【岗位职责】\n");
        sb.append("- 负责").append(title).append("相关核心业务的设计、开发与交付，保证质量与稳定\n");
        sb.append("- 参与技术方案评审与架构演进，输出高质量设计文档\n");
        sb.append("- 与产品、测试等团队高效协作，推动项目按计划落地\n");
        sb.append("- 持续优化系统性能与工程效率，解决线上疑难问题\n\n");
        sb.append("【任职要求】\n");
        sb.append("- 本科及以上学历，计算机相关专业优先\n");
        sb.append("- 具备扎实的计算机基础与编程能力，熟悉主流技术栈\n");
        if (skills != null && !skills.isEmpty()) {
            sb.append("- 熟悉").append(String.join("、", skills)).append("等技能\n");
        }
        sb.append("- 良好的沟通协作与自我驱动力\n\n");
        sb.append("【加分项】\n");
        sb.append("- 有大型分布式系统或高并发项目经验\n");
        sb.append("- 有开源项目贡献或技术博客\n");
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, Object value) {
        if (!(value instanceof java.util.List<?> list) || list.isEmpty()) {
            return;
        }
        sb.append("【").append(title).append("】\n");
        for (Object item : list) {
            sb.append("- ").append(item).append('\n');
        }
        // 模块之间加空行，便于阅读与编辑区分
        sb.append('\n');
    }

    private String levelLabel(Integer level) {
        if (level == null) return "3-5年";
        return switch (level) {
            case 0 -> "应届生";
            case 1 -> "1-3年";
            case 2 -> "3-5年";
            case 3 -> "5-8年";
            case 4 -> "8年以上";
            default -> "3-5年";
        };
    }

    /** 更新记录。 */
    @Override
    @Transactional
    public JobVO update(Long id, UpdateJobRequest request) {
        JobPosition entity = jobPositionMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("JobPosition", id);
        }
        jobConverter.updateEntity(entity, request);
        jobPositionMapper.updateById(entity);
        log.info("Job position updated: id={}, title={}", id, entity.getTitle());
        return jobConverter.toVO(entity);
    }

    /** 根据主键删除记录。 */
    @Override
    @Transactional
    public void delete(Long id) {
        JobPosition entity = jobPositionMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("JobPosition", id);
        }
        jobPositionMapper.deleteById(id);
        log.info("Job position deleted: id={}", id);
    }

    /** 更新职位发布状态。 */
    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        JobPosition entity = jobPositionMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("JobPosition", id);
        }
        // 直接接收职位状态编码：0=DRAFT,1=PUBLISHED,2=PAUSED,3=CLOSED
        RecruitmentEnums.JobStatus target = RecruitmentEnums.JobStatus.fromCode(status);
        if (target == null) {
            throw new ValidationException("非法职位状态: " + status);
        }
        entity.setStatus(target.getCode());
        if (target == RecruitmentEnums.JobStatus.PUBLISHED && entity.getPublishedAt() == null) {
            entity.setPublishedAt(DateUtils.now());
        }
        // 发布时回填发布人（兼容历史草稿）
        if (target == RecruitmentEnums.JobStatus.PUBLISHED) {
            Long userId = UserContextUtil.getUserId();
            String username = UserContextUtil.getUsername();
            if (userId != null) {
                entity.setCreateUserId(userId);
            }
            if (username != null) {
                entity.setCreatedBy(username);
            }
        }
        jobPositionMapper.updateById(entity);
        log.info("Job position status updated: id={}, status={}", id, target);
    }

    /** 按部门统计职位数量。 */
    @Override
    public List<DepartmentJobStatVO> getStatsByDepartment() {
        return jobPositionMapper.statsByDepartment();
    }

    /** 按职级统计职位数量。 */
    @Override
    public List<LevelJobStatVO> getStatsByLevel() {
        return jobPositionMapper.statsByLevel();
    }

    /** 查询统计信息。 */
    @Override
    public JobStatsVO getStats() {
        long total = jobPositionMapper.selectCount(new LambdaQueryWrapper<>());
        long published = jobPositionMapper.selectCount(
                new LambdaQueryWrapper<JobPosition>().eq(JobPosition::getStatus,
                        RecruitmentEnums.JobStatus.PUBLISHED.getCode()));
        long draft = jobPositionMapper.selectCount(
                new LambdaQueryWrapper<JobPosition>().eq(JobPosition::getStatus,
                        RecruitmentEnums.JobStatus.DRAFT.getCode()));
        long closed = jobPositionMapper.selectCount(
                new LambdaQueryWrapper<JobPosition>().eq(JobPosition::getStatus,
                        RecruitmentEnums.JobStatus.CLOSED.getCode()));
        return JobStatsVO.builder()
                .total(total).published(published).draft(draft).closed(closed)
                .build();
    }
}
