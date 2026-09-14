package com.smartrecruit.interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.interview.dto.request.CreateInterviewRequest;
import com.smartrecruit.interview.dto.request.InterviewFeedbackRequest;
import com.smartrecruit.interview.dto.request.SubmitInterviewRequest;
import com.smartrecruit.interview.dto.request.UpdateInterviewRequest;
import com.smartrecruit.interview.dto.request.UpdateInterviewResultRequest;
import com.smartrecruit.interview.dto.response.AiStatsVO;
import com.smartrecruit.interview.dto.response.AssessedCandidateVO;
import com.smartrecruit.interview.dto.response.InterviewDetailVO;
import com.smartrecruit.interview.dto.response.InterviewReportVO;
import com.smartrecruit.interview.dto.response.InterviewScheduleVO;
import com.smartrecruit.interview.dto.response.InterviewStatsVO;
import com.smartrecruit.interview.dto.response.InterviewVO;
import com.smartrecruit.interview.dto.response.NextRoundVO;
import com.smartrecruit.interview.enums.InterviewEnums;
import com.smartrecruit.interview.service.InterviewService;
import com.smartrecruit.common.util.DateUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试管理 REST 控制器。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewService interviewService;

    private LocalDateTime parseStart(String date) {
        LocalDate parsed = DateUtils.parseDate(date);
        return parsed != null ? parsed.atStartOfDay() : null;
    }

    private LocalDateTime parseEnd(String date) {
        LocalDate parsed = DateUtils.parseDate(date);
        return parsed != null ? parsed.plusDays(1).atStartOfDay() : null;
    }

    // ================================================================
    // 列表与统计
    // ================================================================

    /**
     * 分页查询面试列表，支持候选人、职位、姓名、面试官、时间范围、状态与类型筛选。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<PageResult<InterviewVO>> list(
            @RequestParam(required = false) Long candidateId,
            @RequestParam(required = false) Long jobPositionId,
            @RequestParam(required = false) String candidateName,
            @RequestParam(required = false) String interviewerName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> params = new HashMap<>();
        if (candidateId != null) params.put("candidateId", candidateId);
        if (jobPositionId != null) params.put("jobPositionId", jobPositionId);
        if (candidateName != null && !candidateName.isBlank()) params.put("candidateName", candidateName.trim());
        if (interviewerName != null && !interviewerName.isBlank()) params.put("interviewerName", interviewerName.trim());
        if (startDate != null && !startDate.isBlank()) params.put("startDate", startDate.trim());
        if (endDate != null && !endDate.isBlank()) params.put("endDate", endDate.trim());
        if (status != null) {
            Integer statusCode = resolveStatusFilter(status);
            if (statusCode != null) params.put("status", statusCode);
        }
        if (type != null) {
            Integer typeCode = resolveTypeFilter(type);
            if (typeCode != null) params.put("type", typeCode);
        }

        Page<?> pageQuery = new Page<>(page, size);
        PageResult<InterviewVO> result = interviewService.pageQuery(pageQuery, params);
        return ApiResponse.success(result);
    }

    /**
     * 查询指定时间范围内的面试统计数据。
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<InterviewStatsVO> stats(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        InterviewStatsVO stats = interviewService.getStats(
                parseStart(startDate), parseEnd(endDate));
        return ApiResponse.success(stats);
    }

    /**
     * 查询 AI 智能面试统计数据。
     */
    @GetMapping("/ai-stats")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<AiStatsVO> aiStats() {
        log.info("查询 AI 智能面试统计数据");
        AiStatsVO stats = interviewService.getAiStats();
        return ApiResponse.success(stats);
    }

    /**
     * 查询已完成 AI 评估的候选人列表。
     */
    @GetMapping("/assessed-candidates")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<List<AssessedCandidateVO>> assessedCandidates() {
        log.info("查询已评估的候选人列表");
        List<AssessedCandidateVO> list = interviewService.getAssessedCandidates();
        return ApiResponse.success(list);
    }

    /**
     * 查询候选人下一轮面试建议（轮次、类型等）。
     */
    @GetMapping("/candidates/{candidateId}/next-round")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<NextRoundVO> getNextRound(@PathVariable Long candidateId) {
        NextRoundVO result = interviewService.getSuggestedNextRound(candidateId);
        return ApiResponse.success(result);
    }

    /**
     * 查询指定日期的面试日程列表。
     */
    @GetMapping("/schedule")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<List<InterviewScheduleVO>> schedule(
            @RequestParam String date) {
        log.info("查询面试日程: date={}", date);
        List<InterviewScheduleVO> schedule = interviewService.getSchedule(date);
        return ApiResponse.success(schedule);
    }

    /**
     * 查询指定日期范围内的周面试日程列表。
     */
    @GetMapping("/schedule/week")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<List<InterviewScheduleVO>> weekSchedule(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("查询周面试日程: startDate={}, endDate={}", startDate, endDate);
        List<InterviewScheduleVO> schedule = interviewService.getWeekSchedule(startDate, endDate);
        return ApiResponse.success(schedule);
    }

    // ================================================================
    // 创建
    // ================================================================

    /**
     * 创建面试记录。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('interview:create')")
    public ApiResponse<InterviewVO> create(@Valid @RequestBody CreateInterviewRequest request) {
        log.info("Creating interview: candidateId={}, type={}", request.getCandidateId(), request.getInterviewType());
        InterviewVO vo = interviewService.create(request);
        return ApiResponse.success(vo);
    }

    // ================================================================
    // 面试详情与报告
    // ================================================================

    /**
     * 查询面试详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<InterviewDetailVO> detail(@PathVariable Long id) {
        InterviewDetailVO vo = interviewService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 查询面试的 AI 面试题目列表。
     */
    @GetMapping("/{id}/questions")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<List<String>> questions(@PathVariable Long id) {
        return ApiResponse.success(interviewService.getAiQuestions(id));
    }

    /**
     * 生成并查询面试评估报告。
     */
    @GetMapping("/{id}/report")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<InterviewReportVO> report(@PathVariable Long id) {
        InterviewReportVO vo = interviewService.getReport(id);
        return ApiResponse.success(vo);
    }

    // ================================================================
    // 提交与反馈
    // ================================================================

    /**
     * 提交面试结果（状态、评分与反馈）。
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('interview:edit')")
    public ApiResponse<Void> submit(@PathVariable Long id,
                                     @Valid @RequestBody SubmitInterviewRequest request) {
        log.info("Submitting interview result: id={}", id);
        interviewService.submitResult(id, request);
        return ApiResponse.success();
    }

    /**
     * 提交面试官反馈。
     */
    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasAuthority('interview:edit')")
    public ApiResponse<Void> feedback(@PathVariable Long id,
                                       @Valid @RequestBody InterviewFeedbackRequest request) {
        log.info("提交面试反馈: interviewId={}", id);
        interviewService.submitFeedback(id, request);
        return ApiResponse.success();
    }

    // ================================================================
    // 更新与操作
    // ================================================================

    /**
     * 更新面试信息。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('interview:edit')")
    public ApiResponse<InterviewVO> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateInterviewRequest request) {
        log.info("Updating interview: id={}", id);
        InterviewVO vo = interviewService.update(id, request);
        return ApiResponse.success(vo);
    }

    /**
     * 更新面试结果（面试结果、评分与评语）。
     */
    @PutMapping("/{id}/result")
    @PreAuthorize("hasAuthority('interview:edit')")
    public ApiResponse<Void> updateResult(@PathVariable Long id,
                                           @RequestBody UpdateInterviewResultRequest request) {
        Integer result = request.getResult();
        if (result == null) {
            return ApiResponse.error(40001, "面试结果不能为空");
        }
        Integer score = request.getScore();
        String feedback = request.getComment();
        log.info("更新面试结果: id={}, result={}, score={}, hasFeedback={}", id, result, score, feedback != null);
        interviewService.updateResult(id, result, score, feedback);
        return ApiResponse.success();
    }

    private Integer toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /**
     * 开始面试，将状态流转为进行中。
     */
    @PutMapping("/{id}/start")
    @PreAuthorize("hasAuthority('interview:edit')")
    public ApiResponse<Void> start(@PathVariable Long id) {
        log.info("Starting interview: id={}", id);
        interviewService.startInterview(id);
        return ApiResponse.success();
    }

    /**
     * 取消面试。
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('interview:edit')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        log.info("Cancelling interview: id={}", id);
        interviewService.cancel(id);
        return ApiResponse.success();
    }

    /**
     * 删除面试记录。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('interview:edit')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("Deleting interview: id={}", id);
        interviewService.delete(id);
        return ApiResponse.success();
    }

    // ================================================================
    // 辅助方法
    // ================================================================

    /**
     * 将前端发送的字符串状态筛选值转换为数据库中的整数编码。
     */
    private Integer resolveStatusFilter(String status) {
        if (status == null || status.isEmpty()) return null;
        try { return Integer.parseInt(status); } catch (NumberFormatException ignored) {}
        try {
            InterviewEnums.InterviewStatus s = InterviewEnums.InterviewStatus.valueOf(status.toUpperCase());
            return s.getCode();
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    /**
     * 将前端发送的字符串类型筛选值转换为数据库中的整数编码。
     */
    private Integer resolveTypeFilter(String type) {
        if (type == null || type.isEmpty()) return null;
        try { return Integer.parseInt(type); } catch (NumberFormatException ignored) {}
        return switch (type.toUpperCase()) {
            case "AI" -> InterviewEnums.InterviewType.AI.getCode();
            case "TECH", "TECHNICAL" -> InterviewEnums.InterviewType.TECHNICAL.getCode();
            case "HR", "BEHAVIOR" -> InterviewEnums.InterviewType.HR.getCode();
            case "PHONE" -> InterviewEnums.InterviewType.PHONE.getCode();
            case "VIDEO" -> InterviewEnums.InterviewType.VIDEO.getCode();
            case "ONSITE" -> InterviewEnums.InterviewType.ONSITE.getCode();
            case "EXECUTIVE", "LEADERSHIP" -> InterviewEnums.InterviewType.LEADERSHIP.getCode();
            default -> null;
        };
    }
}
