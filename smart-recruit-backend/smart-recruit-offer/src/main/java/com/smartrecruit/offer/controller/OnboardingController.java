package com.smartrecruit.offer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.offer.dto.request.CreateOnboardingRequest;
import com.smartrecruit.offer.dto.request.EmployeeStatusRequest;
import com.smartrecruit.offer.dto.request.UpdateAccountRequest;
import com.smartrecruit.offer.dto.request.UpdateEquipmentRequest;
import com.smartrecruit.offer.dto.request.UpdateMentorRequest;
import com.smartrecruit.offer.dto.request.UpdateTrainingRequest;
import com.smartrecruit.offer.dto.request.UpdateDocumentsRequest;
import com.smartrecruit.offer.dto.request.UpdateSingleDocumentRequest;
import com.smartrecruit.offer.dto.response.OnboardingDetailVO;
import com.smartrecruit.offer.dto.response.OnboardingStatsVO;
import com.smartrecruit.offer.dto.response.OnboardingVO;
import com.smartrecruit.offer.dto.response.RetentionPredictionVO;
import com.smartrecruit.offer.dto.response.FileUploadVO;
import com.smartrecruit.offer.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 入职管理 REST 控制器。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Slf4j
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * 分页查询入职计划列表，支持候选人、状态、时间范围等筛选。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('onboarding:view')")
    public ApiResponse<PageResult<OnboardingVO>> list(
            @RequestParam(required = false) Long candidateId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String employeeNo,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String positionTitle,
            @RequestParam(required = false) String employeeStatus,
            @RequestParam(required = false) String currentStep,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> params = new HashMap<>();
        if (candidateId != null) params.put("candidateId", candidateId);
        if (status != null) params.put("status", status);
        if (startDate != null && !startDate.isBlank()) params.put("startDate", startDate);
        if (endDate != null && !endDate.isBlank()) params.put("endDate", endDate);
        if (employeeNo != null && !employeeNo.isBlank()) params.put("employeeNo", employeeNo);
        if (employeeName != null && !employeeName.isBlank()) params.put("employeeName", employeeName);
        if (departmentName != null && !departmentName.isBlank()) params.put("departmentName", departmentName);
        if (positionTitle != null && !positionTitle.isBlank()) params.put("positionTitle", positionTitle);
        if (employeeStatus != null && !employeeStatus.isBlank()) params.put("employeeStatus", employeeStatus);
        if (currentStep != null && !currentStep.isBlank()) params.put("currentStep", currentStep);

        Page<?> pageQuery = new Page<>(page, size);
        PageResult<OnboardingVO> result = onboardingService.pageQuery(pageQuery, params);
        return ApiResponse.success(result);
    }

    /**
     * 查询入职整体统计数据（各状态数量等）。
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('onboarding:view')")
    public ApiResponse<OnboardingStatsVO> stats() {
        return ApiResponse.success(onboardingService.getStats());
    }

    /**
     * 查询入职计划详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('onboarding:view')")
    public ApiResponse<OnboardingDetailVO> detail(@PathVariable Long id) {
        OnboardingDetailVO vo = onboardingService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 创建入职计划。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<OnboardingVO> create(@RequestBody CreateOnboardingRequest request) {
        log.info("Creating onboarding: offerId={}, candidateId={}", request.getOfferId(), request.getCandidateId());
        OnboardingVO vo = onboardingService.create(request);
        return ApiResponse.success(vo);
    }

    /**
     * 删除入职计划。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("Deleting onboarding: id={}", id);
        onboardingService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 推进入职流程到下一步。
     */
    @PostMapping("/{id}/advance-step")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<OnboardingVO> advanceStep(@PathVariable Long id) {
        log.info("Advancing onboarding step: id={}", id);
        OnboardingVO vo = onboardingService.advanceStep(id);
        return ApiResponse.success(vo);
    }

    /**
     * 手动完成入职流程。
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> completeOnboarding(@PathVariable Long id) {
        log.info("Manually completing onboarding: id={}", id);
        onboardingService.completeOnboarding(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> updateDocuments(@PathVariable Long id,
                                              @RequestBody UpdateDocumentsRequest request) {
        log.info("Updating onboarding documents: id={}", id);
        onboardingService.updateDocuments(id, request.getDocuments());
        return ApiResponse.success();
    }

    @PutMapping("/{id}/equipment/{equipmentId}")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> updateEquipment(@PathVariable Long id,
                                              @PathVariable Long equipmentId,
                                              @RequestBody UpdateEquipmentRequest request) {
        log.info("Updating equipment: onboardingId={}, equipmentId={}", id, equipmentId);
        onboardingService.updateEquipment(id, equipmentId, request);
        return ApiResponse.success();
    }

    /**
     * 查询入职员工的留存风险预测。
     */
    @GetMapping("/{id}/retention-prediction")
    @PreAuthorize("hasAuthority('onboarding:view')")
    public ApiResponse<RetentionPredictionVO> retentionPrediction(@PathVariable Long id) {
        RetentionPredictionVO vo = onboardingService.getRetentionPrediction(id);
        return ApiResponse.success(vo);
    }

    // ==================== 步骤管理 ====================

    @PutMapping("/{id}/mentor")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> updateMentor(@PathVariable Long id,
                                           @RequestBody UpdateMentorRequest request) {
        log.info("Updating mentor for onboarding: id={}", id);
        onboardingService.updateMentor(id, request);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/training")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> updateTraining(@PathVariable Long id,
                                              @RequestBody UpdateTrainingRequest request) {
        log.info("Updating training for onboarding: id={}, progress={}", id, request.getTrainingProgress());
        onboardingService.updateTraining(id, request);
        return ApiResponse.success();
    }

    /**
     * 发送欢迎页给新员工。
     */
    @PostMapping("/{id}/welcome")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> sendWelcome(@PathVariable Long id) {
        log.info("Sending welcome for onboarding: id={}", id);
        onboardingService.sendWelcome(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/account")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> updateAccount(@PathVariable Long id,
                                             @RequestBody UpdateAccountRequest request) {
        log.info("Updating account for onboarding: id={}, username={}", id, request.getUsername());
        onboardingService.updateAccount(id, request);
        return ApiResponse.success();
    }

    /**
     * 更新员工状态（0=待入职,1=试用期,2=正式,3=已离职）。
     */
    @PutMapping("/{id}/employee-status")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> updateEmployeeStatus(@PathVariable Long id,
                                                  @RequestBody EmployeeStatusRequest request) {
        log.info("Updating employee status: id={}, status={}", id, request.status());
        onboardingService.updateEmployeeStatus(id, request.status());
        return ApiResponse.success();
    }

    // ==================== 文件上传 & 单文档更新 ====================

    @PostMapping("/files/upload")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<FileUploadVO> uploadFile(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(onboardingService.uploadFile(file));
    }

    @PutMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> updateSingleDocument(@PathVariable Long id,
                                                   @PathVariable Long documentId,
                                                   @RequestBody UpdateSingleDocumentRequest request) {
        log.info("Updating single document: onboardingId={}, documentId={}", id, documentId);
        onboardingService.updateSingleDocument(id, documentId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasAuthority('onboarding:edit')")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id,
                                             @PathVariable Long documentId) {
        log.info("Deleting document file: onboardingId={}, documentId={}", id, documentId);
        onboardingService.clearDocumentFile(id, documentId);
        return ApiResponse.success();
    }
}
