package com.smartrecruit.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.recruitment.dto.response.ActivityFeedVO;
import com.smartrecruit.recruitment.entity.ActivityFeed;
import com.smartrecruit.recruitment.enums.RecruitmentEnums.ActivityType;
import com.smartrecruit.recruitment.repository.ActivityFeedMapper;
import com.smartrecruit.recruitment.service.ActivityFeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link ActivityFeedService} 的实现类。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityFeedServiceImpl implements ActivityFeedService {

    private final ActivityFeedMapper activityFeedMapper;

    @Override
    public void recordActivity(ActivityType type, String title, String description,
                               Long actorId, String actorName, String relatedType, Long relatedId) {
        ActivityFeed feed = new ActivityFeed();
        feed.setType(type.getCode());
        feed.setTitle(title);
        feed.setDescription(description);
        feed.setActorId(actorId != null ? actorId : 0L);
        feed.setActorName(actorName != null ? actorName : "system");
        feed.setRelatedType(relatedType);
        feed.setRelatedId(relatedId);
        activityFeedMapper.insert(feed);
        log.info("Activity recorded: type={}, title={}, relatedType={}, relatedId={}",
                type.getLabel(), title, relatedType, relatedId);
    }

    /** 查询最近动态列表。 */
    @Override
    public List<ActivityFeedVO> getRecentActivities(int limit) {
        List<ActivityFeed> feeds = activityFeedMapper.selectList(
                new LambdaQueryWrapper<ActivityFeed>()
                        .orderByDesc(ActivityFeed::getCreateTime)
                        .last("LIMIT " + Math.min(limit, 200)));
        return feeds.stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<ActivityFeedVO> pageQuery(int page, int size, Integer type, String keyword) {
        LambdaQueryWrapper<ActivityFeed> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(ActivityFeed::getType, type);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(ActivityFeed::getTitle, keyword)
                    .or().like(ActivityFeed::getDescription, keyword));
        }
        wrapper.orderByDesc(ActivityFeed::getCreateTime);

        long total = activityFeedMapper.selectCount(wrapper);
        int offset = (page - 1) * size;
        List<ActivityFeed> records = activityFeedMapper.selectList(
                wrapper.last("LIMIT " + size + " OFFSET " + offset));
        List<ActivityFeedVO> vos = records.stream().map(this::toVO).collect(Collectors.toList());
        long pages = total == 0 ? 0 : (total + size - 1) / size;
        return new PageResult<>(vos, total, size, page, pages);
    }

    private ActivityFeedVO toVO(ActivityFeed entity) {
        ActivityType type = ActivityType.fromCode(entity.getType());
        return ActivityFeedVO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .typeLabel(type != null ? type.getLabel() : String.valueOf(entity.getType()))
                .title(entity.getTitle())
                .description(entity.getDescription())
                .actorId(entity.getActorId())
                .actorName(entity.getActorName())
                .relatedType(entity.getRelatedType())
                .relatedId(entity.getRelatedId())
                .createTime(entity.getCreateTime())
                .build();
    }
}
