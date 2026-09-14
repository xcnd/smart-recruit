package com.smartrecruit.recruitment.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.request.CareersApplyRequest;
import com.smartrecruit.recruitment.dto.request.CreateCareersJobRequest;
import com.smartrecruit.recruitment.dto.request.UpdateCareersJobRequest;
import com.smartrecruit.recruitment.dto.response.CareersApplicationVO;
import com.smartrecruit.recruitment.dto.response.CareersJobVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 招聘官网职位管理的服务接口。
 *
 * @since 1.0.0
 */
public interface CareersJobService {

    /**
     * 管理端分页查询，支持按类型、关键词、分类筛选。
     */
    PageResult<CareersJobVO> pageQuery(String recType, String keyword, String category, int page, int size);

    /**
     * 公开端点：按类型返回所有已发布职位。
     */
    List<CareersJobVO> listByType(String recType);

    /**
     * 根据ID获取单个职位。
     */
    CareersJobVO getById(Long id);

    /**
     * 创建新职位。
     */
    CareersJobVO create(CreateCareersJobRequest request);

    /**
     * 更新现有职位。
     */
    CareersJobVO update(Long id, UpdateCareersJobRequest request);

    /**
     * 软删除职位。
     */
    void delete(Long id);

    /**
     * 更新职位状态（1=已发布, 0=草稿）。
     */
    void updateStatus(Long id, Integer status);

    /**
     * 上传简历文件，返回公开访问 URL。
     */
    String uploadResume(MultipartFile file);

    /**
     * 提交投递申请。
     */
    void apply(CareersApplyRequest request);

    /**
     * 检查候选人是否已投递过该职位。
     *
     * @param jobId careers_job_position.id
     * @param userId 系统用户 ID（优先使用）
     * @param email 候选人邮箱（备用）
     * @return true 如果已投递
     */
    boolean hasApplied(Long jobId, Long userId, String email);

    /**
     * 查询候选人的投递记录（优先按 userId，其次按邮箱）。
     */
    List<CareersApplicationVO> getMyApplications(Long userId, String email);
}
