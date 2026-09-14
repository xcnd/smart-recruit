package com.smartrecruit.recruitment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.recruitment.entity.Resume;
import com.smartrecruit.recruitment.enums.RecruitmentEnums;
import com.smartrecruit.recruitment.repository.ResumeMapper;
import com.smartrecruit.recruitment.service.FileStorageService;
import com.smartrecruit.recruitment.service.ResumeParseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 简历解析定时任务 — 每隔固定时间扫描 PENDING 状态的简历，逐条触发解析。
 *
 * <p>设计目的：
 * <ul>
 *   <li>上传与解析完全解耦：上传只负责落库，解析由定时任务驱动</li>
 *   <li>避免事务竞争：定时任务读取的数据已经是已提交状态</li>
 *   <li>失败可自动重试：PENDING 状态的记录下次扫描会重新处理</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Component
@Slf4j
public class ResumeParseTask {

    private final ResumeMapper resumeMapper;
    private final ResumeParseService resumeParseService;

    @Autowired(required = false)
    private FileStorageService fileStorageService;

    public ResumeParseTask(ResumeMapper resumeMapper,
                           ResumeParseService resumeParseService) {
        this.resumeMapper = resumeMapper;
        this.resumeParseService = resumeParseService;
    }

    /**
     * 每 5 秒扫描一次待解析简历，每次最多处理 5 条。
     */
    @Scheduled(fixedDelay = 5000)
    public void processPendingResumes() {
        List<Resume> pending = resumeMapper.selectList(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getParseStatus, RecruitmentEnums.ParseStatus.PENDING.getCode())
                        .orderByAsc(Resume::getCreatedAt)
                        .last("LIMIT 5"));

        if (pending.isEmpty()) {
            return;
        }

        log.info("定时任务发现 {} 条待解析简历", pending.size());

        for (Resume resume : pending) {
            try {
                processOne(resume);
            } catch (Exception e) {
                log.error("定时任务处理简历异常: resumeId={}", resume.getId(), e);
                markAsFailed(resume.getId());
            }
        }
    }

    /**
     * 处理单条 PENDING 简历：下载文件 → 触发异步解析。
     */
    private void processOne(Resume resume) {
        String fileUrl = resume.getFileUrl();
        byte[] fileBytes = downloadFile(fileUrl);

        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("无法读取文件，标记为解析失败: resumeId={}, fileUrl={}",
                    resume.getId(), fileUrl);
            markAsFailed(resume.getId());
            return;
        }

        log.info("触发异步解析: resumeId={}, file={}, size={}",
                resume.getId(), resume.getFileName(), fileBytes.length);
        resumeParseService.parseAsync(resume.getId(), fileBytes, resume.getFileName());
    }

    /**
     * 根据 fileUrl 通过 Feign 从 RustFS 下载文件字节数据。
     */
    private byte[] downloadFile(String fileUrl) {
        if (fileUrl == null) {
            return null;
        }

        String relativePath = extractRelativePath(fileUrl);
        if (relativePath == null || fileStorageService == null) {
            log.warn("无法提取相对路径或 FileStorageService 未就绪: fileUrl={}", fileUrl);
            return null;
        }

        try {
            byte[] bytes = fileStorageService.downloadBytes(relativePath);
            if (bytes != null && bytes.length > 0) {
                return bytes;
            }
            log.warn("从 RustFS 下载文件为空: path={}", relativePath);
        } catch (Exception e) {
            log.error("从 RustFS 下载文件失败: path={}", relativePath, e);
        }
        return null;
    }

    /**
     * 从 fileUrl 中提取 RustFS 桶中的相对路径。
     * <p>
     * RustFS URL: http://host:port/bucket/resumes/2026/07/xxx.pdf → resumes/2026/07/xxx.pdf
     * 内推投递: http://host:port/bucket/referrals/2026/08/xxx.doc → referrals/2026/08/xxx.doc
     * 本地路径: /uploads/resumes/xxx.pdf → resumes/xxx.pdf
     * </p>
     */
    private String extractRelativePath(String fileUrl) {
        int idx = fileUrl.indexOf("/resumes/");
        if (idx >= 0) return fileUrl.substring(idx + 1);
        idx = fileUrl.indexOf("/referrals/");
        return idx >= 0 ? fileUrl.substring(idx + 1) : null;
    }

    /**
     * 将简历标记为解析失败。
     */
    private void markAsFailed(Long resumeId) {
        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setParseStatus(RecruitmentEnums.ParseStatus.FAILED.getCode());
        resumeMapper.updateById(resume);
    }
}
