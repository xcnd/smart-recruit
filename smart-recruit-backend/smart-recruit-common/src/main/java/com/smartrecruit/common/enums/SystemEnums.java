package com.smartrecruit.common.enums;

import lombok.Getter;

/**
 * {@code smart_recruit_system} 数据库的枚举定义。
 *
 * @author xdh
 * @since 2026-05-04
 */
public final class SystemEnums {

    private SystemEnums() {}

    /** 0=RECRUIT（招聘）,1=JOB（职位）,2=CANDIDATE（候选人）,3=INTERVIEW（面试）,4=TALENT（人才库）,5=ANALYTICS（数据分析）,6=OFFER（Offer）,7=ONBOARD（入职）,8=REFERRAL（内推）,9=SYSTEM（系统管理）,10=AI（AI引擎） */
    @Getter
    public enum PermissionModule {
        RECRUIT(0, "招聘"),
        JOB(1, "职位"),
        CANDIDATE(2, "候选人"),
        INTERVIEW(3, "面试"),
        TALENT(4, "人才库"),
        ANALYTICS(5, "数据分析"),
        OFFER(6, "Offer"),
        ONBOARD(7, "入职"),
        REFERRAL(8, "内推"),
        SYSTEM(9, "系统管理"),
        AI(10, "AI引擎");

        private final int code;
        private final String label;

        PermissionModule(int code, String label) { this.code = code; this.label = label; }

        public static PermissionModule fromCode(Integer code) {
            if (code == null) return null;
            for (PermissionModule v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=GET,1=POST,2=PUT,3=DELETE */
    @Getter
    public enum ApiMethod {
        GET(0, "GET"),
        POST(1, "POST"),
        PUT(2, "PUT"),
        DELETE(3, "DELETE");

        private final int code;
        private final String label;

        ApiMethod(int code, String label) { this.code = code; this.label = label; }

        public static ApiMethod fromCode(Integer code) {
            if (code == null) return null;
            for (ApiMethod v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=SYSTEM（系统通知）,1=INTERVIEW（面试通知）,2=OFFER（Offer通知）,3=ONBOARDING（入职通知）,4=REFERRAL（内推通知）,5=TALENT（人才库通知） */
    @Getter
    public enum NotificationType {
        SYSTEM(0, "系统通知"),
        INTERVIEW(1, "面试通知"),
        OFFER(2, "Offer通知"),
        ONBOARDING(3, "入职通知"),
        REFERRAL(4, "内推通知"),
        TALENT(5, "人才库通知");

        private final int code;
        private final String label;

        NotificationType(int code, String label) { this.code = code; this.label = label; }

        public static NotificationType fromCode(Integer code) {
            if (code == null) return null;
            for (NotificationType v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=SYSTEM（系统管理）,1=JOB（职位管理）,2=CANDIDATE（候选人）,3=INTERVIEW（面试）,4=OFFER（Offer）,5=ONBOARD（入职）,6=TALENT（人才库）,7=REFERRAL（内推）,8=AI（AI引擎） */
    @Getter
    public enum OperationLogModule {
        SYSTEM(0, "系统管理"),
        JOB(1, "职位管理"),
        CANDIDATE(2, "候选人"),
        INTERVIEW(3, "面试"),
        OFFER(4, "Offer"),
        ONBOARD(5, "入职"),
        TALENT(6, "人才库"),
        REFERRAL(7, "内推"),
        AI(8, "AI引擎");

        private final int code;
        private final String label;

        OperationLogModule(int code, String label) { this.code = code; this.label = label; }

        public static OperationLogModule fromCode(Integer code) {
            if (code == null) return null;
            for (OperationLogModule v : values()) { if (v.code == code) return v; }
            return null;
        }
    }

    /** 0=CREATE（新增）,1=UPDATE（修改）,2=DELETE（删除）,3=EXPORT（导出）,4=IMPORT（导入） */
    @Getter
    public enum OperationLogAction {
        CREATE(0, "新增"),
        UPDATE(1, "修改"),
        DELETE(2, "删除"),
        EXPORT(3, "导出"),
        IMPORT(4, "导入");

        private final int code;
        private final String label;

        OperationLogAction(int code, String label) { this.code = code; this.label = label; }

        public static OperationLogAction fromCode(Integer code) {
            if (code == null) return null;
            for (OperationLogAction v : values()) { if (v.code == code) return v; }
            return null;
        }
    }
}
