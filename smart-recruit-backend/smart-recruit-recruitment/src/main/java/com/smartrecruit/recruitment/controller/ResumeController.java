package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.request.BatchScreenRequest;
import com.smartrecruit.recruitment.dto.request.ResumePageQuery;
import com.smartrecruit.recruitment.dto.request.UpdateScreeningStatusRequest;
import com.smartrecruit.recruitment.dto.request.UpdateResumeJobRequest;
import com.smartrecruit.recruitment.dto.response.AiScreeningResultVO;
import com.smartrecruit.recruitment.dto.response.BatchProgressVO;
import com.smartrecruit.recruitment.dto.response.BatchScreenResultVO;
import com.smartrecruit.recruitment.dto.response.ResumeDetailVO;
import com.smartrecruit.recruitment.dto.response.ResumeParseStatusVO;
import com.smartrecruit.recruitment.dto.response.ResumePreviewVO;
import com.smartrecruit.recruitment.dto.response.ResumeVO;
import com.smartrecruit.recruitment.service.AiScreeningService;
import com.smartrecruit.recruitment.service.FileStorageService;
import com.smartrecruit.recruitment.service.ResumeService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历管理的REST控制器。
 *
 * <p>提供简历上传、批量筛选和查询接口。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/resumes")
@Slf4j
public class ResumeController {

    private final ResumeService resumeService;
    private final AiScreeningService aiScreeningService;
    private final FileStorageService fileStorageService;

