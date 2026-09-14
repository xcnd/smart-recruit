package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.request.CareersApplyRequest;
import com.smartrecruit.recruitment.dto.request.CreateCareersJobRequest;
import com.smartrecruit.recruitment.dto.request.UpdateCareersJobRequest;
import com.smartrecruit.recruitment.dto.request.UpdateStatusRequest;
import com.smartrecruit.recruitment.dto.response.CareersApplicationVO;
import com.smartrecruit.recruitment.dto.response.CareersJobVO;
import com.smartrecruit.recruitment.dto.response.UploadResultVO;
import com.smartrecruit.recruitment.dto.response.AppliedCheckVO;
import com.smartrecruit.recruitment.service.CareersJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 招聘官网职位管理的REST控制器。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/careers/jobs")
@RequiredArgsConstructor
@Slf4j
public class CareersJobController {

    private final CareersJobService careersJobService;

    /**
     * 管理端分页列表。
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('careers:social:list', 'careers:campus:list')")
    public ApiResponse<PageResult<CareersJobVO>> list(
            @RequestParam String recType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(careersJobService.pageQuery(recType, keyword, category, page, size));
    }

    /**
     * 公开端点：按类型获取所有已发布职位（无需认证）。
     */
    @GetMapping("/public")
    public ApiResponse<List<CareersJobVO>> publicList(@RequestParam String recType) {
        return ApiResponse.success(careersJobService.listByType(recType));
    }

    /**
     * 公开端点：按 ID 获取单个职位详情（无需认证）。
     */
    @GetMapping("/public/detail")
    public ApiResponse<CareersJobVO> publicDetail(@RequestParam Long id) {
        return ApiResponse.success(careersJobService.getById(id));
    }

    /**
     * 职位详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('careers:social:list', 'careers:campus:list')")
    public ApiResponse<CareersJobVO> detail(@PathVariable Long id) {
        return ApiResponse.success(careersJobService.getById(id));
    }

    /**
     * 新建职位。
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('careers:social:edit', 'careers:campus:edit')")
    public ApiResponse<CareersJobVO> create(@Valid @RequestBody CreateCareersJobRequest request) {
        return ApiResponse.success(careersJobService.create(request));
    }

    /**
     * 更新职位。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('careers:social:edit', 'careers:campus:edit')")
    public ApiResponse<CareersJobVO> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateCareersJobRequest request) {
        return ApiResponse.success(careersJobService.update(id, request));
    }

    /**
     * 删除职位。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('careers:social:edit', 'careers:campus:edit')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        careersJobService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 状态切换。
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('careers:social:edit', 'careers:campus:edit')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody UpdateStatusRequest request) {
        careersJobService.updateStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    // ---- 公开投递端点 ----

    /**
     * 上传简历文件（公开，无需认证）。
     */
    @PostMapping("/public/upload-resume")
    public ApiResponse<UploadResultVO> publicUploadResume(@RequestParam("file") MultipartFile file) {
        String url = careersJobService.uploadResume(file);
        return ApiResponse.success(new UploadResultVO(url));
    }

    /**
     * 提交投递申请（公开，无需认证）。
     */
    @PostMapping("/public/apply")
    public ApiResponse<Void> publicApply(@Valid @RequestBody CareersApplyRequest request) {
        careersJobService.apply(request);
        return ApiResponse.success();
    }

    /**
     * 检查是否已投递过该职位（公开，无需认证）。
     */
    @GetMapping("/public/check-applied")
    public ApiResponse<AppliedCheckVO> publicCheckApplied(@RequestParam Long jobId,
                                                          @RequestParam(required = false) Long userId,
                                                          @RequestParam(required = false) String email) {
        boolean applied = careersJobService.hasApplied(jobId, userId, email);
        return ApiResponse.success(new AppliedCheckVO(applied));
    }

    /**
     * 获取当前候选人的投递记录（公开，优先按 userId 查询，其次按邮箱）。
     */
    @GetMapping("/public/my-applications")
    public ApiResponse<List<CareersApplicationVO>> publicMyApplications(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String email) {
        return ApiResponse.success(careersJobService.getMyApplications(userId, email));
    }
}
