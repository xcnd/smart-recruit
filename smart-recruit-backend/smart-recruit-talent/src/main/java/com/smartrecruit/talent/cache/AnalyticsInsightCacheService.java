package com.smartrecruit.talent.cache;

import com.smartrecruit.talent.dto.response.InsightVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 数据分析洞察缓存服务。
 *
 * <p>AI 洞察由 LLM 异步生成后缓存到 Redis（30 分钟），
 * 页面后续请求直接命中缓存，不再阻塞等待 LLM；
 * 内存中的生成中标记用于避免同一周期重复触发生成。</p>
 *
 * @since 2026-04-10
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsInsightCacheService {

    /** 缓存键前缀（带版本号，便于升级时整体失效）。 */
    private static final String KEY_PREFIX = "analytics:insights:v1:";

    /** 缓存过期时间：30 分钟。 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 正在异步生成的周期集合（防止并发重复触发 LLM）。 */
    private final Set<String> generatingKeys = ConcurrentHashMap.newKeySet();

    /**
     * 读取指定周期的缓存 AI 洞察。
     *
     * @return 洞察列表；未命中或 Redis 异常时返回 {@code null}
     */
    public List<InsightVO> get(String periodKey) {
        try {
            String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + periodKey);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<List<InsightVO>>() {
            });
        } catch (Exception e) {
            log.warn("读取 AI 洞察缓存失败: periodKey={}, error={}", periodKey, e.getMessage());
            return null;
        }
    }

    /**
     * 写入指定周期的 AI 洞察缓存（30 分钟过期）。
     */
    public void put(String periodKey, List<InsightVO> insights) {
        try {
            String json = objectMapper.writeValueAsString(insights);
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + periodKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("写入 AI 洞察缓存失败（不影响洞察返回）: periodKey={}, error={}",
                    periodKey, e.getMessage());
        }
    }

    /**
     * 尝试抢占该周期的异步生成任务（仅第一个调用者返回 true）。
     */
    public boolean tryStartGenerate(String periodKey) {
        return generatingKeys.add(periodKey);
    }

    /**
     * 结束该周期的异步生成（无论成功失败都要调用，释放生成标记）。
     */
    public void finishGenerate(String periodKey) {
        generatingKeys.remove(periodKey);
    }
}
