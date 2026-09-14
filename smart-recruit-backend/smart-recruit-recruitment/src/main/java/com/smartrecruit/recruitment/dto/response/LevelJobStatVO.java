package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 按经验级别分组的聚合职位统计。
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LevelJobStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 经验级别：应届生、初级、中级、高级、资深/Lead、高管。 */
    private String level;

    /** 该级别的职位总数。 */
    private long jobCount;

    /** 该级别的开放招聘人数。 */
    private long totalHeadCount;
}
