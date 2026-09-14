package com.smartrecruit.offer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.offer.dto.request.CreateOfferRequest;
import com.smartrecruit.offer.dto.request.OfferApprovalRequest;
import com.smartrecruit.offer.dto.request.UpdateOfferRequest;
import com.smartrecruit.offer.dto.response.OfferApprovalListVO;
import com.smartrecruit.offer.dto.response.OfferDetailVO;
import com.smartrecruit.offer.dto.response.OfferOptionVO;
import com.smartrecruit.offer.dto.response.OfferPredictionVO;
import com.smartrecruit.offer.dto.response.OfferVO;

import java.util.List;
import java.util.Map;

/**
 * Offer 管理服务接口。
 *
 * @since 1.0.0
 */
public interface OfferService {

    /**
     * 分页查询，支持可选过滤参数。
     */
    PageResult<OfferVO> pageQuery(Page<?> page, Map<String, Object> params);

    /**
     * 查询可创建合同的 Offer 下拉选项（已审批/已发送/已接受，支持关键字搜索）。
     */
    List<OfferOptionVO> listContractOptions(String keyword);

    /**
     * 根据 ID 获取 Offer 详情，包含审批历史。
     */
    OfferDetailVO getById(Long id);

    /**
     * 创建新的 Offer，根据薪资构成自动计算总包。
     */
    OfferVO create(CreateOfferRequest request);

    /**
     * 更新已有的 Offer（仅限草稿状态）。
     */
    OfferVO update(Long id, UpdateOfferRequest request);

    /**
     * 提交 Offer 审批（草稿 -> 待审批）。
     */
    void submitApproval(Long id);

    /**
     * 审批通过或驳回 Offer，并创建审批记录。
     */
    void approve(Long id, OfferApprovalRequest request);

    /**
     * 向候选人发送 Offer（状态 -> 已发送，设置发送时间）。
     */
    void send(Long id);

    /**
     * 重新发送 Offer 录用邮件。
     */
    void sendEmail(Long id);

    /**
     * 获取 Offer 接受概率的 AI 预测。
     */
    OfferPredictionVO getPrediction(Long id);

    /**
     * 批量刷新 Offer AI 接受度预测（定时任务调用，提前预测落库，
     * 详情页直接读取，避免实时调用 LLM 超时）。
     */
    void refreshAiPredictions();

    /**
     * 通过确认 Token 获取 Offer 详情（候选人无需登录）。
     */
    OfferDetailVO getByConfirmToken(String token);

    /**
     * 候选人确认 Offer：接受或拒绝。
     */
    void confirmOffer(String token, boolean accept, String declineReason);

    /**
     * HR 手动确认候选人答复（候选人未通过邮件确认时的兜底操作）。
     */
    void manualConfirm(Long id, boolean accept, String declineReason);

    /**
     * 删除 Offer（仅限草稿状态，逻辑删除）。
     */
    void delete(Long id);

    /**
     * 分页查询待审批 Offer 列表（审批工作台）。
     */
    PageResult<OfferApprovalListVO> getPendingApprovals(Page<?> page, Map<String, Object> params);

    /**
     * 分页查询已处理的审批历史。
     */
    PageResult<OfferApprovalListVO> getApprovalHistory(Page<?> page, Map<String, Object> params);

    /**
     * 获取审批统计数据（待审批数、今日通过数、今日驳回数）。
     */
    com.smartrecruit.offer.dto.response.OfferApprovalStatsVO getApprovalStats();
}
