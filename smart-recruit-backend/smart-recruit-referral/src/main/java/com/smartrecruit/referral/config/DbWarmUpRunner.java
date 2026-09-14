package com.smartrecruit.referral.config;

import com.smartrecruit.referral.repository.ReferralProgramMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时数据库预热。
 *
 * <p>HikariCP 连接池是懒初始化的，首次用户请求会付出连接建立、JDBC/MyBatis
 * 初始化的开销，导致「第一次访问很慢」。本组件在应用启动后立即执行一次
 * 轻量查询，提前建立连接并完成 Mapper/结果映射预热，让首个用户请求不再卡顿。</p>
 *
 * @since 2026-04-11
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DbWarmUpRunner implements ApplicationRunner {

    private final ReferralProgramMapper programMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            long start = System.currentTimeMillis();
            // 预热最常访问的「启用内推计划」查询，同时建立连接池连接
            programMapper.selectEnabledPrograms();
            log.info("数据库预热完成: {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("数据库预热失败（可忽略，不影响服务启动）: {}", e.getMessage());
        }
    }
}
