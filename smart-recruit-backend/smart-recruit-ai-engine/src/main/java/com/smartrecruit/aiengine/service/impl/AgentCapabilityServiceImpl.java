package com.smartrecruit.aiengine.service.impl;

import com.smartrecruit.aiengine.agents.*;
import com.smartrecruit.aiengine.domain.*;
import com.smartrecruit.aiengine.dto.response.RetentionPredictVO;
import com.smartrecruit.aiengine.dto.response.JdVO;
import com.smartrecruit.aiengine.dto.response.InterviewQuestionVO;
import com.smartrecruit.aiengine.dto.response.ResumeParseVO;
import com.smartrecruit.aiengine.dto.response.ResumeImageParseVO;
import com.smartrecruit.aiengine.dto.response.ScreenResultVO;
import com.smartrecruit.aiengine.dto.response.PredictOfferVO;
import com.smartrecruit.aiengine.dto.response.MatchResultVO;
import com.smartrecruit.aiengine.dto.response.TalentRecommendVO;
import com.smartrecruit.aiengine.dto.request.TalentRecommendCandidate;
import com.smartrecruit.aiengine.dto.request.AnalyticsInsightRequest;
import com.smartrecruit.aiengine.dto.response.AiInsightVO;
import com.smartrecruit.aiengine.domain.TalentMatch;
import com.smartrecruit.aiengine.service.AgentCapabilityService;
import com.smartrecruit.aiengine.service.LlmGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Agent 能力网关服务实现。
 *
 * <p>每个能力都采用「LLM 优先、Agent 启发式兜底」策略：
 * 大模型可用时调用真实 LLM，不可用或失败时降级到确定性启发式实现，
 * 保证业务链路在任何配置下都能跑通。</p>
 *
 * @since 2026-04-06
 */
@Service
@Slf4j
public class AgentCapabilityServiceImpl implements AgentCapabilityService {

    /** LLM 精细排序候选池上限（与人才库「每批 8 人、候选池 Top 24」对齐）。 */
    private static final int LLM_CANDIDATE_LIMIT = 24;

    private final ObjectProvider<LlmGatewayService> llmGatewayProvider;
    private final JdGeneratorAgent jdGeneratorAgent;
    private final InterviewQuestionAgent interviewQuestionAgent;
    private final ResumeParserAgent resumeParserAgent;
    private final SmartScreenerAgent smartScreenerAgent;
    private final OfferPredictorAgent offerPredictorAgent;
    private final RetentionPredictorAgent retentionPredictorAgent;
    private final ReferralMatcherAgent referralMatcherAgent;
    private final TalentRecommenderAgent talentRecommenderAgent;

    public AgentCapabilityServiceImpl(ObjectProvider<LlmGatewayService> llmGatewayProvider,
                                      JdGeneratorAgent jdGeneratorAgent,
                                      InterviewQuestionAgent interviewQuestionAgent,
                                      ResumeParserAgent resumeParserAgent,
                                      SmartScreenerAgent smartScreenerAgent,
                                      OfferPredictorAgent offerPredictorAgent,
                                      RetentionPredictorAgent retentionPredictorAgent,
                                      ReferralMatcherAgent referralMatcherAgent,
                                      TalentRecommenderAgent talentRecommenderAgent) {
        this.llmGatewayProvider = llmGatewayProvider;
        this.jdGeneratorAgent = jdGeneratorAgent;
        this.interviewQuestionAgent = interviewQuestionAgent;
        this.resumeParserAgent = resumeParserAgent;
        this.smartScreenerAgent = smartScreenerAgent;
        this.offerPredictorAgent = offerPredictorAgent;
        this.retentionPredictorAgent = retentionPredictorAgent;
        this.referralMatcherAgent = referralMatcherAgent;
        this.talentRecommenderAgent = talentRecommenderAgent;
    }

    /**
     * 生成职位描述：LLM 生成优先，Agent 启发式兜底。
     */
    @Override
    public JdVO generateJd(String jobTitle, String department, String experienceLevel) {
        Map<String, Object> llm = tryLlm(
                "你是一位资深招聘专家，请为指定岗位生成结构化 JD，输出 JSON："
                        + "{title, department, experienceLevel, responsibilities[], requirements[], plusPoints[]}",
                String.format("岗位：%s；部门：%s；经验要求：%s",
                        nullTo(jobTitle), nullTo(department), nullTo(experienceLevel)),
                "jd-generator");

        Map<String, Object> jd = jdGeneratorAgent.generate(jobTitle, department, experienceLevel);
        if (llm != null && llm.containsKey("responsibilities")) {
            mergeLlm(jd, llm, "responsibilities");
            mergeLlm(jd, llm, "requirements");
            mergeLlm(jd, llm, "plusPoints");
        }
        return new JdVO(
                strList(jd.get("responsibilities")),
                strList(jd.get("requirements")),
                strList(jd.get("plusPoints")));
    }

