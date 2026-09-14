package com.smartrecruit.offer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * <p>文件存储已迁移至系统服务的 RustFS，不再需要本地静态资源映射。</p>
 *
 * @since 1.1.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
}
