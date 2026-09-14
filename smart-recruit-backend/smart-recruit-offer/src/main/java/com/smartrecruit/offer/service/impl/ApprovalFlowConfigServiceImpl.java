package com.smartrecruit.offer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.exception.BusinessException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.util.UserContextUtil;
import com.smartrecruit.offer.dto.request.CreateFlowConfigRequest;
import com.smartrecruit.offer.dto.request.UpdateFlowConfigRequest;
import com.smartrecruit.offer.dto.response.ApprovalFlowConfigVO;
import com.smartrecruit.offer.entity.ApprovalFlowConfig;
import com.smartrecruit.offer.repository.ApprovalFlowConfigMapper;
import com.smartrecruit.offer.service.ApprovalFlowConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 审批流程配置服务实现。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalFlowConfigServiceImpl implements ApprovalFlowConfigService {

    private final ApprovalFlowConfigMapper configMapper;

    /** 查询全部记录。 */
    @Override
    public List<ApprovalFlowConfigVO> listAll() {
        return configMapper.selectList(new LambdaQueryWrapper<ApprovalFlowConfig>()
                        .orderByAsc(ApprovalFlowConfig::getDepartmentName))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /** 根据主键查询详情。 */
    @Override
    public ApprovalFlowConfigVO getById(Long id) {
        ApprovalFlowConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new ResourceNotFoundException("审批流程配置", id);
        }
        return toVO(config);
    }

    /** 按部门名称查询审批流程配置。 */
    @Override
    public ApprovalFlowConfigVO getByDepartment(String departmentName) {
        ApprovalFlowConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<ApprovalFlowConfig>()
                        .eq(ApprovalFlowConfig::getDepartmentName, departmentName)
                        .eq(ApprovalFlowConfig::getIsActive, 1));
        return config != null ? toVO(config) : null;
    }

    /** 创建记录。 */
    @Override
    @Transactional
    public ApprovalFlowConfigVO create(CreateFlowConfigRequest request) {
        // 检查部门唯一性
        Long count = configMapper.selectCount(
                new LambdaQueryWrapper<ApprovalFlowConfig>()
                        .eq(ApprovalFlowConfig::getDepartmentName, request.getDepartmentName()));
        if (count > 0) {
            throw new BusinessException("FLOW_CONFIG_DUPLICATE",
                    "部门 '" + request.getDepartmentName() + "' 已存在审批流程配置");
        }

        ApprovalFlowConfig config = new ApprovalFlowConfig();
        config.setFlowName(request.getFlowName());
        config.setDepartmentName(request.getDepartmentName());
        config.setIsActive(1);
        config.setMaxLevels(request.getNodes().size());
        config.setNodes(request.getNodes());
        config.setDescription(request.getDescription());
        config.setCreateUserId(currentUserId());
        config.setCreateBy(currentUsername());
        config.setUpdateUserId(currentUserId());
        config.setUpdateBy(currentUsername());

        configMapper.insert(config);
        log.info("审批流程配置已创建: id={}, department={}, levels={}",
                config.getId(), config.getDepartmentName(), config.getMaxLevels());

        return toVO(config);
    }

    /** 更新记录。 */
    @Override
    @Transactional
    public ApprovalFlowConfigVO update(Long id, UpdateFlowConfigRequest request) {
        ApprovalFlowConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new ResourceNotFoundException("审批流程配置", id);
        }

        if (request.getFlowName() != null) {
            config.setFlowName(request.getFlowName());
        }
        if (request.getDepartmentName() != null) {
            // 检查新部门名是否与其他记录冲突
            Long count = configMapper.selectCount(
                    new LambdaQueryWrapper<ApprovalFlowConfig>()
                            .eq(ApprovalFlowConfig::getDepartmentName, request.getDepartmentName())
                            .ne(ApprovalFlowConfig::getId, id));
            if (count > 0) {
                throw new BusinessException("FLOW_CONFIG_DUPLICATE",
                        "部门 '" + request.getDepartmentName() + "' 已存在审批流程配置");
            }
            config.setDepartmentName(request.getDepartmentName());
        }
        if (request.getNodes() != null && !request.getNodes().isEmpty()) {
            config.setNodes(request.getNodes());
            config.setMaxLevels(request.getNodes().size());
        }
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }

        config.setUpdateUserId(currentUserId());
        config.setUpdateBy(currentUsername());
        configMapper.updateById(config);
        log.info("审批流程配置已更新: id={}", id);

        return toVO(config);
    }

    /** 根据主键删除记录。 */
    @Override
    @Transactional
    public void delete(Long id) {
        ApprovalFlowConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new ResourceNotFoundException("审批流程配置", id);
        }
        configMapper.deleteById(id);
        log.info("审批流程配置已删除: id={}, department={}", id, config.getDepartmentName());
    }

    /** 启用/停用配置。 */
    @Override
    @Transactional
    public void toggleActive(Long id) {
        ApprovalFlowConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new ResourceNotFoundException("审批流程配置", id);
        }
        config.setIsActive(config.getIsActive() == 1 ? 0 : 1);
        config.setUpdateUserId(currentUserId());
        config.setUpdateBy(currentUsername());
        configMapper.updateById(config);
        log.info("审批流程状态已切换: id={}, isActive={}", id, config.getIsActive());
    }

    // ---- 私有辅助 ----

    @SuppressWarnings("unchecked")
    private ApprovalFlowConfigVO toVO(ApprovalFlowConfig config) {
        List<ApprovalFlowConfigVO.FlowNodeVO> nodeVOs = new ArrayList<>();
        if (config.getNodes() instanceof List<?> nodes) {
            for (Object node : nodes) {
                if (node instanceof Map<?, ?> nodeMap) {
                    List<ApprovalFlowConfigVO.ApproverVO> approverVOs = new ArrayList<>();
                    Object approversObj = nodeMap.get("approvers");
                    if (approversObj instanceof List<?> approvers) {
                        for (Object a : approvers) {
                            if (a instanceof Map<?, ?> am) {
                                approverVOs.add(ApprovalFlowConfigVO.ApproverVO.builder()
                                        .approverId(toLong(am.get("approverId")))
                                        .approverName((String) am.get("approverName"))
                                        .approverRole((String) am.get("approverRole"))
                                        .build());
                            }
                        }
                    }
                    nodeVOs.add(ApprovalFlowConfigVO.FlowNodeVO.builder()
                            .level(toInt(nodeMap.get("level")))
                            .nodeName((String) nodeMap.get("nodeName"))
                            .approvers(approverVOs)
                            .build());
                }
            }
        }

        return ApprovalFlowConfigVO.builder()
                .id(config.getId())
                .flowName(config.getFlowName())
                .departmentName(config.getDepartmentName())
                .isActive(config.getIsActive())
                .maxLevels(config.getMaxLevels())
                .nodes(nodeVOs)
                .description(config.getDescription())
                .createTime(config.getCreateTime())
                .updateTime(config.getUpdateTime())
                .build();
    }

    private Long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) return Long.parseLong(s);
        return null;
    }

    private Integer toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) return Integer.parseInt(s);
        return null;
    }

    /** 当前登录用户 ID（内部调用或未登录时为 null）。 */
    private Long currentUserId() {
        return UserContextUtil.getCurrentUserId();
    }

    /** 当前登录用户名（内部调用时为 system）。 */
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String p
                && !p.isBlank() && !"anonymousUser".equals(p)) {
            return p;
        }
        return null;
    }
}