    /**
     * 生成面试题：LLM 生成优先，Agent 题库兜底。
     */
    @Override
    public List<InterviewQuestionVO> generateInterviewQuestions(
            Integer interviewType, int round, String jobTitle, String candidateName) {
        Map<String, Object> llm = tryLlm(
                "你是一位资深面试官，请为候选人生成面试题，输出 JSON："
                        + "{questions: [{question, type, round, scoreStandard}]}",
                String.format("候选人：%s；职位：%s；面试类型：%d；轮次：%d",
                        nullTo(candidateName), nullTo(jobTitle), interviewType, round),
                "interview-question");

        List<Map<String, Object>> questions =
                interviewQuestionAgent.generate(interviewType, round, jobTitle);
        if (llm != null && llm.get("questions") instanceof List<?> list && !list.isEmpty()) {
            // 容错解析：LLM 输出结构不稳定时，兼容 Map（question/content/text/title 键）
            // 与纯字符串两种形态，任一题解析失败不影响其它题目，全部失败才回退模板
            List<InterviewQuestionVO> llmQuestions = new java.util.ArrayList<>();
            for (Object item : list) {
                InterviewQuestionVO vo = toQuestionVO(item, interviewType, round, jobTitle);
                if (vo != null && vo.question() != null && !vo.question().isBlank()) {
                    llmQuestions.add(vo);
                }
            }
            if (!llmQuestions.isEmpty()) {
                return llmQuestions;
            }
            log.warn("LLM 出题结果解析为空，回退题库模板: interviewType={}, round={}, jobTitle={}",
                    interviewType, round, jobTitle);
        }
        return questions.stream().map(this::toQuestionVO).toList();
    }

    /**
     * 容错转换 LLM 出题条目：支持 Map（question/content/text/title 键）与纯字符串。
     *
     * @return 题目 VO；无法识别时返回 {@code null}
     */
    private InterviewQuestionVO toQuestionVO(Object item, Integer type, int round, String jobTitle) {
        if (item instanceof Map<?, ?> m) {
            String question = firstNonBlank(
                    toStr(m.get("question")),
                    toStr(m.get("content")),
                    toStr(m.get("text")),
                    toStr(m.get("title")));
            if (question == null) {
                return null;
            }
            Integer itemType = toInteger(m.get("type"));
            Integer itemRound = toInteger(m.get("round"));
            String itemJobTitle = toStr(m.get("jobTitle"));
            return new InterviewQuestionVO(
                    question,
                    itemType != null ? itemType : type,
                    itemRound != null ? itemRound : round,
                    itemJobTitle != null ? itemJobTitle : jobTitle,
                    toStr(m.get("scoreStandard")));
        }
        if (item instanceof String s && !s.isBlank()) {
            return new InterviewQuestionVO(s, type, round, jobTitle,
                    "0-100 分，关注逻辑性、深度与实践经验");
        }
        return null;
    }

    /** 取第一个非空白字符串。 */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 解析简历文本为结构化候选人画像。
     */
    @Override
    public ResumeParseVO parseResume(String fileName, String contentText) {
        // LLM 真实解析优先，Agent 模拟解析兜底
        Map<String, Object> llm = tryLlm(
                "你是一位专业简历解析专家，请从简历文本中提取结构化候选人信息，只输出 JSON："
                        + "{name, email, phone, educationLevel, school, major, "
                        + "yearsOfExperience(数字), currentCompany, currentPosition, skills[]}",
                String.format("文件名：%s\n简历文本：\n%s", nullTo(fileName), nullTo(contentText)),
                "resume-parser");

        CandidateProfile profile = resumeParserAgent.parse(fileName);
        if (llm != null) {
            Integer years = toIntegerOrNull(llm.get("yearsOfExperience"));
            List<String> skills = llm.get("skills") instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : profile.getSkills();
            return new ResumeParseVO(
                    fileName,
                    toStr(llm.getOrDefault("name", profile.getName())),
                    toStr(llm.getOrDefault("email", profile.getEmail())),
                    toStr(llm.getOrDefault("phone", profile.getPhone())),
                    toStr(llm.getOrDefault("educationLevel", profile.getEducationLevel())),
                    toStr(llm.getOrDefault("school", profile.getSchool())),
                    toStr(llm.getOrDefault("major", profile.getMajor())),
                    years != null ? years : profile.getYearsOfExperience(),
                    toStr(llm.getOrDefault("currentCompany", profile.getCurrentCompany())),
                    toStr(llm.getOrDefault("currentPosition", profile.getCurrentPosition())),
                    skills != null ? skills : profile.getSkills());
        }
        return new ResumeParseVO(
                fileName, profile.getName(), profile.getEmail(), profile.getPhone(),
                profile.getEducationLevel(), profile.getSchool(), profile.getMajor(),
                profile.getYearsOfExperience(), profile.getCurrentCompany(),
                profile.getCurrentPosition(), profile.getSkills());
    }

