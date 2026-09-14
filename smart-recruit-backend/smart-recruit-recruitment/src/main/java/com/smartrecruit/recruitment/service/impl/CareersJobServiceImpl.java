package com.smartrecruit.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.recruitment.dto.request.CareersApplyRequest;
import com.smartrecruit.recruitment.dto.request.CreateCareersJobRequest;
import com.smartrecruit.recruitment.dto.request.UpdateCareersJobRequest;
import com.smartrecruit.recruitment.dto.response.CareersApplicationVO;
import com.smartrecruit.recruitment.dto.response.CareersJobVO;
import com.smartrecruit.recruitment.entity.Application;
import com.smartrecruit.recruitment.entity.Candidate;
import com.smartrecruit.recruitment.entity.CareersJobPosition;
import com.smartrecruit.recruitment.entity.JobPosition;
import com.smartrecruit.recruitment.entity.Resume;
import com.smartrecruit.recruitment.repository.ApplicationMapper;
import com.smartrecruit.recruitment.repository.CandidateMapper;
import com.smartrecruit.recruitment.repository.CareersJobPositionMapper;
import com.smartrecruit.recruitment.repository.JobPositionMapper;
import com.smartrecruit.recruitment.repository.ResumeMapper;
import com.smartrecruit.recruitment.service.CareersJobService;
import com.smartrecruit.recruitment.service.FileStorageService;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.recruitment.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@link CareersJobService} 的实现类。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CareersJobServiceImpl implements CareersJobService {

    private final CareersJobPositionMapper mapper;
    private final JobPositionMapper jobPositionMapper;
    private final CandidateMapper candidateMapper;
    private final ResumeMapper resumeMapper;
    private final ApplicationMapper applicationMapper;
    private final FileStorageService fileStorageService;

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<CareersJobVO> pageQuery(String recType, String keyword, String category, int page, int size) {
        Page<CareersJobPosition> p = new Page<>(page, size);
        IPage<CareersJobPosition> result = mapper.pageQueryByType(p, recType, keyword, category);
        return PageResult.from(result).map(this::toVO);
    }

    /** 按类型查询记录列表。 */
    @Override
    public List<CareersJobVO> listByType(String recType) {
        LambdaQueryWrapper<CareersJobPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CareersJobPosition::getRecType, recType)
               .eq(CareersJobPosition::getStatus, 1)
               .orderByDesc(CareersJobPosition::getCreateTime)
               .orderByAsc(CareersJobPosition::getSortOrder);
        return mapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 根据主键查询详情。 */
    @Override
    public CareersJobVO getById(Long id) {
        CareersJobPosition entity = mapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("CareersJobPosition", id);
        }
        CareersJobVO vo = toVO(entity);
        // 如果 careers 表中三个字段为空，从 rec_job_position.description 解析回填
        mergeFromRecJobPosition(vo, entity.getTitle());
        return vo;
    }

    /** 创建记录。 */
    @Override
    @Transactional
    public CareersJobVO create(CreateCareersJobRequest request) {
        CareersJobPosition entity = toEntity(request);
        entity.setCreateUserId(UserContextUtil.getUserId());
        entity.setCreateBy(UserContextUtil.getUsername());
        entity.setUpdateUserId(UserContextUtil.getUserId());
        entity.setUpdateBy(UserContextUtil.getUsername());
        mapper.insert(entity);
        log.info("Careers job created: id={}, title={}, recType={}", entity.getId(), entity.getTitle(), entity.getRecType());
        // 同步到 rec_job_position.description
        syncToRecJobPosition(entity);
        return toVO(entity);
    }

    /** 更新记录。 */
    @Override
    @Transactional
    public CareersJobVO update(Long id, UpdateCareersJobRequest request) {
        CareersJobPosition entity = mapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("CareersJobPosition", id);
        }
        applyUpdate(entity, request);
        entity.setUpdateUserId(UserContextUtil.getUserId());
        entity.setUpdateBy(UserContextUtil.getUsername());
        mapper.updateById(entity);
        log.info("Careers job updated: id={}, title={}", id, entity.getTitle());
        // 同步到 rec_job_position.description
        syncToRecJobPosition(entity);
        return toVO(entity);
    }

    /** 根据主键删除记录。 */
    @Override
    @Transactional
    public void delete(Long id) {
        CareersJobPosition entity = mapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("CareersJobPosition", id);
        }
        mapper.deleteById(id);
        log.info("Careers job deleted: id={}", id);
    }

    /** 更新职位发布状态。 */
    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        CareersJobPosition entity = mapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("CareersJobPosition", id);
        }
        entity.setStatus(status);
        entity.setUpdateUserId(UserContextUtil.getUserId());
        entity.setUpdateBy(UserContextUtil.getUsername());
        mapper.updateById(entity);
        log.info("Careers job status updated: id={}, status={}", id, status);
    }

    // ---- 投递相关 ----

    /** 上传简历并创建职位投递。 */
    @Override
    public String uploadResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        String relativePath = "resumes/" + DateUtils.now().getYear() + "/"
                + String.format("%02d", DateUtils.now().getMonthValue()) + "/"
                + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            String url = fileStorageService.upload(file.getBytes(), originalName,
                    file.getContentType(), relativePath);
            log.info("Resume uploaded: path={}, size={}", relativePath, file.getSize());
            return url;
        } catch (Exception e) {
            log.error("Failed to upload resume", e);
            throw new RuntimeException("简历上传失败，请稍后重试", e);
        }
    }

    /** 候选人投递职位。 */
    @Override
    @Transactional
    public void apply(CareersApplyRequest request) {
        // 1. 校验职位存在
        CareersJobPosition careersJob = mapper.selectById(request.getJobId());
        if (careersJob == null) {
            throw new ResourceNotFoundException("CareersJobPosition", request.getJobId());
        }

        // 2. 查找或创建 Candidate（按 userId 或 email 匹配）
        Candidate candidate = findOrCreateCandidate(request);

        // 2.5 如果提供了 candidateId（系统用户 ID），关联到 Candidate
        if (request.getCandidateId() != null && request.getCandidateId() > 0
                && (candidate.getUserId() == null || !candidate.getUserId().equals(request.getCandidateId()))) {
            candidate.setUserId(request.getCandidateId());
            candidateMapper.updateById(candidate);
            log.info("Linked candidate {} to sys_user {}", candidate.getId(), request.getCandidateId());
        }

        // 3. 按 title 查找对应的 rec_job_position
        JobPosition recJob = lookupRecJobPosition(careersJob.getTitle());

        // 4. 如果有简历 URL，创建 Resume 记录
        if (request.getResumeUrl() != null && !request.getResumeUrl().isBlank()) {
            Resume resume = new Resume();
            resume.setCandidateId(candidate.getId());
            if (recJob != null) {
                resume.setJobPositionId(recJob.getId());
            }
            resume.setFileName(extractFileName(request.getResumeUrl()));
            resume.setFileUrl(request.getResumeUrl());
            resume.setFileType(0); // PDF
            resume.setParseStatus(0); // PENDING
            resumeMapper.insert(resume);
            log.info("Resume created for candidate: candidateId={}, resumeId={}",
                    candidate.getId(), resume.getId());
        }

        // 5. 创建 Application 记录
        Application application = new Application();
        application.setCandidateId(candidate.getId());
        application.setJobId(recJob != null ? recJob.getId() : 0L);
        application.setStage(0); // NEW
        application.setApplyAt(DateUtils.now());
        applicationMapper.insert(application);
        log.info("Application created: candidateId={}, jobId={}, appId={}",
                candidate.getId(), application.getJobId(), application.getId());
    }

    /** 判断候选人是否已投递该职位。 */
    @Override
    public boolean hasApplied(Long jobId, Long userId, String email) {
        CareersJobPosition careersJob = mapper.selectById(jobId);
        if (careersJob == null) return false;
        JobPosition recJob = lookupRecJobPosition(careersJob.getTitle());
        if (recJob == null) return false;

        Candidate candidate = resolveCandidate(userId, email);
        if (candidate == null) return false;

        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getCandidateId, candidate.getId())
               .eq(Application::getJobId, recJob.getId());
        return applicationMapper.selectCount(wrapper) > 0;
    }

    /** 查询候选人本人的投递记录。 */
    @Override
    public List<CareersApplicationVO> getMyApplications(Long userId, String email) {
        Candidate candidate = resolveCandidate(userId, email);
        if (candidate == null) return Collections.emptyList();

        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getCandidateId, candidate.getId())
               .orderByDesc(Application::getApplyAt);
        List<Application> apps = applicationMapper.selectList(wrapper);

        return apps.stream().map(app -> {
            CareersApplicationVO vo = new CareersApplicationVO();
            vo.setId(app.getId());
            vo.setStatus(app.getStage());
            vo.setCreateTime(app.getApplyAt());

            // 通过 rec_job_position.id 查找职位信息
            if (app.getJobId() != null && app.getJobId() > 0) {
                JobPosition recJob = jobPositionMapper.selectById(app.getJobId());
                if (recJob != null) {
                    vo.setJobTitle(recJob.getTitle());
                    // 尝试从 careers_job_position 获取部门信息
                    CareersJobPosition careersJob = lookupCareersJobByTitle(recJob.getTitle());
                    if (careersJob != null) {
                        vo.setDepartmentName(careersJob.getDept());
                    }
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    // ---- 私有帮助方法 ----

    private Candidate findOrCreateCandidate(CareersApplyRequest request) {
        Candidate candidate = findCandidateByEmail(request.getCandidateEmail());
        if (candidate != null) {
            // 更新 name / phone（如果传了新的）
            if (request.getCandidateName() != null && !request.getCandidateName().isBlank()) {
                candidate.setName(request.getCandidateName());
            }
            if (request.getCandidatePhone() != null && !request.getCandidatePhone().isBlank()) {
                candidate.setPhone(request.getCandidatePhone());
            }
            candidateMapper.updateById(candidate);
            return candidate;
        }
        // 新建
        candidate = new Candidate();
        candidate.setName(request.getCandidateName());
        candidate.setEmail(request.getCandidateEmail());
        candidate.setPhone(request.getCandidatePhone());
        candidate.setSource(2); // WEBSITE
        candidate.setCurrentStage(0); // NEW
        candidateMapper.insert(candidate);
        log.info("Candidate created: id={}, email={}", candidate.getId(), candidate.getEmail());
        return candidate;
    }

    private Candidate findCandidateByEmail(String email) {
        if (email == null || email.isBlank()) return null;
        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Candidate::getEmail, email).last("LIMIT 1");
        return candidateMapper.selectOne(wrapper);
    }

    private Candidate findCandidateByUserId(Long userId) {
        if (userId == null || userId <= 0) return null;
        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Candidate::getUserId, userId).last("LIMIT 1");
        return candidateMapper.selectOne(wrapper);
    }

    /**
     * 按 userId 优先，其次 email 查找候选人。
     */
    private Candidate resolveCandidate(Long userId, String email) {
        if (userId != null && userId > 0) {
            Candidate byUser = findCandidateByUserId(userId);
            if (byUser != null) return byUser;
        }
        if (email != null && !email.isBlank()) {
            return findCandidateByEmail(email);
        }
        return null;
    }

    private String extractFileName(String url) {
        if (url == null || url.isBlank()) return "resume.pdf";
        int idx = url.lastIndexOf('/');
        return idx >= 0 ? url.substring(idx + 1) : url;
    }

    private CareersJobVO toVO(CareersJobPosition entity) {
        CareersJobVO vo = new CareersJobVO();
        vo.setId(entity.getId());
        vo.setRecType(entity.getRecType());
        vo.setTitle(entity.getTitle());
        vo.setDept(entity.getDept());
        vo.setLocation(entity.getLocation());
        vo.setExp(entity.getExp());
        vo.setSalary(entity.getSalary());
        vo.setCategory(entity.getCategory());
        vo.setTags(entity.getTags());
        vo.setResponsibilities(entity.getResponsibilities());
        vo.setRequirements(entity.getRequirements());
        vo.setBonus(entity.getBonus());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private CareersJobPosition toEntity(CreateCareersJobRequest request) {
        CareersJobPosition entity = new CareersJobPosition();
        entity.setRecType(request.getRecType());
        entity.setTitle(request.getTitle());
        entity.setDept(request.getDept());
        entity.setLocation(request.getLocation());
        entity.setExp(request.getExp());
        entity.setSalary(request.getSalary());
        entity.setCategory(request.getCategory());
        entity.setTags(request.getTags());
        entity.setResponsibilities(request.getResponsibilities());
        entity.setRequirements(request.getRequirements());
        entity.setBonus(request.getBonus());
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        return entity;
    }

    private void applyUpdate(CareersJobPosition entity, UpdateCareersJobRequest request) {
        if (request.getRecType() != null) entity.setRecType(request.getRecType());
        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getDept() != null) entity.setDept(request.getDept());
        if (request.getLocation() != null) entity.setLocation(request.getLocation());
        if (request.getExp() != null) entity.setExp(request.getExp());
        if (request.getSalary() != null) entity.setSalary(request.getSalary());
        if (request.getCategory() != null) entity.setCategory(request.getCategory());
        if (request.getTags() != null) entity.setTags(request.getTags());
        if (request.getResponsibilities() != null) entity.setResponsibilities(request.getResponsibilities());
        if (request.getRequirements() != null) entity.setRequirements(request.getRequirements());
        if (request.getBonus() != null) entity.setBonus(request.getBonus());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
    }

    // ---- rec_job_position 双向同步 ----

    /**
     * 从 {@code rec_job_position.job_description} 解析 Markdown 段落，
     * 回填 careers VO 中为 null 的字段。
     */
    private void mergeFromRecJobPosition(CareersJobVO vo, String title) {
        if (title == null || title.isBlank()) return;
        if (!CollectionUtils.isEmpty(vo.getResponsibilities())
                && !CollectionUtils.isEmpty(vo.getRequirements())
                && !CollectionUtils.isEmpty(vo.getBonus())) return;

        JobPosition rec = lookupRecJobPosition(title);
        if (rec == null || rec.getDescription() == null || rec.getDescription().isBlank()) return;

        parseMarkdownDescription(rec.getDescription(), vo);
    }

    /**
     * 将 careers 职位结构化的三个字段组合为 Markdown，写回 {@code rec_job_position.job_description}。
     */
    private void syncToRecJobPosition(CareersJobPosition entity) {
        if (entity.getTitle() == null || entity.getTitle().isBlank()) return;
        if (CollectionUtils.isEmpty(entity.getResponsibilities())
                && CollectionUtils.isEmpty(entity.getRequirements())
                && CollectionUtils.isEmpty(entity.getBonus())) return;

        JobPosition rec = lookupRecJobPosition(entity.getTitle());
        if (rec == null) return;

        String md = buildMarkdownDescription(entity.getResponsibilities(),
                entity.getRequirements(), entity.getBonus());
        rec.setDescription(md);
        jobPositionMapper.updateById(rec);
        log.info("Synced rec_job_position.description for title={}", entity.getTitle());
    }

    private JobPosition lookupRecJobPosition(String title) {
        LambdaQueryWrapper<JobPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobPosition::getTitle, title)
               .last("LIMIT 1");
        return jobPositionMapper.selectOne(wrapper);
    }

    private CareersJobPosition lookupCareersJobByTitle(String title) {
        LambdaQueryWrapper<CareersJobPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CareersJobPosition::getTitle, title)
               .last("LIMIT 1");
        return mapper.selectOne(wrapper);
    }

    /**
     * 解析 Markdown 中包含"## 岗位职责 / ## 任职要求 / ## 我们提供"的段落。
     */
    private void parseMarkdownDescription(String md, CareersJobVO vo) {
        if (CollectionUtils.isEmpty(vo.getResponsibilities())) {
            vo.setResponsibilities(extractSection(md, "岗位职责"));
        }
        if (CollectionUtils.isEmpty(vo.getRequirements())) {
            vo.setRequirements(extractSection(md, "任职要求"));
        }
        if (CollectionUtils.isEmpty(vo.getBonus())) {
            vo.setBonus(extractSection(md, "我们提供"));
        }
    }

    /** 提取某个 ## 标题下的编号/列表项行。 */
    private List<String> extractSection(String md, String sectionTitle) {
        Pattern sectionPattern = Pattern.compile(
                "^##\\s*" + Pattern.quote(sectionTitle) + "\\s*$",
                Pattern.MULTILINE);
        Matcher m = sectionPattern.matcher(md);
        if (!m.find()) return Collections.emptyList();

        int start = m.end();
        Matcher nextSection = Pattern.compile("^##\\s+", Pattern.MULTILINE).matcher(md);
        int end = nextSection.find(start) ? nextSection.start() : md.length();

        String sectionContent = md.substring(start, end);
        List<String> items = new ArrayList<>();
        for (String line : sectionContent.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            // remove list markers: "1. ", "- ", "* ", "· "
            String cleaned = trimmed.replaceFirst("^(\\d+[、.．]\\s*|-\\s+|\\*\\s+|·\\s+)", "");
            if (!cleaned.isBlank()) items.add(cleaned);
        }
        return items;
    }

    /** 将结构化数组组合为 Markdown。 */
    private String buildMarkdownDescription(List<String> responsibilities,
                                             List<String> requirements,
                                             List<String> bonus) {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "岗位职责", responsibilities, true);
        appendSection(sb, "任职要求", requirements, false);
        appendSection(sb, "我们提供", bonus, false);
        return sb.toString().trim();
    }

    private void appendSection(StringBuilder sb, String title, List<String> items, boolean numbered) {
        if (CollectionUtils.isEmpty(items)) return;
        if (sb.length() > 0) sb.append("\n\n");
        sb.append("## ").append(title).append("\n\n");
        for (int i = 0; i < items.size(); i++) {
            sb.append(numbered ? (i + 1) + ". " : "- ").append(items.get(i)).append("\n");
        }
    }
}
