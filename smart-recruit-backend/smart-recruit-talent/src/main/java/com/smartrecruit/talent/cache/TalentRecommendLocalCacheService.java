package com.smartrecruit.talent.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.smartrecruit.talent.dto.response.TalentPoolVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * AI 人才推荐结果本地缓存服务（L1 缓存，Caffeine）。
 *
 * <p>作为进程内一级缓存，5 分钟过期、最多保留 500 个岗位，
 * 优先于 Redis（L2）命中，进一步降低跨服务调用与 LLM 开销。</p>
 *
 * @since 2026-04-10
 */
@Component
@Slf4j
public class TalentRecommendLocalCacheService {

    /** 本地缓存过期时间：5 分钟。 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /** 最大缓存岗位数，防止内存无限增长。 */
    private static final long MAX_CAPACITY = 500;

    private final Cache<Long, List<TalentPoolVO>> localCache = Caffeine.newBuilder()
            .maximumSize(MAX_CAPACITY)
            .expireAfterWrite(CACHE_TTL)
            .build();

    /**
     * 读取指定岗位的本地缓存推荐结果。
     *
     * @param jobId 招聘岗位 ID
     * @return 推荐结果列表；未命中时返回 {@code null}
     */
    public List<TalentPoolVO> get(Long jobId) {
        return localCache.getIfPresent(jobId);
    }

    /**
     * 写入指定岗位的本地缓存推荐结果（5 分钟过期）。
     *
     * @param jobId   招聘岗位 ID
     * @param results 推荐结果列表
     */
    public void put(Long jobId, List<TalentPoolVO> results) {
        localCache.put(jobId, results);
    }

    /**
     * 主动失效指定岗位的本地缓存（数据变更时调用）。
     *
     * @param jobId 招聘岗位 ID
     */
    public void evict(Long jobId) {
        localCache.invalidate(jobId);
    }
}
