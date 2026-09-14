package com.smartrecruit.aiengine.controller;

import com.smartrecruit.aiengine.dto.request.LlmChatRequest;
import com.smartrecruit.aiengine.service.LlmFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * LLM 网关内部 REST 接口，供其他微服务通过 {@code @HttpExchange} 调用。
 *
 * <p>业务逻辑（可用性校验、任务生命周期记录、网关调用与耗时指标）
 * 统一委托 {@link LlmFacadeService}，本类仅做参数透传。
 * 内部端点直接返回裸数据，不经过 {@code ApiResponse} 包装。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/llm")
@RequiredArgsConstructor
@Slf4j
public class LlmController {

    private final LlmFacadeService llmFacadeService;

    /**
     * 通用 LLM 对话入口，供内部服务通过 {@code @HttpExchange} 调用。
     *
     * @param request 系统提示词与用户提示词
     * @return 大模型原始返回结果
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody LlmChatRequest request) {
        return llmFacadeService.chat(request);
    }
}
