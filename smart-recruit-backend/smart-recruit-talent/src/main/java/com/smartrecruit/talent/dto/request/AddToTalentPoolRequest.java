package com.smartrecruit.talent.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 将候选人加入人才库请求。
 *
 * @since 2026-04-07
 */
@Data
public class AddToTalentPoolRequest {

    /** 候选人 ID。 */
    private Long candidateId;

    /** 人才标签列表。 */
    private List<String> tags;
}
