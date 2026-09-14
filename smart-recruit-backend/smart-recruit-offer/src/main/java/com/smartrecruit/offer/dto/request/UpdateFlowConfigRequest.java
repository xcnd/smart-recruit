package com.smartrecruit.offer.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 更新审批流程配置请求（所有字段可选）。
 *
 * @since 1.0.0
 */
@Data
public class UpdateFlowConfigRequest {

    /** 流程名称。 */
    private String flowName;

    /** 适用部门名称。 */
    private String departmentName;

    /** 审批节点列表。 */
    private List<CreateFlowConfigRequest.NodeConfig> nodes;

    /** 流程说明。 */
    private String description;
}