    /**
     * 解析图片/扫描件简历：AI 视觉模型优先，识别不完整时标记需人工复核。
     */
    @Override
    public ResumeImageParseVO parseResumeImage(String fileName, List<String> base64Images) {
        Map<String, Object> llm = tryLlmImage(fileName, base64Images);
        if (llm != null && toStr(llm.get("name")) != null && !toStr(llm.get("name")).isBlank()) {
            List<String> skills = llm.get("skills") instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : List.of();
            return new ResumeImageParseVO(
                    fileName, false, null,
                    toStr(llm.get("name")),
                    toStr(llm.get("email")),
                    toStr(llm.get("phone")),
                    toStr(llm.get("educationLevel")),
                    toStr(llm.get("school")),
                    toStr(llm.get("major")),
                    toIntegerOrNull(llm.get("yearsOfExperience")),
                    toStr(llm.get("currentCompany")),
                    toStr(llm.get("currentPosition")),
                    skills,
                    toStr(llm.get("skillsText")),
                    parseEducation(llm.get("education")),
                    parseExperience(llm.get("experience")),
                    parseProjects(llm.get("projects")),
                    toStr(llm.get("strengths")),
                    toStr(llm.get("summary")),
                    toStr(llm.get("gender")),
                    toStr(llm.get("household")),
                    toStr(llm.get("location")),
                    toStr(llm.get("age")),
                    toStr(llm.get("birthDate")),
                    toStr(llm.get("workYears")),
                    toStr(llm.get("politicalStatus")),
                    toStr(llm.get("desiredPosition")));
        }
        return new ResumeImageParseVO(fileName, true,
                "图片/扫描件简历无法通过 AI 完整识别，请人工复核",
                null, null, null, null, null, null, null, null, null,
                List.of(), null, List.of(), List.of(), List.of(), null, null,
                null, null, null, null, null, null, null, null);
    }

