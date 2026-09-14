package com.smartrecruit.recruitment.service.impl;

import com.smartrecruit.recruitment.domain.ParsedResume;
import com.smartrecruit.recruitment.dto.request.LlmChatRequest;
import com.smartrecruit.recruitment.feign.AiAgentLlmClient;
import com.smartrecruit.recruitment.parser.ResumeSectionParser;
import com.smartrecruit.recruitment.service.ResumeStructurerService;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简历结构化提取实现 — 本地规则解析 + 大模型增强双引擎。
 *
 * <h3>解析策略（按优先级）</h3>
 * <ol>
 *   <li>优先使用本地 {@link ResumeSectionParser} 进行规则引擎解析，
 *       可提取姓名、教育、经历、技能等核心字段，不依赖外部 API。</li>
 *   <li>若 AI 引擎可用，在本地解析结果之上调用 AI 引擎的 LLM 网关补充细节
 *       （如工作职责描述、自我评价润色等）。调用失败不影响本地结果。</li>
 * </ol>
 *
 * <p>相比初版，本地解析从简单的首行姓名+正则邮箱升级为完整的段落分割+结构化提取引擎，
 * 即使不调用大模型，也能输出教育经历、工作经历、技能列表等生产级数据。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class ResumeStructurerServiceImpl implements ResumeStructurerService {

    private final AiAgentLlmClient aiAgentLlmClient;

    private static final int MAX_TEXT_LENGTH = 12000;

    public ResumeStructurerServiceImpl(
            @Autowired(required = false) AiAgentLlmClient aiAgentLlmClient) {
        this.aiAgentLlmClient = aiAgentLlmClient;
    }

    /** 将解析文本结构化为标准简历模型。 */
    @Override
    public ParsedResume structure(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            log.warn("传入空文本，无法进行结构化提取");
            return defaultResult();
        }

        String text = rawText.length() > MAX_TEXT_LENGTH
                ? rawText.substring(0, MAX_TEXT_LENGTH)
                : rawText;

        // ── 步骤1：本地规则引擎解析（始终执行）──
        long localStart = DateUtils.currentEpochMillis();
        log.info("[计时] 本地规则引擎解析开始: 文本长度={}", text.length());
        ParsedResume result = new ResumeSectionParser().parse(text);
        long localElapsed = DateUtils.currentEpochMillis() - localStart;

        log.info("[计时] 本地解析完成: 姓名={}, 教育{}条, 经历{}条, 技能{}个, 项目{}条, 耗时 {}ms",
                result.getName(),
                result.getEducation().size(),
                result.getExperience().size(),
                result.getSkills().size(),
                result.getProjects().size(),
                localElapsed);

        // ── 步骤2：大模型增强（可选，失败不影响已有结果）──
        if (shouldUseLLM()) {
            try {
                long llmStart = DateUtils.currentEpochMillis();
                Map<String, Object> llmResult = structureWithLLM(text);
                log.info("[计时] 大模型增强完成: 耗时 {}ms", DateUtils.currentEpochMillis() - llmStart);
                result = mergeResult(result, llmResult);
                log.info("大模型增强完成，已合并结果");
            } catch (Exception e) {
                log.warn("大模型增强失败，使用本地解析结果: {}", e.getMessage());
            }
        }

        return result;
    }

    // ─── LLM 判断 ───

    private boolean shouldUseLLM() {
        return aiAgentLlmClient != null;
    }

    // ─── LLM 调用 ───

    private Map<String, Object> structureWithLLM(String text) {
        String systemPrompt =
                "You are a professional resume parser. Respond ONLY with valid JSON, no markdown, no explanation.";
        String userPrompt = buildPrompt(text);

        // 统一走 AI 引擎 LLM 网关，按 resume-parser 路由模型并统计 Token
        return aiAgentLlmClient.chat(new LlmChatRequest(systemPrompt, userPrompt, "resume-parser"));
    }

    private String buildPrompt(String resumeText) {
        return """
            Parse the following resume text and extract structured information.
            Return ONLY a JSON object (no markdown, no explanation).

            Required JSON format:
            {
              "name": "candidate name",
              "email": "email address",
              "phone": "phone number",
              "gender": "male/female (男/女)",
              "birthDate": "birth date in YYYY-MM format",
              "household": "household registration city (户籍所在地), e.g. '广东广州'",
              "politicalStatus": "political status (政治面貌), e.g. '中共党员' or '共青团员' or '群众'",
              "location": "city or region",
              "education": [{"school": "school name", "major": "major", "degree": "Bachelor/Master/PhD", "start": "YYYY-MM", "end": "YYYY-MM"}],
              "experience": [{"company": "company name", "position": "job title", "start": "YYYY-MM", "end": "YYYY-MM", "description": "key responsibilities and achievements"}],
              "projects": [{"company": "project name or client", "position": "your role", "start": "YYYY-MM", "end": "YYYY-MM", "description": "project overview, tech stack, your contributions"}],
              "skills": ["skill1", "skill2"],
              "skillsText": "original full text of the skills section, including all technical skills, soft skills, and tools listed by the candidate",
              "languages": ["language1", "language2"],
              "certifications": ["cert1", "cert2"],
              "awards": [{"name": "award name", "date": "YYYY-MM", "description": "brief description"}],
              "summary": "brief 1-2 sentence professional summary"
            }

            Rules:
            - Use empty array [] for missing list fields, empty string "" for missing text fields
            - Format dates as "YYYY-MM" (e.g., "2020-06"), use "至今" for current/present
            - Preserve original skill names (e.g., "Spring Boot" not "springboot")
            - If text is in Chinese, keep Chinese content in fields
            - Extract actual company names and job titles from the text
            - For education, look for school, degree, and major keywords in the text
            - Parse the text in the original language (Chinese or English)

            Resume text:
            """ + resumeText;
    }

    // ─── 结果合并：LLM 结果覆盖本地结果中的同名非空字段 ───

    private ParsedResume mergeResult(ParsedResume local, Map<String, Object> llm) {
        if (llm == null || llm.isEmpty()) return local;

        return ParsedResume.builder()
                // String fields: LLM non-empty value wins
                .name(coalesce(llmStr(llm, "name"), local.getName()))
                .email(coalesce(llmStr(llm, "email"), local.getEmail()))
                .phone(coalesce(llmStr(llm, "phone"), local.getPhone()))
                .gender(coalesce(llmStr(llm, "gender"), local.getGender()))
                .birthDate(coalesce(llmStr(llm, "birthDate"), local.getBirthDate()))
                .household(coalesce(llmStr(llm, "household"), local.getHousehold()))
                .politicalStatus(coalesce(llmStr(llm, "politicalStatus"), local.getPoliticalStatus()))
                .location(coalesce(llmStr(llm, "location"), local.getLocation()))
                .skillsText(coalesce(llmStr(llm, "skillsText"), local.getSkillsText()))
                .summary(coalesce(llmStr(llm, "summary"), local.getSummary()))
                .workYears(coalesce(llmStr(llm, "workYears"), local.getWorkYears()))
                .desiredPosition(coalesce(llmStr(llm, "desiredPosition"), local.getDesiredPosition()))
                .age(coalesce(llmStr(llm, "age"), local.getAge()))
                // List fields: LLM fills in when local is empty
                .education(local.getEducation().isEmpty()
                        ? llmEducationList(llm) : local.getEducation())
                .experience(local.getExperience().isEmpty()
                        ? llmExperienceList(llm) : local.getExperience())
                .projects(local.getProjects().isEmpty()
                        ? llmProjectList(llm) : local.getProjects())
                .skills(local.getSkills().isEmpty()
                        ? llmStringList(llm, "skills") : local.getSkills())
                .languages(local.getLanguages().isEmpty()
                        ? llmStringList(llm, "languages") : local.getLanguages())
                .certifications(local.getCertifications().isEmpty()
                        ? llmStringList(llm, "certifications") : local.getCertifications())
                .awards(local.getAwards().isEmpty()
                        ? llmAwardList(llm) : local.getAwards())
                .build();
    }

    // ─── 默认空结果 ───

    private ParsedResume defaultResult() {
        return ParsedResume.builder()
                .name("").email("").phone("").location("").gender("")
                .birthDate("").household("").politicalStatus("").workYears("")
                .desiredPosition("").age("").skillsText("").summary("")
                .build();
    }

    // ─── LLM Map 提取工具方法 ───

    private static String llmStr(Map<String, Object> llm, String key) {
        Object val = llm.get(key);
        return val != null ? val.toString().trim() : "";
    }

    private static String coalesce(String llmValue, String localValue) {
        return (llmValue != null && !llmValue.isBlank()) ? llmValue : (localValue != null ? localValue : "");
    }

    @SuppressWarnings("unchecked")
    private static List<String> llmStringList(Map<String, Object> llm, String key) {
        Object obj = llm.get(key);
        if (obj instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ParsedResume.EducationEntry> llmEducationList(Map<String, Object> llm) {
        Object obj = llm.get("education");
        if (!(obj instanceof List<?> list)) return List.of();
        List<ParsedResume.EducationEntry> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add(ParsedResume.EducationEntry.builder()
                        .school(strFromMap(m, "school"))
                        .major(strFromMap(m, "major"))
                        .degree(strFromMap(m, "degree"))
                        .start(strFromMap(m, "start"))
                        .end(strFromMap(m, "end"))
                        .build());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ParsedResume.ExperienceEntry> llmExperienceList(Map<String, Object> llm) {
        Object obj = llm.get("experience");
        if (!(obj instanceof List<?> list)) return List.of();
        List<ParsedResume.ExperienceEntry> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add(ParsedResume.ExperienceEntry.builder()
                        .company(strFromMap(m, "company"))
                        .position(strFromMap(m, "position"))
                        .start(strFromMap(m, "start"))
                        .end(strFromMap(m, "end"))
                        .description(strFromMap(m, "description"))
                        .build());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ParsedResume.ProjectEntry> llmProjectList(Map<String, Object> llm) {
        Object obj = llm.get("projects");
        if (!(obj instanceof List<?> list)) return List.of();
        List<ParsedResume.ProjectEntry> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add(ParsedResume.ProjectEntry.builder()
                        .company(strFromMap(m, "company"))
                        .position(strFromMap(m, "position"))
                        .start(strFromMap(m, "start"))
                        .end(strFromMap(m, "end"))
                        .description(strFromMap(m, "description"))
                        .build());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ParsedResume.AwardEntry> llmAwardList(Map<String, Object> llm) {
        Object obj = llm.get("awards");
        if (!(obj instanceof List<?> list)) return List.of();
        List<ParsedResume.AwardEntry> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add(ParsedResume.AwardEntry.builder()
                        .name(strFromMap(m, "name"))
                        .date(strFromMap(m, "date"))
                        .description(strFromMap(m, "description"))
                        .build());
            }
        }
        return result;
    }

    private static String strFromMap(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString().trim() : "";
    }
}
