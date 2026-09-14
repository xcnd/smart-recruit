package com.smartrecruit.offer.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批流程配置视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowConfigVO {

    /** 配置 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 流程名称。 */
    private String flowName;

    /** 适用部门名称。 */
    private String departmentName;

    /** 是否启用。 */
    private Integer isActive;

    /** 审批层级总数。 */
    private Integer maxLevels;

    /** 审批节点列表。 */
    private List<FlowNodeVO> nodes;

    /** 流程说明。 */
    private String description;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /**
     * 审批节点VO。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlowNodeVO {
        private Integer level;
        private String nodeName;
        private List<ApproverVO> approvers;
    }

    /**
     * 审批人VO。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproverVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long approverId;
        private String approverName;
        private String approverRole;
    }
}
