package com.smartrecruit.interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.interview.dto.request.QuestionBankSaveRequest;
import com.smartrecruit.interview.dto.response.QuestionBankVO;
import com.smartrecruit.interview.service.InterviewQuestionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 面试题库 REST 控制器。
 *
 * <p>管理按部门/职位组织的面试套题，支持查看题目、选项、答案与解读。</p>
 *
 * @since 2026-04-10
 */
@RestController
@RequestMapping("/api/v1/question-banks")
@RequiredArgsConstructor
@Slf4j
public class QuestionBankController {

    private final InterviewQuestionBankService questionBankService;

    /**
     * 分页查询面试题库套题。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<PageResult<QuestionBankVO>> page(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> params = new HashMap<>();
        if (departmentId != null) params.put("departmentId", departmentId);
        if (jobTitle != null) params.put("jobTitle", jobTitle);
        if (status != null) params.put("status", status);
        if (questionType != null) params.put("questionType", questionType);
        if (keyword != null) params.put("keyword", keyword);
        if (startDate != null && !startDate.isBlank()) params.put("startDate", startDate);
        if (endDate != null && !endDate.isBlank()) params.put("endDate", endDate);
        return ApiResponse.success(questionBankService.pageQuery(
                new Page<>(page, size), params));
    }

    /**
     * 查询套题详情（含题目、选项、答案与解读）。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<QuestionBankVO> detail(@PathVariable Long id) {
        return ApiResponse.success(questionBankService.getById(id));
    }

    /**
     * 新建面试题库套题。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<QuestionBankVO> create(@Valid @RequestBody QuestionBankSaveRequest request) {
        return ApiResponse.success(questionBankService.create(request));
    }

    /**
     * 更新面试题库套题（题目整体替换）。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<QuestionBankVO> update(@PathVariable Long id,
                                              @Valid @RequestBody QuestionBankSaveRequest request) {
        return ApiResponse.success(questionBankService.update(id, request));
    }

    /**
     * 删除面试题库套题。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionBankService.delete(id);
        return ApiResponse.success();
    }
}
