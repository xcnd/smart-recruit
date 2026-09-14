package com.smartrecruit.talent.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.talent.dto.remote.CandidateDTO;
import com.smartrecruit.talent.entity.TalentPool;
import com.smartrecruit.talent.feign.RecruitmentClient;
import com.smartrecruit.talent.repository.TalentPoolMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Elasticsearch 人才语义检索组件。
 *
 * <p>使用低层 REST Client 对接远程 ES，人才库变更后懒加载同步索引，
 * 搜索走 BM25 多字段加权（技能/岗位/标签等），ES 不可用时上层自动降级
 * 本地语义排序。</p>
 *
 * @since 2026-04-09
 */
@Component
@Slf4j
public class ElasticsearchTalentSearch {

    /** 失败冷却时间（毫秒）：ES 异常后 60 秒内不再重试，避免拖慢搜索。 */
    private static final long FAIL_COOLDOWN_MS = 60_000;

    private final ObjectMapper objectMapper;
    private final TalentPoolMapper talentPoolMapper;
    private final RecruitmentClient recruitmentClient;

    @Value("${talent.search.elasticsearch.enabled:false}")
    private boolean enabled;

    @Value("${talent.search.elasticsearch.uris:}")
    private String uris;

    @Value("${talent.search.elasticsearch.username:}")
    private String username;

    @Value("${talent.search.elasticsearch.password:}")
    private String password;

    @Value("${talent.search.elasticsearch.index:talent_pool}")
    private String index;

    private volatile RestClient client;
    private volatile boolean synced;
    private volatile long lastFailAt = 0L;

    public ElasticsearchTalentSearch(ObjectMapper objectMapper,
                                     TalentPoolMapper talentPoolMapper,
                                     RecruitmentClient recruitmentClient) {
        this.objectMapper = objectMapper;
        this.talentPoolMapper = talentPoolMapper;
        this.recruitmentClient = recruitmentClient;
    }

    /** ES 是否启用。 */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 语义搜索人才（按相关度返回人才记录；ES 异常返回空列表由上层降级）。
     */
    public List<TalentPool> search(String keyword, int limit) {
        if (!enabled || keyword == null || keyword.isBlank()) {
            return List.of();
        }
        RestClient restClient = client();
        if (restClient == null || cooledDown()) {
            return List.of();
        }
        try {
            ensureIndex(restClient);
            syncIfNeeded(restClient);

            Map<String, Object> multiMatch = new LinkedHashMap<>();
            multiMatch.put("query", keyword.trim());
            multiMatch.put("fields", List.of(
                    "candidateName^3", "skills^4", "positionTitle^2", "expectedPosition^2",
                    "tags^2", "aiTags^2", "currentCompany", "education", "experience", "note"));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("query", Map.of("multi_match", multiMatch));
            body.put("size", Math.min(limit > 0 ? limit : 20, 50));

            Request request = new Request("POST", "/" + index + "/_search");
            request.setJsonEntity(objectMapper.writeValueAsString(body));
            Response response = restClient.performRequest(request);

            JsonNode root = objectMapper.readTree(response.getEntity().getContent());
            JsonNode hits = root.path("hits").path("hits");
            List<Long> ids = new ArrayList<>();
            for (JsonNode hit : hits) {
                try {
                    ids.add(Long.parseLong(hit.path("_id").asText()));
                } catch (NumberFormatException ignored) {
                    // 忽略异常 ID
                }
            }
            if (ids.isEmpty()) {
                return List.of();
            }
            return ids.stream()
                    .map(talentPoolMapper::selectById)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            lastFailAt = System.currentTimeMillis();
            log.warn("ES 人才搜索失败（本次降级本地语义搜索）: keyword={}, error={}",
                    keyword, e.getMessage());
            return List.of();
        }
    }

    /** 手动触发一次索引同步（供维护/接口调用）。 */
    public void syncIndex() {
        if (!enabled) return;
        RestClient restClient = client();
        if (restClient == null) return;
        try {
            ensureIndex(restClient);
            syncIfNeeded(restClient);
        } catch (Exception e) {
            log.warn("ES 索引同步失败: {}", e.getMessage());
        }
    }

    // ================================================================
    // 私有方法
    // ================================================================

