package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 职位的分页查询筛选条件。
 *
 * @since 1.0.0
 */
@Data
public class JobPageQuery {

    /** 当前页码（从1开始）。 */
    private int page = 1;

    /** 每页大小，最大100。 */
    private int size = 20;

    /** 按标题筛选（模糊匹配）。 */
    private String title;

    /** 按部门ID筛选。 */
    private Long departmentId;

    /** 按状态筛选：草稿、已发布、已关闭、已取消。 */
    private Integer status;

    /** 按职位类型筛选：全职、兼职、实习、外包。 */
    private Integer type;

    /** 按工作地点城市筛选。 */
    private String location;

    /** 发布时间起始（含）。 */
    private String startDate;

    /** 发布时间截止（含）。 */
    private String endDate;
}
