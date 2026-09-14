package com.smartrecruit.recruitment.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 我的投递记录 VO（招聘官网渠道）。
 *
 * @since 1.0.0
 */
@Data
public class CareersApplicationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Application ID。 */
    private Long id;
    /** 职位标题。 */
    private String jobTitle;
    /** 部门。 */
    private String departmentName;
    /** 阶段状态。 */
    private Integer status;
    /** 投递时间。 */
    private LocalDateTime createTime;
}
