package com.smartrecruit.referral.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 内推记录展示的VO。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
public class ReferralRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 内推记录 ID。 */
    private Long id;
    /** 内推计划 ID。 */
    private Long programId;
    /** 计划-职位关联ID。 */
    private Long programJobId;
    /** 推荐人 ID。 */
    private Long referrerId;
    /** 候选人 ID。 */
    private Long candidateId;
    /** 职位 ID。 */
    private Long jobPositionId;
    /** 推荐人与候选人关系。 */
    private String relationship;
    /** 推荐备注。 */
    private String referralNote;
    /** 简历文件URL。 */
    private String resumeUrl;
    /** 状态：PENDING/SCREENING/INTERVIEWING/OFFERED/HIRED/REJECTED。 */
    private Integer status;
    /** 奖金发放状态。 */
    private Integer bonusStatus;
    /** 总奖金金额。 */
    private BigDecimal bonusAmount;
    /** 已发放奖金。 */
    private BigDecimal bonusPaid;
    /** 入职时间。 */
    private LocalDateTime hiredTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
    /** 分阶段奖金发放记录。 */
    private List<BonusRecordVO> bonusRecords;
    /** 候选人姓名。 */
    private String candidateName;
    /** 候选人用户名。 */
    private String candidateUsername;
    /** 目标职位名称。 */
    private String jobTitle;
    /** 目标职位所属部门。 */
    private String departmentName;
    /** 计划关联职位名称。 */
    private String programJobTitle;
}
