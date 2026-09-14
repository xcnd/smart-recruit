package com.smartrecruit.recruitment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 招聘模块线程池配置属性，前缀 {@code smart-recruit.thread-pool}。
 *
 * <p>所有参数均可在 {@code application.yml} 中覆盖，未配置时使用代码内默认值，
 * 避免线程池参数散落在业务代码中。</p>
 *
 * @since 2026-04-07
 */
@Data
@ConfigurationProperties(prefix = "smart-recruit.thread-pool")
public class RecruitmentThreadPoolProperties {

    /** AI 引擎执行器配置。 */
    private AiEngine aiEngine = new AiEngine();

    /** 简历解析执行器配置。 */
    private ResumeParse resumeParse = new ResumeParse();

    /**
     * 线程池拒绝策略。
     */
    public enum RejectionPolicy {
        /** 直接抛出 RejectedExecutionException。 */
        ABORT,
        /** 由提交任务的线程直接执行被拒绝的任务。 */
        CALLER_RUNS,
        /** 静默丢弃被拒绝的任务。 */
        DISCARD,
        /** 丢弃队列中最旧的任务后重新提交。 */
        DISCARD_OLDEST
    }

    /**
     * 线程池公共参数。
     */
    public interface PoolParams {

        int getCorePoolSize();

        int getMaxPoolSize();

        int getQueueCapacity();

        int getKeepAliveSeconds();

        int getAwaitTerminationSeconds();

        String getThreadNamePrefix();

        RejectionPolicy getRejectionPolicy();
    }

    /**
     * AI 引擎执行器参数（限制并发，避免系统颠簸 / LLM API 过载）。
     */
    @Data
    public static class AiEngine implements PoolParams {

        /** 核心线程数。 */
        private int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

        /** 最大线程数。 */
        private int maxPoolSize = Runtime.getRuntime().availableProcessors();

        /** 队列容量。 */
        private int queueCapacity = 100;

        /** 非核心线程空闲存活时间（秒）。 */
        private int keepAliveSeconds = 120;

        /** 关闭时等待任务完成的时间（秒）。 */
        private int awaitTerminationSeconds = 120;

        /** 线程名前缀。 */
        private String threadNamePrefix = "ai-";

        /** 拒绝策略。 */
        private RejectionPolicy rejectionPolicy = RejectionPolicy.ABORT;
    }

    /**
     * 简历解析执行器参数（IO 密集型，限制并发避免 LLM/解析服务过载）。
     */
    @Data
    public static class ResumeParse implements PoolParams {

        /** 核心线程数。 */
        private int corePoolSize = 2;

        /** 最大线程数。 */
        private int maxPoolSize = 4;

        /** 队列容量。 */
        private int queueCapacity = 50;

        /** 非核心线程空闲存活时间（秒）。 */
        private int keepAliveSeconds = 120;

        /** 关闭时等待任务完成的时间（秒）。 */
        private int awaitTerminationSeconds = 60;

        /** 线程名前缀。 */
        private String threadNamePrefix = "resume-parse-";

        /** 拒绝策略。 */
        private RejectionPolicy rejectionPolicy = RejectionPolicy.CALLER_RUNS;
    }
}
