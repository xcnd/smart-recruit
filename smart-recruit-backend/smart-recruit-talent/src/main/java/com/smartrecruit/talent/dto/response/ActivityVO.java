package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 最近动态消息项。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityVO {

    /** 动态ID。 */
    private Long id;

    /** 动态类型：面试完成、Offer已发、候选人申请等。 */
    private Integer type;

    /** 用于展示的动态标题。 */
    private String title;

    /** 动态描述。 */
    private String description;

    /** 相关候选人姓名。 */
    private String candidateName;

    /** 相关职位名称。 */
    private String jobTitle;

    /** 动态创建时间。 */
    private LocalDateTime createdAt;

    /** 关联业务类型：CANDIDATE, JOB, INTERVIEW, OFFER, ONBOARDING。 */
    private String relatedType;

    /** 关联业务ID。 */
    private Long relatedId;
}
