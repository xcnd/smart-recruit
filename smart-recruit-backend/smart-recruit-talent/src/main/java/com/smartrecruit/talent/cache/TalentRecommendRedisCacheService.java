package com.smartrecruit.talent.cache;

import com.smartrecruit.talent.dto.response.TalentPoolVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

/**
 * AI 人才推荐结果 Redis 缓存服务（L2 缓存）。
 *
 * <p>同一岗位的推荐结果缓存 15 分钟：后续相同岗位推荐直接命中 Redis，
 * 避免重复调用大模型；Redis 异常时自动降级为实时计算，不影响主流程。</p>
 *
 * @since 2026-04-10
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TalentRecommendRedisCacheService {

    /** 缓存键前缀（带版本号，便于后续升级时整体失效）。 */
    private static final String KEY_PREFIX = "talent:recommend:v1:";

    /** 缓存过期时间：15 分钟。 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 读取指定岗位的缓存推荐结果。
     *
     * @param jobId 招聘岗位 ID
     * @return 推荐结果列表；未命中或 Redis 异常时返回 {@code null}
     */
    public List<TalentPoolVO> get(Long jobId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(buildKey(jobId));
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<List<TalentPoolVO>>() {
            });
        } catch (Exception e) {
            log.warn("读取人才推荐缓存失败，降级实时计算: jobId={}, error={}", jobId, e.getMessage());
            return null;
        }
    }

    /**
     * 写入指定岗位的推荐结果缓存（15 分钟过期）。
     *
     * @param jobId   招聘岗位 ID
     * @param results 推荐结果列表
     */
    public void put(Long jobId, List<TalentPoolVO> results) {
        try {
            String json = objectMapper.writeValueAsString(results);
            stringRedisTemplate.opsForValue().set(buildKey(jobId), json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("写入人才推荐缓存失败（不影响推荐结果）: jobId={}, error={}", jobId, e.getMessage());
        }
    }

    /** 构造缓存键：talent:recommend:v1:{jobId}。 */
    private String buildKey(Long jobId) {
        return KEY_PREFIX + jobId;
    }
}
