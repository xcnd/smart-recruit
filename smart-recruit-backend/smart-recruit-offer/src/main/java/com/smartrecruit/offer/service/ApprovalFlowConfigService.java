package com.smartrecruit.offer.service;

import com.smartrecruit.offer.dto.request.CreateFlowConfigRequest;
import com.smartrecruit.offer.dto.request.UpdateFlowConfigRequest;
import com.smartrecruit.offer.dto.response.ApprovalFlowConfigVO;

import java.util.List;

/**
 * 审批流程配置服务接口。
 *
 * @since 1.0.0
 */
public interface ApprovalFlowConfigService {

    /** 查询所有流程配置。 */
    List<ApprovalFlowConfigVO> listAll();

    /** 根据 ID 查询。 */
    ApprovalFlowConfigVO getById(Long id);

    /** 根据部门名称查询流程配置。 */
    ApprovalFlowConfigVO getByDepartment(String departmentName);

    /** 创建流程配置。 */
    ApprovalFlowConfigVO create(CreateFlowConfigRequest request);

    /** 更新流程配置。 */
    ApprovalFlowConfigVO update(Long id, UpdateFlowConfigRequest request);

    /** 删除流程配置。 */
    void delete(Long id);

    /** 切换启用/停用状态。 */
    void toggleActive(Long id);
}
