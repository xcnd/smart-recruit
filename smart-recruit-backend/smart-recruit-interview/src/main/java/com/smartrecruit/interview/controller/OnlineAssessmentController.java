package com.smartrecruit.interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.interview.dto.request.CreateAssessmentRequest;
import com.smartrecruit.interview.dto.request.UpdateAssessmentScoreRequest;
import com.smartrecruit.interview.dto.response.AssessmentQuestionItem;
import com.smartrecruit.interview.dto.response.OnlineAssessmentVO;
import com.smartrecruit.interview.service.OnlineAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线测评管理 REST 控制器。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
@Slf4j
public class OnlineAssessmentController {

    private final OnlineAssessmentService assessmentService;

    /**
     * 分页查询在线测评列表。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<PageResult<OnlineAssessmentVO>> list(
            @RequestParam(required = false) Long candidateId,
            @RequestParam(required = false) String candidateName,
            @RequestParam(required = false) String candidateEmail,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> params = new HashMap<>();
        if (candidateId != null) params.put("candidateId", candidateId);
        if (candidateName != null && !candidateName.isBlank()) params.put("candidateName", candidateName);
        if (candidateEmail != null && !candidateEmail.isBlank()) params.put("candidateEmail", candidateEmail);
        if (status != null) params.put("status", status);
        if (type != null) params.put("type", type);
        if (startDate != null && !startDate.isBlank()) params.put("startDate", startDate);
        if (endDate != null && !endDate.isBlank()) params.put("endDate", endDate);

        Page<?> pageQuery = new Page<>(page, size);
        PageResult<OnlineAssessmentVO> result = assessmentService.pageQuery(pageQuery, params);
        return ApiResponse.success(result);
    }

    /**
     * 创建在线测评。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('interview:create')")
    public ApiResponse<OnlineAssessmentVO> create(@Valid @RequestBody CreateAssessmentRequest request) {
        log.info("创建在线测评: candidateId={}, type={}", request.getCandidateId(), request.getType());
        OnlineAssessmentVO vo = assessmentService.create(request);
        return ApiResponse.success(vo);
    }

    /**
     * 发送测评给候选人。
     */
    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('interview:edit')")
    public ApiResponse<Void> send(@PathVariable Long id) {
        log.info("发送在线测评: id={}", id);
        assessmentService.send(id);
        return ApiResponse.success();
    }

    /**
     * 查询测评题目详情（含正确答案，仅内部管理端调用）。
     */
    @GetMapping("/{id}/questions")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<List<AssessmentQuestionItem>> getQuestions(@PathVariable Long id) {
        List<AssessmentQuestionItem> questions = assessmentService.getQuestions(id);
        return ApiResponse.success(questions);
    }

    /**
     * 更新测评成绩。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('interview:edit')")
    public ApiResponse<Void> updateScore(@PathVariable Long id,
                                          @RequestBody UpdateAssessmentScoreRequest request) {
        String score = request.getScore();
        log.info("更新测评成绩: id={}, score={}", id, score);
        assessmentService.updateScore(id, score);
        return ApiResponse.success();
    }

    /**
     * 直接保存测评题目（前端传入完整题目列表，序列化存入 questions_json）。
     */
    @PutMapping("/{id}/questions")
    @PreAuthorize("hasAuthority('interview:create')")
    public ApiResponse<Void> saveQuestions(@PathVariable Long id,
                                            @RequestBody List<AssessmentQuestionItem> questions) {
        log.info("保存测评题目: assessmentId={}, count={}", id, questions.size());
        assessmentService.saveQuestions(id, questions);
        return ApiResponse.success("题目保存成功", null);
    }

    /**
     * 为编程测试生成 AI 题目（仅限 type=0 且状态为未发送的测评）。
     */
    @PostMapping("/{id}/generate-questions")
    @PreAuthorize("hasAuthority('interview:create')")
    public ApiResponse<Void> generateQuestions(@PathVariable Long id) {
        log.info("AI 生成测评题目: assessmentId={}", id);
        assessmentService.generateQuestions(id);
        return ApiResponse.success("AI 题目生成成功", null);
    }
}
