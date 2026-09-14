package com.smartrecruit.common.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis-Plus 装配兜底（Spring Boot 4 兼容）。
 *
 * <p>当前使用的 {@code mybatis-plus-spring-boot3-starter:3.5.16} 面向 Spring Boot 3，
 * 在 Spring Boot 4 下其自动配置（{@code MybatisPlusAutoConfiguration}）可能不会被触发，
 * 导致 {@code SqlSessionFactory} 缺失、Mapper 注入失败。本配置仅在容器中不存在
 * {@code SqlSessionFactory} 时提供等价装配：数据源 + 全局配置 + 拦截器 + 常用 Mapper XML。</p>
 *
 * <p>如果未来升级到兼容 Boot 4 的 MyBatis-Plus 版本，自动配置恢复后本类会因
 * {@code @ConditionalOnMissingBean} 自动失效，无需删除。</p>
 *
 * @since 2026-04-07
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(SqlSessionFactory.class)
public class MyBatisPlusAutoConfigBridge {

    @Bean
    public SqlSessionFactory sqlSessionFactory(
            DataSource dataSource,
            @Qualifier("mybatisPlusInterceptor") MybatisPlusInterceptor mybatisPlusInterceptor,
            ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider)
            throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:/mapper/**/*.xml"));

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);

        // 复刻 application-common.yml 中的 global-config.db-config
        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(IdType.ASSIGN_ID);
        dbConfig.setLogicDeleteField("deleted");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);
        // 注册审计字段自动填充处理器，否则 @TableField(fill = INSERT/INSERT_UPDATE)
        // 的 createTime/updateTime 等字段不会被填充，插入时列值为 null 触发约束异常
        MetaObjectHandler metaObjectHandler = metaObjectHandlerProvider.getIfAvailable();
        if (metaObjectHandler != null) {
            globalConfig.setMetaObjectHandler(metaObjectHandler);
        }
        factory.setGlobalConfig(globalConfig);

        if (mybatisPlusInterceptor != null) {
            factory.setPlugins(mybatisPlusInterceptor);
        }
        return factory.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
