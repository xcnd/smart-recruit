package com.smartrecruit.aiengine.dto.request;

import java.util.List;

/**
 * AI 人才推荐-候选人信息。
 *
 * @param candidateId        候选人 ID
 * @param name               候选人姓名
 * @param skills             技能列表
 * @param yearsOfExperience  工作年限
 * @param educationLevel     学历（ASSOCIATE/BACHELOR/MASTER/PHD）
 * @param currentCompany     当前公司
 * @param currentPosition    当前职位
 * @param summary            简介摘要
 * @since 2026-04-09
 */
public record TalentRecommendCandidate(
        Long candidateId,
        String name,
        List<String> skills,
        Integer yearsOfExperience,
        String educationLevel,
        String currentCompany,
        String currentPosition,
        String summary) {
}
