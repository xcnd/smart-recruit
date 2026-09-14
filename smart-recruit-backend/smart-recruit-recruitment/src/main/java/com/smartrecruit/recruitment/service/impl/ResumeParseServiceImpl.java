package com.smartrecruit.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.recruitment.domain.ParsedResume;
import com.smartrecruit.recruitment.entity.Candidate;
import com.smartrecruit.recruitment.entity.Resume;
import com.smartrecruit.recruitment.enums.RecruitmentEnums;
import com.smartrecruit.recruitment.repository.CandidateMapper;
import com.smartrecruit.recruitment.repository.ResumeMapper;
import com.smartrecruit.recruitment.feign.AiAgentCapabilityClient;
import com.smartrecruit.recruitment.service.DocumentParserService;
import com.smartrecruit.recruitment.service.AiScreeningService;
import com.smartrecruit.recruitment.service.FileStorageService;
import com.smartrecruit.recruitment.service.ResumeParseService;
import com.smartrecruit.recruitment.service.ResumeStructurerService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.imageio.ImageIO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 异步简历解析编排实现。
 *
 * <p>流水线步骤：
 * <ol>
 *   <li>更新状态为 PARSING</li>
 *   <li>调用 {@link DocumentParserService} 提取文本</li>
 *   <li>调用 {@link ResumeStructurerService} 结构化提取</li>
 *   <li>自动丰富 Candidate 记录（姓名/邮箱/手机/技能/学历/工作经验等）</li>
 *   <li>更新 Resume 的 parsedContent 和解析状态</li>
 * </ol>
 * 任何步骤失败均会捕获异常，将状态设为 FAILED 并记录错误日志。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeParseServiceImpl implements ResumeParseService {

    private final ResumeMapper resumeMapper;
    private final CandidateMapper candidateMapper;
    private final DocumentParserService documentParserService;
    private final ResumeStructurerService resumeStructurerService;
    private final FileStorageService fileStorageService;
    private final AiScreeningService aiScreeningService;
    private final AiAgentCapabilityClient aiAgentCapabilityClient;

    /** 图片/扫描件 AI 解析的大小上限（5MB，避免超大 base64 请求）。 */
    private static final long MAX_AI_IMAGE_BYTES = 5 * 1024 * 1024;

    /** PDF 渲染识别的最大页数（简历通常 1-3 页）。 */
    private static final int MAX_AI_PDF_PAGES = 3;

    /** AI 图片输入的最大宽度（像素），超出按比例缩放，控制请求体大小。 */
    private static final int MAX_AI_IMAGE_WIDTH = 1280;

    /** 通用工具/IDE 等不应作为技能标签输出的关键词（AI 解析兜底过滤）。 */
    private static final Set<String> AI_SKILL_DENY = Set.of(
            "idea", "intellij", "vscode", "visual studio code", "eclipse", "webstorm",
            "pycharm", "sublime", "notepad", "hbuilder", "navicat", "datagrip",
            "postman", "apifox", "xshell", "securecrt", "git", "maven", "gradle",
            "docker desktop", "jira", "confluence", "svn");

    /** 异步解析简历文本。 */
    @Override
    @Async("resumeParseExecutor")
    public void parseAsync(Long resumeId, byte[] fileBytes, String fileName) {
        long pipelineStart = DateUtils.currentEpochMillis();
        log.info("[计时] 简历解析流水线启动: resumeId={}, file={}, thread={}",
                resumeId, fileName, Thread.currentThread().getName());

        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            log.error("简历不存在（可能已被删除）: id={}", resumeId);
            Resume fallback = new Resume();
            fallback.setId(resumeId);
            fallback.setParseStatus(RecruitmentEnums.ParseStatus.FAILED.getCode());
            resumeMapper.updateById(fallback);
            return;
        }

        // 步骤1: 设置解析中状态
        resume.setParseStatus(RecruitmentEnums.ParseStatus.PARSING.getCode());
        resumeMapper.updateById(resume);

        try {
            // 步骤2: 文本提取
            long step2Start = DateUtils.currentEpochMillis();
            log.info("[计时] 步骤2开始-文本提取: resumeId={}, file={}", resumeId, fileName);
            String rawText = documentParserService.extractText(fileBytes, fileName);
            log.info("[计时] 步骤2完成-文本提取: resumeId={}, chars={}, 耗时 {}ms",
                    resumeId, rawText.length(), DateUtils.currentEpochMillis() - step2Start);
            if (rawText.isEmpty()) {
                log.warn("所有文本提取策略均返回空: resumeId={}, fileName={}", resumeId, fileName);
            }

            // 步骤2.5: 尝试提取头像并上传到 RustFS
            long step25Start = DateUtils.currentEpochMillis();
            Candidate candidate = candidateMapper.selectById(resume.getCandidateId());
            if (candidate != null && (candidate.getAvatarUrl() == null || candidate.getAvatarUrl().isEmpty())) {
                try {
                    byte[] avatarBytes = documentParserService.extractProfileImage(fileBytes, fileName);
                    if (avatarBytes != null) {
                        long uploadStart = DateUtils.currentEpochMillis();
                        String avatarPath = "avatars/" + candidate.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
                        String avatarUrl = fileStorageService.upload(avatarBytes, "avatar.jpg", "image/jpeg", avatarPath);
                        log.info("[计时] 头像上传完成: 耗时 {}ms", DateUtils.currentEpochMillis() - uploadStart);
                        candidate.setAvatarUrl(avatarUrl);
                        candidateMapper.updateById(candidate);
                        log.info("头像已提取并上传: resumeId={}, candidateId={}, url={}", resumeId, candidate.getId(), avatarUrl);
                    }
                } catch (Exception e) {
                    log.warn("头像提取/上传失败（非致命错误）: resumeId={}, msg={}", resumeId, e.getMessage());
                }
            }
            log.info("[计时] 步骤2.5完成-头像提取: resumeId={}, 耗时 {}ms",
                    resumeId, DateUtils.currentEpochMillis() - step25Start);

            // 步骤3: 结构化提取（文本走本地规则 + AI 增强；图片/扫描件走 AI 视觉解析）
            long step3Start = DateUtils.currentEpochMillis();
            log.info("[计时] 步骤3开始-结构化提取: resumeId={}", resumeId);
            ParsedResume structured;
            if (rawText.isBlank() && isImageOrScannedPdf(fileName)) {
                structured = parseImageWithAi(fileBytes, fileName, resumeId);
                if (structured == null) {
                    // AI 解析失败：明确标记失败，让筛选页面显示「解析失败」
                    throw new IllegalStateException(
                            "图片/扫描件简历 AI 解析失败，无法识别内容，请人工复核或重新上传");
                }
            } else {
                structured = resumeStructurerService.structure(rawText);
            }
            log.info("[计时] 步骤3完成-结构化提取: resumeId={}, 姓名={}, 教育{}条, 经历{}条, 技能{}个, 耗时 {}ms",
                    resumeId, structured.getName(), structured.getEducation().size(),
                    structured.getExperience().size(), structured.getSkills().size(),
                    DateUtils.currentEpochMillis() - step3Start);

            // 步骤4: 使用解析数据丰富 Candidate（含去重逻辑）
            long step4Start = DateUtils.currentEpochMillis();
            log.info("[计时] 步骤4开始-Candidate数据回填: resumeId={}", resumeId);
            candidate = candidateMapper.selectById(resume.getCandidateId());
            if (candidate != null && isPlaceholderCandidate(candidate)) {
                // 占位候选人：直接回填解析数据。
                // 不再因邮箱/手机号匹配而把简历改绑到其他候选人，
                // 避免简历「来源」在解析后被意外修改。
                enrichCandidateFromParsed(candidate, structured);
                tryCandidateUpdate(candidate);
            } else if (candidate != null) {
                // 非占位候选人（已有真实数据）— 补充可能缺失的字段
                enrichCandidateFromParsed(candidate, structured);
                tryCandidateUpdate(candidate);
            }
            log.info("[计时] 步骤4完成-Candidate数据回填: resumeId={}, 耗时 {}ms",
                    resumeId, DateUtils.currentEpochMillis() - step4Start);

            // 步骤5: 持久化解析内容并标记成功
            long step5Start = DateUtils.currentEpochMillis();
            resume.setParsedContent(structured);
            resume.setParseStatus(RecruitmentEnums.ParseStatus.SUCCESS.getCode());
            resume.setParseError(null); // 清除之前的失败原因
            resumeMapper.updateById(resume);
            log.info("[计时] 步骤5完成-持久化: resumeId={}, 耗时 {}ms",
                    resumeId, DateUtils.currentEpochMillis() - step5Start);

            log.info("[计时] 简历解析流水线成功完成: resumeId={}, 总耗时 {}ms",
                    resumeId, DateUtils.currentEpochMillis() - pipelineStart);

            // 步骤6: 自动触发AI筛选（仅当上传时开启 AI 筛选开关；非致命，失败不影响解析结果）
            if (Boolean.TRUE.equals(resume.getAutoScreen())) {
                try {
                    aiScreeningService.screenAsync(resumeId);
                    log.info("[计时] 步骤6触发-AI筛选已提交: resumeId={}", resumeId);
                } catch (Exception e) {
                    log.warn("自动触发AI筛选失败: resumeId={}, msg={}", resumeId, e.getMessage());
                }
            } else {
                log.info("上传时已关闭 AI 筛选开关，跳过自动筛选: resumeId={}", resumeId);
            }

        } catch (Throwable t) {
            log.error("[计时] 简历解析流水线失败: resumeId={}, thread={}, 已耗时 {}ms, errorType={}",
                    resumeId, Thread.currentThread().getName(),
                    DateUtils.currentEpochMillis() - pipelineStart,
                    t.getClass().getName(), t);

            String errorDetail = buildErrorDetail(t);
            try {
                resume.setParseStatus(RecruitmentEnums.ParseStatus.FAILED.getCode());
                resume.setParseError(errorDetail);
                resumeMapper.updateById(resume);
                log.info("[计时] 解析失败原因已记录: resumeId={}, errorType={}", resumeId, t.getClass().getName());
            } catch (Exception dbEx) {
                log.error("[计时] 更新简历失败状态也失败了: resumeId={}", resumeId, dbEx);
            }
        }
    }

    // ─── Candidate 数据回填（从解析结果中提取所有可用信息）──

    private void enrichCandidateFromParsed(Candidate candidate, ParsedResume structured) {
        boolean isPlaceholder = isPlaceholderCandidate(candidate);

        // 姓名 — 占位候选人或被清空时覆盖
        String name = structured.getName();
        if (isNotEmpty(name) && (isPlaceholder || candidate.getName() == null)) {
            candidate.setName(name);
        }

        // 邮箱 — 占位候选人、空值或占位邮箱时覆盖
        String email = structured.getEmail();
        if (isNotEmpty(email) && (isPlaceholder || candidate.getEmail() == null
                || candidate.getEmail().isEmpty()
                || candidate.getEmail().contains("@placeholder.local"))) {
            candidate.setEmail(email);
        }

        // 手机 — 占位候选人、空值时覆盖
        String phone = structured.getPhone();
        if (isNotEmpty(phone) && (isPlaceholder || candidate.getPhone() == null
                || candidate.getPhone().isEmpty())) {
            candidate.setPhone(phone);
        }

        // 技能列表 — 占位候选人或空值时覆盖
        List<String> skillsList = structured.getSkills();
        if (!skillsList.isEmpty()
                && (isPlaceholder || candidate.getSkills() == null || candidate.getSkills().isEmpty())) {
            candidate.setSkills(new ArrayList<>(skillsList));
        }

        // 教育背景 — 提取最高学历、学校、专业
        List<ParsedResume.EducationEntry> educationList = structured.getEducation();
        if (!educationList.isEmpty()) {
            // 学历：占位候选人或空值时覆盖（DB education_level DEFAULT NULL）
            boolean needEducation = isPlaceholder || candidate.getEducation() == null;
            if (needEducation) {
                for (ParsedResume.EducationEntry edu : educationList) {
                    Integer level = inferEducationLevel(edu);
                    if (level != null) {
                        candidate.setEducation(level);
                        break; // 找到最高学历后退出
                    }
                }
            }
            // 学校、专业：占位候选人或空值时覆盖
            ParsedResume.EducationEntry firstEdu = educationList.get(0);
            String school = firstEdu.getSchool();
            if (!school.isEmpty() && (isPlaceholder || candidate.getSchool() == null)) {
                candidate.setSchool(school);
            }
            String major = firstEdu.getMajor();
            if (!major.isEmpty() && (isPlaceholder || candidate.getMajor() == null)) {
                candidate.setMajor(major);
            }
        }

        // 工作经历 — 提取公司、职位、工作年限
        List<ParsedResume.ExperienceEntry> experienceList = structured.getExperience();
        if (!experienceList.isEmpty()) {
            ParsedResume.ExperienceEntry firstExp = experienceList.get(0);
            String company = firstExp.getCompany();
            if (!company.isEmpty() && (isPlaceholder || candidate.getCurrentCompany() == null)) {
                candidate.setCurrentCompany(company);
            }
            String position = firstExp.getPosition();
            if (!position.isEmpty() && (isPlaceholder || candidate.getCurrentPosition() == null)) {
                candidate.setCurrentPosition(position);
            }

            // 工作年限：DB 有 NOT NULL DEFAULT 0，占位候选人会是 0
            boolean needWorkYears = isPlaceholder || candidate.getYearsOfExperience() == null
                    || candidate.getYearsOfExperience() == 0;
            if (needWorkYears) {
                String parsedWorkYears = structured.getWorkYears();
                if (isNotEmpty(parsedWorkYears)) {
                    try {
                        int wy = Integer.parseInt(parsedWorkYears);
                        if (wy > 0) candidate.setYearsOfExperience(wy);
                    } catch (NumberFormatException ignored) {
                    }
                }
                // 回退：从经历列表估算
                if (candidate.getYearsOfExperience() == null
                        || candidate.getYearsOfExperience() == 0) {
                    String startDate = firstExp.getStart();
                    if (!startDate.isEmpty()) {
                        int years = estimateWorkYears(experienceList);
                        if (years > 0) {
                            candidate.setYearsOfExperience(years);
                        }
                    }
                }
            }
        }

        // 性别：DB 有 NOT NULL DEFAULT 0，占位候选人会是 0
        String genderStr = structured.getGender();
        if (isNotEmpty(genderStr) && (isPlaceholder || candidate.getGender() == null
                || candidate.getGender() == 0)) {
            Integer genderCode = mapGender(genderStr);
            if (genderCode != null) {
                candidate.setGender(genderCode);
            }
        }

        // 出生日期 / 年龄
        String birthDateStr = structured.getBirthDate();
        if (isNotEmpty(birthDateStr) && (isPlaceholder || candidate.getBirthDate() == null)) {
            try {
                candidate.setBirthDate(parseDate(birthDateStr));
            } catch (DateTimeParseException ignored) {
            }
        }
        // 年龄 → 推算出生年份
        if (isPlaceholder || candidate.getBirthDate() == null) {
            String ageStr = structured.getAge();
            if (isNotEmpty(ageStr)) {
                try {
                    int age = Integer.parseInt(ageStr);
                    int birthYear = DateUtils.today().getYear() - age;
                    candidate.setBirthDate(LocalDate.of(birthYear, 1, 1));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // 城市 — 从 "location" 字段提取
        String location = structured.getLocation();
        if (isNotEmpty(location) && (isPlaceholder || candidate.getCity() == null)) {
            candidate.setCity(location);
        }

        // 来源详情 — 标记为简历解析
        if (isPlaceholder || candidate.getSourceDetail() == null) {
            candidate.setSourceDetail("简历解析");
        }

        // 头像背景色 — 占位候选人设置随机颜色
        if (isPlaceholder) {
            candidate.setAvatarColor(generateAvatarColor(candidate.getName()));
        }

        log.info("Candidate enrichment done: id={}, name={}, email={}, phone={}, education={}, "
                + "yearsOfExp={}, gender={}, city={}, school={}, major={}, currentCompany={}, "
                + "currentPosition={}",
                candidate.getId(), candidate.getName(), candidate.getEmail(),
                candidate.getPhone(), candidate.getEducation(),
                candidate.getYearsOfExperience(), candidate.getGender(),
                candidate.getCity(), candidate.getSchool(), candidate.getMajor(),
                candidate.getCurrentCompany(), candidate.getCurrentPosition());
    }

    /**
     * 尝试更新候选人，遇到邮箱唯一冲突时逐字段降级（清空冲突字段后重试）。
     */
    private void tryCandidateUpdate(Candidate candidate) {
        try {
            candidateMapper.updateById(candidate);
            log.info("候选人信息已从解析结果中更新: candidateId={}, name={}",
                    candidate.getId(), candidate.getName());
        } catch (DuplicateKeyException e) {
            String savedEmail = candidate.getEmail();
            String savedPhone = candidate.getPhone();
            log.warn("候选人邮箱/手机与已有记录冲突: candidateId={}, email={}, phone={}",
                    candidate.getId(), savedEmail, savedPhone);
            boolean updated = false;
            if (savedEmail != null) {
                candidate.setEmail(null);
                try {
                    candidateMapper.updateById(candidate);
                    updated = true;
                    log.info("候选人信息已更新(跳过邮箱冲突): candidateId={}, name={}",
                            candidate.getId(), candidate.getName());
                } catch (DuplicateKeyException ignored) {
                    candidate.setEmail(savedEmail);
                }
            }
            if (!updated && savedPhone != null) {
                candidate.setPhone(null);
                try {
                    candidateMapper.updateById(candidate);
                    updated = true;
                    log.info("候选人信息已更新(跳过手机冲突): candidateId={}, name={}",
                            candidate.getId(), candidate.getName());
                } catch (DuplicateKeyException ignored) {
                    candidate.setPhone(savedPhone);
                }
            }
            if (!updated) {
                candidate.setEmail(null);
                candidate.setPhone(null);
                try {
                    candidateMapper.updateById(candidate);
                    log.info("候选人信息已更新(跳过邮箱+手机冲突): candidateId={}, name={}",
                            candidate.getId(), candidate.getName());
                } catch (DuplicateKeyException ignored) {
                    log.warn("即使清空邮箱和手机仍然冲突: candidateId={}", candidate.getId());
                }
            }
        }
    }

    /**
     * 根据解析出的邮箱或手机号查找已存在的非占位候选人。
     * 邮箱和手机号任一匹配即返回（email 优先）。
     */
    private Candidate findExistingCandidateByContact(String email, String phone) {
        if (!isNotEmpty(email) && !isNotEmpty(phone)) {
            return null;
        }
        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();
        if (isNotEmpty(email)) {
            wrapper.eq(Candidate::getEmail, email);
        }
        if (isNotEmpty(phone)) {
            if (isNotEmpty(email)) {
                wrapper.or();
            }
            wrapper.eq(Candidate::getPhone, phone);
        }
        // 排除占位候选人和已删除记录
        wrapper.ne(Candidate::getEmail, "");
        wrapper.notLike(Candidate::getEmail, "@placeholder.local");
        wrapper.last("LIMIT 1");
        return candidateMapper.selectOne(wrapper);
    }

    /** 根据姓名生成一个柔和的头像背景色。 */
    private static final String[] AVATAR_COLORS = {
        "#6366f1", "#8b5cf6", "#0ea5e9", "#10b981", "#f59e0b",
        "#ef4444", "#ec4899", "#14b8a6", "#f97316", "#3b82f6"
    };
    private String generateAvatarColor(String name) {
        if (name == null || name.isEmpty()) return AVATAR_COLORS[0];
        int hash = name.hashCode();
        return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
    }

    // ─── 错误详情构建 ───

    /**
     * 将异常堆栈转换为可存储的错误详情字符串。
     * 最多保留 4000 字符，以免超出数据库字段限制。
     */
    private String buildErrorDetail(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        String fullTrace = sw.toString();
        if (fullTrace.length() > 4000) {
            fullTrace = fullTrace.substring(0, 4000) + "\n... (truncated)";
        }
        return fullTrace;
    }

    // ─── 工具方法 ───

    private boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    private boolean isPlaceholderCandidate(Candidate candidate) {
        return "待解析".equals(candidate.getName())
                || (candidate.getEmail() != null
                && candidate.getEmail().contains("@placeholder.local"));
    }

    /**
     * 将学历字符串映射为枚举值。
     * 枚举映射：0=高中, 1=大专, 2=本科, 3=硕士, 4=博士
     */
    private Integer mapDegreeToLevel(String degree) {
        if (degree == null || degree.isEmpty()) return null;
        String d = degree.trim();
        // 博士
        if (strContainsAny(d, "博士", "Doctorate", "PhD", "Ph.D.", "PHD")) return 4;
        // 硕士
        if (strContainsAny(d, "硕士", "Master", "MBA", "EMBA", "M.S.", "M.A.", "MSc", "M.Eng")) return 3;
        // 本科
        if (strContainsAny(d, "本科", "学士", "Bachelor", "B.S.", "B.A.", "BSc", "BA", "BS", "B.Eng")) return 2;
        // 大专
        if (strContainsAny(d, "大专", "专科", "Associate", "College", "Diploma")) return 1;
        // 高中
        if (strContainsAny(d, "高中", "中专", "中技", "High School", "Secondary")) return 0;
        return null;
    }

    /**
     * 综合推断学历等级：优先用 degree 字段，若缺失则从学校名和时间范围动态推断。
     * 0=高中, 1=大专, 2=本科, 3=硕士, 4=博士
     */
    private Integer inferEducationLevel(ParsedResume.EducationEntry edu) {
        // 1. 优先用 degree 字段显式推断
        Integer level = mapDegreeToLevel(edu.getDegree());
        if (level != null) return level;

        // 2. 没有显式学历声明，从学校名和时间范围动态推断
        String school = edu.getSchool();
        if (school == null || school.isEmpty()) return null;

        int duration = calcDurationYears(edu.getStart(), edu.getEnd());

        // 研究生院 / 博士生 → 硕/博
        if (strContainsAny(school, "研究生院")) return 3;
        if (strContainsAny(school, "博士")) return 4;

        // 大学
        if (school.contains("大学")) {
            // 学制 2~3 年 → 多为硕士；4 年及以上 → 本科
            return (duration >= 2 && duration <= 3) ? 3 : 2;
        }

        // 学院
        if (school.contains("学院")) {
            // 学制 4 年及以上 → 本科；否则 → 大专
            return duration >= 4 ? 2 : 1;
        }

        // 专科 / 职业技术院校
        if (strContainsAny(school, "专科", "职业技术", "高职")) return 1;

        // 高中 / 中专
        if (strContainsAny(school, "高中", "中专", "中学")) return 0;

        return null;
    }

    /** 从起止日期字符串中估算就读年数 */
    private static int calcDurationYears(String start, String end) {
        int sy = extractYear(start);
        if (sy <= 0) return 0;
        int ey = extractYear(end);
        if (ey <= 0) ey = DateUtils.today().getYear();
        return Math.max(0, ey - sy);
    }

    /** 从日期字符串中提取年份（支持 "2020-06", "2020", "2020.09" 等） */
    private static int extractYear(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return 0;
        try {
            String cleaned = dateStr.replaceAll("[^0-9]", "");
            if (cleaned.length() >= 4) {
                return Integer.parseInt(cleaned.substring(0, 4));
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    private boolean strContainsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    /**
     * 将性别字符串映射为编码：0=未知, 1=男, 2=女。
     */
    private Integer mapGender(String gender) {
        return switch (gender) {
            case "男", "男性", "Male", "male", "M", "m" -> 1;
            case "女", "女性", "Female", "female", "F", "f" -> 2;
            default -> null;
        };
    }

    /**
     * 尝试多种格式解析日期字符串。
     */
    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateUtils.DATE_FORMATTER,
        DateUtils.SLASH_DATE_FORMATTER,
        DateUtils.DOT_DATE_FORMATTER,
        DateUtils.CN_DATE_FORMATTER,
        DateUtils.COMPACT_DATE_FORMATTER,
    };

    private LocalDate parseDate(String dateStr) {
        String cleaned = dateStr.replaceAll("\\s+", "").replaceAll("出生|生日|日期", "");
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new DateTimeParseException("Unparseable date", cleaned, 0);
    }

    /**
     * 从工作经历列表中估算工作年限。
     * 取最早开始日期到最晚结束日期的年份差。
     */
    private int estimateWorkYears(List<ParsedResume.ExperienceEntry> expList) {
        int earliestYear = Integer.MAX_VALUE;
        int latestYear = 0;

        for (ParsedResume.ExperienceEntry exp : expList) {
            String start = exp.getStart();
            String end = exp.getEnd();

            if (!start.isEmpty() && start.length() >= 4) {
                try {
                    int y = Integer.parseInt(start.substring(0, 4));
                    if (y < earliestYear) earliestYear = y;
                } catch (NumberFormatException ignored) {
                }
            }
            if (!end.isEmpty() && end.length() >= 4 && !"至今".equals(end)) {
                try {
                    int y = Integer.parseInt(end.substring(0, 4));
                    if (y > latestYear) latestYear = y;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // 如果最新年份为 0（可能全是"至今"或未提取到）则使用当前年份
        if (latestYear == 0) {
            latestYear = DateUtils.today().getYear();
        }

        if (earliestYear < 2000 || latestYear < earliestYear) return 0;
        return latestYear - earliestYear;
    }

    // ==================== 图片/扫描件简历 AI 解析 ====================

    /**
     * 同步解析简历文件（供「全流程编排」复用，不创建简历/候选人记录）。
     *
     * @return {@code {"rawText": 提取文本, "parsed": ParsedResume}}
     */
    @Override
    public Map<String, Object> parseFileForPipeline(byte[] fileBytes, String fileName) {
        String rawText = documentParserService.extractText(fileBytes, fileName);
        ParsedResume parsed;
        if (rawText.isBlank() && isImageOrScannedPdf(fileName)) {
            // 图片/扫描件：AI 视觉解析（失败会抛出带原因的异常）
            parsed = parseImageWithAi(fileBytes, fileName, null);
        } else {
            // 文本型：本地规则 + AI 增强结构化
            parsed = resumeStructurerService.structure(rawText);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rawText", rawText);
        result.put("parsed", parsed);
        return result;
    }

    /** 判断是否为图片文件或 PDF（扫描件场景）。 */
    private boolean isImageOrScannedPdf(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp")
                || lower.endsWith(".bmp") || lower.endsWith(".gif")
                || lower.endsWith(".pdf");
    }

    /**
     * 调用 AI 引擎视觉模型解析图片/扫描件简历。
     *
     * @return 结构化结果；AI 不可用或识别不完整时返回 {@code null}，由上层回退原流程
     */
    @SuppressWarnings("unchecked")
    private ParsedResume parseImageWithAi(byte[] fileBytes, String fileName, Long resumeId) {
        if (fileBytes == null || fileBytes.length == 0 || fileBytes.length > MAX_AI_IMAGE_BYTES) {
            log.warn("图片/扫描件超出 AI 解析大小限制，跳过: resumeId={}, size={}",
                    resumeId, fileBytes == null ? 0 : fileBytes.length);
            return null;
        }
        try {
            // PDF 先渲染成高清图片（多页），图片统一压缩转 JPEG，保证 qwen-vl 可读且 payload 可控
            List<String> dataUrls = toImageDataUrls(fileBytes, fileName, resumeId);
            if (dataUrls == null || dataUrls.isEmpty()) {
                throw new IllegalStateException("图片/PDF 转码失败，无法生成可识别的图片");
            }
            Map<String, Object> request = Map.of(
                    "fileName", fileName == null ? "resume" : fileName,
                    "base64Images", dataUrls);
            var response = aiAgentCapabilityClient.parseResumeImage(request);
            if (response == null) {
                throw new IllegalStateException("AI 引擎无响应");
            }
            if (response.data() == null) {
                String reason = response.message() != null ? response.message() : "AI 引擎返回空结果";
                throw new IllegalStateException("图片简历 AI 解析失败：" + reason);
            }
            Map<String, Object> data = response.data();
            boolean reviewRequired = Boolean.TRUE.equals(data.get("reviewRequired"));

            ParsedResume parsed = new ParsedResume();
            parsed.setName(strValue(data.get("name")));
            parsed.setGender(strValue(data.get("gender")));
            parsed.setHousehold(strValue(data.get("household")));
            parsed.setLocation(strValue(data.get("location")));
            parsed.setAge(strValue(data.get("age")));
            parsed.setBirthDate(strValue(data.get("birthDate")));
            parsed.setPoliticalStatus(strValue(data.get("politicalStatus")));
            parsed.setDesiredPosition(strValue(data.get("desiredPosition")));
            parsed.setEmail(strValue(data.get("email")));
            parsed.setPhone(strValue(data.get("phone")));
            if (data.get("yearsOfExperience") != null) {
                parsed.setWorkYears(String.valueOf(data.get("yearsOfExperience")));
            }
            if (data.get("workYears") != null) {
                parsed.setWorkYears(String.valueOf(data.get("workYears")));
            }
            List<String> skills = new ArrayList<>();
            if (data.get("skills") instanceof List<?> skillList) {
                for (Object s : skillList) {
                    // 兼容模型把多个技能写进一个字符串的情况（逗号/顿号/分号分隔）
                    for (String part : String.valueOf(s).split("[,，、;；]")) {
                        String skill = part.trim();
                        if (!skill.isBlank() && !isAiSkillNoise(skill)) {
                            skills.add(skill);
                        }
                    }
                }
            }
            parsed.setSkills(skills);
            String skillsTextRaw = strValue(data.get("skillsText"));
            if (skillsTextRaw != null && !skillsTextRaw.isBlank()) {
                // 优先使用 AI 保留的原始技能板块文本（保留原始分组与换行）
                parsed.setSkillsText(skillsTextRaw.trim());
            } else {
                // 回退：每条技能独立成行（\n\n 记录分隔 + • 圆点）
                parsed.setSkillsText(skills.stream()
                        .map(sk -> "• " + sk)
                        .collect(java.util.stream.Collectors.joining("\n\n")));
            }

            // 教育经历列表（模型可能返回完整列表；空列表时回退单字段）
            if (data.get("education") instanceof List<?> eduList) {
                for (Object item : eduList) {
                    if (item instanceof Map<?, ?> m) {
                        ParsedResume.EducationEntry edu = new ParsedResume.EducationEntry();
                        edu.setSchool(strValue(m.get("school")));
                        edu.setMajor(strValue(m.get("major")));
                        edu.setDegree(strValue(m.get("degree")));
                        edu.setStart(strValue(m.get("start")));
                        edu.setEnd(strValue(m.get("end")));
                        if (edu.getSchool() != null || edu.getMajor() != null || edu.getDegree() != null) {
                            parsed.getEducation().add(edu);
                        }
                    }
                }
            }
            if (parsed.getEducation().isEmpty()
                    && (strValue(data.get("school")) != null || strValue(data.get("major")) != null
                    || strValue(data.get("educationLevel")) != null)) {
                ParsedResume.EducationEntry edu = new ParsedResume.EducationEntry();
                edu.setSchool(strValue(data.get("school")));
                edu.setMajor(strValue(data.get("major")));
                edu.setDegree(strValue(data.get("educationLevel")));
                parsed.getEducation().add(edu);
            }

            // 工作经历列表（每家公司一段，含起止时间与工作内容）
            if (data.get("experience") instanceof List<?> expList) {
                for (Object item : expList) {
                    if (item instanceof Map<?, ?> m) {
                        ParsedResume.ExperienceEntry exp = new ParsedResume.ExperienceEntry();
                        exp.setCompany(strValue(m.get("company")));
                        exp.setPosition(strValue(m.get("position")));
                        exp.setStart(strValue(m.get("start")));
                        exp.setEnd(strValue(m.get("end")));
                        exp.setDescription(strValue(m.get("description")));
                        if (exp.getCompany() != null || exp.getPosition() != null) {
                            parsed.getExperience().add(exp);
                        }
                    }
                }
            }
            if (parsed.getExperience().isEmpty()
                    && (strValue(data.get("currentCompany")) != null
                    || strValue(data.get("currentPosition")) != null)) {
                ParsedResume.ExperienceEntry exp = new ParsedResume.ExperienceEntry();
                exp.setCompany(strValue(data.get("currentCompany")));
                exp.setPosition(strValue(data.get("currentPosition")));
                parsed.getExperience().add(exp);
            }

            // 项目经历列表
            if (data.get("projects") instanceof List<?> projList) {
                for (Object item : projList) {
                    if (item instanceof Map<?, ?> m) {
                        ParsedResume.ProjectEntry proj = new ParsedResume.ProjectEntry();
                        proj.setCompany(strValue(m.get("name")));
                        proj.setPosition(strValue(m.get("role")));
                        proj.setStart(strValue(m.get("start")));
                        proj.setEnd(strValue(m.get("end")));
                        proj.setDescription(strValue(m.get("description")));
                        if (proj.getCompany() != null || proj.getDescription() != null) {
                            parsed.getProjects().add(proj);
                        }
                    }
                }
            }

            // 个人优势 / 综合评语
            String strengths = strValue(data.get("strengths"));
            String summary = strValue(data.get("summary"));
            if (strengths != null && summary != null) {
                // 自我评价按行展示：个人优势与综合评语分行（前端 summary 支持 pre-line）
                parsed.setSummary("个人优势：\n" + strengths + "\n\n综合评语：\n" + summary);
            } else if (strengths != null) {
                parsed.setSummary("个人优势：\n" + strengths);
            } else if (summary != null) {
                parsed.setSummary(summary);
            } else {
                parsed.setSummary(strValue(data.get("message")) != null
                        ? strValue(data.get("message")) : "AI 视觉模型识别图片简历生成");
            }

            if (reviewRequired) {
                log.warn("图片简历 AI 解析需人工复核: resumeId={}, fileName={}", resumeId, fileName);
                String reason = strValue(data.get("message"));
                throw new IllegalStateException("图片简历 AI 解析需人工复核："
                        + (reason != null ? reason : "未识别到有效内容"));
            }
            log.info("图片简历 AI 解析成功: resumeId={}, name={}, skills={}",
                    resumeId, parsed.getName(), skills.size());
            return parsed;
        } catch (Exception e) {
            if (e instanceof IllegalStateException ise) {
                throw ise;
            }
            log.warn("图片简历 AI 解析失败: resumeId={}, error={}", resumeId, e.getMessage());
            throw new IllegalStateException("图片简历 AI 解析失败：" + e.getMessage(), e);
        }
    }

    /** 将图片/PDF 统一转换为 JPEG base64 Data URL 列表（PDF 最多渲染前 {@value #MAX_AI_PDF_PAGES} 页）。 */
    private List<String> toImageDataUrls(byte[] fileBytes, String fileName, Long resumeId) {
        try {
            String lower = fileName == null ? "" : fileName.toLowerCase();
            List<byte[]> imageList = new ArrayList<>();
            if (lower.endsWith(".pdf")) {
                imageList.addAll(renderPdfPagesToImage(fileBytes, resumeId));
            } else {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
                if (image == null) {
                    log.warn("无法读取图片文件: resumeId={}, file={}", resumeId, fileName);
                    return null;
                }
                imageList.add(toJpegBytes(scaleDown(image, MAX_AI_IMAGE_WIDTH)));
            }
            if (imageList.isEmpty()) {
                return null;
            }
            List<String> urls = new ArrayList<>();
            for (byte[] image : imageList) {
                urls.add("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image));
            }
            return urls;
        } catch (Exception e) {
            log.warn("图片/PDF 转码失败: resumeId={}, error={}", resumeId, e.getMessage());
            return null;
        }
    }

    /** 使用 PDFBox 把 PDF 每页渲染为高清图片（130 DPI，压缩 JPEG）。 */
    private List<byte[]> renderPdfPagesToImage(byte[] fileBytes, Long resumeId) {
        List<byte[]> pages = new ArrayList<>();
        try (PDDocument document = PDDocument.load(fileBytes)) {
            if (document.isEncrypted() || document.getNumberOfPages() == 0) {
                return pages;
            }
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = Math.min(document.getNumberOfPages(), MAX_AI_PDF_PAGES);
            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 130);
                pages.add(toJpegBytes(scaleDown(image, MAX_AI_IMAGE_WIDTH)));
            }
            log.info("PDF 渲染完成: resumeId={}, pages={}", resumeId, pages.size());
        } catch (Exception e) {
            log.warn("PDF 渲染失败: resumeId={}, error={}", resumeId, e.getMessage());
        }
        return pages;
    }

    private byte[] toJpegBytes(BufferedImage image) throws java.io.IOException {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "jpeg", out)) {
                throw new java.io.IOException("JPEG 编码失败");
            }
            return out.toByteArray();
        }
    }

    /** 限制图片宽度，避免超大 payload 导致识别失败或超时。 */
    private BufferedImage scaleDown(BufferedImage image, int maxWidth) {
        if (image.getWidth() <= maxWidth) {
            return image;
        }
        int newWidth = maxWidth;
        int newHeight = (int) Math.round(image.getHeight()
                * (double) maxWidth / image.getWidth());
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaled;
    }

    private String strValue(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    /** 判断技能是否属于通用工具/IDE 噪声（不作为标签展示）。 */
    private boolean isAiSkillNoise(String skill) {
        String lower = skill.toLowerCase();
        return AI_SKILL_DENY.stream().anyMatch(lower::contains);
    }
}
