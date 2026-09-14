package com.smartrecruit.recruitment.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.request.BatchScreenRequest;
import com.smartrecruit.recruitment.dto.request.ResumePageQuery;
import com.smartrecruit.recruitment.dto.response.AiScreeningResultVO;
import com.smartrecruit.recruitment.dto.response.BatchScreenResultVO;
import com.smartrecruit.recruitment.dto.response.ResumeDetailVO;
import com.smartrecruit.recruitment.dto.response.ResumeParseStatusVO;
import com.smartrecruit.recruitment.dto.response.ResumePreviewVO;
import com.smartrecruit.recruitment.dto.response.ResumeStatsVO;
import com.smartrecruit.recruitment.dto.response.ResumeVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历管理的服务接口。
 *
 * @since 1.0.0
 */
public interface ResumeService {

    /**
     * 为候选人上传简历文件。
     *
     * @param jobPositionId 可选，关联职位 ID（仅允许已发布或暂停的职位）
     */
    ResumeVO upload(MultipartFile file, Long candidateId, Long jobPositionId,
                    Long referrerId, Boolean autoScreen);

    /**
     * 通过AI评分批量筛选简历（异步执行，返回任务ID用于进度查询）。
     */
    BatchScreenResultVO batchScreen(BatchScreenRequest request);

    /**
     * 带筛选条件的简历分页查询。
     */
    PageResult<ResumeVO> pageQuery(ResumePageQuery query);

    /**
     * 根据ID获取单个简历。
     */
    ResumeDetailVO getById(Long id);

    /**
     * 获取简历文件的在线预览内容。
     *
     * <p>.docx 返回原始字节（由前端 mammoth.js 渲染）；
     * .doc 由服务端 POI 转换为 HTML 后返回。</p>
     *
     * @param id 简历 ID
     * @return 预览结果（含 HTTP 状态、内容字节与 Content-Type）
     */
    ResumePreviewVO preview(Long id);

    /**
     * 获取单个简历的AI筛选结果。
     */
    AiScreeningResultVO getAiResult(Long resumeId);

    /**
     * 更新简历的筛选状态（通过/淘汰/重置）。
     */
    void updateScreeningStatus(Long resumeId, Integer screeningStatus);

    /**
     * 获取简历筛选统计（总数、待处理、已通过、已淘汰）。
     */
    ResumeStatsVO getStats();

    /**
     * 获取简历解析状态（用于前端轮询）。
     *
     * @param resumeId 简历 ID
     * @return 包含 parseStatus 的简单 VO
     */
    ResumeParseStatusVO getParseStatus(Long resumeId);

    /**
     * 更新简历关联的职位。
     *
     * @param resumeId      简历 ID
     * @param jobPositionId 职位 ID（可为 null，表示取消关联）
     */
    void updateJobPosition(Long resumeId, Long jobPositionId);

    /**
     * 删除简历及关联的文件。
     *
     * @param id 简历 ID
     */
    void delete(Long id);
}
