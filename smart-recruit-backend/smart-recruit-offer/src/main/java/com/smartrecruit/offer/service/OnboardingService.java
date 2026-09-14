package com.smartrecruit.offer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.offer.dto.request.CreateOnboardingRequest;
import com.smartrecruit.offer.dto.request.UpdateAccountRequest;
import com.smartrecruit.offer.dto.request.UpdateEquipmentRequest;
import com.smartrecruit.offer.dto.request.UpdateMentorRequest;
import com.smartrecruit.offer.dto.request.UpdateTrainingRequest;
import com.smartrecruit.offer.dto.request.UpdateSingleDocumentRequest;
import com.smartrecruit.offer.dto.response.OnboardingDetailVO;
import com.smartrecruit.offer.dto.response.OnboardingStatsVO;
import com.smartrecruit.offer.dto.response.OnboardingVO;
import com.smartrecruit.offer.dto.response.RetentionPredictionVO;
import com.smartrecruit.offer.dto.response.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 入职管理服务接口。
 *
 * @since 1.0.0
 */
public interface OnboardingService {

    /**
     * 分页查询，支持可选过滤条件。
     */
    PageResult<OnboardingVO> pageQuery(Page<?> page, Map<String, Object> params);

    /**
     * 根据 ID 获取入职详情，包含步骤进度和文档状态。
     */
    OnboardingDetailVO getById(Long id);

    /**
     * 创建入职记录（内部调用，当 Offer 被接受时）。
     */
    OnboardingVO create(CreateOnboardingRequest request);

    /**
     * 删除入职记录（逻辑删除，仅允许 PENDING 或 AT_RISK 状态）。
     */
    void delete(Long id);

    /**
     * 将入职流程推进到下一步（1->2->...->6）。
     * 进入最后一步时，仅当所有文档已上传才自动将状态设为已完成。
     */
    OnboardingVO advanceStep(Long id);

    /**
     * 手动完成入职（所有步骤完成后可手动标记已完成，即使有文档缺失也允许）。
     */
    void completeOnboarding(Long id);

    /**
     * 更新入职记录的文档状态。
     */
    void updateDocuments(Long id, Map<String, String> documentStatuses);

    /**
     * 更新入职记录的设备状态。
     */
    void updateEquipment(Long onboardingId, Long equipmentId, UpdateEquipmentRequest request);

    /**
     * 获取入职员工的留任预测。
     */
    RetentionPredictionVO getRetentionPrediction(Long id);

    /**
     * 批量刷新入职留任预测（定时任务调用）。
     *
     * <p>按入职 5 步数据指纹判断：hash 未变化则跳过，不重复调用 AI；
     * 预测结果保存到数据库，查询时直接返回保存的数据。</p>
     */
    void refreshRetentionPredictions();

    /**
     * 获取入职统计数据。
     */
    OnboardingStatsVO getStats();

    /**
     * 更新导师/伙伴分配。
     */
    void updateMentor(Long id, UpdateMentorRequest request);

    /**
     * 更新培训进度。
     */
    void updateTraining(Long id, UpdateTrainingRequest request);

    /**
     * 发送欢迎消息（步骤4）。
     */
    void sendWelcome(Long id);

    /**
     * 创建系统账号（步骤3）。
     */
    void updateAccount(Long id, UpdateAccountRequest request);

    /**
     * 更新员工状态（0=待入职,1=试用期,2=正式,3=已离职）。
     */
    void updateEmployeeStatus(Long id, Integer status);

    /**
     * 更新单个入职文档记录（文件路径、状态、备注等）。
     */
    void updateSingleDocument(Long onboardingId, Long documentId, UpdateSingleDocumentRequest request);

    /**
     * 删除入职文档文件（清空文件路径并将状态重置为缺失）。
     */
    void clearDocumentFile(Long onboardingId, Long documentId);

    /**
     * 上传入职文档附件。
     *
     * <p>校验文件类型与大小后，通过系统服务上传到对象存储（RustFS），
     * 返回可直接访问的文件 URL 与元数据。</p>
     *
     * @param file 上传的文件
     * @return 上传结果（文件 URL、原始文件名、大小）
     * @throws com.smartrecruit.common.exception.ValidationException 文件为空、类型不支持、大小超限或上传失败
     */
    FileUploadVO uploadFile(MultipartFile file);
}
