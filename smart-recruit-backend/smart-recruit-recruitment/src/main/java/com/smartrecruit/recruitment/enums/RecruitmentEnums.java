package com.smartrecruit.recruitment.enums;

import lombok.Getter;

/**
 * {@code smart_recruit_recruitment}数据库的枚举定义。
 *
 * @author xdh
 * @since 2026-05-04
 */
public final class RecruitmentEnums {

    private RecruitmentEnums() {}

    /** 0=P5,1=P6,2=P7,3=P8,4=P9,5=T2,6=T3,7=T4,8=T5,9=T6,10=T7,11=T8,12=T9 */
    @Getter
    public enum PositionLevel {
        P5(0, "P5"),
        P6(1, "P6"),
        P7(2, "P7"),
        P8(3, "P8"),
        P9(4, "P9"),
        T2(5, "T2"),
        T3(6, "T3"),
        T4(7, "T4"),
        T5(8, "T5"),
        T6(9, "T6"),
        T7(10, "T7"),
        T8(11, "T8"),
        T9(12, "T9");

        private final int code;
        private final String label;

        PositionLevel(int code, String label) { this.code = code; this.label = label; }

        public static PositionLevel fromCode(Integer code) {
            if (code == null) return null;
            for (PositionLevel v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=FULL_TIME,1=PART_TIME,2=INTERN,3=CONTRACT */
    @Getter
    public enum PositionType {
        FULL_TIME(0, "全职"),
        PART_TIME(1, "兼职"),
        INTERN(2, "实习"),
        CONTRACT(3, "外包");

        private final int code;
        private final String label;

        PositionType(int code, String label) { this.code = code; this.label = label; }

        public static PositionType fromCode(Integer code) {
            if (code == null) return null;
            for (PositionType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=ENTRY,1=JUNIOR,2=MID,3=SENIOR,4=LEAD,5=EXECUTIVE */
    @Getter
    public enum ExperienceLevel {
        ENTRY(0, "应届生"),
        JUNIOR(1, "初级"),
        MID(2, "中级"),
        SENIOR(3, "高级"),
        LEAD(4, "资深/Lead"),
        EXECUTIVE(5, "高管");

        private final int code;
        private final String label;

        ExperienceLevel(int code, String label) { this.code = code; this.label = label; }

        public static ExperienceLevel fromCode(Integer code) {
            if (code == null) return null;
            for (ExperienceLevel v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=HIGH_SCHOOL,1=ASSOCIATE,2=BACHELOR,3=MASTER,4=PHD */
    @Getter
    public enum EducationLevel {
        HIGH_SCHOOL(0, "高中"),
        ASSOCIATE(1, "大专"),
        BACHELOR(2, "本科"),
        MASTER(3, "硕士"),
        PHD(4, "博士");

        private final int code;
        private final String label;

        EducationLevel(int code, String label) { this.code = code; this.label = label; }

        public static EducationLevel fromCode(Integer code) {
            if (code == null) return null;
            for (EducationLevel v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=DRAFT,1=PUBLISHED,2=PAUSED,3=CLOSED */
    @Getter
    public enum JobStatus {
        DRAFT(0, "草稿"),
        PUBLISHED(1, "已发布"),
        PAUSED(2, "暂停招聘"),
        CLOSED(3, "已关闭");

        private final int code;
        private final String label;

        JobStatus(int code, String label) { this.code = code; this.label = label; }

        public static JobStatus fromCode(Integer code) {
            if (code == null) return null;
            for (JobStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=BOSS,1=LAGOU,2=LIEPIN,3=WEBSITE,4=LINKEDIN,5=OTHER */
    @Getter
    public enum ChannelCode {
        BOSS(0, "BOSS直聘"),
        LAGOU(1, "拉勾"),
        LIEPIN(2, "猎聘"),
        WEBSITE(3, "官网"),
        LINKEDIN(4, "LinkedIn"),
        OTHER(5, "其他");

        private final int code;
        private final String label;

        ChannelCode(int code, String label) { this.code = code; this.label = label; }

        public static ChannelCode fromCode(Integer code) {
            if (code == null) return null;
            for (ChannelCode v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=DIRECT,1=REFERRAL,2=WEBSITE,3=LINKEDIN,4=BOSS,5=LAGOU,6=LIEPIN,7=OTHER */
    @Getter
    public enum CandidateSource {
        DIRECT(0, "主动投递"),
        REFERRAL(1, "内推"),
        WEBSITE(2, "官网"),
        LINKEDIN(3, "LinkedIn"),
        BOSS(4, "BOSS直聘"),
        LAGOU(5, "拉勾"),
        LIEPIN(6, "猎聘"),
        OTHER(7, "其他");

        private final int code;
        private final String label;

        CandidateSource(int code, String label) { this.code = code; this.label = label; }

        public static CandidateSource fromCode(Integer code) {
            if (code == null) return null;
            for (CandidateSource v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=NEW,1=SCREENING,2=SCREEN_PASSED,3=INTERVIEWING,4=OFFERED,5=HIRED,6=REJECTED,7=WITHDRAWN */
    @Getter
    public enum CandidateStatus {
        NEW(0, "新入库"),
        SCREENING(1, "筛选中"),
        SCREEN_PASSED(2, "筛选通过"),
        INTERVIEWING(3, "面试中"),
        OFFERED(4, "已发Offer"),
        HIRED(5, "已入职"),
        REJECTED(6, "已淘汰"),
        WITHDRAWN(7, "已放弃");

        private final int code;
        private final String label;

        CandidateStatus(int code, String label) { this.code = code; this.label = label; }

        public static CandidateStatus fromCode(Integer code) {
            if (code == null) return null;
            for (CandidateStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=PDF,1=DOCX,2=JPG,3=PNG */
    @Getter
    public enum FileType {
        PDF(0, "PDF"),
        DOCX(1, "DOCX"),
        JPG(2, "JPG"),
        PNG(3, "PNG");

        private final int code;
        private final String label;

        FileType(int code, String label) { this.code = code; this.label = label; }

        public static FileType fromCode(Integer code) {
            if (code == null) return null;
            for (FileType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=PENDING,1=PARSING,2=SUCCESS,3=FAILED */
    @Getter
    public enum ParseStatus {
        PENDING(0, "待解析"),
        PARSING(1, "解析中"),
        SUCCESS(2, "解析成功"),
        FAILED(3, "解析失败");

        private final int code;
        private final String label;

        ParseStatus(int code, String label) { this.code = code; this.label = label; }

        public static ParseStatus fromCode(Integer code) {
            if (code == null) return null;
            for (ParseStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=PENDING,1=PASSED,2=REJECTED,3=SCREENING,4=SCREEN_FAILED */
    @Getter
    public enum ScreeningStatus {
        PENDING(0, "待筛选"),
        PASSED(1, "已通过"),
        REJECTED(2, "已淘汰"),
        SCREENING(3, "筛选中"),
        SCREEN_FAILED(4, "筛选失败");

        private final int code;
        private final String label;

        ScreeningStatus(int code, String label) { this.code = code; this.label = label; }

        public static ScreeningStatus fromCode(Integer code) {
            if (code == null) return null;
            for (ScreeningStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=DIRECT,1=REFERRAL,2=WEBSITE,3=LINKEDIN,4=BOSS,5=LAGOU,6=LIEPIN,7=OTHER */
    @Getter
    public enum SourceChannel {
        DIRECT(0, "主动投递"),
        REFERRAL(1, "内推"),
        WEBSITE(2, "官网"),
        LINKEDIN(3, "LinkedIn"),
        BOSS(4, "BOSS直聘"),
        LAGOU(5, "拉勾"),
        LIEPIN(6, "猎聘"),
        OTHER(7, "其他");

        private final int code;
        private final String label;

        SourceChannel(int code, String label) { this.code = code; this.label = label; }

        public static SourceChannel fromCode(Integer code) {
            if (code == null) return null;
            for (SourceChannel v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=PENDING,1=SCREENING,2=SCREEN_PASSED,3=SCREEN_FAILED,4=INTERVIEWING,5=INTERVIEW_PASSED,6=INTERVIEW_FAILED,7=OFFER_PENDING,8=OFFER_ACCEPTED,9=OFFER_DECLINED,10=HIRED,11=REJECTED,12=WITHDRAWN */
    @Getter
    public enum ApplicationStatus {
        PENDING(0, "待处理"),
        SCREENING(1, "筛选中"),
        SCREEN_PASSED(2, "筛选通过"),
        SCREEN_FAILED(3, "筛选未通过"),
        INTERVIEWING(4, "面试中"),
        INTERVIEW_PASSED(5, "面试通过"),
        INTERVIEW_FAILED(6, "面试未通过"),
        OFFER_PENDING(7, "待发Offer"),
        OFFER_ACCEPTED(8, "接受Offer"),
        OFFER_DECLINED(9, "拒绝Offer"),
        HIRED(10, "已入职"),
        REJECTED(11, "已淘汰"),
        WITHDRAWN(12, "已放弃");

        private final int code;
        private final String label;

        ApplicationStatus(int code, String label) { this.code = code; this.label = label; }

        public static ApplicationStatus fromCode(Integer code) {
            if (code == null) return null;
            for (ApplicationStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=RESUME_SCREEN,1=INTERVIEW,2=OFFER,3=ONBOARDING */
    @Getter
    public enum ApplicationStage {
        RESUME_SCREEN(0, "简历筛选"),
        INTERVIEW(1, "面试"),
        OFFER(2, "Offer"),
        ONBOARDING(3, "入职");

        private final int code;
        private final String label;

        ApplicationStage(int code, String label) { this.code = code; this.label = label; }

        public static ApplicationStage fromCode(Integer code) {
            if (code == null) return null;
            for (ApplicationStage v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=AI_SCREEN,1=PHONE_CALL,2=EMAIL,3=INTERVIEW,4=NOTE,5=REFERRAL,6=OFFER_COMM,7=OTHER */
    @Getter
    public enum CommunicationType {
        AI_SCREEN(0, "AI筛选"),
        PHONE_CALL(1, "电话沟通"),
        EMAIL(2, "邮件"),
        INTERVIEW(3, "面试"),
        NOTE(4, "备注"),
        REFERRAL(5, "内推"),
        OFFER_COMM(6, "Offer沟通"),
        OTHER(7, "其他");

        private final int code;
        private final String label;

        CommunicationType(int code, String label) { this.code = code; this.label = label; }

        public static CommunicationType fromCode(Integer code) {
            if (code == null) return null;
            for (CommunicationType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=APPLY,1=SCREEN,2=INTERVIEW,3=OFFER,4=HIRE,5=REFERRAL,6=SYSTEM */
    @Getter
    public enum ActivityType {
        APPLY(0, "投递"),
        SCREEN(1, "筛选"),
        INTERVIEW(2, "面试"),
        OFFER(3, "Offer"),
        HIRE(4, "入职"),
        REFERRAL(5, "内推"),
        SYSTEM(6, "系统");

        private final int code;
        private final String label;

        ActivityType(int code, String label) { this.code = code; this.label = label; }

        public static ActivityType fromCode(Integer code) {
            if (code == null) return null;
            for (ActivityType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }
}
