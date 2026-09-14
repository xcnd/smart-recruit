package com.smartrecruit.interview.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.interview.dto.request.QuestionGenerateRequest;
import com.smartrecruit.interview.dto.request.SendQuestionsEmailRequest;
import com.smartrecruit.interview.dto.response.QuestionGenerateResult;
import com.smartrecruit.interview.dto.response.QuestionGenerateTaskVO;
import com.smartrecruit.interview.dto.response.QuestionGenerateVO;
import com.smartrecruit.interview.dto.response.CompletedQuestionResultVO;
import com.smartrecruit.interview.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 智能出题 REST 控制器。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/interviews/questions")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 异步 AI 出题 — 立即返回任务 ID，前端轮询 {@code GET /generate/{taskId}} 获取结果。
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<QuestionGenerateVO> generate(
            @Valid @RequestBody QuestionGenerateRequest request) {
        log.info("AI 出题请求（异步）: positionType={}", request.getPositionType());
        String taskId = questionService.generateAsync(request);
        return ApiResponse.success(new QuestionGenerateVO(taskId));
    }

    /**
     * 查询异步出题任务状态与结果。
     * <ul>
     *   <li>PENDING / PROCESSING — 前端继续轮询</li>
     *   <li>COMPLETED — 返回完整出题结果</li>
     *   <li>FAILED — 返回错误信息</li>
     *   <li>NOT_FOUND — 任务不存在或已过期</li>
     * </ul>
     */
    @GetMapping("/generate/{taskId}")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<QuestionGenerateTaskVO> getGenerateResult(@PathVariable String taskId) {
        QuestionGenerateTaskVO vo = questionService.getGenerateResult(taskId);
        return ApiResponse.success(vo);
    }

    /**
     * 将生成的面试题通过邮件发送给面试官。
     */
    @PostMapping("/send-email")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<Void> sendQuestionsByEmail(
            @Valid @RequestBody SendQuestionsEmailRequest request) {
        log.info("发送面试题邮件: to={}, positionLabel={}", request.getEmail(), request.getPositionLabel());
        questionService.sendQuestionsByEmail(request);
        return ApiResponse.success("面试题邮件已发送", null);
    }

    /**
     * 查询所有已完成的出题任务摘要 — 供新建测评时选择已有 AI 出题结果。
     */
    @GetMapping("/results")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<List<CompletedQuestionResultVO>> listCompletedResults() {
        List<CompletedQuestionResultVO> results = questionService.listCompletedResults();
        return ApiResponse.success(results);
    }
}
