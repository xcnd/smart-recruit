package com.smartrecruit.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.response.PendingTaskVO;
import com.smartrecruit.recruitment.entity.WorkbenchTask;
import com.smartrecruit.recruitment.repository.WorkbenchTaskMapper;
import com.smartrecruit.recruitment.service.WorkbenchTaskService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link WorkbenchTaskService} 的实现类。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkbenchTaskServiceImpl implements WorkbenchTaskService {

    private final WorkbenchTaskMapper workbenchTaskMapper;

    /** 查询当前用户待办任务。 */
    @Override
    public List<PendingTaskVO> getPendingTasks(Long userId, int limit) {
        LambdaQueryWrapper<WorkbenchTask> wrapper = new LambdaQueryWrapper<WorkbenchTask>()
                .eq(WorkbenchTask::getStatus, 0);
        if (userId != null) {
            wrapper.and(w -> w.eq(WorkbenchTask::getUserId, userId).or().eq(WorkbenchTask::getUserId, 0L));
        }
        wrapper.orderByDesc(WorkbenchTask::getPriority)
                .orderByAsc(WorkbenchTask::getCreateTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        return workbenchTaskMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /** 完成任务。 */
    @Override
    @Transactional
    public void completeTask(Long id) {
        WorkbenchTask task = workbenchTaskMapper.selectById(id);
        if (task == null) {
            return;
        }
        task.setStatus(1);
        task.setCompletedAt(DateUtils.now());
        workbenchTaskMapper.updateById(task);
        log.info("Task completed: id={}, title={}", id, task.getTitle());
    }

    /** 忽略任务。 */
    @Override
    @Transactional
    public void dismissTask(Long id) {
        WorkbenchTask task = workbenchTaskMapper.selectById(id);
        if (task == null) {
            return;
        }
        task.setStatus(2);
        task.setCompletedAt(DateUtils.now());
        workbenchTaskMapper.updateById(task);
        log.info("Task dismissed: id={}, title={}", id, task.getTitle());
    }

    /**
     * 创建工作台待办任务。
     *
     * <p>单条 INSERT 本身具备原子性，不声明事务。
     * 该方法常被简历/候选人等业务在各自事务内作为「best-effort 副作用」调用，
     * 若此处声明 {@code @Transactional}，内层异常会把外层事务标记为 rollback-only，
     * 即使被调用方 try/catch 吞掉，外层提交时仍会抛出
     * {@code UnexpectedRollbackException}。</p>
     */
    @Override
    public void createTask(Long userId, String title, String description, Integer type,
                           Integer priority, String relatedType, Long relatedId,
                           String candidateName, LocalDateTime dueDate) {
        WorkbenchTask task = new WorkbenchTask();
        task.setUserId(userId != null ? userId : 0L);
        task.setTitle(title);
        task.setDescription(description);
        task.setType(type != null ? type : 5);
        task.setPriority(priority != null ? priority : 1);
        task.setRelatedType(relatedType);
        task.setRelatedId(relatedId);
        task.setCandidateName(candidateName);
        task.setDueDate(dueDate);
        task.setStatus(0);
        workbenchTaskMapper.insert(task);
        log.info("Task created: id={}, title={}, userId={}", task.getId(), title, userId);
    }

    @Override
    public PageResult<PendingTaskVO> pageQuery(int page, int size, Long userId,
                                               Integer type, Integer priority,
                                               Integer status, String keyword) {
        LambdaQueryWrapper<WorkbenchTask> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.and(w -> w.eq(WorkbenchTask::getUserId, userId).or().eq(WorkbenchTask::getUserId, 0L));
        }
        if (type != null) {
            wrapper.eq(WorkbenchTask::getType, type);
        }
        if (priority != null) {
            wrapper.eq(WorkbenchTask::getPriority, priority);
        }
        if (status != null) {
            wrapper.eq(WorkbenchTask::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(WorkbenchTask::getTitle, keyword)
                    .or().like(WorkbenchTask::getCandidateName, keyword)
                    .or().like(WorkbenchTask::getDescription, keyword));
        }
        wrapper.orderByDesc(WorkbenchTask::getPriority)
                .orderByAsc(WorkbenchTask::getCreateTime);

        long total = workbenchTaskMapper.selectCount(wrapper);
        int offset = (page - 1) * size;
        List<WorkbenchTask> records = workbenchTaskMapper.selectList(
                wrapper.last("LIMIT " + size + " OFFSET " + offset));
        List<PendingTaskVO> vos = records.stream().map(this::toVO).collect(Collectors.toList());
        long pages = total == 0 ? 0 : (total + size - 1) / size;
        return new PageResult<>(vos, total, size, page, pages);
    }

    private PendingTaskVO toVO(WorkbenchTask entity) {
        return PendingTaskVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .type(entity.getType())
                .priority(entity.getPriority())
                .relatedType(entity.getRelatedType())
                .relatedId(entity.getRelatedId())
                .candidateName(entity.getCandidateName())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .build();
    }
}
