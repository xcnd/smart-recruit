package com.smartrecruit.recruitment.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 简历解析结果域对象 — 替代原有的 {@code Map<String, Object>} 传递解析结果。
 *
 * <p>字段名与前端 JSON key 一一对应，Jackson 序列化后与原 Map 格式完全兼容。
 * 所有列表字段使用 {@link Builder.Default} 保证不为 null。</p>
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedResume implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    // ── 个人信息 ──
    /** 姓名。 */
    private String name;
    /** 电子邮箱。 */
    private String email;
    /** 联系电话。 */
    private String phone;
    /** 性别。 */
    private String gender;
    /** 出生日期（字符串，保持简历原文格式）。 */
    private String birthDate;
    /** 年龄（字符串，可能保留“岁”等原文内容）。 */
    private String age;
    /** 所在城市/地区。 */
    private String location;
    /** 户籍所在地。 */
    private String household;
    /** 政治面貌。 */
    private String politicalStatus;
    /** 期望职位。 */
    private String desiredPosition;
    /** 工作年限（字符串，可能保留“年”等原文内容）。 */
    private String workYears;

    // ── 段落内容 ──
    /** 技能描述原文段落。 */
    private String skillsText;
    /** 个人简介/自我评价段落。 */
    private String summary;

    // ── 集合字段 ──
    /** 教育经历列表。 */
    @Builder.Default
    private List<EducationEntry> education = new ArrayList<>();
    /** 工作经历列表。 */
    @Builder.Default
    private List<ExperienceEntry> experience = new ArrayList<>();
    /** 项目经历列表。 */
    @Builder.Default
    private List<ProjectEntry> projects = new ArrayList<>();
    /** 技能标签列表。 */
    @Builder.Default
    private List<String> skills = new ArrayList<>();
    /** 语言能力列表。 */
    @Builder.Default
    private List<String> languages = new ArrayList<>();
    /** 证书/资质列表。 */
    @Builder.Default
    private List<String> certifications = new ArrayList<>();
    /** 获奖经历列表。 */
    @Builder.Default
    private List<AwardEntry> awards = new ArrayList<>();

    // ────────────────────────────────────────────────
    //  嵌套条目类型
    // ────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationEntry implements Serializable {
        /** 序列化版本号。 */
        @Serial
        private static final long serialVersionUID = 1L;
        /** 学校名称。 */
        private String school;
        /** 所学专业。 */
        private String major;
        /** 学历/学位。 */
        private String degree;
        @JsonAlias("startDate")
        /** 开始时间（yyyy-MM 或原文格式）。 */
        private String start;
        @JsonAlias("endDate")
        /** 结束时间（yyyy-MM 或原文格式）。 */
        private String end;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceEntry implements Serializable {
        /** 序列化版本号。 */
        @Serial
        private static final long serialVersionUID = 1L;
        /** 公司名称。 */
        private String company;
        /** 职位/岗位。 */
        private String position;
        @JsonAlias("startDate")
        /** 开始时间（yyyy-MM 或原文格式）。 */
        private String start;
        @JsonAlias("endDate")
        /** 结束时间（yyyy-MM 或原文格式）。 */
        private String end;
        /** 工作内容描述。 */
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectEntry implements Serializable {
        /** 序列化版本号。 */
        @Serial
        private static final long serialVersionUID = 1L;
        /** 项目名称（parser 以 "company" 字段存储，前端同时读取 "company" 和 "name" 作为兼容） */
        private String company;
        /** 角色（前端同时读取 "position" 和 "role"） */
        private String position;
        @JsonAlias("startDate")
        /** 开始时间（yyyy-MM 或原文格式）。 */
        private String start;
        @JsonAlias("endDate")
        /** 结束时间（yyyy-MM 或原文格式）。 */
        private String end;
        /** 项目内容描述。 */
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AwardEntry implements Serializable {
        /** 序列化版本号。 */
        @Serial
        private static final long serialVersionUID = 1L;
        /** 姓名。 */
        private String name;
        /** 获奖时间。 */
        private String date;
        /** 奖项说明。 */
        private String description;
    }
}
