package com.smartrecruit.referral.enums;

import lombok.Getter;

/**
 * {@code smart_recruit_referral} 数据库的枚举定义。
 *
 * @author xdh
 * @since 2026-05-04
 */
public final class ReferralEnums {

    private ReferralEnums() {}

    /** 0=URGENT（急聘）,1=HIGH_BONUS（高奖金）,2=TECH（技术）,3=INTERN（实习） */
    @Getter
    public enum ProgramJobTag {
        URGENT(0, "急聘"),
        HIGH_BONUS(1, "高奖金"),
        TECH(2, "技术"),
        INTERN(3, "实习");

        private final int code;
        private final String label;

        ProgramJobTag(int code, String label) { this.code = code; this.label = label; }

        public static ProgramJobTag fromCode(Integer code) {
            if (code == null) return null;
            for (ProgramJobTag v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=PENDING（待处理）,1=CONTACTED（已联系）,2=INTERVIEWING（面试中）,3=HIRED（已入职）,4=REJECTED（已淘汰）,5=CANCELLED（已取消） */
    @Getter
    public enum ReferralStatus {
        PENDING(0, "待处理"),
        CONTACTED(1, "已联系"),
        INTERVIEWING(2, "面试中"),
        HIRED(3, "已入职"),
        REJECTED(4, "已淘汰"),
        CANCELLED(5, "已取消");

        private final int code;
        private final String label;

        ReferralStatus(int code, String label) { this.code = code; this.label = label; }

        public static ReferralStatus fromCode(Integer code) {
            if (code == null) return null;
            for (ReferralStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=PENDING（待发放）,1=PARTIAL_PAID（部分发放）,2=FULL_PAID（已全额发放） */
    @Getter
    public enum BonusStatus {
        PENDING(0, "待发放"),
        PARTIAL_PAID(1, "部分发放"),
        FULL_PAID(2, "已全额发放");

        private final int code;
        private final String label;

        BonusStatus(int code, String label) { this.code = code; this.label = label; }

        public static BonusStatus fromCode(Integer code) {
            if (code == null) return null;
            for (BonusStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=ONBOARD（入职）,1=INITIAL（入职首月）,2=ONBOARD_PROBATION（试用期）,3=PROBATION_PASS（转正）,4=FULL（全额） */
    @Getter
    public enum BonusStage {
        ONBOARD(0, "入职"),
        INITIAL(1, "入职首月"),
        ONBOARD_PROBATION(2, "试用期"),
        PROBATION_PASS(3, "转正"),
        FULL(4, "全额");

        private final int code;
        private final String label;

        BonusStage(int code, String label) { this.code = code; this.label = label; }

        public static BonusStage fromCode(Integer code) {
            if (code == null) return null;
            for (BonusStage v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=PENDING（待发放）,1=PAID（已发放）,2=CANCELLED（已取消） */
    @Getter
    public enum BonusRecordStatus {
        PENDING(0, "待发放"),
        PAID(1, "已发放"),
        CANCELLED(2, "已取消");

        private final int code;
        private final String label;

        BonusRecordStatus(int code, String label) { this.code = code; this.label = label; }

        public static BonusRecordStatus fromCode(Integer code) {
            if (code == null) return null;
            for (BonusRecordStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=MONTHLY（月度）,1=QUARTERLY（季度）,2=YEARLY（年度） */
    @Getter
    public enum PeriodType {
        MONTHLY(0, "月度"),
        QUARTERLY(1, "季度"),
        YEARLY(2, "年度");

        private final int code;
        private final String label;

        PeriodType(int code, String label) { this.code = code; this.label = label; }

        public static PeriodType fromCode(Integer code) {
            if (code == null) return null;
            for (PeriodType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }
}
