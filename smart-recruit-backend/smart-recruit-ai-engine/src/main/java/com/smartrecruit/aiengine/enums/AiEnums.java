package com.smartrecruit.aiengine.enums;

import lombok.Getter;

/**
 * {@code smart_recruit_ai}数据库的枚举类。
 *
 * @author xdh
 * @since 2026-05-04
 */
public final class AiEnums {

    private AiEnums() {}

    /** 0=RESUME_PARSE,1=SCREEN,2=EVALUATE,3=PREDICT,4=RECOMMEND,5=QUESTION_GEN,6=JD_GEN,7=ORCHESTRATE */
    @Getter
    public enum TaskType {
        RESUME_PARSE(0, "简历解析"),
        SCREEN(1, "简历筛选"),
        EVALUATE(2, "面试评估"),
        PREDICT(3, "Offer预测"),
        RECOMMEND(4, "人才推荐"),
        QUESTION_GEN(5, "题目生成"),
        JD_GEN(6, "JD生成"),
        ORCHESTRATE(7, "流程编排"),
        INSIGHTS(8, "智能洞察");

        private final int code;
        private final String label;

        TaskType(int code, String label) { this.code = code; this.label = label; }

        public static TaskType fromCode(Integer code) {
            if (code == null) return null;
            for (TaskType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=LOW,1=MEDIUM,2=HIGH */
    @Getter
    public enum Priority {
        LOW(0, "低"),
        MEDIUM(1, "中"),
        HIGH(2, "高");

        private final int code;
        private final String label;

        Priority(int code, String label) { this.code = code; this.label = label; }

        public static Priority fromCode(Integer code) {
            if (code == null) return null;
            for (Priority v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=QUEUED,1=RUNNING,2=COMPLETED,3=FAILED,4=RETRYING */
    @Getter
    public enum TaskStatus {
        QUEUED(0, "排队中"),
        RUNNING(1, "执行中"),
        COMPLETED(2, "已完成"),
        FAILED(3, "失败"),
        RETRYING(4, "重试中");

        private final int code;
        private final String label;

        TaskStatus(int code, String label) { this.code = code; this.label = label; }

        public static TaskStatus fromCode(Integer code) {
            if (code == null) return null;
            for (TaskStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=INFO,1=WARNING,2=ERROR,3=SUCCESS */
    @Getter
    public enum EventType {
        INFO(0, "信息"),
        WARNING(1, "警告"),
        ERROR(2, "错误"),
        SUCCESS(3, "成功");

        private final int code;
        private final String label;

        EventType(int code, String label) { this.code = code; this.label = label; }

        public static EventType fromCode(Integer code) {
            if (code == null) return null;
            for (EventType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=ORCHESTRATOR,1=PARSER,2=SCREENER,3=GENERATOR,4=ASSESSOR,5=PREDICTOR */
    @Getter
    public enum EventSource {
        ORCHESTRATOR(0, "编排器"),
        PARSER(1, "解析器"),
        SCREENER(2, "筛选器"),
        GENERATOR(3, "生成器"),
        ASSESSOR(4, "评估器"),
        PREDICTOR(5, "预测器");

        private final int code;
        private final String label;

        EventSource(int code, String label) { this.code = code; this.label = label; }

        public static EventSource fromCode(Integer code) {
            if (code == null) return null;
            for (EventSource v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=SUCCESS,1=FAILED */
    @Getter
    public enum ParseLogStatus {
        SUCCESS(0, "成功"),
        FAILED(1, "失败");

        private final int code;
        private final String label;

        ParseLogStatus(int code, String label) { this.code = code; this.label = label; }

        public static ParseLogStatus fromCode(Integer code) {
            if (code == null) return null;
            for (ParseLogStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=SPEECH,1=CONTENT,2=VOICE,3=OVERALL */
    @Getter
    public enum AssessmentType {
        SPEECH(0, "语音识别"),
        CONTENT(1, "内容分析"),
        VOICE(2, "声纹分析"),
        OVERALL(3, "综合评估");

        private final int code;
        private final String label;

        AssessmentType(int code, String label) { this.code = code; this.label = label; }

        public static AssessmentType fromCode(Integer code) {
            if (code == null) return null;
            for (AssessmentType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=SUCCESS,1=FAILED */
    @Getter
    public enum InterviewLogStatus {
        SUCCESS(0, "成功"),
        FAILED(1, "失败");

        private final int code;
        private final String label;

        InterviewLogStatus(int code, String label) { this.code = code; this.label = label; }

        public static InterviewLogStatus fromCode(Integer code) {
            if (code == null) return null;
            for (InterviewLogStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }
}
