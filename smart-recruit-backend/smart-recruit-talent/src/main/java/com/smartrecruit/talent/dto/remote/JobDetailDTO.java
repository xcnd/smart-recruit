package com.smartrecruit.talent.dto.remote;

import lombok.Data;

import java.util.List;

/**
 * 招聘服务岗位详情 DTO。
 *
 * <p>包含智能匹配所需的核心字段。</p>
 *
 * @since 2.0.0
 */
@Data
public class JobDetailDTO {
    private Long id;
    private String title;
    /** 经验级别：0=ENTRY, 1=JUNIOR, 2=MID, 3=SENIOR, 4=LEAD */
    private Integer level;
    private String location;
    private List<String> skills;
    /** 学历要求：0=高中, 1=大专, 2=本科, 3=硕士, 4=博士 */
    private Integer educationRequired;
}
