package com.smartrecruit.offer.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.offer.dto.response.ContractDetailVO;
import com.smartrecruit.offer.dto.response.ContractStatsVO;
import com.smartrecruit.offer.dto.response.ContractVO;

/**
 * 合同管理服务。
 *
 * @since 2026-04-09
 */
public interface ContractService {

    /** 分页查询合同。 */
    PageResult<ContractVO> pageQuery(Integer page, Integer size, String keyword,
                                     Integer status, String startDate, String endDate);

    /** 合同统计。 */
    ContractStatsVO getStats();

    /** 合同详情（含签署记录）。 */
    ContractDetailVO getDetail(Long id);

    /** 通过签署 Token 获取合同（候选人公开签署页）。 */
    ContractDetailVO getBySignToken(String token);

    /** 从 Offer 创建合同。 */
    ContractVO createFromOffer(Long offerId, String operator);

    /** 提交审批。 */
    void submitApproval(Long id, String operator);

    /** 审批通过。 */
    void approve(Long id, String operator);

    /** 发送给候选人签署（生成签署 Token）。 */
    void send(Long id, String operator);

    /** HR 签署。 */
    void hrSign(Long id, String signerName, String operator);

    /** 候选人签署/拒绝。 */
    void signByCandidate(String token, String name, boolean accept, String remark,
                         String signature, String idCard, String phone, String address, String ip);

    /** 合同生效（仅已签署合同可生效）。 */
    void makeEffective(Long id, String operator);

    /** 合同作废（仅已签署/生效中合同可作废）。 */
    void voidContract(Long id, String reason, String operator);

    /** 删除（仅草稿/已拒绝可删）。 */
    void delete(Long id, String operator);
}
