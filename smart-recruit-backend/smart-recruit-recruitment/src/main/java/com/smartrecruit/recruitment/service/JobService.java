package com.smartrecruit.recruitment.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.request.CreateJobRequest;
import com.smartrecruit.recruitment.dto.request.AiGenerateJdRequest;
import com.smartrecruit.recruitment.dto.request.JobPageQuery;
import com.smartrecruit.recruitment.dto.request.UpdateJobRequest;
import com.smartrecruit.recruitment.dto.response.DepartmentJobStatVO;
import com.smartrecruit.recruitment.dto.response.JdGenerateVO;
import com.smartrecruit.recruitment.dto.response.JdGenerateTaskVO;
import com.smartrecruit.recruitment.dto.response.JobDetailVO;
import com.smartrecruit.recruitment.dto.response.JobStatsVO;
import com.smartrecruit.recruitment.dto.response.JobVO;
import com.smartrecruit.recruitment.dto.response.LevelJobStatVO;

import java.util.List;

/**
 * 职位管理的服务接口。
 *
 * @since 1.0.0
 */
public interface JobService {

    /**
     * 带筛选条件的职位分页查询。
     */
    PageResult<JobVO> pageQuery(JobPageQuery query);

    /**
     * 根据ID获取单个职位。
     */
    JobDetailVO getById(Long id);

    /**
     * 创建一个新职位（状态为草稿）。
     */
    JobVO create(CreateJobRequest request);

    /**
     * AI 生成职位描述（LLM 优先，模板兜底）。
     *
     * @param request 职位标题、部门、经验要求、地点与技能
     * @return 生成的职位描述文本
     */
    JdGenerateVO generateJd(AiGenerateJdRequest request);

    /**
     * 异步启动 AI 职位描述生成（LLM 耗时较长，避免同步阻塞请求）。
     *
     * @param request 职位标题、部门、经验要求、地点与技能
     * @return 任务 ID（前端轮询 {@link #getJdGenerateTask(String)} 获取结果）
     */
    String generateJdAsync(AiGenerateJdRequest request);

    /**
     * 查询异步 AI 职位描述生成任务状态与结果。
     *
     * @param taskId 任务 ID
     * @return 任务状态与生成结果
     */
    JdGenerateTaskVO getJdGenerateTask(String taskId);

    /**
     * 更新现有职位。
     */
    JobVO update(Long id, UpdateJobRequest request);

    /**
     * 软删除一个职位。
     */
    void delete(Long id);

    /**
     * 更新职位的状态。
     *
     * @param id     职位 ID
     * @param status 职位状态编码：0=草稿，1=已发布，2=暂停招聘，3=已关闭
     */
    void updateStatus(Long id, Integer status);

    /**
     * 按部门分组获取职位统计数据。
     */
    List<DepartmentJobStatVO> getStatsByDepartment();

    /**
     * 按经验等级分组获取职位统计数据。
     */
    List<LevelJobStatVO> getStatsByLevel();

    /**
     * 获取职位汇总统计（总数、已发布、草稿、已关闭）。
     */
    JobStatsVO getStats();
}
