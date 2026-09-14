package com.smartrecruit.interview.config;

import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.interview.repository.InterviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时数据库预热。
 *
 * <p>提前建立数据库连接并完成 MyBatis 结果映射预热，避免第一个用户请求
 * 付出连接池初始化与 JDBC 冷启动开销（首次访问秒级变慢的主要原因之一）。</p>
 *
 * @since 2026-04-11
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DbWarmUpRunner implements ApplicationRunner {

    private final InterviewMapper interviewMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            long start = System.currentTimeMillis();
            // 预热最常访问的「当日面试日程」查询
            interviewMapper.selectScheduleByDate(DateUtils.today().toString());
            log.info("数据库预热完成: {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("数据库预热失败（可忽略，不影响服务启动）: {}", e.getMessage());
        }
    }
}
