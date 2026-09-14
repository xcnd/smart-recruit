package com.smartrecruit.offer.enums;

import lombok.Getter;

/**
 * {@code smart_recruit_offer} 数据库的枚举类。
 *
 * @author xdh
 * @since 2026-05-04
 */
public final class OfferEnums {

    private OfferEnums() {}

    /** 0=草稿,1=待审批,2=已审批,3=已发送,4=已接受,5=已拒绝,6=洽谈中,7=已过期 */
    @Getter
    public enum OfferStatus {
        DRAFT(0, "草稿"),
        PENDING(1, "待审批"),
        APPROVED(2, "已审批"),
        SENT(3, "已发送"),
        ACCEPTED(4, "已接受"),
        REJECTED(5, "已拒绝"),
        NEGOTIATING(6, "洽谈中"),
        EXPIRED(7, "已过期");

        private final int code;
        private final String label;

        OfferStatus(int code, String label) { this.code = code; this.label = label; }

        public static OfferStatus fromCode(Integer code) {
            if (code == null) return null;
            for (OfferStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=待审批,1=已通过,2=已驳回 */
    @Getter
    public enum ApprovalStatus {
        PENDING(0, "待审批"),
        APPROVED(1, "已通过"),
        REJECTED(2, "已驳回");

        private final int code;
        private final String label;

        ApprovalStatus(int code, String label) { this.code = code; this.label = label; }

        public static ApprovalStatus fromCode(Integer code) {
            if (code == null) return null;
            for (ApprovalStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=待入职,1=入职中,2=已完成,3=有风险 */
    @Getter
    public enum OnboardingStatus {
        PENDING(0, "待入职"),
        ACTIVE(1, "入职中"),
        DONE(2, "已完成"),
        AT_RISK(3, "有风险");

        private final int code;
        private final String label;

        OnboardingStatus(int code, String label) { this.code = code; this.label = label; }

        public static OnboardingStatus fromCode(Integer code) {
            if (code == null) return null;
            for (OnboardingStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=低风险,1=中风险,2=高风险 */
    @Getter
    public enum RiskLevel {
        LOW(0, "低风险"),
        MEDIUM(1, "中风险"),
        HIGH(2, "高风险");

        private final int code;
        private final String label;

        RiskLevel(int code, String label) { this.code = code; this.label = label; }

        public static RiskLevel fromCode(Integer code) {
            if (code == null) return null;
            for (RiskLevel v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=身份证,1=学历证明,2=离职证明,3=体检报告,4=银行卡,5=证件照 */
    @Getter
    public enum DocumentType {
        ID_CARD(0, "身份证"),
        DIPLOMA(1, "学历证明"),
        RESIGNATION(2, "离职证明"),
        PHYSICAL(3, "体检报告"),
        BANK_CARD(4, "银行卡"),
        PHOTO(5, "证件照");

        private final int code;
        private final String label;

        DocumentType(int code, String label) { this.code = code; this.label = label; }

        public static DocumentType fromCode(Integer code) {
            if (code == null) return null;
            for (DocumentType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=缺失,1=待审核,2=已审核 */
    @Getter
    public enum DocumentStatus {
        MISSING(0, "缺失"),
        PENDING(1, "已上传"),
        VERIFIED(2, "已审核");

        private final int code;
        private final String label;

        DocumentStatus(int code, String label) { this.code = code; this.label = label; }

        public static DocumentStatus fromCode(Integer code) {
            if (code == null) return null;
            for (DocumentStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=笔记本电脑,1=显示器,2=手机,3=工牌,4=工位 */
    @Getter
    public enum EquipmentType {
        LAPTOP(0, "笔记本电脑"),
        MONITOR(1, "显示器"),
        PHONE(2, "手机"),
        BADGE(3, "工牌"),
        DESK(4, "工位");

        private final int code;
        private final String label;

        EquipmentType(int code, String label) { this.code = code; this.label = label; }

        public static EquipmentType fromCode(Integer code) {
            if (code == null) return null;
            for (EquipmentType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=待分配,1=已分配,2=已发出,3=已签收 */
    @Getter
    public enum EquipmentStatus {
        PENDING(0, "待分配"),
        ASSIGNED(1, "已分配"),
        SHIPPED(2, "已发出"),
        DELIVERED(3, "已签收");

        private final int code;
        private final String label;

        EquipmentStatus(int code, String label) { this.code = code; this.label = label; }

        public static EquipmentStatus fromCode(Integer code) {
            if (code == null) return null;
            for (EquipmentStatus v : values()) { if (v.code == code) return v; }
            return null;
        }
    }
}
