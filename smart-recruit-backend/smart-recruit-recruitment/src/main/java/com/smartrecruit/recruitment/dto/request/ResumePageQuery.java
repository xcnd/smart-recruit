package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 简历分页查询过滤器。
 *
 * @since 1.0.0
 */
@Data
public class ResumePageQuery {

    /** 当前页码（从 1 开始）。 */
    private int page = 1;

    /** 每页大小，最大 100。 */
    private int size = 20;

    /** 按候选人 ID 过滤。 */
    private Long candidateId;

    /** 按解析状态过滤：PENDING, PARSING, SUCCESS, FAILED。 */
    private Integer parseStatus;

    /** 按筛选状态过滤：0=待处理,1=已通过,2=已淘汰。 */
    private Integer screeningStatus;

    /** 按候选人姓名模糊搜索。 */
    private String keyword;

    /** 按职位 ID 过滤（String 类型以兼容空字符串）。 */
    private String jobId;

    /** 最低 AI 匹配分（0 表示不过滤）。 */
    private Integer minScore;

    /** 按来源渠道过滤：0=主动投递,1=内推,2=官网,3=LinkedIn,4=BOSS直聘,5=拉勾,6=猎聘,7=其他。 */
    private Integer source;
}
