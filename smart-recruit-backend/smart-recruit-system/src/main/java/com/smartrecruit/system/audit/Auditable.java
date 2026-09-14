package com.smartrecruit.system.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解。
 *
 * <p>标注在需要自动记录审计日志的方法上（CUD 操作）。
 * 配合 {@link AuditLogAspect} AOP 切面实现自动采集。</p>
 *
 * @since 2026-04-26
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * 操作类型，例如: CREATE, UPDATE, DELETE, LOGIN, EXPORT。
     */
    String action();

    /**
     * 资源类型，例如: USER, ROLE, DEPT, PERMISSION。
     */
    String resourceType();

    /**
     * 所属模块编码：0=SYSTEM,1=JOB,2=CANDIDATE,3=INTERVIEW,4=OFFER,5=ONBOARD,6=TALENT,7=REFERRAL,8=AI。
     */
    int module() default 0;
}
