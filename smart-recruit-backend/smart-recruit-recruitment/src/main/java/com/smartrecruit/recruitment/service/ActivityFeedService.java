package com.smartrecruit.recruitment.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.response.ActivityFeedVO;
import com.smartrecruit.recruitment.enums.RecruitmentEnums.ActivityType;

import java.util.List;

/**
 * 活动动态服务接口。
 *
 * @since 1.0.0
 */
public interface ActivityFeedService {

    /** 记录一条活动动态。 */
    void recordActivity(ActivityType type, String title, String description,
                        Long actorId, String actorName, String relatedType, Long relatedId);

    /** 查询最近N条活动动态。 */
    List<ActivityFeedVO> getRecentActivities(int limit);

    /** 分页查询活动动态。 */
    PageResult<ActivityFeedVO> pageQuery(int page, int size, Integer type, String keyword);
}
