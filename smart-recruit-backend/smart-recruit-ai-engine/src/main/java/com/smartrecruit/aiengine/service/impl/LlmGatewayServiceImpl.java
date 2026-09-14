package com.smartrecruit.aiengine.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecruit.aiengine.config.LlmProperties;
import com.smartrecruit.aiengine.config.LlmProperties.ProviderConfig;
import com.smartrecruit.aiengine.service.AgentTaskRecorder;
import com.smartrecruit.aiengine.service.LlmGatewayService;
import com.smartrecruit.common.util.DateUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Engine 大模型网关服务实现，基于 LangChain4j 实现多厂商路由与自动降级。
 *
 * @since 2026-04-07
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "ai.llm.enabled", havingValue = "true")
public class LlmGatewayServiceImpl implements LlmGatewayService {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final AgentTaskRecorder taskRecorder;
    private final Map<String, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();

    private static final Pattern JSON_BLOCK = Pattern.compile(
            "```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
    private static final Pattern BRACE_JSON = Pattern.compile("\\{[\\s\\S]*\\}");

    public LlmGatewayServiceImpl(LlmProperties llmProperties, ObjectMapper objectMapper,
                                 AgentTaskRecorder taskRecorder) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.taskRecorder = taskRecorder;
    }

    /**
     * 发起一次 LLM 对话：优先调用主提供商，失败自动降级到备用提供商。
     */
    @Override
    public Map<String, Object> chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, null);
    }

    /**
     * 发起一次 LLM 对话，按 Agent 路由模型。
     *
     * <p>调用链路：Agent 绑定模型（若配置且可解析）→ 主提供商 → 备用提供商，
     * 逐级降级直至成功或全部失败。</p>
     */
    @Override
    public Map<String, Object> chat(String systemPrompt, String userPrompt, String agentId) {
        long start = DateUtils.currentEpochMillis();
        log.info("[LLM网关] 收到请求: agentId={}, systemPrompt长度={}, userPrompt长度={}",
                agentId,
                systemPrompt != null ? systemPrompt.length() : 0,
                userPrompt != null ? userPrompt.length() : 0);
        List<ModelTarget> chain = resolveCallChain(agentId);
        if (chain.isEmpty()) {
            throw new IllegalStateException("未配置任何 LLM 提供商");
        }
        RuntimeException lastError = null;
        for (ModelTarget target : chain) {
            try {
                LlmCallResult result = callProviderRaw(
                        target.config, target.modelName, systemPrompt, userPrompt);
                log.info("[LLM网关] 请求完成: agentId={}, model={}, totalElapsed={}ms",
                        agentId, target.modelName, DateUtils.currentEpochMillis() - start);
                Map<String, Object> parsed = parseResponse(result.text(), target.modelName);
                recordTokenIfNeeded(agentId, target.modelName, result.totalTokens());
                return parsed;
            } catch (Exception ex) {
                lastError = new RuntimeException(
                        "LLM 提供商调用失败: model=" + target.modelName, ex);
                log.warn("LLM 提供商调用失败，尝试下一个: agentId={}, model={}, error={}",
                        agentId, target.modelName, ex.getMessage());
            }
        }
        throw new RuntimeException("所有 LLM 提供商均调用失败", lastError);
    }

    /**
     * 发起一次 LLM 对话并返回模型原始输出文本（不做 JSON 解析）。
     *
     * <p>供 AgentScope2 等编排框架使用：模型可能返回工具调用、Markdown 或
     * 自由文本等非 JSON 内容，JSON 化解析会破坏这类场景。</p>
     */
    @Override
    public String chatRaw(String systemPrompt, String userPrompt) {
        return chatRaw(systemPrompt, userPrompt, null);
    }

    /**
     * 发起一次 LLM 对话并返回原始输出文本，按 Agent 路由模型。
     *
     * <p>供 AgentScope2 等编排框架使用：模型可能返回工具调用、Markdown 或
     * 自由文本等非 JSON 内容，JSON 化解析会破坏这类场景。</p>
     */
    @Override
    public String chatRaw(String systemPrompt, String userPrompt, String agentId) {
        long start = DateUtils.currentEpochMillis();
        log.info("[LLM网关] 收到原始文本请求: agentId={}, systemPrompt长度={}, userPrompt长度={}",
                agentId,
                systemPrompt != null ? systemPrompt.length() : 0,
                userPrompt != null ? userPrompt.length() : 0);
        List<ModelTarget> chain = resolveCallChain(agentId);
        if (chain.isEmpty()) {
            throw new IllegalStateException("未配置任何 LLM 提供商");
        }
        RuntimeException lastError = null;
        for (ModelTarget target : chain) {
            try {
                LlmCallResult result = callProviderRaw(
                        target.config, target.modelName, systemPrompt, userPrompt);
                log.info("[LLM网关] 原始文本请求完成: agentId={}, model={}, totalElapsed={}ms",
                        agentId, target.modelName, DateUtils.currentEpochMillis() - start);
                recordTokenIfNeeded(agentId, target.modelName, result.totalTokens());
                return result.text();
            } catch (Exception ex) {
                lastError = new RuntimeException(
                        "LLM 提供商调用失败: model=" + target.modelName, ex);
                log.warn("LLM 提供商调用失败，尝试下一个: agentId={}, model={}, error={}",
                        agentId, target.modelName, ex.getMessage());
            }
        }
        throw new RuntimeException("所有 LLM 提供商均调用失败", lastError);
    }

    /**
     * 发起一次多模态（图片）LLM 对话，按 Agent 路由视觉模型。
     *
     * <p>仅尝试路由到的视觉模型（如图片解析的 qwen-vl-max），
     * 不会降级到纯文本模型，避免把图片发给不支持图像输入的模型。</p>
     */
    @Override
    public Map<String, Object> chatWithImage(String systemPrompt, String textPrompt,
                                             List<String> base64Images, String agentId,
                                             String routingAgentId) {
        long start = DateUtils.currentEpochMillis();
        log.info("[LLM网关] 收到多模态图片请求: agentId={}, routingAgentId={}, imageCount={}",
                agentId, routingAgentId, base64Images != null ? base64Images.size() : 0);
        if (base64Images == null || base64Images.isEmpty()) {
            throw new IllegalArgumentException("多模态调用必须至少包含一张图片");
        }
        List<ModelTarget> chain = resolveImageChain(routingAgentId);
        if (chain.isEmpty()) {
            throw new IllegalStateException("未配置可用的视觉 LLM 提供商");
        }
        RuntimeException lastError = null;
        for (ModelTarget target : chain) {
            try {
                LlmCallResult result = callProviderRawWithImage(
                        target.config, target.modelName, systemPrompt, textPrompt, base64Images);
                log.info("[LLM网关] 多模态请求完成: agentId={}, model={}, totalElapsed={}ms",
                        agentId, target.modelName, DateUtils.currentEpochMillis() - start);
                Map<String, Object> parsed = parseResponse(result.text(), target.modelName);
                // Token 计入统计归属的 Agent（如 resume-parser），模型按路由 Agent 选择
                recordTokenIfNeeded(agentId, target.modelName, result.totalTokens());
                return parsed;
            } catch (Exception ex) {
                lastError = new RuntimeException(
                        "视觉 LLM 提供商调用失败: model=" + target.modelName, ex);
                log.warn("视觉 LLM 提供商调用失败: agentId={}, model={}, error={}",
                        agentId, target.modelName, ex.getMessage());
            }
        }
        throw new RuntimeException("所有视觉 LLM 提供商均调用失败", lastError);
    }

    private LlmCallResult callProviderRaw(ProviderConfig config, String systemPrompt, String userPrompt) {
        return callProviderRaw(config, config.getModel(), systemPrompt, userPrompt);
    }

    private LlmCallResult callProviderRaw(ProviderConfig config, String modelName,
                                          String systemPrompt, String userPrompt) {
        ChatLanguageModel model = getOrCreateModel(config, modelName);
        log.info("[LLM调用] 开始: provider={}, model={}, baseUrl={}, maxTokens={}, temperature={}, userPromptPreview={}",
                llmProperties.getPrimary(), modelName, config.getApiUrl(),
                config.getMaxTokens(), config.getTemperature(),
                truncate(userPrompt, 200));
        long start = DateUtils.currentEpochMillis();

        Response<AiMessage> response = model.generate(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userPrompt));
        String rawResponse = response.content().text();
        int totalTokens = 0;
        TokenUsage usage = response.tokenUsage();
        if (usage != null && usage.totalTokenCount() != null) {
            totalTokens = usage.totalTokenCount();
        } else {
            log.warn("[LLM调用] 模型响应未返回 token 用量，本次调用无法计入 Token 统计: model={}",
                    modelName);
        }

        long elapsed = DateUtils.currentEpochMillis() - start;
        log.info("[LLM调用] 完成: model={}, elapsed={}ms, rawResponseLength={}, rawResponsePreview={}",
                modelName, elapsed, rawResponse != null ? rawResponse.length() : 0,
                truncate(rawResponse, 500));
        return new LlmCallResult(rawResponse, totalTokens);
    }

    /**
     * 多模态调用：文本 + 图片（base64 Data URL）。
     */
    private LlmCallResult callProviderRawWithImage(ProviderConfig config, String modelName,
                                                   String systemPrompt, String textPrompt,
                                                   List<String> base64Images) {
        ChatLanguageModel model = getOrCreateModel(config, modelName);
        log.info("[LLM调用] 开始(多模态): provider={}, model={}, baseUrl={}, imageCount={}",
                llmProperties.getPrimary(), modelName, config.getApiUrl(),
                base64Images != null ? base64Images.size() : 0);
        long start = DateUtils.currentEpochMillis();

        ImageContent[] images = base64Images.stream()
                .map(ImageContent::from)
                .toArray(ImageContent[]::new);
        Response<AiMessage> response = model.generate(
                SystemMessage.from(systemPrompt),
                UserMessage.from(textPrompt, List.of((Content[]) images)));
        String rawResponse = response.content().text();
        int totalTokens = 0;
        TokenUsage usage = response.tokenUsage();
        if (usage != null && usage.totalTokenCount() != null) {
            totalTokens = usage.totalTokenCount();
        } else {
            log.warn("[LLM调用] 模型响应未返回 token 用量，本次调用无法计入 Token 统计: model={}",
                    modelName);
        }

        long elapsed = DateUtils.currentEpochMillis() - start;
        log.info("[LLM调用] 完成(多模态): model={}, elapsed={}ms, rawResponseLength={}",
                modelName, elapsed, rawResponse != null ? rawResponse.length() : 0);
        return new LlmCallResult(rawResponse, totalTokens);
    }

    /**
     * 上报真实 Token 消耗（仅当调用方传入了 Agent ID 且确有 token 用量时）。
     */
    private void recordTokenIfNeeded(String agentId, String modelName, int tokens) {
        if (agentId == null || agentId.isBlank() || tokens <= 0) {
            return;
        }
        try {
            taskRecorder.recordTokenUsage(agentId, modelName, tokens);
        } catch (Exception e) {
            log.warn("Token 消耗上报失败（可忽略）: agentId={}, error={}", agentId, e.getMessage());
        }
    }

    private ChatLanguageModel getOrCreateModel(ProviderConfig config) {
        return getOrCreateModel(config, config.getModel());
    }

    private ChatLanguageModel getOrCreateModel(ProviderConfig config, String modelName) {
        // 同一提供商不同模型使用不同的缓存键，避免路由切换时串模型
        String cacheKey = config.getApiUrl() + "|" + modelName;
        return modelCache.computeIfAbsent(cacheKey, k -> {
            log.info("构建 OpenAiChatModel: model={}, baseUrl={}", modelName, config.getApiUrl());
            return OpenAiChatModel.builder()
                    .baseUrl(config.getApiUrl())
                    .apiKey(config.getApiKey())
                    .modelName(modelName)
                    .maxTokens(config.getMaxTokens())
                    .temperature(config.getTemperature())
                    .timeout(Duration.ofSeconds(240))
                    .maxRetries(1)
                    .build();
        });
    }

    /**
     * 构建模型调用链路：Agent 绑定模型（若可解析）→ 主提供商 → 备用提供商。
     */
    private List<ModelTarget> resolveCallChain(String agentId) {
        List<ModelTarget> chain = new ArrayList<>();

        // 1) 按 Agent 路由的模型
        String routedModel = llmProperties.resolveModelForAgent(agentId);
        if (routedModel != null && !routedModel.isBlank()) {
            ProviderConfig routed = llmProperties.findProviderByModel(routedModel);
            if (routed != null) {
                chain.add(new ModelTarget(routed, routedModel));
                log.info("[LLM网关] 按 Agent 路由模型: agentId={}, model={}", agentId, routedModel);
            } else {
                log.warn("[LLM网关] 路由模型未匹配到提供商，回退全局链路: agentId={}, model={}",
                        agentId, routedModel);
            }
        }

        // 2) 主提供商
        ProviderConfig primary = llmProperties.getPrimaryConfig();
        if (primary != null) {
            addTargetIfAbsent(chain, primary, primary.getModel());
        }

        // 3) 备用提供商
        ProviderConfig fallback = llmProperties.getFallbackConfig();
        if (fallback != null) {
            addTargetIfAbsent(chain, fallback, fallback.getModel());
        }
        return chain;
    }

    /**
     * 构建图片调用链路：仅使用路由到的视觉模型（无路由时使用主提供商），
     * 不降级到纯文本模型。
     */
    private List<ModelTarget> resolveImageChain(String agentId) {
        List<ModelTarget> chain = new ArrayList<>();
        String routedModel = llmProperties.resolveModelForAgent(agentId);
        if (routedModel != null && !routedModel.isBlank()) {
            ProviderConfig routed = llmProperties.findProviderByModel(routedModel);
            if (routed != null) {
                chain.add(new ModelTarget(routed, routedModel));
                log.info("[LLM网关] 按 Agent 路由视觉模型: agentId={}, model={}",
                        agentId, routedModel);
                return chain;
            }
        }
        ProviderConfig primary = llmProperties.getPrimaryConfig();
        if (primary != null) {
            chain.add(new ModelTarget(primary, primary.getModel()));
        }
        return chain;
    }

    /** 避免同一（提供商 + 模型）在链路中重复出现。 */
    private void addTargetIfAbsent(List<ModelTarget> chain, ProviderConfig config, String modelName) {
        boolean exists = chain.stream()
                .anyMatch(t -> t.config == config && t.modelName.equals(modelName));
        if (!exists) {
            chain.add(new ModelTarget(config, modelName));
        }
    }

    /** 模型调用目标：提供商配置 + 实际使用的模型名。 */
    private record ModelTarget(ProviderConfig config, String modelName) {
    }

    /** 模型调用结果：原始文本 + 真实 Token 消耗。 */
    private record LlmCallResult(String text, int totalTokens) {
    }

    private Map<String, Object> parseResponse(String raw, String model) {
        if (raw == null || raw.isBlank()) {
            log.warn("[LLM解析] 响应为空: model={}", model);
            return Map.of();
        }
        String json = extractJson(raw);
        log.info("[LLM解析] 提取JSON: model={}, jsonLength={}, jsonPreview={}",
                model, json.length(), truncate(json, 300));
        try {
            Map<String, Object> result = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});
            log.info("[LLM解析] 成功: model={}, resultKeys={}", model, result.keySet());
            return result;
        } catch (Exception e) {
            // 输出被 maxTokens 截断时，尝试补齐未闭合的引号/括号后二次解析
            String repaired = repairTruncatedJson(json);
            if (!repaired.equals(json)) {
                try {
                    Map<String, Object> result = objectMapper.readValue(repaired,
                            new TypeReference<Map<String, Object>>() {});
                    log.warn("[LLM解析] 响应被截断，已自动修复 JSON 后成功解析: model={}, resultKeys={}",
                            model, result.keySet());
                    return result;
                } catch (Exception repairEx) {
                    log.error("[LLM解析] 失败（含截断修复尝试）: model={}, jsonPreview={}, error={}",
                            model, truncate(json, 300), e.getMessage());
                }
            } else {
                log.error("[LLM解析] 失败: model={}, jsonPreview={}, error={}",
                        model, truncate(json, 300), e.getMessage());
            }
            return Map.of();
        }
    }

    /**
     * 修复被截断的 JSON：补齐未闭合的字符串引号与括号。
     *
     * <p>大模型输出达到 maxTokens 上限时会在任意位置截断，例如
     * {@code {"candidates":[{...},{...}} 缺少结尾的 {@code ]} 和 {@code }}。
     * 该方法通过逐字符扫描补齐缺失的闭合符，使不完整 JSON 可被二次解析。</p>
     *
     * @param raw 原始（可能被截断的）JSON 文本
     * @return 修复后的 JSON；若输入无需修复或无法定位 JSON 结构则原样返回
     */
    private String repairTruncatedJson(String raw) {
        String s = raw == null ? "" : raw.trim();
        // 定位第一个 JSON 结构起点（{ 或 [），跳过前置噪声文本
        int start = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return s;
        }

        StringBuilder out = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                out.append(c);
                if (c == '\\') {
                    if (i + 1 < s.length()) {
                        out.append(s.charAt(++i));
                    }
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            switch (c) {
                case '"' -> {
                    inString = true;
                    out.append(c);
                }
                case '{', '[' -> {
                    stack.push(c);
                    out.append(c);
                }
                case '}', ']' -> {
                    if (!stack.isEmpty()) {
                        stack.pop();
                    }
                    out.append(c);
                }
                default -> out.append(c);
            }
        }
        // 截断点可能落在逗号之后：先移除尾部空白与逗号
        int end = out.length();
        while (end > 0) {
            char tail = out.charAt(end - 1);
            if (tail == ',' || tail == ' ' || tail == '\n' || tail == '\r' || tail == '\t') {
                end--;
            } else {
                break;
            }
        }
        out.setLength(end);
        // 字符串未闭合时补上引号
        if (inString) {
            out.append('"');
        }
        // 括号未闭合时按后进先出补全
        while (!stack.isEmpty()) {
            out.append(stack.pop() == '{' ? '}' : ']');
        }
        return out.toString();
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        Matcher m = JSON_BLOCK.matcher(trimmed);
        if (m.find()) {
            log.debug("[LLM解析] 从 markdown 代码块中提取 JSON");
            return m.group(1).trim();
        }
        m = BRACE_JSON.matcher(trimmed);
        if (m.find()) {
            log.debug("[LLM解析] 从裸文本中提取 JSON");
            return m.group();
        }
        log.debug("[LLM解析] 未匹配到 JSON 结构，返回原始文本");
        return trimmed;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "null";
        String cleaned = text.replace('\n', ' ').replace('\r', ' ');
        if (cleaned.length() <= maxLen) return cleaned;
        return cleaned.substring(0, maxLen) + "...";
    }
}
