package com.smartrecruit.recruitment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 招聘模块线程池配置。
 *
 * <p>线程池属于业务服务自身的运行时资源，应该由使用它的服务按自身负载定义，
 * 而不是放在公共模块中让所有服务共同加载。招聘模块使用两个线程池：
 * 简历解析（IO 密集型）与 AI 筛选（限制并发避免 LLM API 过载）。
 * 所有参数通过 {@link RecruitmentThreadPoolProperties} 从
 * {@code smart-recruit.thread-pool.*} 配置读取，未配置时使用默认值。</p>
 *
 * @since 2026-04-07
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(RecruitmentThreadPoolProperties.class)
public class RecruitmentThreadPoolConfig {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentThreadPoolConfig.class);

    private final RecruitmentThreadPoolProperties properties;

    public RecruitmentThreadPoolConfig(RecruitmentThreadPoolProperties properties) {
        this.properties = properties;
    }

    /**
     * AI 引擎执行器（限制并发，避免系统颠簸 / LLM API 过载）。
     */
    @Bean("aiEngineExecutor")
    public Executor aiEngineExecutor() {
        return buildExecutor(properties.getAiEngine(), "AI 引擎执行器");
    }

    /**
     * 简历解析执行器（IO 密集型，限制并发避免 LLM/解析服务过载）。
     */
    @Bean("resumeParseExecutor")
    public Executor resumeParseExecutor() {
        return buildExecutor(properties.getResumeParse(), "简历解析执行器");
    }

    /**
     * 按配置属性构建线程池执行器。
     *
     * @param params 线程池参数
     * @param name   执行器名称（仅用于日志）
     */
    private Executor buildExecutor(RecruitmentThreadPoolProperties.PoolParams params, String name) {
        int corePoolSize = Math.max(1, params.getCorePoolSize());
        int maxPoolSize = Math.max(corePoolSize, params.getMaxPoolSize());
        int queueCapacity = Math.max(1, params.getQueueCapacity());

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(params.getKeepAliveSeconds());
        executor.setThreadNamePrefix(params.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(params.getAwaitTerminationSeconds());
        executor.setRejectedExecutionHandler(resolveHandler(params.getRejectionPolicy()));
        executor.initialize();

        log.info("{}已初始化: core={}, max={}, queue={}, keepAlive={}s, await={}s, policy={}, prefix={}",
                name, corePoolSize, maxPoolSize, queueCapacity,
                params.getKeepAliveSeconds(), params.getAwaitTerminationSeconds(),
                params.getRejectionPolicy(), params.getThreadNamePrefix());
        return executor;
    }

    /**
     * 将配置的拒绝策略枚举解析为对应拒绝处理器。
     */
    private RejectedExecutionHandler resolveHandler(RecruitmentThreadPoolProperties.RejectionPolicy policy) {
        return switch (policy == null
                ? RecruitmentThreadPoolProperties.RejectionPolicy.ABORT : policy) {
            case CALLER_RUNS -> new ThreadPoolExecutor.CallerRunsPolicy();
            case DISCARD -> new ThreadPoolExecutor.DiscardPolicy();
            case DISCARD_OLDEST -> new ThreadPoolExecutor.DiscardOldestPolicy();
            default -> new ThreadPoolExecutor.AbortPolicy();
        };
    }
}
