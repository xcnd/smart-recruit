package com.smartrecruit.interview.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 将候选人加入人才库请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToTalentPoolRequest {

    /** 候选人 ID。 */
    private Long candidateId;

    /** 人才标签列表。 */
    private List<String> tags;
}
