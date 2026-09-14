package com.smartrecruit.common.config;

import com.smartrecruit.common.util.DateUtils;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置，包含分页、乐观锁、全表操作拦截保护以及审计字段自动填充。
 *
 * @since 1.0.0
 */
@Configuration
public class MyBatisPlusConfig {

    private static final Logger log = LoggerFactory.getLogger(MyBatisPlusConfig.class);

    // ================================================================
    // 拦截器链
    // ================================================================

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件（MySQL 方言）
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L); // 硬限制：每页最多 500 条
        pagination.setOverflow(true); // 溢出时跳转到第 1 页
        interceptor.addInnerInterceptor(pagination);

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 阻止全表删除/更新攻击
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        log.info("MyBatis-Plus 拦截器已配置: 分页(max=500), 乐观锁, 全表操作拦截");
        return interceptor;
    }

    // ================================================================
    // 全局配置自定义器
    // ================================================================

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setCacheEnabled(true);
            configuration.setLogPrefix("mybatis.");
            // 在生成的 SQL 中使用实际列名（而非别名）
            configuration.setUseGeneratedKeys(true);
            configuration.setDefaultStatementTimeout(30);
        };
    }

    // ================================================================
    // 自动填充元数据处理器（审计字段）
    // ================================================================

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = DateUtils.now();
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "deleted", Boolean.class, false);
                this.strictInsertFill(metaObject, "version", Integer.class, 1);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, DateUtils.now());
            }
        };
    }
}
