package com.smartrecruit.interview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.interview.entity.Interview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 面试 Mapper，提供 CRUD 与分页筛选查询。
 *
 * @since 1.0.0
 */
@Mapper
public interface InterviewMapper extends BaseMapper<Interview> {

    /**
     * 分页查询，支持可选过滤条件：candidateId、jobPositionId、status、type。
     */
    IPage<Interview> selectPageWithFilters(Page<Interview> page, @Param("params") Map<String, Object> params);

    /**
     * 查询面试统计数据（总数、今日、已通过、已取消）。
     */
    Map<String, Object> selectStats();

    /**
     * 查询指定时间范围内（按创建时间）的面试统计数据。
     *
     * @param start 起始时间（含），可为 null
     * @param end   结束时间（不含），可为 null
     */
    Map<String, Object> selectStatsRange(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    /**
     * 查询指定日期的面试日程，按时段排序。
     */
    List<Interview> selectScheduleByDate(@Param("date") String date);

    /**
     * 查询日期范围内的面试日程（周视图），按时段排序。
     */
    List<Interview> selectScheduleByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 查询 AI 智能面试统计数据（含评估和通过率）。
     */
    Map<String, Object> selectAiStats();

    /** 跨库查询用户真实姓名。 */
    @org.apache.ibatis.annotations.Select("SELECT real_name FROM smart_recruit_system.sys_user WHERE id = #{id}")
    String selectUserRealName(@Param("id") Long id);

    /** 批量查询用户真实姓名（面试官姓名一次查回，避免 N+1）。 */
    List<Map<String, Object>> selectRealNamesByIds(@Param("ids") List<Long> ids);

    /** 根据姓名模糊查询用户 ID 列表。 */
    @org.apache.ibatis.annotations.Select("SELECT id FROM smart_recruit_system.sys_user WHERE real_name LIKE CONCAT('%', #{name}, '%')")
    List<Long> selectUserIdsByName(@Param("name") String name);

    /** 查询已评估的候选人列表（JOIN rec_interview + rec_interview_assessment）。 */
    List<Map<String, Object>> selectAssessedCandidates();

    /** 直接更新 evaluation JSON 字段（绕过 JacksonTypeHandler，确保可靠写入）。 */
    @org.apache.ibatis.annotations.Update("UPDATE rec_interview SET evaluation = #{evaluation} WHERE id = #{id} AND deleted = 0")
    int updateEvaluation(@Param("id") Long id, @Param("evaluation") String evaluation);

    /** 更新 AI 评估状态字段。 */
    @org.apache.ibatis.annotations.Update("UPDATE rec_interview SET evaluation_status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateEvaluationStatus(@Param("id") Long id, @Param("status") String status);

    /** 同时更新 evaluation JSON 和状态为 COMPLETED（异步评估完成时使用）。 */
    @org.apache.ibatis.annotations.Update("UPDATE rec_interview SET evaluation = #{evaluation}, evaluation_status = 'COMPLETED' WHERE id = #{id} AND deleted = 0")
    int updateEvaluationAndStatus(@Param("id") Long id, @Param("evaluation") String evaluation);

    /**
     * 查询候选人最近一次已完成的"终面"或"第7轮及以上"面试。
     * 返回 id, is_final_round, round, update_time，不存在返回 null。
     * 用于判断候选人是否已达到面试终点（终面 or 第7轮面试），1年内不可再安排。
     */
    @org.apache.ibatis.annotations.Select(
        "SELECT id, is_final_round, round, update_time " +
        "FROM rec_interview " +
        "WHERE candidate_id = #{candidateId} AND deleted = 0 AND status = 2 " +
        "  AND (is_final_round = 1 OR round >= 7) " +
        "ORDER BY update_time DESC LIMIT 1")
    Map<String, Object> getLatestEndedInterview(@Param("candidateId") Long candidateId);

    /**
     * 查询候选人一年内已有的面试轮次列表（排除已取消的）。
     * 用于轮次重复校验和自动建议下一轮。
     */
    @org.apache.ibatis.annotations.Select(
        "SELECT DISTINCT round FROM rec_interview " +
        "WHERE candidate_id = #{candidateId} AND deleted = 0 AND status != 3 " +
        "AND create_time >= #{oneYearAgo} ORDER BY round")
    List<Integer> selectExistingRounds(@Param("candidateId") Long candidateId,
                                        @Param("oneYearAgo") LocalDateTime oneYearAgo);
}