    public ResumeController(ResumeService resumeService,
                            AiScreeningService aiScreeningService,
                            @Autowired(required = false) FileStorageService fileStorageService) {
        this.resumeService = resumeService;
        this.aiScreeningService = aiScreeningService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 上传简历文件并触发异步解析流水线。
     *
     * <p>文件上传后立即返回，后台异步执行
     * Apache Tika 文本提取 → 大模型结构化 → 候选人自动创建。
     * 解析状态通过 {@code GET /{id}/parse-status} 轮询。</p>
     *
     * @param file          简历文件（PDF / DOCX / DOC / TXT）
     * @param candidateId   可选，关联已有候选人 ID
     * @param jobPositionId 关联职位 ID（仅允许已发布或暂停的职位）
     * @param referrerId    可选，推荐人用户 ID，设置后候选人来源自动标记为内推
     * @return 简历基本信息，含解析状态 PENDING
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('resume:upload')")
    public ApiResponse<ResumeVO> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "candidateId", required = false) Long candidateId,
                                        @RequestParam("jobPositionId") Long jobPositionId,
                                        @RequestParam(value = "referrerId", required = false) Long referrerId,
                                        @RequestParam(value = "autoScreen", required = false) Boolean autoScreen) {
        return ApiResponse.success(
                resumeService.upload(file, candidateId, jobPositionId, referrerId, autoScreen));
    }

    /**
     * 批量 AI 简历筛选。
     *
     * <p>对指定简历列表执行 AI 评分，返回五维度匹配分析和录用建议。
     * 评分结果即时回写到简历记录的 {@code aiMatchScore} 字段。</p>
     *
     * @param request 简历 ID 列表及可选职位 ID 和阈值
     * @return 每份简历的 AI 筛选结果（综合评分、维度分析、关键词匹配等）
     */
    @PostMapping("/batch-screen")
    @PreAuthorize("hasAuthority('resume:view')")
    public ApiResponse<BatchScreenResultVO> batchScreen(@Valid @RequestBody BatchScreenRequest request) {
        return ApiResponse.success(resumeService.batchScreen(request));
    }

    /**
     * 查询批量 AI 筛选进度。
     *
     * @param taskId 批量任务 ID
     * @return {@code {taskId, total, completed, failed, status}}
     */
    @GetMapping("/batch-screen/{taskId}/progress")
    @PreAuthorize("hasAuthority('resume:view')")
    public ApiResponse<BatchProgressVO> getBatchProgress(@PathVariable String taskId) {
        return ApiResponse.success(aiScreeningService.getBatchProgress(taskId));
    }

    /**
     * 分页查询简历列表。
     *
     * <p>支持按候选人姓名、职位、筛选状态、最低匹配分等条件过滤。</p>
     *
     * @param query 分页查询参数（关键字、职位 ID、筛选状态、最低分等）
     * @return 分页简历列表，含候选人姓名、技能标签、AI 匹配分
     */
    @GetMapping
    @PreAuthorize("hasAuthority('resume:view')")
    public ApiResponse<PageResult<ResumeVO>> list(@Valid ResumePageQuery query) {
        return ApiResponse.success(resumeService.pageQuery(query));
    }

    /**
     * 获取简历详情。
     *
     * <p>包含候选人基本信息、解析后的结构化和 AI 筛选结果
     * （仅在解析状态为 SUCCESS 时返回 AI 结果）。</p>
     *
     * @param id 简历 ID
     * @return 简历完整详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('resume:view')")
    public ApiResponse<ResumeDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(resumeService.getById(id));
    }

    /**
     * 获取单份简历的 AI 筛选结果。
     *
     * <p>独立查询 AI 五维度评分和录用建议，不依赖解析状态。
     * 可用于重新生成或查看历史筛选结果。</p>
     *
     * @param id 简历 ID
     * @return AI 筛选结果（综合评分、维度雷达图数据、优劣势分析等）
     */
    @GetMapping("/{id}/ai-result")
    @PreAuthorize("hasAuthority('resume:view')")
    public ApiResponse<AiScreeningResultVO> getAiResult(@PathVariable Long id) {
        return ApiResponse.success(resumeService.getAiResult(id));
    }

    /**
     * 更新简历筛选状态（通过/淘汰）。
     *
     * @param id   简历 ID
     * @param request 请求体
     * @return 空响应体
     */
    @PutMapping("/{id}/screening-status")
    @PreAuthorize("hasAuthority('resume:edit')")
    public ApiResponse<Void> updateScreeningStatus(@PathVariable Long id,
                                                    @RequestBody UpdateScreeningStatusRequest request) {
        resumeService.updateScreeningStatus(id, request.getScreeningStatus());
        return ApiResponse.success();
    }

    /**
     * 更新简历关联的职位。
     *
     * <p>请求体: {@code {"jobPositionId": 123}}，设为 null 可取消关联。
     * 仅允许关联已发布或暂停状态的职位。</p>
     *
     * @param id   简历 ID
     * @param request 请求体
     * @return 空响应体
     */
    @PutMapping("/{id}/job")
    @PreAuthorize("hasAuthority('resume:edit')")
    public ApiResponse<Void> updateJobPosition(@PathVariable Long id,
                                                @RequestBody UpdateResumeJobRequest request) {
        resumeService.updateJobPosition(id, request.getJobPositionId());
        return ApiResponse.success();
    }

    /**
     * 删除简历及 RustFS 上的文件。
     *
     * @param id 简历 ID
     * @return 空响应体
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('resume:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        resumeService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 查询简历解析状态，供前端轮询使用。
     *
     * <p>状态枚举：</p>
     * <ul>
     *   <li>{@code PENDING(0)} — 等待解析</li>
     *   <li>{@code PARSING(1)} — 解析中</li>
     *   <li>{@code SUCCESS(2)} — 解析成功</li>
     *   <li>{@code FAILED(3)} — 解析失败</li>
     * </ul>
     *
     * @param id 简历 ID
     * @return {@code {"resumeId": Long, "parseStatus": Integer}}
     */
    @GetMapping("/{id}/parse-status")
    @PreAuthorize("hasAuthority('resume:view')")
    public ApiResponse<ResumeParseStatusVO> getParseStatus(@PathVariable Long id) {
        return ApiResponse.success(resumeService.getParseStatus(id));
    }

    /**
     * Word 简历在线预览。
     * .docx → 返回原始字节，由前端 mammoth.js 渲染。
     * .doc  → 服务端 POI 转 HTML，直接返回。
     */
    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAuthority('resume:view')")
    public ResponseEntity<?> preview(@PathVariable Long id) {
        ResumePreviewVO vo = resumeService.preview(id);
        if (vo.status() == 404) {
            return ResponseEntity.notFound().build();
        }
        if (vo.status() != 200) {
            if ("text/html".equals(vo.contentType())) {
                return ResponseEntity.status(vo.status())
                        .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                        .body("<p style='color:#e74c3c;text-align:center;padding:60px 0;'>"
                                + vo.errorMessage() + "</p>");
            }
            return ResponseEntity.status(vo.status()).body(vo.errorMessage());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(vo.contentType()))
                .body(vo.content());
    }
}
