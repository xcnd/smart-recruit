package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 人才推荐异步任务 VO。
 *
 * @since 2026-04-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationTaskVO {

    /** 任务 ID。 */
    private String taskId;

    /** 任务状态：PENDING/RUNNING/COMPLETED/FAILED。 */
    private String status;

    /** 提示信息（失败原因等）。 */
    private String message;

    /** 当前页匹配结果（COMPLETED 后返回，默认每批 8 人）。 */
    private List<TalentPoolVO> results;

    /** 匹配总人数（全部分页）。 */
    private Integer total;

    /** 当前页码（从 1 开始）。 */
    private Integer page;

    /** 每批人数（默认 8）。 */
    private Integer size;
}
