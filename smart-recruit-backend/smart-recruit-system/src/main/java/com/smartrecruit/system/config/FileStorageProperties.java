package com.smartrecruit.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RustFS 文件存储配置属性。
 *
 * @since 2026-05-09
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /** 存储类型：rustfs */
    private String type = "rustfs";

    private Rustfs rustfs = new Rustfs();

    @Data
    public static class Rustfs {
        private String endpoint = "117.72.88.11";
        private int port = 9091;
        private String accessKey;
        private String secretKey;
        private boolean secure = false;
        private String bucketName = "knowledge-dev";
        private long connectTimeout = 30000;
        private long readTimeout = 60000;
        private long writeTimeout = 60000;
    }
}
