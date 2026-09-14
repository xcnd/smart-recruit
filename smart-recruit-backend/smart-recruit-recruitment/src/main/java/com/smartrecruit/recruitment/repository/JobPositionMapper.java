package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.recruitment.dto.response.DepartmentJobStatVO;
import com.smartrecruit.recruitment.dto.response.DepartmentProgressVO;
import com.smartrecruit.recruitment.dto.response.LevelJobStatVO;
import com.smartrecruit.recruitment.entity.JobPosition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code rec_job_position} 表的 Mapper 接口。
 *
 * @since 1.0.0
 */
@Mapper
public interface JobPositionMapper extends BaseMapper<JobPosition> {

    /**
     * 分页查询，支持动态过滤条件。
     */
    IPage<JobPosition> pageQuery(Page<JobPosition> page,
                                 @Param("title") String title,
                                 @Param("departmentId") Long departmentId,
                                 @Param("status") String status,
                                 @Param("type") String type,
                                 @Param("location") String location,
                                 @Param("startDate") String startDate,
                                 @Param("endDate") String endDate);

    /**
     * 按部门聚合职位统计数据。
     */
    List<DepartmentJobStatVO> statsByDepartment();

    /**
     * 按经验水平聚合职位统计数据。
     */
    List<LevelJobStatVO> statsByLevel();

    /**
     * 按部门聚合招聘进度（含已入职人数）。
     */
    List<DepartmentProgressVO> departmentProgress();
}
