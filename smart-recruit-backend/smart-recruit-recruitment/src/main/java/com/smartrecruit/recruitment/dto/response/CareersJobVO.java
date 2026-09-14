package com.smartrecruit.recruitment.dto.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 招聘官网职位视图对象。
 *
 * @since 1.0.0
 */
@Data
public class CareersJobVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 职位 ID。 */
    private Long id;

    /** 招聘类型。 */
    private String recType;

    /** 职位名称。 */
    private String title;

    /** 部门/团队。 */
    private String dept;

    /** 工作地点。 */
    private String location;

    /** 经验要求。 */
    private String exp;

    /** 薪资范围。 */
    private String salary;

    /** 分类。 */
    private String category;

    /** 标签数组 [{text, cls}]。 */
    private List<Map<String, String>> tags;

    /** 岗位职责。 */
    private List<String> responsibilities;

    /** 任职要求。 */
    private List<String> requirements;

    /** 加分项。 */
    private List<String> bonus;

    /** 排序。 */
    private Integer sortOrder;

    /** 状态：1=已发布, 0=草稿。 */
    private Integer status;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
