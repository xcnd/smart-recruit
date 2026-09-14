package com.smartrecruit.system.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.time.Duration;

/**
 * RustFS S3 客户端配置。
 * 当 file.storage.type=rustfs 时创建 S3Client 指向 RustFS 服务。
 *
 * @since 2026-05-09
 */
@Configuration
@ConditionalOnProperty(name = "file.storage.type", havingValue = "rustfs")
public class RustFsS3Config {

    private final FileStorageProperties storageProperties;

    public RustFsS3Config(FileStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Bean
    public S3Client s3Client() {
        FileStorageProperties.Rustfs config = storageProperties.getRustfs();

        String protocol = config.isSecure() ? "https" : "http";
        URI endpointUri = URI.create(protocol + "://" + config.getEndpoint() + ":" + config.getPort());

        Duration connectTimeout = Duration.ofMillis(config.getConnectTimeout());
        Duration readTimeout = Duration.ofMillis(config.getReadTimeout());
        Duration writeTimeout = Duration.ofMillis(config.getWriteTimeout());

        S3Configuration s3Config = S3Configuration.builder()
                .checksumValidationEnabled(false)
                .chunkedEncodingEnabled(true)
                .build();

        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(readTimeout.plus(writeTimeout))
                .apiCallAttemptTimeout(readTimeout)
                .build();

        return S3Client.builder()
                .endpointOverride(endpointUri)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())))
                .region(Region.of("us-east-1"))
                .serviceConfiguration(s3Config)
                .overrideConfiguration(overrideConfig)
                .build();
    }
}
