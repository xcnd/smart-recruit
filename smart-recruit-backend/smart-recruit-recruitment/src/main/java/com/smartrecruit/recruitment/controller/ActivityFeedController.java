package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.response.ActivityFeedVO;
import com.smartrecruit.recruitment.enums.RecruitmentEnums.ActivityType;
import com.smartrecruit.recruitment.service.ActivityFeedService;
import com.smartrecruit.recruitment.dto.request.RecordActivityRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 活动动态控制器，提供最近动态查询和跨模块内部写入接口。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ActivityFeedController {

    private final ActivityFeedService activityFeedService;

    /** 查询最近N条活动动态。 */
    @GetMapping("/activities/recent")
    public ApiResponse<List<ActivityFeedVO>> getRecent(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(activityFeedService.getRecentActivities(Math.min(limit, 200)));
    }

    /** 分页查询活动动态（带过滤条件）。 */
    @GetMapping("/activities")
    public ApiResponse<PageResult<ActivityFeedVO>> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(activityFeedService.pageQuery(page, Math.min(size, 100), type, keyword));
    }

    /** 内部接口：写入活动动态。调用方：Interview/Offer 模块的 Feign 客户端。 */
    @PostMapping("/internal/activity-feed")
    public ApiResponse<Void> recordActivity(@RequestBody RecordActivityRequest request) {
        ActivityType type = request.getType() != null
                ? ActivityType.fromCode(request.getType()) : null;
        if (type == null) {
            type = ActivityType.SYSTEM;
        }
        String title = request.getTitle() != null ? request.getTitle() : "";
        String description = request.getDescription() != null ? request.getDescription() : "";
        Long actorId = request.getActorId() != null ? request.getActorId() : 0L;
        String actorName = request.getActorName() != null ? request.getActorName() : "system";
        String relatedType = request.getRelatedType();
        Long relatedId = request.getRelatedId();

        activityFeedService.recordActivity(type, title, description,
                actorId, actorName, relatedType, relatedId);
        return ApiResponse.success(null);
    }
}
