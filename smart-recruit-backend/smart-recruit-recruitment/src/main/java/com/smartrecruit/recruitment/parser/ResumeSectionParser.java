package com.smartrecruit.recruitment.parser;

import lombok.extern.slf4j.Slf4j;

import com.smartrecruit.recruitment.domain.ParsedResume;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 简历段落解析引擎 — 生产级简历文本结构化提取。
 *
 * <p>基于段落识别 + 正则模式匹配，支持中英文混合简历。
 * 无需依赖外部大模型即可提取姓名、教育经历、工作经历、技能等核心字段。
 * 大模型可作为增强层叠加在解析结果之上。</p>
 *
 * <h3>解析流程</h3>
 * <ol>
 *   <li>文本预处理（规范化空白、统一换行）</li>
 *   <li>段落分割（按标题行自动切分为个人信息、教育、经历、技能等段落）</li>
 *   <li>逐段应用专用提取逻辑</li>
 *   <li>汇总为结构化 Map</li>
 * </ol>
 *
 * @since 2026-05-14
 */
@Slf4j
public class ResumeSectionParser {

    // ================================================================
    // 常量
    // ================================================================

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:电话|手机|Phone|Mobile|Tel|联系电话)?[：:.\\s]*((?:\\+?86[\\s\\-]?)?1[3-9]\\d{9})");

    /** 日期分隔符：连字符、破折号、全角/半角波浪线、中文"至""到" */
    private static final String DATE_SEP = "[\\-–—－〜~至到]";

    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "(\\d{4}[.\\-–—/年]\\s*\\d{1,2}|至今|Present|\\d{4}[.\\-–—/年])\\s*"
                    + DATE_SEP + "+\\s*"
                    + "(\\d{4}[.\\-–—/年]\\s*\\d{1,2}|至今|Present|\\d{4}[.\\-–—/年]|至今|Present)");

    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile(
            "(\\d{4})[.\\-–—/年]\\s*(\\d{1,2})?[月]?");

    private static final Pattern CHINESE_NAME_PATTERN = Pattern.compile(
            "^[\\u4e00-\\u9fa5·]{2,4}$");

    /** "姓名" 前缀提取模式："姓名：张三" / "姓名: 张三" */
    private static final Pattern NAME_LABEL_PATTERN = Pattern.compile(
            "姓名[：:\\s]*([\\u4e00-\\u9fa5·]{2,4})");

    /** 行首中文姓名提取：从行首提取2-3个连续中文字符作为姓名候选 */
    private static final Pattern LEADING_NAME_PATTERN = Pattern.compile(
            "^([\\u4e00-\\u9fa5]{2,3})(?:[\\s|,，、/·◆●○■□▲△▼▽★☆●○]|$)");

    /** 常见中英文姓氏首字符（用于判断中文姓名） */
    private static final Set<Character> CHINESE_SURNAME_CHARS = Set.of(
            '王', '李', '张', '刘', '陈', '杨', '黄', '赵', '吴', '周',
            '徐', '孙', '马', '朱', '胡', '郭', '何', '高', '林', '罗',
            '郑', '梁', '谢', '宋', '唐', '许', '韩', '冯', '邓', '曹',
            '彭', '曾', '肖', '田', '董', '潘', '袁', '蔡', '蒋', '余',
            '于', '杜', '叶', '程', '苏', '魏', '吕', '丁', '任', '沈',
            '姚', '卢', '姜', '崔', '钟', '谭', '陆', '汪', '范', '金',
            '石', '廖', '贾', '夏', '韦', '付', '方', '白', '邹', '孟',
            '熊', '秦', '邱', '江', '尹', '薛', '闫', '段', '雷', '侯',
            '龙', '史', '陶', '黎', '贺', '顾', '毛', '郝', '龚', '邵',
            '万', '钱', '严', '覃', '武', '戴', '莫', '孔', '向', '汤'
    );

    /**
     * 非姓名的文档标题/关键字，即使匹配中文姓名模式也应排除。
     * 包括简历标题、求职术语、问候语等。
     */
    private static final Set<String> NON_NAME_KEYWORDS = Set.of(
            "求职简历", "个人简历", "我的简历", "中文简历", "英文简历", "应聘简历",
            "简历模板", "在线简历", "附件简历", "最新简历",
            "求职意向", "求职目标", "应聘岗位", "期望职位",
            "个人优势", "个人亮点", "个人主页",
            "职业技能", "职业能力", "工作技能",
            "resume", "cv", "curriculum vitae", "curriculum"
    );

    private static final Pattern ENGLISH_NAME_PATTERN = Pattern.compile(
            "^[A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,2}$");

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s]+|linkedin\\.com/in/[^\\s]+|github\\.com/[^\\s]+");

    // ================================================================
    // 段落标题词库
    // ================================================================

    /** 个人信息/联系方式段落标题 */
    private static final Set<String> CONTACT_HEADERS = Set.of(
            "联系方式", "基本信息", "个人信息", "个人资料",
            "contact", "personal info", "personal information",
            "profile", "个人概况", "基本资料"
    );

    /** 教育经历段落标题 */
    private static final Set<String> EDUCATION_HEADERS = Set.of(
            "教育背景", "教育经历", "教育", "学历", "学习经历",
            "education", "academic background", "academic",
            "educational background", "学历背景", "学业背景"
    );

    /** 工作/实习经历段落标题 */
    private static final Set<String> EXPERIENCE_HEADERS = Set.of(
            "工作经历", "工作经验", "实习经历",
            "工作履历", "职业经历", "任职经历",
            "experience", "work experience", "professional experience",
            "employment", "work history", "career history",
            "internship", "internship experience"
    );

    /** 项目经历段落标题 */
    private static final Set<String> PROJECT_HEADERS = Set.of(
            "项目经历", "项目经验",
            "projects", "project experience",
            "主要项目", "参与项目"
    );

    /** 技能段落标题 */
    private static final Set<String> SKILLS_HEADERS = Set.of(
            "技能", "专业技能", "技术栈", "技术能力", "掌握技能",
            "技能特长", "专业能力", "个人技能", "核心能力",
            "职业技能", "职业能力", "工作技能", "职业技能与特长",
            "skills", "technical skills", "core skills",
            "technologies", "tech stack", "expertise",
            "核心技能", "技术特长", "编程语言"
    );

    /** 语言能力段落标题 */
    private static final Set<String> LANGUAGES_HEADERS = Set.of(
            "语言能力", "语言", "外语水平", "外语能力",
            "languages", "language skills", "language"
    );

    /** 证书/资质段落标题 */
    private static final Set<String> CERTIFICATIONS_HEADERS = Set.of(
            "证书", "资质", "资格证书", "所获证书", "证书资质",
            "certifications", "certificates", "credentials",
            "专业证书", "资质证书", "职业资格"
    );

    /** 奖项荣誉段落标题 */
    private static final Set<String> AWARDS_HEADERS = Set.of(
            "奖项", "荣誉", "获奖", "奖励", "所获奖励", "所获荣誉",
            "个人荣誉", "获奖经历", "奖项荣誉", "荣誉奖项",
            "奖项荣誉证书", "获奖情况", "在校奖励",
            "awards", "honors", "achievements", "awards and honors",
            "awards & honors", "honors & awards"
    );

    /** 自我评价/总结段落标题 */
    private static final Set<String> SUMMARY_HEADERS = Set.of(
            "自我评价", "个人总结", "自我描述", "自我介绍", "个人简介",
            "summary", "profile summary", "professional summary",
            "objective", "career objective", "about me",
            "个人陈述", "求职意向", "求职目标", "职业目标"
    );

    /** 所有已知的段落标题（用于姓名识别时排除） */
    private static final Set<String> ALL_KNOWN_HEADERS = new HashSet<>();

    static {
        ALL_KNOWN_HEADERS.addAll(CONTACT_HEADERS);
        ALL_KNOWN_HEADERS.addAll(EDUCATION_HEADERS);
        ALL_KNOWN_HEADERS.addAll(EXPERIENCE_HEADERS);
        ALL_KNOWN_HEADERS.addAll(PROJECT_HEADERS);
        ALL_KNOWN_HEADERS.addAll(SKILLS_HEADERS);
        ALL_KNOWN_HEADERS.addAll(LANGUAGES_HEADERS);
        ALL_KNOWN_HEADERS.addAll(CERTIFICATIONS_HEADERS);
        ALL_KNOWN_HEADERS.addAll(AWARDS_HEADERS);
        ALL_KNOWN_HEADERS.addAll(SUMMARY_HEADERS);
    }

    // ================================================================
    // 技能关键词库（中英文）
    // ================================================================

    /** 编程语言 */
    private static final Set<String> PROGRAMMING_LANGUAGES = Set.of(
            "Java", "Python", "JavaScript", "TypeScript", "Go", "Golang",
            "Rust", "C++", "C#", "C", "PHP", "Ruby", "Scala", "Kotlin",
            "Swift", "Objective-C", "Dart", "R", "MATLAB", "Perl",
            "Shell", "Bash", "PowerShell", "Lua", "Groovy"
    );

    /** 后端框架 */
    private static final Set<String> BACKEND_FRAMEWORKS = Set.of(
            "Spring Boot", "Spring Cloud", "Spring MVC", "Spring Security",
            "MyBatis", "MyBatis-Plus", "Hibernate", "JPA", "Struts",
            "Django", "Flask", "FastAPI", "Express", "Koa", "NestJS",
            "Gin", "Beego", "Echo", "Laravel", "ThinkPHP", "Yii",
            "ASP\\.NET", ".NET Core", "Entity Framework",
            "Rocket", "Actix", "Axum"
    );

    /** 前端框架 */
    private static final Set<String> FRONTEND_FRAMEWORKS = Set.of(
            "React", "Vue", "Vue\\.js", "Vue3", "Vue2", "Angular", "AngularJS",
            "Next\\.js", "Nuxt\\.js", "Svelte", "jQuery", "Bootstrap",
            "Element UI", "Element Plus", "Ant Design", "Antd",
            "Tailwind CSS", "Sass", "Less", "Webpack", "Vite",
            "Redux", "Vuex", "Pinia", "Zustand", "MobX",
            "React Native", "Flutter", "Uni-app", "Taro", "Electron"
    );

    /** 数据库与缓存 */
    private static final Set<String> DATABASES = Set.of(
            "MySQL", "PostgreSQL", "Oracle", "SQL Server", "SQLite",
            "MongoDB", "Redis", "Elasticsearch", "Cassandra", "HBase",
            "Neo4j", "ClickHouse", "TiDB", "Doris", "Hive",
            "Memcached", "DynamoDB", "Couchbase", "InfluxDB",
            "MyBatis", "JDBC", "Druid", "HikariCP"
    );

    /** 中间件与消息队列 */
    private static final Set<String> MIDDLEWARE = Set.of(
            "Kafka", "RabbitMQ", "RocketMQ", "ActiveMQ", "Pulsar",
            "Nginx", "Apache", "Tomcat", "Undertow", "Jetty",
            "Zookeeper", "Nacos", "Consul", "Eureka", "Apollo",
            "Sentinel", "Hystrix", "Resilience4j", "OpenFeign", "Dubbo",
            "gRPC", "Protobuf", "Thrift", "Netty", "WebSocket"
    );

    /** DevOps / 云原生 */
    private static final Set<String> DEVOPS = Set.of(
            "Docker", "Kubernetes", "K8s", "Jenkins", "GitLab CI",
            "GitHub Actions", "Terraform", "Ansible", "Helm",
            "Prometheus", "Grafana", "ELK", "EFK", "Fluentd", "Logstash",
            "AWS", "Azure", "GCP", "阿里云", "腾讯云", "华为云",
            "Serverless", "Lambda", "ECS", "EKS", "ACK",
            "Istio", "Envoy", "Linkerd", "Service Mesh"
    );

    /** AI / 数据科学 */
    private static final Set<String> AI_DATA = Set.of(
            "TensorFlow", "PyTorch", "Keras", "Scikit-learn", "Pandas",
            "NumPy", "Spark", "Flink", "Hadoop", "HDFS", "Hive",
            "机器学习", "深度学习", "自然语言处理", "计算机视觉",
            "NLP", "CV", "LLM", "大模型", "Transformer", "GPT",
            "数据挖掘", "数据分析", "数据仓库", "ETL",
            "Tableau", "Power BI", "FineBI", "Metabase",
            "LangChain", "LlamaIndex", "RAG", "Prompt Engineering"
    );

    /** 设计工具 */
    private static final Set<String> DESIGN_TOOLS = Set.of(
            "Figma", "Sketch", "Adobe XD", "Photoshop", "Illustrator",
            "After Effects", "Premiere", "Axure", "蓝湖", "即时设计",
            "MasterGo", "墨刀"
    );

    /** 项目管理与协作 */
    private static final Set<String> MANAGEMENT = Set.of(
            "Git", "SVN", "Maven", "Gradle", "Ant", "SBT",
            "Jira", "Confluence", "Trello", "Notion", "飞书", "钉钉",
            "Scrum", "Agile", "敏捷开发", "Waterfall", "DevOps",
            "TDD", "DDD", "微服务", "领域驱动设计", "CI/CD",
            "项目管理", "团队管理", "需求分析", "技术方案"
    );

    /** 中文技能关键词 */
    private static final Set<String> CHINESE_SKILLS = Set.of(
            "团队协作", "沟通能力", "领导力", "解决问题", "学习能力",
            "执行力", "抗压能力", "逻辑思维", "创新思维", "时间管理",
            "前端开发", "后端开发", "全栈开发", "移动端开发",
            "系统架构", "性能优化", "高并发", "分布式系统",
            "数据库设计", "接口设计", "API设计", "系统设计",
            "代码审查", "单元测试", "集成测试", "自动化测试",
            "需求文档", "技术文档", "技术选型", "架构设计"
    );

    // ================================================================
    // 学历关键词
    // ================================================================

    private static final Map<String, String> DEGREE_MAP = new LinkedHashMap<>();

    static {
        DEGREE_MAP.put("博士研究生|博士|Ph\\.D|PhD|Doctor|Doctorate", "博士");
        DEGREE_MAP.put("硕士研究生|硕士|Master|MBA|M\\.S\\.|M\\.A\\.|M\\.Eng|MSc", "硕士");
        DEGREE_MAP.put("本科|学士|Bachelor|B\\.S\\.|B\\.A\\.|B\\.Eng|BSc|大学本科", "本科");
        DEGREE_MAP.put("大专|专科|Associate|Diploma|高职|高专", "大专");
        DEGREE_MAP.put("高中|中专|High School|Secondary", "高中");
    }

    // ================================================================
    // 公共入口
    // ================================================================

    /**
     * 解析简历文本为结构化数据。
     *
     * @param rawText Tika 提取的原始文本
     * @return 结构化 Map（name, email, phone, education[], experience[], skills[], 等）
     */
    public ParsedResume parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return emptyResult();
        }

        String text = normalize(rawText);
        List<Section> sections = splitSections(text);

        log.info("[计时] 简历段落分割完成: 总字符={}, 段落数={}", text.length(), sections.size());

        // 首先尝试从所有段落中提取个人信息
        log.info("[计时] 开始提取个人信息...");
        PersonalInfo personal = extractPersonalInfo(text, sections);
        log.info("[计时] 个人信息提取完成: name={}", personal.name());

        // 逐段解析
        Section educationSection = findSection(sections, EDUCATION_HEADERS);
        Section experienceSection = findSection(sections, EXPERIENCE_HEADERS);
        Section projectSection = findSection(sections, PROJECT_HEADERS);
        Section skillsSection = findSection(sections, SKILLS_HEADERS);

        log.info("[计时] 开始逐段解析: education={}, experience={}, projects={}, skills={}",
                educationSection != null, experienceSection != null,
                projectSection != null, skillsSection != null);

        ParsedResume result = ParsedResume.builder()
                .name(personal.name())
                .email(personal.email())
                .phone(personal.phone())
                .location(personal.location())
                .gender(personal.gender())
                .birthDate(personal.birthDate())
                .household(personal.household())
                .politicalStatus(personal.politicalStatus())
                .workYears(personal.workYears())
                .desiredPosition(personal.desiredPosition())
                .age(personal.age())
                .education(educationSection != null
                        ? parseEducation(educationSection.content) : List.of())
                .experience(experienceSection != null
                        ? parseExperience(experienceSection.content) : List.of())
                .projects(projectSection != null
                        ? parseProjects(projectSection.content) : List.of())
                .skills(skillsSection != null
                        ? extractSkillsFromText(skillsSection.content)
                        : extractSkillsFromText(text))
                .skillsText(skillsSection != null
                        ? processSkillsText(skillsSection.content) : "")
                .languages(parseLanguages(
                        findSectionContent(sections, LANGUAGES_HEADERS, text)))
                .certifications(parseCertifications(
                        findSectionContent(sections, CERTIFICATIONS_HEADERS, "")))
                .awards(parseAwards(
                        findSectionContent(sections, AWARDS_HEADERS, "")))
                .summary(parseSummary(
                        findSectionContent(sections, SUMMARY_HEADERS, "")))
                .build();

        log.info("简历解析完成: 姓名={}, 教育={}条, 工作={}条, 项目={}条, 技能={}个",
                personal.name(),
                result.getEducation().size(),
                result.getExperience().size(),
                result.getProjects().size(),
                result.getSkills().size());

        return result;
    }

    // ================================================================
    // 文本预处理
    // ================================================================

    private String normalize(String text) {
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                // 合并连续的空白行
                .replaceAll("\n{3,}", "\n\n")
                // 保留中文和英文关键标点
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    // ================================================================
    // 段落分割
    // ================================================================

    /**
     * 将文本按标题行切割为段落。
     * 识别模式：短行、可能带 "：" 或部分大写，且是已知标题关键词。
     */
    private List<Section> splitSections(String text) {
        String[] lines = text.split("\n");
        List<Section> sections = new ArrayList<>();

        int headerIdx = -1;
        String currentHeader = "";
        StringBuilder currentContent = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                if (!currentContent.isEmpty()) {
                    currentContent.append("\n");
                }
                continue;
            }

            String header = matchHeader(line);
            if (header != null) {
                // 保存上一个段落
                if (headerIdx >= 0 && !currentContent.isEmpty()) {
                    sections.add(new Section(currentHeader, currentContent.toString().trim()));
                }
                headerIdx = i;
                currentHeader = header;
                currentContent = new StringBuilder();
            } else {
                if (headerIdx < 0) {
                    headerIdx = 0;
                    currentHeader = "_HEADER_";
                }
                currentContent.append(line).append("\n");
            }
        }

        // 保存最后一个段落
        if (headerIdx >= 0 && !currentContent.isEmpty()) {
            sections.add(new Section(currentHeader, currentContent.toString().trim()));
        }

        // 如果没有任何标题被识别，整个文本作为一个段落
        if (sections.isEmpty()) {
            sections.add(new Section("_FULL_", text));
        }

        return sections;
    }

    /**
     * 判断一行是否为已知段落标题。若是，返回标准化标题名；否则返回 null。
     */
    private String matchHeader(String line) {
        String trimmed = line.replace("：", "").replace(":", "").trim().toLowerCase();

        // 标题行通常较短（< 20 字符）
        if (trimmed.length() > 25) return null;

        for (String h : CONTACT_HEADERS) {
            if (trimmed.equals(h.toLowerCase()) || trimmed.startsWith(h.toLowerCase() + " ")) {
                return "contact";
            }
        }
        for (String h : EDUCATION_HEADERS) {
            if (trimmed.equals(h.toLowerCase()) || trimmed.startsWith(h.toLowerCase() + " ")) {
                return "education";
            }
        }
        for (String h : EXPERIENCE_HEADERS) {
            if (trimmed.equals(h.toLowerCase()) || trimmed.startsWith(h.toLowerCase() + " ")) {
                return "experience";
            }
        }
        for (String h : PROJECT_HEADERS) {
            if (trimmed.equals(h.toLowerCase()) || trimmed.startsWith(h.toLowerCase() + " ")) {
                return "projects";
            }
        }
        for (String h : SKILLS_HEADERS) {
            if (trimmed.equals(h.toLowerCase()) || trimmed.startsWith(h.toLowerCase() + " ")) {
                return "skills";
            }
        }
        for (String h : LANGUAGES_HEADERS) {
            if (trimmed.equals(h.toLowerCase()) || trimmed.startsWith(h.toLowerCase() + " ")) {
                return "languages";
            }
        }
        for (String h : CERTIFICATIONS_HEADERS) {
            if (trimmed.equals(h.toLowerCase()) || trimmed.startsWith(h.toLowerCase() + " ")) {
                return "certifications";
            }
        }
        for (String h : AWARDS_HEADERS) {
            if (trimmed.equals(h.toLowerCase()) || trimmed.startsWith(h.toLowerCase() + " ")) {
                return "awards";
            }
        }
        for (String h : SUMMARY_HEADERS) {
            if (trimmed.equals(h.toLowerCase()) || trimmed.startsWith(h.toLowerCase() + " ")) {
                return "summary";
            }
        }

        return null;
    }

    private Section findSection(List<Section> sections, Set<String> headers) {
        for (Section s : sections) {
            if (headers.contains(s.header.toLowerCase())) return s;
        }
        return null;
    }

    private String findSectionContent(List<Section> sections, Set<String> headers, String fallback) {
        Section s = findSection(sections, headers);
        return s != null ? s.content : fallback;
    }

    // ================================================================
    // 个人信息提取
    // ================================================================

    private PersonalInfo extractPersonalInfo(String fullText, List<Section> sections) {
        // 优先在 contact 段落中寻找，其次用全文
        String contactSection = findSectionContent(sections, CONTACT_HEADERS, "");

        // 如果有 _HEADER_ 段落（未分类的起始段落），优先用来提取个人信息
        String candidateText = !contactSection.isEmpty()
                ? contactSection
                : sections.stream()
                    .filter(s -> "_HEADER_".equals(s.header))
                    .map(s -> s.content)
                    .findFirst()
                    .orElse(fullText);

        return new PersonalInfo(
                extractName(candidateText, fullText),
                extractEmail(candidateText),
                extractPhone(candidateText),
                extractLocation(candidateText),
                extractGender(candidateText),
                extractBirthDate(candidateText),
                extractHousehold(candidateText),
                extractPoliticalStatus(candidateText),
                extractWorkYears(candidateText),
                extractDesiredPosition(candidateText),
                extractAge(candidateText)
        );
    }

    private String extractName(String text, String fullText) {
        // ── 策略1: 查找 "姓名：" / "姓名:" 前缀 ──
        Matcher labelMatcher = NAME_LABEL_PATTERN.matcher(text);
        if (labelMatcher.find()) {
            String name = labelMatcher.group(1);
            if (isPlausibleName(name)) {
                return name;
            }
        }

        // 如果 contact 段落没找到，尝试全文搜索
        if (!text.equals(fullText)) {
            labelMatcher = NAME_LABEL_PATTERN.matcher(fullText);
            if (labelMatcher.find()) {
                String name = labelMatcher.group(1);
                if (isPlausibleName(name)) {
                    return name;
                }
            }
        }

        // ── 策略2: 逐行匹配独立中文姓名 ──
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.length() > 30) continue;

            String lower = trimmed.toLowerCase();
            if (lower.contains("@") || lower.matches(".*\\d{8,}.*")
                    || lower.contains("http") || lower.contains("www.")) continue;
            if (isNonNameKeyword(lower)) continue;

            boolean isKnownHeader = ALL_KNOWN_HEADERS.stream()
                    .anyMatch(h -> lower.equals(h.replace("：", "").replace(":", "")));
            if (isKnownHeader) continue;

            // 独立中文姓名行
            if (CHINESE_NAME_PATTERN.matcher(trimmed).matches()) {
                if (isPlausibleName(trimmed)) return trimmed;
                continue;
            }

            // 英文姓名
            if (ENGLISH_NAME_PATTERN.matcher(trimmed).matches()) return trimmed;
        }

        // ── 策略3: 行首中文姓名提取（名字 + 其他内容的行，如 "苏三 | 男 | 28岁"）──
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 2 || trimmed.length() > 30) continue;
            String lower = trimmed.toLowerCase();
            if (lower.contains("@") || isNonNameKeyword(lower)) continue;

            Matcher leadingMatcher = LEADING_NAME_PATTERN.matcher(trimmed);
            if (leadingMatcher.find()) {
                String name = leadingMatcher.group(1);
                if (isPlausibleName(name)) return name;
            }
        }

        // 在全文也尝试策略1和策略3
        if (!text.equals(fullText)) {
            for (String line : fullText.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.length() < 2 || trimmed.length() > 30) continue;
                String lower = trimmed.toLowerCase();
                if (lower.contains("@") || isNonNameKeyword(lower)) continue;

                Matcher leadingMatcher = LEADING_NAME_PATTERN.matcher(trimmed);
                if (leadingMatcher.find()) {
                    String name = leadingMatcher.group(1);
                    if (isPlausibleName(name)) return name;
                }
            }
        }

        // ── 策略4: 回退 — 全文第一行 ──
        String firstLine = fullText.split("\n")[0].trim();
        if (firstLine.length() >= 2 && firstLine.length() <= 20
                && !firstLine.contains("@") && !isNonNameKeyword(firstLine.toLowerCase())) {
            // 尝试用 LEADING_NAME_PATTERN 从第一行提取
            Matcher m = LEADING_NAME_PATTERN.matcher(firstLine);
            if (m.find() && isPlausibleName(m.group(1))) {
                return m.group(1);
            }
        }

        return "";
    }

    /** 判断候选姓名是否合理：非标题、非标点、全部为中文 */
    private boolean isPlausibleName(String name) {
        if (name == null || name.isEmpty()) return false;
        String lower = name.toLowerCase();
        if (isNonNameKeyword(lower)) return false;
        // 检查是否命中已知段落标题（包括标题的前几个字）
        for (String header : ALL_KNOWN_HEADERS) {
            String clean = header.replace("：", "").replace(":", "");
            if (lower.equals(clean) || clean.contains(lower)) return false;
        }
        // 只接受中文姓名（全部为中文字符或 · 分隔符）
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x4e00 || c > 0x9fa5) {
                if (c != '·') return false;
            }
        }
        return true;
    }

    /** 判断给定字符串是否为非姓名的文档标题/关键词 */
    private boolean isNonNameKeyword(String lower) {
        return NON_NAME_KEYWORDS.stream().anyMatch(kw -> lower.contains(kw));
    }

    private String extractEmail(String text) {
        Matcher m = EMAIL_PATTERN.matcher(text);
        return m.find() ? m.group() : "";
    }

    private String extractPhone(String text) {
        Matcher m = PHONE_PATTERN.matcher(text);
        if (m.find()) {
            String phone = m.group(1);
            // 规范化
            phone = phone.replaceAll("[\\s\\-]", "");
            return phone;
        }
        // 回退：直接匹配 11 位中国大陆手机号
        Matcher direct = Pattern.compile("1[3-9]\\d{9}").matcher(text);
        return direct.find() ? direct.group() : "";
    }

    // 个人信息行中常见字段标签（用于非贪婪截断）
    private static final String FIELD_BOUNDARY =
            "户籍|籍贯|户口|所在地|学历|学位|政治面貌|政治派别|性别|"
                    + "出生|生日|电话|手机|邮箱|专业|学校|年龄|民族|婚姻";

    private String extractLocation(String text) {
        // 城市：非贪婪匹配，遇到下一个字段标签就停止
        Pattern cityPattern = Pattern.compile(
                "(?:城市|地点|地址|Location|City|所在地)[：:.\\s]*"
                        + "([^\\n|]+?)(?=\\s*(?:" + FIELD_BOUNDARY + ")|$)");
        Matcher m = cityPattern.matcher(text);
        if (m.find()) {
            String result = m.group(1).trim();
            if (!result.isEmpty() && result.length() <= 20) return result;
        }

        // 尝试匹配常见的中国城市名
        Pattern cityNamePattern = Pattern.compile(
                "(北京|上海|广州|深圳|杭州|成都|武汉|南京|西安|重庆|苏州|天津|"
                        + "长沙|郑州|东莞|青岛|沈阳|宁波|昆明|无锡|佛山|合肥|大连|福州|"
                        + "厦门|哈尔滨|济南|温州|南宁|长春|泉州|石家庄|贵阳|南昌|金华|"
                        + "常州|南通|嘉兴|太原|徐州|惠州|珠海|中山|兰州|临沂|潍坊|"
                        + "烟台|绍兴|台州|海口|乌鲁木齐|呼和浩特|银川|西宁|拉萨)");
        m = cityNamePattern.matcher(text);
        if (m.find()) return m.group(1);

        return "";
    }

    private String extractGender(String text) {
        Pattern genderPattern = Pattern.compile("性别[：:\\s]*(男|女|Male|Female)", Pattern.CASE_INSENSITIVE);
        Matcher m = genderPattern.matcher(text);
        if (m.find()) {
            String g = m.group(1);
            if ("Male".equalsIgnoreCase(g)) return "男";
            if ("Female".equalsIgnoreCase(g)) return "女";
            return g;
        }
        // 回退：信息区顶部独立出现的 "男" / "女"
        String[] lines = text.split("\n");
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String t = lines[i].trim();
            if (t.equals("男") || t.equals("女")) return t;
        }
        return "";
    }

    private String extractBirthDate(String text) {
        // "出生年月：1998-06" / "出生日期：1998年6月" / "出生年月: 1998.06"
        Pattern birthPattern = Pattern.compile(
                "(?:出生年月|出生日期|生日|Birth|Birthday|Date of Birth)[：:\\s]*"
                        + "(\\d{4})[年.\\-–/]\\s*(\\d{1,2})?[月]?");
        Matcher m = birthPattern.matcher(text);
        if (m.find()) {
            String year = m.group(1);
            String month = m.group(2);
            if (month != null && !month.isEmpty()) {
                return year + "-" + String.format("%02d", Integer.parseInt(month));
            }
            return year;
        }
        // 回退：匹配独立出生年份行
        Pattern birthLinePattern = Pattern.compile(
                "^(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(?:出生)?$");
        for (String line : text.split("\n")) {
            Matcher lm = birthLinePattern.matcher(line.trim());
            if (lm.find()) {
                String month = lm.group(2);
                if (month != null) {
                    return lm.group(1) + "-" + String.format("%02d", Integer.parseInt(month));
                }
                return lm.group(1);
            }
        }
        return "";
    }

    private String extractHousehold(String text) {
        // 户籍：非贪婪匹配，遇到下一个字段标签就停止
        Pattern hPattern = Pattern.compile(
                "(?:户籍|籍贯|户口所在地|户口|Hukou|Domicile)[：:\\s]*"
                        + "([^\\n|]+?)(?=\\s*(?:" + FIELD_BOUNDARY + ")|$)");
        Matcher m = hPattern.matcher(text);
        if (m.find()) {
            String h = m.group(1).trim();
            if (!h.isEmpty() && h.length() <= 20) return h;
        }
        return "";
    }

    private String extractPoliticalStatus(String text) {
        // 政治面貌：中共党员 / 共青团员 / 群众 / 民主党派
        Pattern pPattern = Pattern.compile(
                "(?:政治面貌|政治派别|党派|Political)[：:\\s]*"
                        + "(中共党员|中共预备党员|共青团员|群众|民革党员|民盟盟员|民建会员|"
                        + "民进会员|农工党党员|致公党党员|九三学社社员|台盟盟员|无党派人士|"
                        + "Communist|Party Member|League Member)");
        Matcher m = pPattern.matcher(text);
        if (m.find()) return m.group(1).trim();
        // 回退：单独一行匹配
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.equals("中共党员") || t.equals("中共预备党员") || t.equals("共青团员")
                    || t.equals("群众") || t.equals("无党派人士")) {
                return t;
            }
        }
        return "";
    }

    private String extractWorkYears(String text) {
        // "N 年经验" / "N年工作经验" / "工作年限：N年" / "年经验：N"
        Pattern pattern = Pattern.compile(
                "(?:工作经验|工作年限|工龄|从业年限|经验|experience)[：:.\\s]*"
                        + "(\\d+)\\s*年");
        Matcher m = pattern.matcher(text);
        if (m.find()) return m.group(1);

        // 回退：header 中的 "· N年经验" 模式（N后紧跟"年经验"）
        Pattern headerPattern = Pattern.compile("(\\d+)\\s*年(?:\s*经验|\s*工作经验)?");
        m = headerPattern.matcher(text);
        if (m.find()) return m.group(1);

        return "";
    }

    private String extractDesiredPosition(String text) {
        // 从简历头部 · 分隔的片段中提取期望职位/当前职位
        // 匹配在 · 或空格分隔的片段中包含职位关键词的部分
        Pattern positionPattern = Pattern.compile(
                "(?:应聘职位|应聘岗位|期望职位|期望岗位|求职意向|期望方向"
                        + "|目标职位|目标岗位|期望从事|求职岗位|意向岗位|意向职位"
                        + "|职位|岗位|Position|Target)[：:.\\s]*"
                        + "([^\\n|]+?)(?=\\s*(?:" + FIELD_BOUNDARY + ")|$)");
        Matcher m = positionPattern.matcher(text);
        if (m.find()) {
            String pos = m.group(1).trim();
            if (!pos.isEmpty() && pos.length() <= 30) return pos;
        }

        // 回退：从 header 行 "·" 分隔的片段中寻找职位关键词
        // 典型格式: "苏三 · 7年经验 · 研发团队负责人 · 30岁"
        // 匹配包含明确职位后缀的片段
        Pattern segmentPattern = Pattern.compile(
                "[·|,，、]\\s*([^·|,，、\\n]{2,20}(?:负责人|工程师|架构师|经理|总监|主管|设计师"
                        + "|程序员|分析师|专家|顾问|专员|开发|测试|运维|运营|产品经理"
                        + "|前端|后端|全栈|数据|算法|AI|实习生))\\s*(?:[·|,，、]|$)");
        m = segmentPattern.matcher(text);
        if (m.find()) {
            String pos = m.group(1).trim();
            if (!pos.isEmpty()) return pos;
        }

        // 二次回退：header 行中 · 分隔的任何看起来像职位名称的片段
        String[] segments = text.split("[·]");
        for (String seg : segments) {
            String s = seg.trim();
            if (s.isEmpty() || s.length() > 25) continue;

            // 先剥离尾部附加信息（年龄、工作年限等），保留纯职位文本
            String cleaned = s
                    .replaceAll("\\s*\\d{1,3}\\s*岁\\s*$", "")   // "30岁" / "30 岁"
                    .replaceAll("\\s*\\d+\\s*年.*$", "")          // "7年经验" / "7 年"
                    .trim();

            if (cleaned.isEmpty()) continue;

            // 跳过名字、纯数字、邮箱/电话、城市名
            if (cleaned.matches("\\d+.*")) continue;
            if (cleaned.contains("@") || cleaned.matches(".*\\d{8,}.*")) continue;
            if (cleaned.matches("[\\u4e00-\\u9fa5]{2,4}") || cleaned.matches("[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?")) continue;
            // 包含典型职位关键词
            if (cleaned.matches(".*(?:负责|工程师|架构师|经理|总监|主管|设计师|实习生|专家|顾问|"
                    + "开发|测试|运维|运营|前端|后端|全栈|算法|数据|产品|设计|管理).*")) {
                return cleaned;
            }
        }

        // 三次回退：逐行扫描，对含 · 的行先拆分再匹配
        for (String line : text.split("\n")) {
            String l = line.trim();
            if (l.isEmpty()) continue;

            // 如果行中有 · 分隔符，拆分成片段再逐段检查
            if (l.contains("·")) {
                for (String seg : l.split("[·]")) {
                    String s = seg.trim();
                    if (s.isEmpty() || s.length() > 25) continue;
                    String cleaned = s
                            .replaceAll("\\s*\\d{1,3}\\s*岁\\s*$", "")
                            .replaceAll("\\s*\\d+\\s*年.*$", "")
                            .trim();
                    if (cleaned.isEmpty()) continue;
                    if (cleaned.matches("\\d+.*")) continue;
                    if (cleaned.matches("[\\u4e00-\\u9fa5]{2,4}")) continue;
                    if (cleaned.matches(".*?(?:负责人|工程师|架构师|经理|总监|主管|设计师"
                            + "|开发工程师|测试工程师|运维工程师|前端工程师|后端工程师|全栈工程师"
                            + "|算法工程师|数据工程师|产品经理|项目经理|技术经理|技术总监).*")) {
                        return cleaned;
                    }
                }
                continue;
            }

            if (l.length() > 30 || l.length() < 3) continue;
            if (l.contains("@") || l.contains("http") || l.matches(".*\\d{8,}.*")) continue;
            // 跳过纯个人信息行
            if (l.matches("[\\u4e00-\\u9fa5]{2,4}") || l.matches("\\d+.*") || l.matches(".*\\d+岁.*")) continue;
            if (l.matches(".*?(?:负责人|工程师|架构师|经理|总监|主管|设计师"
                    + "|开发工程师|测试工程师|运维工程师|前端工程师|后端工程师|全栈工程师"
                    + "|算法工程师|数据工程师|产品经理|项目经理|技术经理|技术总监).*")) {
                return l;
            }
        }

        return "";
    }

    private String extractAge(String text) {
        // "年龄：30" / "年龄: 30" / "30岁"
        Pattern ageLabelPattern = Pattern.compile("年龄[：:.\\s]*(\\d{1,2})\\s*岁?");
        Matcher m = ageLabelPattern.matcher(text);
        if (m.find()) return m.group(1);

        // 回退：header 中的独立 "30岁" 模式
        Pattern ageInline = Pattern.compile("(\\d{2,3})\\s*岁");
        m = ageInline.matcher(text);
        if (m.find()) {
            int age = Integer.parseInt(m.group(1));
            // 合理性检查：年龄应在 16-70 之间
            if (age >= 16 && age <= 70) return m.group(1);
        }

        return "";
    }

    // ================================================================
    // 教育经历解析
    // ================================================================

    private List<ParsedResume.EducationEntry> parseEducation(String text) {
        List<ParsedResume.EducationEntry> education = new ArrayList<>();
        String[] lines = text.split("\n");

        // 先尝试按空白行分隔条目
        List<String> entries = splitEntries(text);

        for (String entry : entries) {
            // 尝试从单个 entry 中再次拆分（处理没有空行分隔的多条教育经历）
            List<String> subEntries = splitEducationEntry(entry);
            for (String sub : subEntries) {
                ParsedResume.EducationEntry edu = parseEducationEntry(sub);
                if (edu != null) {
                    boolean duplicate = education.stream()
                            .anyMatch(e -> nullSafeEquals(e.getSchool(), edu.getSchool())
                                    && nullSafeEquals(e.getMajor(), edu.getMajor())
                                    && nullSafeEquals(e.getDegree(), edu.getDegree()));
                    if (!duplicate) {
                        education.add(edu);
                    }
                }
            }
        }

        // 如果按条目未解析出结果，尝试逐行匹配
        if (education.isEmpty()) {
            for (String line : lines) {
                ParsedResume.EducationEntry edu = parseEducationEntry(line);
                if (edu != null) {
                    education.add(edu);
                }
            }
        }

        return education;
    }

    private ParsedResume.EducationEntry parseEducationEntry(String entry) {
        String clean = entry.trim();
        if (clean.isEmpty()) return null;

        // 提取学校名
        String school = extractSchool(clean);

        // 提取专业
        String major = extractMajor(clean);

        // 提取学历
        String degree = extractDegree(clean);

        // 提取时间范围
        String[] dates = extractDates(clean);

        if (school.isEmpty() && major.isEmpty() && degree.isEmpty()) {
            return null;
        }

        return ParsedResume.EducationEntry.builder()
                .school(school).major(major).degree(degree)
                .start(dates[0]).end(dates[1])
                .build();
    }

    private String extractSchool(String text) {
        // 中文：XX大学、XX学院
        Pattern cnSchool = Pattern.compile("([\\u4e00-\\u9fa5]{2,20}(?:大学|学院|研究院))");
        Matcher m = cnSchool.matcher(text);
        if (m.find()) return m.group(1);

        // 英文：University of ..., ... Institute of ...
        Pattern enSchool = Pattern.compile(
                "((?:University|College|Institute|School)\\s+of\\s+[\\w\\s]+|"
                        + "[\\w\\s]+\\s+(?:University|College|Institute))",
                Pattern.CASE_INSENSITIVE);
        m = enSchool.matcher(text);
        if (m.find()) return m.group(1).trim();

        return "";
    }

    private String extractMajor(String text) {
        // 中文专业关键词
        Pattern cnMajor = Pattern.compile(
                "(?:专业|Major)[：:.\\s]*([\\u4e00-\\u9fa5A-Za-z\\s]{2,40})");
        Matcher m = cnMajor.matcher(text);
        if (m.find()) return m.group(1).trim();

        // 常见专业名模式
        Pattern majorPattern = Pattern.compile(
                "(计算机科学|软件工程|信息工程|电子信息|通信工程|自动化|"
                        + "人工智能|数据科学|网络工程|信息安全|"
                        + "金融学|会计学|经济学|管理学|市场营销|"
                        + "英语|日语|法语|德语|翻译|"
                        + "计算机科学与技术|电子科学与技术|机械工程|土木工程|"
                        + "数学与应用数学|物理学|化学|生物科学|"
                        + "法学|新闻学|传播学|广告学|设计学|"
                        + "Computer Science|Software Engineering|Electrical Engineering|"
                        + "Mechanical Engineering|Business Administration|Finance|"
                        + "Data Science|Information Technology|Mathematics|Physics)");
        m = majorPattern.matcher(text);
        if (m.find()) return m.group(1).trim();

        return "";
    }

    private String extractDegree(String text) {
        for (Map.Entry<String, String> entry : DEGREE_MAP.entrySet()) {
            Pattern p = Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE);
            if (p.matcher(text).find()) {
                return entry.getValue();
            }
        }
        return "";
    }

    private boolean nullSafeEquals(String a, String b) {
        if (a == null || a.isEmpty()) return (b == null || b.isEmpty());
        return a.equals(b);
    }

    /**
     * 将一个可能包含多条教育经历的文本块拆分为独立的条目。
     * 当简历中没有用空行分隔多条教育经历时（常见于中文简历），
     * 按包含学校关键词的行进行拆分。
     */
    private List<String> splitEducationEntry(String entry) {
        String[] lines = entry.split("\n");
        if (lines.length <= 1) return List.of(entry);

        // 检查是否存在多条教育经历的迹象：多行包含学校关键词
        Pattern schoolLinePattern = Pattern.compile(
                "([\\u4e00-\\u9fa5]{2,20}(?:大学|学院|研究院))|" +
                "(?:University|College|Institute)",
                Pattern.CASE_INSENSITIVE);
        Pattern datePattern = Pattern.compile("\\d{4}[-.年]\\s*\\d{1,2}?\\s*[-–—至到~]+\\s*(\\d{4}|至今|Present)");

        int schoolLineCount = 0;
        for (String line : lines) {
            if (schoolLinePattern.matcher(line).find() || datePattern.matcher(line).find()) {
                schoolLineCount++;
            }
        }

        // 如果只有一个学校/日期行，说明确实是一个条目
        if (schoolLineCount <= 1) return List.of(entry);

        // 按学校/日期行拆分为多个条目
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if ((schoolLinePattern.matcher(line).find() || datePattern.matcher(line).find())
                    && !current.isEmpty()) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) current.append("\n");
            current.append(line);
        }
        if (!current.isEmpty()) {
            result.add(current.toString().trim());
        }

        return result.isEmpty() ? List.of(entry) : result;
    }

    // ================================================================
    // 工作经历解析
    // ================================================================

    /**
     * 简单判断一行文本是否包含明确的公司名后缀。
     * 不依赖 {@link #extractCompany} 的复杂正则，避免把"保障千万级数据"、
     * "健康数据"等描述行误判为公司名。
     */
    private boolean containsCompanyName(String line) {
        return line.contains("有限公司")
                || line.contains("有限责任公司")
                || line.contains("股份有限公司")
                || line.matches("(?i).*(?:Inc\\.|Corp\\.|Ltd\\.|Limited|LLC|Corporation)\\b.*");
    }

    /**
     * 合并孤立块：不以公司名开头的空行分割块会被合并到前一个公司块中。
     * <p>例如 "◆ 保障千万级数据..." 本身不含公司名，应归入上一条经历。</p>
     */
    private List<String> mergeExperienceBlocks(List<String> blocks) {
        List<String> result = new ArrayList<>();
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;

            String firstLine = trimmed.lines().findFirst().orElse("").trim();
            boolean hasCompany = containsCompanyName(firstLine);

            if (hasCompany) {
                result.add(trimmed);
            } else if (!result.isEmpty()) {
                // 合并到上一个公司块
                int last = result.size() - 1;
                result.set(last, result.get(last) + "\n" + trimmed);
            } else {
                // 开头的非公司块（极少见），保留
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * 按公司名行切分文本（用于空行分割失败时的回退）。
     */
    private List<String> splitByCompanyLines(String text) {
        String[] lines = text.split("\n");
        List<String> entries = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (!current.isEmpty()) current.append("\n");
                continue;
            }
            if (containsCompanyName(trimmed)) {
                String prev = current.toString().trim();
                if (!prev.isEmpty()) entries.add(prev);
                current = new StringBuilder();
            }
            current.append(trimmed).append("\n");
        }
        String last = current.toString().trim();
        if (!last.isEmpty()) entries.add(last);
        return entries.size() >= 2 ? entries : List.of(text);
    }

    private List<ParsedResume.ExperienceEntry> parseExperience(String text) {
        // 先按空行分割，再把不含公司名的块合并到前一个公司块
        List<String> rawBlocks = splitEntries(text);
        List<String> blocks = mergeExperienceBlocks(rawBlocks);

        // 如果合并后只有一个块且包含多家公司，改为按公司名行切分
        if (blocks.size() == 1 && countCompaniesInText(blocks.get(0)) >= 2) {
            blocks = mergeExperienceBlocks(splitByCompanyLines(blocks.get(0)));
        }

        List<ParsedResume.ExperienceEntry> experience = new ArrayList<>();
        for (String block : blocks) {
            ParsedResume.ExperienceEntry exp = parseExperienceEntry(block);
            if (exp != null) {
                experience.add(exp);
            }
        }
        return experience;
    }

    /** 统计文本中包含多少个"有限公司"等公司名标记。 */
    private int countCompaniesInText(String text) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf("有限公司", idx)) != -1) {
            count++;
            idx += 4;
        }
        return count;
    }

    // ================================================================
    // 统一 bullet 处理引擎
    // ================================================================

    /**
     * 统一描述行处理引擎，用于经验条目和项目条目。
     *
     * <p>按 4 条规则判定每行是否加列表标记：</p>
     * <ol>
     *   <li>标签行（匹配 {@link #PROJECT_LABEL_PATTERN}）— 纯文本，不加点</li>
     *   <li>显式 bullet 行 — 按缩进加 {@code ●} 或 {@code ○}</li>
     *   <li>隐式列表 — 在列表引入标签后，匹配动作动词，加 {@code ●}</li>
     *   <li>正文 — 纯文本，不加点</li>
     * </ol>
     *
     * @param lines 原始描述行（未 trim，用于缩进检测）
     * @return 格式化后的描述文本
     */
    private String processDescriptionLines(String[] lines) {
        if (lines == null || lines.length == 0) return "";

        StringBuilder out = new StringBuilder();
        boolean insideListContext = false;
        boolean previousWasList = false;
        int previousLevel = 0;
        int pendingNewlines = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                pendingNewlines = Math.max(pendingNewlines, 1);
                continue;
            }

            // ── 规则 1：标签:内容行 ──
            Matcher labelM = PROJECT_LABEL_PATTERN.matcher(line);
            if (labelM.find()) {
                String label = labelM.group(1);
                String content = line.substring(labelM.end()).trim();
                String labelPrefix = line.substring(0, labelM.end()).trim();
                boolean isListLabel = LIST_INTRODUCING_LABELS.contains(label);

                if (previousWasList || out.length() > 0) {
                    pendingNewlines = Math.max(pendingNewlines, 2);
                }

                // 列表引入标签 + 内容含中文分号 → 拆分为多条独立列表项
                if (isListLabel && !content.isEmpty() && content.contains("；")) {
                    String[] parts = content.split("[；;]");
                    emitWithPending(out, pendingNewlines, labelPrefix + parts[0].trim());
                    pendingNewlines = 2;
                    for (int i = 1; i < parts.length; i++) {
                        String part = parts[i].trim();
                        if (part.isEmpty()) continue;
                        emitWithPending(out, pendingNewlines,
                                LIST_INDENT + MARKER_LEVEL_1 + " " + part);
                        pendingNewlines = 2;
                    }
                    insideListContext = false;
                    previousWasList = true;
                    previousLevel = 1;
                } else {
                    if (!content.isEmpty()) {
                        emitWithPending(out, pendingNewlines, labelPrefix + content);
                    } else {
                        emitWithPending(out, pendingNewlines, labelPrefix);
                    }
                    // 列表引入标签后加空行，让下一项与标签之间有视觉层次
                    pendingNewlines = isListLabel ? 2 : 1;
                    insideListContext = isListLabel;
                    previousWasList = false;
                    previousLevel = 0;
                }
                continue;
            }

            // ── 规则 2：显式 bullet 行 ──
            Matcher bulletM = BULLET_LINE_PATTERN.matcher(rawLine);
            if (bulletM.find()) {
                int indentSpaces = bulletM.group(1).length();
                String content = bulletM.group(3).trim();
                if (content.isEmpty()) continue;

                int level = indentSpaces > 0 ? 2 : 1;
                char marker = level == 1 ? MARKER_LEVEL_1 : MARKER_LEVEL_2;

                if (previousWasList) {
                    pendingNewlines = level == 1
                            ? Math.max(pendingNewlines, 2)   // 新顶级项
                            : Math.max(pendingNewlines, 1);  // 子项软换行
                } else if (out.length() > 0) {
                    pendingNewlines = Math.max(pendingNewlines, 2);
                }

                emitWithPending(out, pendingNewlines, marker + " " + content);
                pendingNewlines = 1;
                previousWasList = true;
                previousLevel = level;
                insideListContext = false;
                continue;
            }

            // ── 规则 3：隐式列表项（列表引入标签后的行全部视为列表项）──
            if (insideListContext) {
                if (previousWasList || out.length() > 0) {
                    pendingNewlines = Math.max(pendingNewlines, 2);
                }
                emitWithPending(out, pendingNewlines,
                        LIST_INDENT + MARKER_LEVEL_1 + " " + line);
                pendingNewlines = 2;
                previousWasList = true;
                previousLevel = 1;
                continue;
            }

            // ── 规则 4：正文段落 ──
            if (previousWasList || out.length() > 0) {
                pendingNewlines = Math.max(pendingNewlines, 2);
            }
            emitWithPending(out, pendingNewlines, line);
            pendingNewlines = 2;
            previousWasList = false;
            previousLevel = 0;
            insideListContext = false;
        }

        return out.toString().trim().replaceAll("\n{3,}", "\n\n");
    }

    /** 向输出缓冲区写入 n 个换行 + 文本。 */
    private void emitWithPending(StringBuilder out, int newlineCount, String text) {
        for (int i = 0; i < newlineCount; i++) {
            out.append('\n');
        }
        out.append(text);
    }

    private ParsedResume.ExperienceEntry parseExperienceEntry(String entry) {
        String clean = entry.trim();
        if (clean.isEmpty()) return null;

        String[] lines = clean.split("\n");

        // ── 公司名：第一行非空行 ──
        String company = "";
        int companyLine = -1;
        for (int i = 0; i < lines.length; i++) {
            String t = lines[i].trim();
            if (t.isEmpty()) continue;
            company = extractCompany(t);
            if (!company.isEmpty()) {
                companyLine = i;
                break;
            }
        }
        if (company.isEmpty()) return null;

        // ── 日期：从前几行中提取（含公司行）──
        StringBuilder headerArea = new StringBuilder();
        for (int i = 0; i < lines.length && i < 5; i++) {
            if (!lines[i].trim().isEmpty()) {
                headerArea.append(lines[i].trim()).append(" ");
            }
        }
        String[] dates = extractDates(headerArea.toString());
        if (dates[0].isEmpty() && dates[1].isEmpty()) {
            dates = extractDates(clean);
        }

        // ── 职位：从前几行中提取 ──
        String title = extractJobTitle(headerArea.toString());

        // ── 描述：公司行 + 日期行 + 职位行之外的所有非空行，统一 bullet 处理 ──
        List<String> descLines = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (i == companyLine) continue;           // 公司行
            if (isDateOnlyLine(line)) continue;       // 纯日期行
            if (isJobDateLine(line)) continue;        // 职位+日期混合行
            if (!title.isEmpty() && line.contains(title)) continue; // 职位行
            descLines.add(line);
        }
        String description = processDescriptionLines(descLines.toArray(new String[0]));

        ParsedResume.ExperienceEntry result = ParsedResume.ExperienceEntry.builder()
                .company(company).position(title)
                .start(dates[0]).end(dates[1]).description(description)
                .build();

        return result;
    }

    /**
     * 判断行是否只包含日期范围，如 "2022 - 至今"。
     */
    private boolean isDateOnlyLine(String line) {
        return line.matches(
                "^\\s*\\d{4}[-.]?(?:\\d{1,2})?\\s*[-–—至到~]+\\s*(?:\\d{4}[-.]?(?:\\d{1,2})?|至今|Present)\\s*$");
    }

    /**
     * 判断行是否像"职位 + 日期"混合行（如 "Java开发 | 2022 - 至今"）。
     */
    private boolean isJobDateLine(String line) {
        if (line.length() < 8 || line.length() > 60) return false;
        return DATE_RANGE_PATTERN.matcher(line).find();
    }

    // ================================================================
    // 项目经历解析（独立于工作经历）
    // ================================================================

    /** 行首日期范围：如 "2026-01 - 2026-06 xxx"、"2025.02 – 2025.06 项目名 · 角色" */
    private static final Pattern PROJECT_DATE_PREFIX_PATTERN = Pattern.compile(
            "^(\\d{4}[-.]\\d{1,2})\\s*[-–—至到~]+\\s*(\\d{4}[-.]\\d{1,2}|至今|Present)",
            Pattern.CASE_INSENSITIVE);

    /** 项目标签：支持 ▌ 前缀、【】括号、技术亮点等常见格式。group(1) 捕获标签名。 */
    private static final Pattern PROJECT_LABEL_PATTERN = Pattern.compile(
            "^(?:▌\\s*)?(?:【?)?(项目名称|项目名|所属公司|软件架构|项目概述|项目职责|我的职责"
                    + "|项目描述|项目介绍|项目说明|项目内容|项目背景|项目主要内容|项目亮点"
                    + "|职位|工作描述|职责"
                    + "|技术栈|技术亮点|技术|使用技术|开发工具|开发环境"
                    + "|project name|project description|tech stack)(?:】)?[：:]?\\s*",
            Pattern.CASE_INSENSITIVE);

    // ================================================================
    // 统一 bullet 处理常量
    // ================================================================

    /** 所有简历中可能出现的 bullet 字符集合（统一来源）。 */
    private static final String BULLET_CHARS = "●•◆►▸\\-·>○■□▲△▼▽★☆";

    /** 匹配行首 bullet：group(1)=前导空白, group(2)=bullet 字符, group(3)=内容。 */
    private static final Pattern BULLET_LINE_PATTERN = Pattern.compile(
            "^([\\s]*)([" + BULLET_CHARS + "])\\s*(.*)$");

    /** Level 1 列表输出标记：实心圆（●）。 */
    private static final char MARKER_LEVEL_1 = '\u25CF'; // ●

    /** Level 2 列表输出标记：空心圆（○）。 */
    private static final char MARKER_LEVEL_2 = '\u25CB'; // ○

    /** 列表引入标签下内容的缩进（两个全角空格 = 两个中文字符宽度）。 */
    private static final String LIST_INDENT = "\u3000\u3000";

    /** 列表引入标签：这些标签后面的内容通常是列表项而非段落。 */
    private static final java.util.Set<String> LIST_INTRODUCING_LABELS = java.util.Set.of(
            "项目亮点", "我的职责", "技术亮点", "项目职责", "职责",
            "工作描述", "职位"
    );

    /**
     * 按 ▌ 标志分割项目条目。
     */
    private List<String> splitProjectEntriesByMarker(String text) {
        if (!text.contains("▌")) return List.of();
        // 在 ▌ 前换行处切分
        String[] parts = text.split("\\n?(?=▌)");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 按行首日期范围分割项目条目。
     */
    private List<String> splitProjectEntriesByDate(String text) {
        String[] lines = text.split("\n");
        List<String> entries = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean foundDateBoundary = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (!current.isEmpty()) current.append("\n");
                continue;
            }

            if (PROJECT_DATE_PREFIX_PATTERN.matcher(trimmed).find()) {
                String prev = current.toString().trim();
                if (!prev.isEmpty()) entries.add(prev);
                current = new StringBuilder();
                current.append(trimmed).append("\n");
                foundDateBoundary = true;
            } else {
                current.append(trimmed).append("\n");
            }
        }

        String last = current.toString().trim();
        if (!last.isEmpty()) entries.add(last);

        return foundDateBoundary ? entries : List.of();
    }

    private List<ParsedResume.ProjectEntry> parseProjects(String text) {
        List<String> entries = null;
        // 优先 ▌ 分割
        entries = splitProjectEntriesByMarker(text);
        if (entries.isEmpty()) entries = splitProjectEntriesByDate(text);
        if (entries.isEmpty()) entries = splitEntries(text);

        List<ParsedResume.ProjectEntry> projects = new ArrayList<>();
        for (String entry : entries) {
            ParsedResume.ProjectEntry proj = parseProjectEntry(entry);
            if (proj != null) {
                projects.add(proj);
            }
        }
        return projects;
    }

    /**
     * 从单条项目描述中提取结构化信息。
     */
    private ParsedResume.ProjectEntry parseProjectEntry(String entry) {
        String clean = entry.trim();
        if (clean.isEmpty()) return null;

        String[] lines = clean.split("\n");
        String projectName = "";
        String role = "";
        String[] dates = new String[]{"", ""};
        StringBuilder techStack = new StringBuilder();
        List<String> descLines = new ArrayList<>(); // 收集描述行，统一 bullet 处理
        boolean hasContent = false;
        boolean techStackWasLast = false;

        // 从头部区域提取日期
        StringBuilder headerArea = new StringBuilder();
        for (int i = 0; i < lines.length && i < 5; i++) {
            if (!lines[i].trim().isEmpty()) {
                headerArea.append(lines[i].trim()).append(" ");
            }
        }
        dates = extractDates(headerArea.toString());
        if (dates[0].isEmpty() && dates[1].isEmpty()) {
            dates = extractDates(clean);
        }

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // ── 标签行 ──
            Matcher labelM = PROJECT_LABEL_PATTERN.matcher(line);
            if (labelM.find()) {
                String label = labelM.group(1);
                String content = line.substring(labelM.end()).trim();

                if (label.contains("项目名称") || label.contains("project name")) {
                    if (!content.isEmpty()) {
                        projectName = content;
                        Matcher dm = DATE_RANGE_PATTERN.matcher(content);
                        if (dm.find()) {
                            if (dates[0].isEmpty()) dates[0] = normalizeDate(dm.group(1));
                            if (dates[1].isEmpty()) dates[1] = normalizeDate(dm.group(2));
                            projectName = content.replaceAll(
                                    "\\s*\\d{4}[-.]\\d{1,2}\\s*[-–—至到~]+\\s*(?:\\d{4}[-.]\\d{1,2}|至今|Present).*$",
                                    "").trim();
                        }
                    }
                } else if (label.contains("技术栈") || label.contains("技术")
                        || label.contains("软件架构") || label.contains("tech stack")
                        || label.contains("开发工具") || label.contains("开发环境")) {
                    if (!content.isEmpty()) {
                        if (!techStack.isEmpty()) techStack.append("; ");
                        techStack.append(content);
                        techStackWasLast = content.endsWith("+") || content.endsWith(",")
                                || content.endsWith("、");
                    }
                    hasContent = true;
                } else {
                    // 所属公司/项目描述/项目亮点/我的职责/职位/工作描述 →
                    // 保持原始行格式，交给 processDescriptionLines 统一处理
                    descLines.add(line);
                    hasContent = true;
                }
                continue;
            }

            // ── 技术栈续行检测 ──
            if (techStackWasLast && isTechContinuationLine(line)) {
                techStack.append(" ").append(line);
                techStackWasLast = line.endsWith("+") || line.endsWith(",") || line.endsWith("、");
                continue;
            }
            techStackWasLast = false;

            String trimmed = line.replaceAll("^[\\s▌]+", "").trim();
            if (trimmed.isEmpty()) continue;

            // ── 项目名 · 角色 ──
            if (projectName.isEmpty() && (trimmed.contains("·") || trimmed.contains("|") || trimmed.contains("｜"))) {
                String[] nameRole = extractNameAndRole(trimmed);
                if (!nameRole[0].isEmpty()) projectName = nameRole[0];
                if (!nameRole[1].isEmpty()) role = nameRole[1];
                continue;
            }

            // ── 纯项目名称行 ──
            if (!hasContent && projectName.isEmpty() && trimmed.length() >= 3 && trimmed.length() <= 60
                    && !trimmed.startsWith("负责") && !trimmed.startsWith("基于")
                    && !trimmed.startsWith("使用") && !trimmed.startsWith("设计")
                    && !trimmed.startsWith("构建")) {
                projectName = trimmed;
                continue;
            }

            // ── 描述内容行 → 收集用于统一 bullet 处理 ──
            descLines.add(line);
            hasContent = true;
        }

        // 回退：提取项目名
        if (projectName.isEmpty()) {
            for (String l : lines) {
                String t = l.trim().replaceAll("^▌\\s*", "");
                if (t.isEmpty()) continue;
                if (PROJECT_DATE_PREFIX_PATTERN.matcher(t).find()) continue;
                if (PROJECT_LABEL_PATTERN.matcher(t).find()) continue;
                String cleaned = t.replaceAll("^[" + BULLET_CHARS + "]\\s*", "").trim();
                if (cleaned.length() >= 3 && cleaned.length() <= 60) {
                    projectName = cleaned;
                    break;
                }
            }
        }

        if (projectName.isEmpty() && descLines.isEmpty() && techStack.isEmpty()) return null;

        // ── 统一 bullet 处理 + 技术栈前缀 ──
        String processedDesc = processDescriptionLines(descLines.toArray(new String[0]));
        StringBuilder finalDesc = new StringBuilder();
        if (!techStack.isEmpty()) {
            finalDesc.append("技术栈：").append(techStack).append("\n");
        }
        if (!processedDesc.isEmpty()) {
            finalDesc.append(processedDesc);
        }

        ParsedResume.ProjectEntry result = ParsedResume.ProjectEntry.builder()
                .company(projectName).position(role)
                .start(dates[0]).end(dates[1])
                .description(finalDesc.toString().trim())
                .build();

        return result;
    }

    /** 判断一行是否为技术栈续行（如 "SkyWalking" 紧跟在 "Sentinel +" 后面） */
    private boolean isTechContinuationLine(String line) {
        if (line.isEmpty()) return false;
        // 续行特征：短、不含中文字符、看起来是技术术语
        boolean hasNoChinese = !line.matches(".*[\\u4e00-\\u9fa5].*");
        boolean isShort = line.length() <= 40;
        // 以 + / , / 、 结尾或包含技术相关字符
        boolean looksTechy = line.matches("^[A-Za-z0-9+#.\\-_\\s]+$")
                || line.matches("^[A-Z][a-z]*$")  // PascalCase single word (like SkyWalking)
                || line.contains("+") || line.contains("#");
        return hasNoChinese && isShort && looksTechy;
    }

    /** 从 "电商交易与营销平台 主要成员" 中解析项目名和角色 */
    private String[] extractNameAndRole(String text) {
        String t = text.replaceAll("[·]$", "").trim(); // 去掉末尾的 ·
        if (t.isEmpty()) return new String[]{"", ""};

        // 尝试按 · | ｜ 分割
        String[] parts = t.split("[·|｜]");
        String name = "";
        String r = "";

        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            if (p.matches(".*\\d{4}.*")) continue; // skip date fragments
            if (p.length() <= 2 && (p.equals("Java") || p.equals("Go") || p.equals("C++"))) {
                r = p;
            } else if (isRoleText(p)) {
                r = p;
            } else if (name.isEmpty() && p.length() >= 3) {
                name = p;
            } else if (!name.isEmpty()) {
                r = p;
            }
        }

        // 没有分隔符时，尝试按空格拆分，检查最后部分是否为角色关键词
        if (name.isEmpty() && parts.length == 1 && t.length() >= 3) {
            String[] words = t.split("\\s+");
            if (words.length >= 2 && isRoleText(words[words.length - 1])) {
                r = words[words.length - 1];
                name = String.join(" ", java.util.Arrays.copyOf(words, words.length - 1));
            } else {
                name = t;
            }
        }

        return new String[]{name, r};
    }

    /** 判断文本是否为角色描述（主要负责/主要成员/核心开发/负责人 等） */
    private boolean isRoleText(String text) {
        return text.contains("主要") || text.contains("负责") || text.contains("核心")
                || text.contains("开发") || text.contains("架构") || text.contains("设计")
                || text.contains("成员") || text.contains("组长") || text.matches("^[A-Z][a-z]+$")
                || text.equals("Java") || text.equals("Go") || text.equals("Python")
                || text.equals("C++") || text.equals("前端") || text.equals("后端")
                || text.equals("全栈") || text.equals("测试");
    }

    private String extractCompany(String text) {
        // 中文公司名：常见后缀
        Pattern cnCompany = Pattern.compile(
                "([\\u4e00-\\u9fa5A-Za-z0-9]{2,40}(?:公司|集团|科技|网络|软件|"
                        + "信息|数据|金融|银行|证券|保险|"
                        + "有限公司|股份有限公司|有限责任公司|"
                        + "Inc|Corp|Ltd|LLC|Co\\.|Corporation|Limited|"
                        + "ByteDance|Alibaba|Tencent|Baidu|Huawei|"
                        + "Google|Microsoft|Apple|Amazon|Meta|Facebook|Netflix|"
                        + "字节跳动|阿里巴巴|腾讯|百度|华为|美团|京东|网易|"
                        + "拼多多|滴滴|小米|快手|哔哩哔哩))");
        Matcher m = cnCompany.matcher(text);
        if (m.find()) return m.group(1).trim();

        return "";
    }

    private String extractJobTitle(String text) {
        // 第一优先级：完整中文职位（级别 + 技术栈 + 职位后缀）
        Pattern fullPattern = Pattern.compile(
                "(?:高级|资深|首席|初级|中级|主管)\\s*"
                        + "(?:Java|Python|Golang|Go|C\\+\\+|PHP|Node\\.?js|\\.NET|"
                        + "前端|后端|全栈|Web|Android|iOS|客户端|服务端|"
                        + "大数据|算法|AI|人工智能|NLP|CV|自然语言|计算机视觉|机器学习|深度学习|"
                        + "测试|运维|SRE|DevOps|安全|架构|数据|产品|"
                        + "嵌入式|驱动|内核|网络|云计算|区块链|"
                        + "TypeScript|JavaScript|Kotlin|Swift|Flutter|React|Vue|Angular|Spring)"
                        + "\\s*(?:开发|测试|研发|设计)?\\s*"
                        + "(?:工程师|架构师|程序员|经理|总监|主管|负责人|专家|"
                        + "Software\\s*Engineer|Developer|Architect|Manager|Designer)",
                Pattern.CASE_INSENSITIVE);
        Matcher m = fullPattern.matcher(text);
        if (m.find()) return m.group().trim();

        // 第一.五优先级：技术栈 + 级别 + 职位后缀（如 "Java中级开发工程师"、"前端高级工程师"）
        Pattern techLevelPattern = Pattern.compile(
                "(?:Java|Python|Golang|Go|C\\+\\+|PHP|Node\\.?js|\\.NET|"
                        + "前端|后端|全栈|Web|Android|iOS|客户端|服务端|"
                        + "大数据|算法|AI|人工智能|NLP|CV|测试|运维|SRE|DevOps|安全|架构|数据|产品|"
                        + "嵌入式|驱动|内核|网络|云计算|区块链|"
                        + "TypeScript|JavaScript|Kotlin|Swift|Flutter|React|Vue|Angular|Spring)"
                        + "\\s*(?:高级|资深|首席|初级|中级|主管)\\s*"
                        + "(?:开发|测试|研发|设计)?\\s*"
                        + "(?:工程师|架构师|程序员|经理|总监|主管|负责人|专家|"
                        + "Software\\s*Engineer|Developer|Architect|Manager|Designer)",
                Pattern.CASE_INSENSITIVE);
        m = techLevelPattern.matcher(text);
        if (m.find()) return m.group().trim();

        // 第二优先级：技术栈 + 简单职位后缀（如 "Java开发"、"后端实习"）
        Pattern midPattern = Pattern.compile(
                "(?:Java|Python|Golang|Go|C\\+\\+|PHP|Node\\.?js|\\.NET|"
                        + "前端|后端|全栈|Web|Android|iOS|嵌入式|驱动|"
                        + "大数据|算法|AI|测试|运维|SRE|DevOps|数据|产品|"
                        + "TypeScript|JavaScript|Kotlin|Swift|Flutter|React|Vue|Angular|Spring)"
                        + "\\s*(?:开发|测试|研发|设计|实习|实习生|"
                        + "工程师|架构师|程序员|设计师|分析师|经理|总监|"
                        + "Software\\s+Engineer|Developer|Architect|Manager)",
                Pattern.CASE_INSENSITIVE);
        m = midPattern.matcher(text);
        if (m.find()) return m.group().trim();

        // 第三优先级：纯英文职位
        Pattern engPattern = Pattern.compile(
                "(?:Senior|Staff|Principal|Lead)\\s+(?:Software\\s+)?(?:Engineer|Developer|Architect|Designer|Analyst)"
                        + "|(?:Engineering\\s+)?Manager|Director\\s+of\\s+Engineering|VP\\s+of\\s+Engineering"
                        + "|CTO|CIO|CEO|SRE|DevOps\\s+Engineer",
                Pattern.CASE_INSENSITIVE);
        m = engPattern.matcher(text);
        if (m.find()) return m.group().trim();

        return "";
    }

    // ================================================================
    // 技能文本记录边界处理
    // ================================================================

    /**
     * 技能分类标签关键词（用于判断记录边界）。
     * 匹配时需结合后面的中文冒号 "：" 或英文冒号 ":"。
     */
    private static final Set<String> SKILL_CATEGORY_LABELS = Set.of(
            "编程语言", "脚本语言", "标记语言",
            "核心框架", "后端框架", "前端框架", "后端技术", "前端技术", "UI框架",
            "ORM框架", "RPC框架", "微服务框架",
            "数据库", "关系型数据库", "非关系型数据库", "NoSQL", "缓存", "搜索引擎",
            "中间件", "消息队列", "消息中间件",
            "开发工具", "构建工具", "依赖管理", "IDE", "编辑器",
            "DevOps", "CI/CD", "容器化", "容器编排", "云计算", "云平台",
            "版本控制", "代码管理",
            "操作系统", "Linux",
            "大数据", "数据处理", "流处理", "数据仓库",
            "网络协议", "通信协议",
            "测试框架", "自动化测试", "单元测试", "集成测试",
            "项目管理", "协作工具", "文档工具",
            "设计模式", "架构设计", "系统设计",
            "AI", "人工智能", "机器学习", "深度学习", "自然语言处理",
            "语言能力", "外语水平",
            "业务领域", "行业经验"
    );

    /**
     * 处理技能段落文本，将记录边界用 {@code \n\n} 分隔，
     * 同一记录内的续行用 {@code \n} 连接。
     *
     * <p>记录起始识别规则：
     * <ol>
     *   <li>行首为 bullet 字符（●、•、◆ 等）</li>
     *   <li>行首匹配已知分类标签 + 冒号（"核心框架："、"编程语言" 等）</li>
     *   <li>行首形如 "熟练"、"熟悉"、"精通"、"了解" 等动词开头</li>
     * </ol>
     * 不满足以上条件的行视为上一条记录的续行（如 PDF 换行导致的断行）。</p>
     */
    private String processSkillsText(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) return "";

        String text = rawContent.trim();

        // 单段落无换行 → 按中文句号/分号拆分为独立记录
        if (!text.contains("\n") && text.length() > 80) {
            String[] sentences = text.split("[。；]");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sentences.length; i++) {
                String s = sentences[i].trim();
                if (s.isEmpty()) continue;
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(s);
            }
            return sb.toString();
        }

        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();
        boolean isFirst = true;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            boolean isNewRecord = isSkillRecordStart(trimmed);

            if (isNewRecord) {
                if (!isFirst) result.append("\n\n");
                result.append(trimmed);
                isFirst = false;
            } else {
                // 续行：用单个 \n 连接
                result.append("\n").append(trimmed);
            }
        }

        return result.toString();
    }

    /**
     * 判断一行技能文本是否为新的记录起始。
     */
    private boolean isSkillRecordStart(String line) {
        if (line.isEmpty()) return false;

        // 1. 行首 bullet 字符
        if (line.matches("^[" + BULLET_CHARS + "]")) return true;

        // 2. 已知分类标签 + 冒号
        for (String label : SKILL_CATEGORY_LABELS) {
            if (line.startsWith(label + "：") || line.startsWith(label + ":")) {
                return true;
            }
        }

        // 3. 以描述动词开头（常见于中文技能描述行，后接任意非空字符）
        if (line.matches("^(熟练|熟悉|精通|掌握|了解|擅长|能够|具备|具有|使用|运用|"
                + "深入|多年|丰富|扎实|良好|较强|优秀)\\s*\\S")) {
            return true;
        }

        // 4. 行首大写字母开头 + 空格（英文技能标签，如 "Java 开发"）
        if (line.matches("^[A-Z][a-zA-Z0-9+#.\\-]*\\s")) return true;

        // 默认：续行
        return false;
    }

    // ================================================================
    // 技能提取
    // ================================================================

    private List<String> extractSkillsFromText(String text) {
        Set<String> found = new LinkedHashSet<>();

        // 检查编程语言
        for (String skill : PROGRAMMING_LANGUAGES) {
            checkSkill(text, skill, found);
        }
        // 检查后端框架
        for (String skill : BACKEND_FRAMEWORKS) {
            checkSkill(text, skill, found);
        }
        // 检查前端框架
        for (String skill : FRONTEND_FRAMEWORKS) {
            checkSkill(text, skill, found);
        }
        // 检查数据库
        for (String skill : DATABASES) {
            checkSkill(text, skill, found);
        }
        // 检查中间件
        for (String skill : MIDDLEWARE) {
            checkSkill(text, skill, found);
        }
        // 检查 DevOps
        for (String skill : DEVOPS) {
            checkSkill(text, skill, found);
        }
        // 检查 AI/数据
        for (String skill : AI_DATA) {
            checkSkill(text, skill, found);
        }
        // 检查设计工具
        for (String skill : DESIGN_TOOLS) {
            checkSkill(text, skill, found);
        }
        // 检查项目管理
        for (String skill : MANAGEMENT) {
            checkSkill(text, skill, found);
        }
        // 检查中文技能
        for (String skill : CHINESE_SKILLS) {
            checkSkill(text, skill, found);
        }

        return new ArrayList<>(found);
    }

    private void checkSkill(String text, String skill, Set<String> found) {
        try {
            String pattern = "(?i)(^|[\\s,，;；、/|+])" + Pattern.quote(skill) + "([\\s,，;；、/|+]|$)";
            if (Pattern.compile(pattern).matcher(text).find()) {
                found.add(skill.replace("\\", ""));
            }
        } catch (Exception ignored) {
            // skip invalid patterns
        }
    }

    // ================================================================
    // 语言能力
    // ================================================================

    private List<String> parseLanguages(String text) {
        List<String> languages = new ArrayList<>();
        if (text.isEmpty()) return languages;

        Map<String, String> langMap = new LinkedHashMap<>();
        langMap.put("英语|英文|English|英语", "英语");
        langMap.put("日语|日文|Japanese|日本語", "日语");
        langMap.put("韩语|韩文|Korean|한국어", "韩语");
        langMap.put("法语|法文|French|Français", "法语");
        langMap.put("德语|德文|German|Deutsch", "德语");
        langMap.put("西班牙语|Spanish|Español", "西班牙语");
        langMap.put("俄语|Russian|Русский", "俄语");
        langMap.put("阿拉伯语|Arabic", "阿拉伯语");
        langMap.put("葡萄牙语|Portuguese", "葡萄牙语");

        for (Map.Entry<String, String> entry : langMap.entrySet()) {
            if (Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                languages.add(entry.getValue());
            }
        }
        return languages;
    }

    // ================================================================
    // 证书
    // ================================================================

    private List<String> parseCertifications(String text) {
        List<String> certs = new ArrayList<>();
        if (text.isEmpty()) return certs;

        Pattern certPattern = Pattern.compile(
                "((?:PMP|ACP|NPDP|PRINCE2|ITIL|TOGAF|CPA|CFA|FRM|ACCA|"
                        + "CISSP|CISA|CEH|CompTIA|"
                        + "AWS\\s*(?:Certified|Solutions|Developer|SysOps)|"
                        + "Azure\\s*(?:Solutions|Developer|Administrator)|"
                        + "Google\\s*(?:Cloud|Professional)|"
                        + "CKA|CKAD|CKS|RHCSA|RHCE|"
                        + "Oracle\\s*OCP|MySQL\\s*OCP|"
                        + "PETS[\\s0-9]*|"
                        + "CET[-–]?[4|6]|TEM[-–]?[4|8]|"
                        + "GRE|GMAT|IELTS|TOEFL|TOEIC|"
                        + "JLPT\\s*[Nn]?[1-5]|"
                        + "[\\u4e00-\\u9fa5]{2,}(?:证书|资格|执业|工程师)|"
                        + "(?:律师|医师|教师|会计|建造师|建筑师|"
                        + "造价师|监理师|资产评估师|"
                        + "软考|计算机等级|普通话|"
                        + "CCIE|CCNP|CCNA|HCIP|HCIA|HCIE)))",
                Pattern.CASE_INSENSITIVE);

        Matcher m = certPattern.matcher(text);
        while (m.find()) {
            String cert = m.group().trim();
            if (cert.length() >= 2 && !certs.contains(cert)) {
                certs.add(cert);
            }
        }

        return certs;
    }

    // ================================================================
    // 奖项荣誉
    // ================================================================

    private List<ParsedResume.AwardEntry> parseAwards(String text) {
        List<ParsedResume.AwardEntry> awards = new ArrayList<>();
        if (text == null || text.isBlank()) return awards;

        // 按行解析，每行可能包含一个奖项
        String[] lines = text.split("\n");
        // 日期+内容模式的奖项，如 "2023-06 全国大学生数学建模竞赛一等奖"
        Pattern dateAwardPattern = Pattern.compile(
                "(\\d{4}[-.年]\\s*(?:\\d{1,2})?)\\s*(.+)");
        // 纯内容模式，匹配包含奖项关键词的行
        Pattern awardPattern = Pattern.compile(
                "((?:国家|省部|市级|校级|院级)?[\\u4e00-\\u9fa5]{2,}(?:奖|杯|称号|荣誉))" +
                "|((?:一等|二等|三等|优秀|特等|金|银|铜)(?:奖))" +
                "|((?:全国|国际|省|市|校)(?:[\\u4e00-\\u9fa5]{2,}(?:大赛|竞赛|比赛|奖学金)))" +
                "|(?:Scholarship|Award|Prize|Honor|Fellowship)");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher dm = dateAwardPattern.matcher(trimmed);
            if (dm.find()) {
                String content = dm.group(2).trim();
                // 尝试从内容中提取奖项名称、颁发单位
                awards.add(ParsedResume.AwardEntry.builder()
                        .date(dm.group(1).trim()).name(content).description(trimmed).build());
            } else if (awardPattern.matcher(trimmed).find()) {
                awards.add(ParsedResume.AwardEntry.builder()
                        .name(trimmed).date("").description(trimmed).build());
            }
        }

        return awards;
    }

    // ================================================================
    // 自我评价/总结
    // ================================================================

    private String parseSummary(String text) {
        if (text.isEmpty()) return "";

        // 收集所有有意义的段落（长度 > 20），段落间用两个换行分隔
        String[] paragraphs = text.split("\n\n");
        StringBuilder result = new StringBuilder();
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (trimmed.length() > 20) {
                if (!result.isEmpty()) result.append("\n");
                result.append(trimmed);
            }
        }

        if (!result.isEmpty()) {
            String full = result.toString();
            // 放宽限制：最多 2000 字
            if (full.length() > 2000) {
                full = full.substring(0, 2000) + "...";
            }
            return full;
        }

        // 回退：整个文本
        return text.length() > 2000 ? text.substring(0, 2000) + "..." : text;
    }

    // ================================================================
    // 通用工具方法
    // ================================================================

    /**
     * 按空行将文本分割为条目列表。
     */
    private List<String> splitEntries(String text) {
        String[] blocks = text.split("\n{2,}");
        return Arrays.stream(blocks)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 从文本中提取日期范围。
     *
     * @return [start, end]，未找到时对应的元素为 ""
     */
    private String[] extractDates(String text) {
        Matcher m = DATE_RANGE_PATTERN.matcher(text);
        if (m.find()) {
            String start = normalizeDate(m.group(1));
            String end = normalizeDate(m.group(2));
            if (end == null) end = "";
            return new String[]{start, end};
        }

        // 回退：尝试提取两个年份（文本中可能存在多个年份但分隔符未被识别）
        Pattern yearPattern = Pattern.compile("(\\d{4})[.\\-–—/年]");
        m = yearPattern.matcher(text);
        String firstYear = "";
        String secondYear = "";
        while (m.find()) {
            if (firstYear.isEmpty()) {
                firstYear = m.group(1);
            } else if (secondYear.isEmpty()) {
                secondYear = m.group(1);
            }
        }
        if (!firstYear.isEmpty()) {
            return new String[]{firstYear, secondYear};
        }

        return new String[]{"", ""};
    }

    private String normalizeDate(String date) {
        if (date == null) return "";
        if ("至今".equals(date) || "Present".equalsIgnoreCase(date) || "至今".equals(date)) {
            return "至今";
        }
        Matcher m = YEAR_MONTH_PATTERN.matcher(date);
        if (m.find()) {
            String month = m.group(2);
            if (month != null && !month.isEmpty()) {
                return m.group(1) + "-" + String.format("%02d", Integer.parseInt(month));
            }
            return m.group(1);
        }
        return date.trim();
    }

    // ================================================================
    // 空结果
    // ================================================================

    private ParsedResume emptyResult() {
        return ParsedResume.builder()
                .name("").email("").phone("").location("").gender("")
                .birthDate("").household("").politicalStatus("").workYears("")
                .desiredPosition("").age("").skillsText("").summary("")
                .build();
    }

    // ================================================================
    // 内部数据类
    // ================================================================

    private record Section(String header, String content) {}

    private record PersonalInfo(String name, String email, String phone, String location,
                                  String gender, String birthDate,
                                  String household, String politicalStatus,
                                  String workYears, String desiredPosition, String age) {}
}
