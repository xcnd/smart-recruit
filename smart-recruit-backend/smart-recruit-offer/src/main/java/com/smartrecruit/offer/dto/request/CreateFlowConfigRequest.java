package com.smartrecruit.offer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建审批流程配置请求。
 *
 * @since 1.0.0
 */
@Data
public class CreateFlowConfigRequest {

    /** 流程名称。 */
    @NotBlank(message = "流程名称不能为空")
    private String flowName;

    /** 适用部门名称。 */
    @NotBlank(message = "部门名称不能为空")
    private String departmentName;

    /** 审批节点列表。 */
    @NotEmpty(message = "审批节点不能为空")
    private List<NodeConfig> nodes;

    /** 流程说明。 */
    private String description;

    /**
     * 审批节点配置。
     */
    @Data
    public static class NodeConfig {
        /** 审批级别。 */
        @NotNull(message = "审批级别不能为空")
        private Integer level;

        /** 节点名称。 */
        @NotBlank(message = "节点名称不能为空")
        private String nodeName;

        /** 审批人列表。 */
        @NotEmpty(message = "审批人不能为空")
        private List<ApproverConfig> approvers;
    }

    /**
     * 审批人配置。
     */
    @Data
    public static class ApproverConfig {
        /** 审批人ID。 */
        @NotNull(message = "审批人ID不能为空")
        private Long approverId;

        /** 审批人姓名。 */
        @NotBlank(message = "审批人姓名不能为空")
        private String approverName;

        /** 审批人角色。 */
        private String approverRole;
    }
}
