package com.smartrecruit.interview.enums;

import lombok.Getter;

/**
 * {@code smart_recruit_interview}数据库的枚举定义。
 *
 * @author xdh
 * @since 2026-05-04
 */
public final class InterviewEnums {

    private InterviewEnums() {}

    /** 0=PHONE,1=VIDEO,2=ONSITE,3=AI,4=TECHNICAL,5=HR,6=LEADERSHIP */
    @Getter
    public enum InterviewType {
        PHONE(0, "电话面试"),
        VIDEO(1, "视频面试"),
        ONSITE(2, "现场面试"),
        AI(3, "AI面试"),
        TECHNICAL(4, "技术面试"),
        HR(5, "HR面试"),
        LEADERSHIP(6, "领导面试");

        private final int code;
        private final String label;

        InterviewType(int code, String label) { this.code = code; this.label = label; }

        public static InterviewType fromCode(Integer code) {
            if (code == null) return null;
            for (InterviewType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=SCHEDULED,1=IN_PROGRESS,2=COMPLETED,3=CANCELLED,4=RESCHEDULED,5=NO_SHOW */
    @Getter
    public enum InterviewStatus {
        SCHEDULED(0, "已安排"),
        IN_PROGRESS(1, "进行中"),
        COMPLETED(2, "已完成"),
        CANCELLED(3, "已取消"),
        RESCHEDULED(4, "已改期"),
        NO_SHOW(5, "未到场");

        private final int code;
        private final String label;

        InterviewStatus(int code, String label) { this.code = code; this.label = label; }

        public static InterviewStatus fromCode(Integer code) {
            if (code == null) return null;
            for (InterviewStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=PASS,1=FAIL,2=HOLD */
    @Getter
    public enum InterviewResult {
        PASS(0, "通过"),
        FAIL(1, "不通过"),
        HOLD(2, "待定");

        private final int code;
        private final String label;

        InterviewResult(int code, String label) { this.code = code; this.label = label; }

        public static InterviewResult fromCode(Integer code) {
            if (code == null) return null;
            for (InterviewResult v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=FRONTEND,1=BACKEND,2=AI,3=PM,4=DEVOPS,5=DATA_ENGINEER,6=FULLSTACK,7=MOBILE,8=QA */
    @Getter
    public enum QuestionPositionType {
        FRONTEND(0, "前端"),
        BACKEND(1, "后端"),
        AI(2, "AI"),
        PM(3, "产品"),
        DEVOPS(4, "运维"),
        DATA_ENGINEER(5, "数据"),
        FULLSTACK(6, "全栈"),
        MOBILE(7, "移动端"),
        QA(8, "测试");

        private final int code;
        private final String label;

        QuestionPositionType(int code, String label) { this.code = code; this.label = label; }

        public static QuestionPositionType fromCode(Integer code) {
            if (code == null) return null;
            for (QuestionPositionType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=TECHNOLOGY,1=PROJECT,2=BEHAVIORAL,3=MANAGEMENT */
    @Getter
    public enum QuestionCategory {
        TECHNOLOGY(0, "技术"),
        PROJECT(1, "项目"),
        BEHAVIORAL(2, "行为"),
        MANAGEMENT(3, "管理");

        private final int code;
        private final String label;

        QuestionCategory(int code, String label) { this.code = code; this.label = label; }

        public static QuestionCategory fromCode(Integer code) {
            if (code == null) return null;
            for (QuestionCategory v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=EASY,1=MEDIUM,2=HARD */
    @Getter
    public enum QuestionDifficulty {
        EASY(0, "简单"),
        MEDIUM(1, "中等"),
        HARD(2, "困难");

        private final int code;
        private final String label;

        QuestionDifficulty(int code, String label) { this.code = code; this.label = label; }

        public static QuestionDifficulty fromCode(Integer code) {
            if (code == null) return null;
            for (QuestionDifficulty v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=STRONG_HIRE,1=HIRE,2=HOLD,3=REJECT,4=RETEST */
    @Getter
    public enum AssessmentSuggestion {
        STRONG_HIRE(0, "强烈推荐录用"),
        HIRE(1, "推荐录用"),
        HOLD(2, "待定"),
        REJECT(3, "不推荐"),
        RETEST(4, "加试");

        private final int code;
        private final String label;

        AssessmentSuggestion(int code, String label) { this.code = code; this.label = label; }

        public static AssessmentSuggestion fromCode(Integer code) {
            if (code == null) return null;
            for (AssessmentSuggestion v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=STRONG_HIRE,1=HIRE,2=HOLD,3=REJECT */
    @Getter
    public enum HireRecommendation {
        STRONG_HIRE(0, "强烈推荐录用"),
        HIRE(1, "推荐录用"),
        HOLD(2, "待定"),
        REJECT(3, "不推荐");

        private final int code;
        private final String label;

        HireRecommendation(int code, String label) { this.code = code; this.label = label; }

        public static HireRecommendation fromCode(Integer code) {
            if (code == null) return null;
            for (HireRecommendation v : values()) { if (v.code == code) return v; }
            return null;
        }
    }
}
