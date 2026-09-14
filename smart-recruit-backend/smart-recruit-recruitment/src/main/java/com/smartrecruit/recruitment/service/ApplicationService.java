package com.smartrecruit.recruitment.service;

import com.smartrecruit.recruitment.entity.Application;

/**
 * 求职申请服务接口。
 *
 * <p>管理候选人与职位之间的申请关系，跟踪申请在招聘各阶段的生命周期。</p>
 *
 * @since 1.0.0
 */
public interface ApplicationService {

    /**
     * 创建求职申请记录，关联候选人与职位。
     *
     * @param candidateId 候选人 ID
     * @param jobId       职位 ID
     * @return 创建的申请记录
     */
    Application create(Long candidateId, Long jobId);

    /**
     * 根据候选人和职位查找现有申请记录。
     *
     * @param candidateId 候选人 ID
     * @param jobId       职位 ID
     * @return 申请记录，不存在返回 {@code null}
     */
    Application findByCandidateAndJob(Long candidateId, Long jobId);

    /**
     * 查找或创建申请记录（幂等）。
     */
    Application findOrCreate(Long candidateId, Long jobId);

    /**
     * 更新申请的招聘阶段。
     *
     * @param id    申请 ID
     * @param stage 目标阶段码
     */
    void updateStage(Long id, Integer stage);

    /**
     * 查找候选人最新的申请记录。
     *
     * @param candidateId 候选人 ID
     * @return 最新的申请记录，不存在返回 {@code null}
     */
    Application findLatestByCandidate(Long candidateId);
}
