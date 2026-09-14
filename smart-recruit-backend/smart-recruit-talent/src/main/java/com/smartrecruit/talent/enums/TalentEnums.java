package com.smartrecruit.talent.enums;

import lombok.Getter;

/**
 * {@code smart_recruit_talent} 数据库的枚举类。
 *
 * @author xdh
 * @since 2026-05-04
 */
public final class TalentEnums {

    private TalentEnums() {}

    /** 0=通用,1=技术,2=管理,3=实习,4=高管 */
    @Getter
    public enum PoolType {
        GENERAL(0, "通用"),
        TECHNICAL(1, "技术"),
        MANAGEMENT(2, "管理"),
        INTERN(3, "实习"),
        EXECUTIVE(4, "高管");

        private final int code;
        private final String label;

        PoolType(int code, String label) { this.code = code; this.label = label; }

        public static PoolType fromCode(Integer code) {
            if (code == null) return null;
            for (PoolType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=初级,1=中级,2=高级,3=专家,4=Leader */
    @Getter
    public enum SkillLevel {
        JUNIOR(0, "初级"),
        MID(1, "中级"),
        SENIOR(2, "高级"),
        EXPERT(3, "专家"),
        LEAD(4, "Leader");

        private final int code;
        private final String label;

        SkillLevel(int code, String label) { this.code = code; this.label = label; }

        public static SkillLevel fromCode(Integer code) {
            if (code == null) return null;
            for (SkillLevel v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=主动求职,1=被动观望,2=暂不考虑 */
    @Getter
    public enum Availability {
        ACTIVE(0, "主动求职"),
        PASSIVE(1, "被动观望"),
        NOT_AVAILABLE(2, "暂不考虑");

        private final int code;
        private final String label;

        Availability(int code, String label) { this.code = code; this.label = label; }

        public static Availability fromCode(Integer code) {
            if (code == null) return null;
            for (Availability v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=月度问候,1=职位推荐,2=活动邀请,3=重新激活,4=自定义 */
    @Getter
    public enum TemplateType {
        MONTHLY_GREETING(0, "月度问候"),
        JOB_RECOMMEND(1, "职位推荐"),
        EVENT_INVITE(2, "活动邀请"),
        REACTIVATION(3, "重新激活"),
        CUSTOM(4, "自定义");

        private final int code;
        private final String label;

        TemplateType(int code, String label) { this.code = code; this.label = label; }

        public static TemplateType fromCode(Integer code) {
            if (code == null) return null;
            for (TemplateType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=邮件,1=短信,2=App推送,3=微信 */
    @Getter
    public enum SendMethod {
        EMAIL(0, "邮件"),
        SMS(1, "短信"),
        PUSH(2, "App推送"),
        WECHAT(3, "微信");

        private final int code;
        private final String label;

        SendMethod(int code, String label) { this.code = code; this.label = label; }

        public static SendMethod fromCode(Integer code) {
            if (code == null) return null;
            for (SendMethod v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=草稿,1=已排期,2=发送中,3=已完成,4=已取消 */
    @Getter
    public enum CampaignStatus {
        DRAFT(0, "草稿"),
        SCHEDULED(1, "已排期"),
        SENDING(2, "发送中"),
        COMPLETED(3, "已完成"),
        CANCELLED(4, "已取消");

        private final int code;
        private final String label;

        CampaignStatus(int code, String label) { this.code = code; this.label = label; }

        public static CampaignStatus fromCode(Integer code) {
            if (code == null) return null;
            for (CampaignStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }
}
