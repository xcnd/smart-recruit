package com.smartrecruit.recruitment.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.response.PendingTaskVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作台待办任务服务接口。
 *
 * @since 1.0.0
 */
public interface WorkbenchTaskService {

    /**
     * 查询用户的待办任务。
     *
     * @param userId 用户ID，为null时返回所有
     * @param limit  最大条数
     * @return 待办列表
     */
    List<PendingTaskVO> getPendingTasks(Long userId, int limit);

    /**
     * 完成任务。
     *
     * @param id 任务ID
     */
    void completeTask(Long id);

    /**
     * 忽略任务。
     *
     * @param id 任务ID
     */
    void dismissTask(Long id);

    /**
     * 创建待办任务。
     *
     * @param userId        归属人ID
     * @param title         标题
     * @param description   描述
     * @param type          类型
     * @param priority      优先级
     * @param relatedType   关联业务类型
     * @param relatedId     关联业务ID
     * @param candidateName 候选人姓名
     * @param dueDate       截止日期
     */
    void createTask(Long userId, String title, String description, Integer type,
                    Integer priority, String relatedType, Long relatedId,
                    String candidateName, LocalDateTime dueDate);

    /** 分页查询待办任务。userId为null时不按用户过滤。 */
    PageResult<PendingTaskVO> pageQuery(int page, int size, Long userId, Integer type,
                                        Integer priority, Integer status, String keyword);
}