    /** 解析教育经历列表。 */
    @SuppressWarnings("unchecked")
    private List<ResumeImageParseVO.EducationItem> parseEducation(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<ResumeImageParseVO.EducationItem> result = new java.util.ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add(new ResumeImageParseVO.EducationItem(
                        toStr(m.get("school")), toStr(m.get("major")),
                        toStr(m.get("degree")), toStr(m.get("start")), toStr(m.get("end"))));
            }
        }
        return result;
    }

    /** 解析工作经历列表。 */
    @SuppressWarnings("unchecked")
    private List<ResumeImageParseVO.ExperienceItem> parseExperience(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<ResumeImageParseVO.ExperienceItem> result = new java.util.ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add(new ResumeImageParseVO.ExperienceItem(
                        toStr(m.get("company")), toStr(m.get("position")),
                        toStr(m.get("start")), toStr(m.get("end")),
                        toStr(m.get("description"))));
            }
        }
        return result;
    }

    /** 解析项目经历列表。 */
    @SuppressWarnings("unchecked")
    private List<ResumeImageParseVO.ProjectItem> parseProjects(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<ResumeImageParseVO.ProjectItem> result = new java.util.ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add(new ResumeImageParseVO.ProjectItem(
                        toStr(m.get("name")), toStr(m.get("role")),
                        toStr(m.get("start")), toStr(m.get("end")),
                        toStr(m.get("description"))));
            }
        }
        return result;
    }

    /** 调用 AI 视觉模型解析图片简历（支持 PDF 多页渲染的多图输入）。 */
    private Map<String, Object> tryLlmImage(String fileName, List<String> base64Images) {
        LlmGatewayService llm = llmGatewayProvider.getIfAvailable();
        if (llm == null) {
            return null;
        }
        try {
            String systemPrompt = "你是一位专业简历解析专家，请识别图片/扫描件中的简历内容，"
                    + "只输出一个 JSON 对象，不要输出任何解释："
                    + "{name, gender(男/女), household(籍贯), location(现居地), age(数字), "
                    + "birthDate(出生日期), politicalStatus(政治面貌), desiredPosition(期望职位), "
                    + "email, phone, educationLevel, school, major, "
                    + "yearsOfExperience(数字), currentCompany, currentPosition, skills[], "
                    + "skillsText(技能板块原文，逐行保留原始简历的换行与分组，"
                    + "一行一个分组，组内技能用顿号/逗号分隔，不要逐项拆成单行), "
                    + "education:[{school,major,degree,start,end}], "
                    + "experience:[{company,position,start,end,description}], "
                    + "projects:[{name,role,start,end,description}], "
                    + "strengths(个人优势), summary(综合评语)}。"
                    + "请完整提取工作经历（每家公司一段，含起止时间与工作内容）、"
                    + "项目经历（每个项目名称/角色/时间/内容）和个人优势，不要遗漏。"
                    + "experience[] 每项必须包含 company(公司名称)/position(职位)/start/end/description，"
                    + "不要缺字段，也不要将公司名称、职位、工作描述合并进同一个字段；"
                    + "projects[] 每项必须包含 name(项目名称)/role(角色)/start/end/description，"
                    + "字段间相互独立；"
                    + "description 的文本格式要求（与系统其它简历一致）："
                    + "多条内容必须用换行符 \\n 分隔，每条一行，保留原有编号或列表符号（如 1. 2. 3. 或 •），"
                    + "不要合并成一段文本；"
                    + "projects[].description 的格式为："
                    + "第一行写「技术栈：<使用技术>」，"
                    + "随后用 \\n 分行分别列出「项目描述：」「我的职责：」「项目亮点：」等内容；"
                    + "experience[].description 直接分行列出各条工作内容（可加「工作描述：」前缀）。"
                    + "skills[] 只输出简历中的主要技术栈，最多 15 项（如 Java、Spring、MySQL、Redis、"
                    + "Kafka、微服务架构），不要输出子知识点（如 JUC框架、ConcurrentHashMap源码实现、"
                    + "JVM内存模型）、通用工具或 IDE（如 IDEA、VSCode、Navicat、DataGrip、Postman、"
                    + "Apifox、Git、Maven、Gradle）；skillsText 仍必须保留原始技能板块的完整分组换行，"
                    + "两者都要输出，不要省略 skillsText。"
                    + "如果输入是多页图片，请综合全部页面识别；"
                    + "如果图片不是简历或无法识别，返回 {\"name\":\"\"}。";
            String textPrompt = "文件名：" + nullTo(fileName)
                    + "\n共 " + (base64Images == null ? 0 : base64Images.size())
                    + " 页图片，请识别并提取以上简历信息。";
            Map<String, Object> result = llm.chatWithImage(
                    systemPrompt, textPrompt, base64Images,
                    "resume-parser", "resume-parser-image");
            return result == null || result.isEmpty() ? null : result;
        } catch (Exception e) {
            log.warn("图片简历 AI 解析失败，标记需人工复核: fileName={}, error={}",
                    fileName, e.getMessage());
            return null;
        }
    }

    /**
     * 智能筛选候选人：LLM 评分优先，Agent 规则评分兜底。
     */
    @Override
    public ScreenResultVO screenCandidate(CandidateProfile candidate, JobRequirement requirement) {
        Map<String, Object> llm = tryLlm(
                "你是一位招聘筛选专家，请评估候选人匹配度，输出 JSON："
                        + "{overallScore, passed, recommendation, summary, dimensionScores}",
                String.format("候选人技能：%s；岗位要求技能：%s；经验：%s年",
                        candidate.getSkills(), requirement.getRequiredSkills(),
                        candidate.getYearsOfExperience()),
                "smart-screener");

        ScreeningResult result = smartScreenerAgent.screen(candidate, requirement);
        Double overallScore = result.getOverallScore();
        String recommendation = result.getRecommendation();
        if (llm != null && llm.get("overallScore") != null) {
            overallScore = toDouble(llm.get("overallScore"));
            recommendation = toStr(llm.getOrDefault("recommendation", recommendation));
        }
        return new ScreenResultVO(overallScore, result.getPassed(), recommendation,
                result.getSummary(), result.getDimensionScores());
    }

    /**
     * 预测候选人接受 Offer 的概率与风险。
     */
    @Override
    public PredictOfferVO predictOffer(CandidateProfile candidate, OfferDetail offer) {
        Map<String, Object> llm = tryLlm(
                "你是一位薪酬与招聘专家，请预测候选人接受 Offer 的概率，输出 JSON："
                        + "{acceptanceProbability(0-100的整数百分比，如72), riskLevel(LOW/MEDIUM/HIGH), "
                        + "recommendation, factorInfluences(因素名到影响值的映射)}",
                String.format("候选人：%s；职位：%s；薪资总包：%s",
                        nullTo(candidate.getName()), nullTo(offer.getJobTitle()), offer.getTotalPackage()),
                "offer-predictor");

        PredictionResult result = offerPredictorAgent.predict(candidate, offer);
        Double acceptanceProbability = result.getAcceptanceProbability();
        String riskLevel = result.getRiskLevel();
        Map<String, Double> factorInfluences = result.getFactorInfluences();
        String recommendation = result.getRecommendation();
        if (llm != null && llm.get("acceptanceProbability") != null) {
            acceptanceProbability = normalizeProbability(
                    toDoubleOrNull(llm.get("acceptanceProbability")));
            recommendation = toStr(llm.getOrDefault("recommendation", recommendation));
            riskLevel = toStr(llm.getOrDefault("riskLevel", riskLevel));
            if (llm.get("factorInfluences") instanceof Map<?, ?> factors) {
                Map<String, Double> parsed = new java.util.LinkedHashMap<>();
                factors.forEach((k, v) -> {
                    Double d = toDoubleOrNull(v);
                    if (d != null) {
                        parsed.put(String.valueOf(k), d);
                    }
                });
                if (!parsed.isEmpty()) {
                    factorInfluences = parsed;
                }
            }
        }
        return new PredictOfferVO(acceptanceProbability, normalizeRiskLevel(riskLevel),
                recommendation, factorInfluences);
    }

    /** 概率规范化：0-1 小数视为百分比小数转 0-100，并钳制到 0-100。 */
    private Double normalizeProbability(Double value) {
        if (value == null) {
            return null;
        }
        double p = value <= 1.0 ? value * 100.0 : value;
        return Math.max(0.0, Math.min(100.0, Math.round(p * 10.0) / 10.0));
    }

    /**
     * 新员工留存风险预测：LLM 生成优先，Agent 启发式兜底。
     */
    @Override
    @SuppressWarnings("unchecked")
    public RetentionPredictVO predictRetention(OnboardingCandidate candidate) {
        Map<String, Object> llm = tryLlm(
                "你是一位资深HR专家，请基于入职候选人画像预测留任风险，只输出一个 JSON 对象："
                        + "{\"retentionScore6M\":0-100整数,\"retentionScore12M\":0-100整数,"
                        + "\"riskLevel\":\"LOW/MEDIUM/HIGH\","
                        + "\"riskFactors\":{\"jobHoppingHistory\":0-1,\"compensationAlignment\":0-1,"
                        + "\"commuteDistance\":0-1,\"careerGrowth\":0-1,\"culturalFit\":0-1},"
                        + "\"recommendation\":\"不超过30字的干预建议\","
                        + "\"interventions\":[\"可执行的干预动作1\",\"干预动作2\"]}。"
                        + "注意：不同候选人的评分必须依据各自信息有明显区分度，"
                        + "不得对所有候选人给出相同分数；薪资、学历、年限等信息缺失时，"
                        + "请结合岗位级别与行业经验合理推断，并在风险因素中体现差异。"
                        + "入职流程尚未完成（onboardingCompletion 越低）应显著提高留任风险；"
                        + "历史跳槽次数越多、平均在职时长越短，jobHoppingHistory 风险越高。",
                String.format("员工：%s；部门：%s；岗位：%s；入职日期：%s；薪资：%s；学历：%s；"
                                + "工作年限：%s；历史跳槽次数：%s；平均在职时长：%s个月；"
                                + "入职流程完成度：%s%%；通勤距离：%s；是否内推：%s",
                        nullTo(candidate.getName()), nullTo(candidate.getDepartment()),
                        nullTo(candidate.getPosition()), candidate.getOnboardDate(),
                        candidate.getSalary(), nullTo(candidate.getEducationLevel()),
                        candidate.getYearsOfExperience(), candidate.getPreviousJobCount(),
                        candidate.getAvgTenureMonths(), candidate.getOnboardingCompletion(),
                        nullTo(candidate.getCommuteDistance()), candidate.getIsReferral()),
                "retention-predictor");

        RetentionRisk risk = retentionPredictorAgent.predict(candidate);

        if (llm != null && llm.get("retentionScore6M") != null) {
            double score6M = clampScore(toDoubleOrNull(llm.get("retentionScore6M")));
            double score12M = clampScore(toDoubleOrNull(llm.get("retentionScore12M")));
            String riskLevel = toStr(llm.getOrDefault("riskLevel", risk.getRiskLevel()));
            Map<String, Double> riskFactors = new java.util.LinkedHashMap<>();
            if (llm.get("riskFactors") instanceof Map<?, ?> factors) {
                factors.forEach((k, v) -> {
                    Double d = toDoubleOrNull(v);
                    if (d != null) {
                        riskFactors.put(String.valueOf(k), Math.max(0.0, Math.min(1.0, d)));
                    }
                });
            }
            String recommendation = toStr(llm.getOrDefault("recommendation", risk.getRecommendation()));
            List<String> interventions = new java.util.ArrayList<>();
            if (llm.get("interventions") instanceof List<?> list) {
                list.forEach(item -> {
                    String s = item == null ? "" : item.toString();
                    if (!s.isBlank()) {
                        interventions.add(s);
                    }
                });
            }
            if (interventions.isEmpty()) {
                interventions.add(recommendation);
            }
            log.info("AI 留任预测完成(LLM): employeeId={}, score6M={}, riskLevel={}",
                    candidate.getEmployeeId(), score6M, riskLevel);
            return new RetentionPredictVO(score6M, score12M,
                    normalizeRiskLevel(riskLevel), riskFactors, recommendation, interventions);
        }

        // Agent 兜底：风险分数反转为留任概率
        double score6M = Math.round((1.0 - risk.getRiskScore()) * 1000.0) / 10.0;
        double score12M = Math.max(0.0, score6M - 10.0);
        String recommendation = risk.getRecommendation() == null || risk.getRecommendation().isBlank()
                ? "定期跟踪观察" : risk.getRecommendation();
        log.info("AI 留任预测完成(Agent兜底): employeeId={}, score6M={}, riskLevel={}",
                candidate.getEmployeeId(), score6M, risk.getRiskLevel());
        return new RetentionPredictVO(score6M, score12M,
                normalizeRiskLevel(risk.getRiskLevel()),
                risk.getRiskFactors() == null ? Map.of() : risk.getRiskFactors(),
                recommendation, List.of(recommendation));
    }

    /** 留任/风险分数钳制到 0-100。 */
    private double clampScore(Double value) {
        if (value == null) {
            return 70.0;
        }
        return Math.round(Math.max(0.0, Math.min(100.0, value)) * 10.0) / 10.0;
    }

    /** 风险级别规范化：仅允许 LOW/MEDIUM/HIGH。 */
    private String normalizeRiskLevel(String level) {
        String upper = level == null ? "" : level.trim().toUpperCase();
        return switch (upper) {
            case "LOW", "MEDIUM", "HIGH" -> upper;
            default -> "MEDIUM";
        };
    }

    /**
     * 内推职位智能匹配：LLM 匹配优先，Agent 规则匹配兜底。
     */
    @Override
    public MatchResultVO matchReferral(CandidateProfile candidate, List<JobMatchTarget> jobs) {
        Map<String, Object> llm = tryLlm(
                "你是一位内推匹配专家，请为候选人匹配最合适的职位，输出 JSON："
                        + "{matchedJobId, matchedJobTitle, matchScore, recommendation, suggestedApproach}",
                String.format("候选人：%s；技能：%s；候选职位数：%d",
                        nullTo(candidate.getName()), candidate.getSkills(), jobs.size()),
                "referral-matcher");

        ReferralMatch result = referralMatcherAgent.match(candidate, jobs);
        Double matchScore = result.getMatchScore();
        String recommendation = result.getRecommendation();
        if (llm != null && llm.get("matchedJobId") != null) {
            matchScore = toDouble(llm.get("matchScore"));
            recommendation = toStr(llm.getOrDefault("recommendation", recommendation));
        }
        return new MatchResultVO(result.getMatchedJobId(), result.getMatchedJobTitle(),
                matchScore, recommendation, result.getSuggestedApproach(), result.getMatchDimensions());
    }

    /**
     * AI 人才推荐：LLM 排序优先，Agent 启发式兜底。
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<TalentRecommendVO> recommendTalent(JobRequirement job,
                                                   List<TalentRecommendCandidate> candidates) {
        List<CandidateProfile> profiles = candidates.stream()
                .map(this::toProfile)
                .toList();

        // 1. 启发式 Agent 粗筛：按规则匹配分排序，取 Top 24 作为 LLM 精细排序的候选池。
        //    避免候选人数过多导致 LLM 输出被 maxTokens 截断、JSON 不完整。
        List<TalentMatch> matches = talentRecommenderAgent.recommend(job, profiles);
        List<TalentRecommendCandidate> llmCandidates = matches.stream()
                .sorted(java.util.Comparator
                        .comparing(TalentMatch::getMatchScore,
                                java.util.Comparator.nullsFirst(Double::compareTo)).reversed())
                .limit(LLM_CANDIDATE_LIMIT)
                .map(m -> candidates.stream()
                        .filter(c -> c.candidateId() != null
                                && c.candidateId().equals(m.getCandidateId()))
                        .findFirst().orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<String, Object> llm = llmCandidates.isEmpty() ? null : tryLlm(
                "你是一位资深招聘专家，正在为【" + nullTo(job.getJobTitle())
                        + "】岗位筛选候选人。请严格按照岗位角色匹配："
                        + "如果是产品/运营/市场/HR 类岗位，只推荐具备产品经理或相关业务经验的候选人，"
                        + "纯技术背景（后端/前端开发、算法等）且无产品经验的候选人必须给出 ≤40 的低分，不要推荐；"
                        + "反之技术岗只推荐技术背景候选人。"
                        + "请只输出一个 JSON 对象，不要输出任何解释或其他内容："
                        + "{\"candidates\":[{candidateId,candidateName,"
                        + "matchScore(0-100整数),recommendation(不超过20字),"
                        + "matchDimensions:{skillMatch,roleMatch,experienceMatch,educationMatch,semanticMatch"
                        + "(均为0-100整数)}}]}",
                String.format("目标职位：%s；要求技能：%s；最低年限：%s；候选人数：%d",
                        nullTo(job.getJobTitle()), job.getRequiredSkills(),
                        job.getMinYearsOfExperience(), llmCandidates.size())
                        + "\n候选人明细：" + llmCandidates.stream()
                        .map(c -> c.candidateId() + "(" + nullTo(c.name())
                                + ",当前岗位:" + nullTo(c.currentPosition())
                                + ",技能:" + (c.skills() == null ? "" : String.join(",", c.skills())))
                        .reduce("", (a, b) -> a + "\n" + b),
                "talent-recommender");

        // 2. 组装结果：LLM 评分优先，缺失的候选人保留启发式结果
        List<TalentRecommendVO> vos = matches.stream()
                .map(m -> new TalentRecommendVO(m.getCandidateId(), m.getCandidateName(),
                        m.getMatchScore(), m.getRecommendation(), m.getMatchDimensions()))
                .collect(java.util.stream.Collectors.toList());

        if (llm != null && llm.get("candidates") instanceof List<?> list && !list.isEmpty()) {
            Map<Long, TalentRecommendVO> llmMap = new java.util.LinkedHashMap<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Long candidateId = m.get("candidateId") instanceof Number n
                            ? n.longValue() : null;
                    if (candidateId == null) continue;
                    Double score = toDoubleOrNull(m.get("matchScore"));
                    Map<String, Double> dims = new java.util.LinkedHashMap<>();
                    if (m.get("matchDimensions") instanceof Map<?, ?> dm) {
                        dm.forEach((k, v) -> {
                            Double d = toDoubleOrNull(v);
                            if (d != null) dims.put(String.valueOf(k), d);
                        });
                    }
                    llmMap.put(candidateId, new TalentRecommendVO(
                            candidateId,
                            m.get("candidateName") != null ? m.get("candidateName").toString()
                                    : String.valueOf(candidateId),
                            score != null ? score : 0.0,
                            m.get("recommendation") != null ? m.get("recommendation").toString() : "",
                            dims));
                }
            }
            if (!llmMap.isEmpty()) {
                // 用 LLM 评分覆盖，缺失的候选人保留启发式结果
                vos = vos.stream()
                        .map(v -> llmMap.getOrDefault(v.candidateId(), v))
                        .sorted(java.util.Comparator
                                .comparing(TalentRecommendVO::matchScore).reversed())
                        .toList();
            }
        }
        // 3. 过滤明显不匹配（低分）候选人，避免技术背景被误推荐到产品岗；
        //    候选池限制 Top 24，与前端「每批 8 人、换一批」的分页逻辑对齐
        return vos.stream()
                .filter(v -> v.matchScore() != null && v.matchScore() >= 45.0)
                .sorted(java.util.Comparator
                        .comparing(TalentRecommendVO::matchScore).reversed())
                .limit(LLM_CANDIDATE_LIMIT)
                .toList();
    }

    /** 请求候选人 → 候选人画像（携带 candidateId 回传）。 */
    private CandidateProfile toProfile(TalentRecommendCandidate c) {
        return CandidateProfile.builder()
                .candidateId(c.candidateId())
                .name(c.name())
                .educationLevel(c.educationLevel())
                .yearsOfExperience(c.yearsOfExperience())
                .currentCompany(c.currentCompany())
                .currentPosition(c.currentPosition())
                .skills(c.skills() == null ? List.of() : c.skills())
                .summary(c.summary())
                .build();
    }

    private Double toDoubleOrNull(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /** 安全转整数：非法输入返回 {@code null}。 */
    private Integer toIntegerOrNull(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * AI 数据分析洞察：LLM 生成优先，规则模板兜底。
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<AiInsightVO> generateAnalyticsInsights(AnalyticsInsightRequest request) {
        Map<String, Object> llm = tryLlm(
                "你是一位资深招聘数据分析师，请基于以下招聘统计数据生成 4-6 条数据洞察，"
                        + "输出 JSON：{\"insights\":[{title,description,trend,value}]}。"
                        + "trend 取值 UP/DOWN/STABLE，value 为关键数值（如 28.5%），"
                        + "description 需包含具体业务建议。",
                summarize(request),
                null);

        if (llm != null && llm.get("insights") instanceof List<?> list) {
            List<AiInsightVO> vos = new java.util.ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    String trend = toStr(m.get("trend"));
                    vos.add(new AiInsightVO(
                            toStr(m.get("title")),
                            toStr(m.get("description")),
                            "DOWN".equalsIgnoreCase(trend) ? "DOWN"
                                    : "UP".equalsIgnoreCase(trend) ? "UP" : "STABLE",
                            toStr(m.get("value"))));
                }
            }
            if (!vos.isEmpty()) {
                return vos.stream().limit(6).toList();
            }
        }
        return heuristicInsights(request);
    }

    /** 把统计数据压缩成 LLM 可读的文本。 */
    private String summarize(AnalyticsInsightRequest request) {
        StringBuilder sb = new StringBuilder("统计周期：")
                .append(nullTo(request.startDate())).append(" ~ ").append(nullTo(request.endDate())).append('\n');
        if (request.kpis() != null) {
            for (var k : request.kpis()) {
                sb.append("KPI-").append(k.label()).append('=').append(k.value())
                        .append(k.unit()).append("（上期 ").append(k.prevValue()).append("）\n");
            }
        }
        if (request.funnel() != null) {
            sb.append("漏斗：");
            request.funnel().forEach(f -> sb.append(f.name()).append(':').append(f.count()).append(' '));
            sb.append('\n');
        }
        if (request.channels() != null) {
            for (var c : request.channels()) {
                sb.append("渠道-").append(c.sourceName()).append("：候选人 ").append(c.candidateCount())
                        .append("，入职 ").append(c.hireCount()).append("，转化率 ")
                        .append(c.hireRate()).append("%\n");
            }
        }
        if (request.candidateTrend() != null) {
            sb.append("候选人趋势：");
            request.candidateTrend().forEach(t -> sb.append(t.date()).append(':').append(t.count()).append(' '));
            sb.append('\n');
        }
        if (request.onboardingTrend() != null) {
            sb.append("入职趋势：");
            request.onboardingTrend().forEach(t -> sb.append(t.date()).append(':').append(t.count()).append(' '));
        }
        return sb.toString();
    }

    /** 规则模板兜底洞察。 */
    private List<AiInsightVO> heuristicInsights(AnalyticsInsightRequest request) {
        List<AiInsightVO> insights = new java.util.ArrayList<>();
        if (request.channels() != null) {
            request.channels().stream()
                    .filter(c -> c.candidateCount() != null && c.candidateCount() > 0)
                    .max(java.util.Comparator.comparingDouble(
                            c -> c.hireRate() == null ? -1 : c.hireRate()))
                    .ifPresent(c -> insights.add(new AiInsightVO(
                            "最佳渠道：" + nullTo(c.sourceName()),
                            c.sourceName() + " 渠道入职转化率 " + c.hireRate()
                                    + "%，共入职 " + c.hireCount() + " 人，建议加大该渠道投入。",
                            "UP", String.format("%.1f%%", c.hireRate()))));
        }
        if (request.kpis() != null) {
            request.kpis().stream()
                    .filter(k -> "candidates".equals(k.key()))
                    .findFirst()
                    .ifPresent(k -> insights.add(new AiInsightVO(
                            "候选人增长",
                            "本周期新增候选人 " + k.value() + k.unit()
                                    + "，较上期变化 " + diffText(k.value(), k.prevValue(), k.unit()) + "。",
                            k.value() != null && k.prevValue() != null
                                    && k.value() >= k.prevValue() ? "UP" : "STABLE",
                            k.value() + k.unit())));
        }
        if (insights.isEmpty()) {
            insights.add(new AiInsightVO("暂无显著洞察",
                    "当前统计数据不足以生成洞察，请扩大统计周期后重试。", "STABLE", "—"));
        }
        return insights;
    }

    private String diffText(Double value, Double prev, String unit) {
        if (value == null || prev == null || prev == 0) return "—";
        double diff = (value - prev) / prev * 100;
        return String.format("%+.1f%%", diff);
    }

    // ==================== 工具 ====================

    private Map<String, Object> tryLlm(String systemPrompt, String userPrompt, String agentId) {
        LlmGatewayService llm = llmGatewayProvider.getIfAvailable();
        if (llm == null) {
            return null;
        }
        try {
            Map<String, Object> result = llm.chat(systemPrompt, userPrompt, agentId);
            return result == null || result.isEmpty() ? null : result;
        } catch (Exception e) {
            log.warn("LLM 能力调用失败，降级到 Agent 启发式: error={}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeLlm(Map<String, Object> target, Map<String, Object> llm, String key) {
        if (llm.get(key) instanceof List<?> list && !list.isEmpty()) {
            target.put(key, list);
        }
    }

    private String nullTo(String value) {
        return value == null || value.isBlank() ? "未填写" : value.trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> strList(Object obj) {
        return obj instanceof List<?> list ? (List<String>) list : List.of();
    }

    private InterviewQuestionVO toQuestionVO(Map<String, Object> q) {
        return new InterviewQuestionVO(
                toStr(q.get("question")),
                toInteger(q.get("type")),
                toInteger(q.get("round")),
                toStr(q.get("jobTitle")),
                toStr(q.get("scoreStandard")));
    }

    private String toStr(Object obj) {
        return obj != null ? String.valueOf(obj) : null;
    }

    private Integer toInteger(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        if (obj instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