    private RestClient client() {
        if (client != null) {
            return client;
        }
        synchronized (this) {
            if (client != null) {
                return client;
            }
            try {
                if (uris == null || uris.isBlank()) {
                    log.warn("ES 未配置地址（talent.search.elasticsearch.uris），人才搜索使用本地降级");
                    return null;
                }
                URI uri = new URI(uris);
                HttpHost host = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
                RestClientBuilder builder = RestClient.builder(host);
                if (username != null && !username.isBlank()) {
                    String token = Base64.getEncoder().encodeToString(
                            (username + ":" + (password == null ? "" : password))
                                    .getBytes(StandardCharsets.UTF_8));
                    builder.setDefaultHeaders(new org.apache.http.Header[]{
                            new BasicHeader("Authorization", "Basic " + token)});
                }
                builder.setRequestConfigCallback(cb -> cb.setConnectTimeout(3000).setSocketTimeout(8000));
                client = builder.build();
                return client;
            } catch (Exception e) {
                log.warn("ES 客户端初始化失败，人才搜索使用本地降级: {}", e.getMessage());
                return null;
            }
        }
    }

    private void ensureIndex(RestClient restClient) throws Exception {
        Request head = new Request("HEAD", "/" + index);
        Response resp = restClient.performRequest(head);
        if (resp.getStatusLine().getStatusCode() == 404) {
            Request put = new Request("PUT", "/" + index);
            put.setJsonEntity(indexMappingJson());
            restClient.performRequest(put);
            synced = false;
            log.info("ES 人才索引已创建: {}", index);
        }
    }

    private String indexMappingJson() throws Exception {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("candidateId", Map.of("type", "long"));
        for (String field : List.of("candidateName", "skills", "positionTitle",
                "currentCompany", "education", "experience",
                "expectedPosition", "expectedLocation", "tags", "aiTags", "note")) {
            properties.put(field, Map.of("type", "text"));
        }
        return objectMapper.writeValueAsString(
                Map.of("mappings", Map.of("properties", properties)));
    }

    private void syncIfNeeded(RestClient restClient) throws Exception {
        if (synced) {
            return;
        }
        List<TalentPool> all = talentPoolMapper.selectList(
                new LambdaQueryWrapper<TalentPool>().eq(TalentPool::getDeleted, 0));
        for (TalentPool talent : all) {
            indexDoc(restClient, talent);
        }
        synced = true;
        log.info("ES 人才索引同步完成: count={}", all.size());
    }

    private void indexDoc(RestClient restClient, TalentPool talent) throws Exception {
        CandidateDTO candidate = fetchCandidate(talent.getCandidateId());
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("candidateId", talent.getCandidateId());
        doc.put("candidateName", candidate != null ? candidate.getName() : "");
        doc.put("skills", candidate != null && candidate.getSkills() != null
                ? candidate.getSkills() : List.of());
        doc.put("currentCompany", candidate != null ? candidate.getCurrentCompany() : "");
        doc.put("education", candidate != null && candidate.getEducation() != null
                ? String.valueOf(candidate.getEducation()) : "");
        doc.put("experience", candidate != null && candidate.getYearsOfExperience() != null
                ? String.valueOf(candidate.getYearsOfExperience()) : "");
        doc.put("positionTitle", candidate != null ? candidate.getJobTitle() : "");
        doc.put("expectedPosition", nullTo(talent.getExpectedPosition()));
        doc.put("expectedLocation", nullTo(talent.getExpectedLocation()));
        doc.put("tags", nullTo(talent.getTags()));
        doc.put("aiTags", nullTo(talent.getAiTags()));
        doc.put("note", nullTo(talent.getNote()));

        Request request = new Request("PUT", "/" + index + "/_doc/" + talent.getId());
        request.setJsonEntity(objectMapper.writeValueAsString(doc));
        restClient.performRequest(request);
    }

    private CandidateDTO fetchCandidate(Long candidateId) {
        if (candidateId == null) {
            return null;
        }
        try {
            var resp = recruitmentClient.getCandidate(candidateId);
            return resp != null ? resp.data() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean cooledDown() {
        return System.currentTimeMillis() - lastFailAt < FAIL_COOLDOWN_MS;
    }

    private String nullTo(String value) {
        return value == null ? "" : value;
    }
}
