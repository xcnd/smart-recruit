package com.smartrecruit.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HTTP 工具类。
 *
 * <p>提供基于 {@link HttpURLConnection} 的简单下载能力，
 * 供各业务服务在需要直接下载远程文件时统一复用。</p>
 *
 * @since 2026-04-12
 */
public final class HttpUtils {

    /** 默认连接超时（毫秒）。 */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;

    /** 默认读取超时（毫秒）。 */
    private static final int DEFAULT_READ_TIMEOUT_MS = 30_000;

    /** 下载缓冲区大小。 */
    private static final int BUFFER_SIZE = 8192;

    private HttpUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 从 URL 下载内容为字节数组（默认 10s 连接超时、30s 读取超时）。
     *
     * @param url 下载地址（http/https）
     * @return 下载内容字节数组
     * @throws IOException 连接失败、读取失败或响应非 2xx 时抛出
     */
    public static byte[] downloadBytes(String url) throws IOException {
        return downloadBytes(url, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
    }

    /**
     * 从 URL 下载内容为字节数组，支持自定义超时。
     *
     * @param url              下载地址（http/https）
     * @param connectTimeoutMs 连接超时（毫秒）
     * @param readTimeoutMs    读取超时（毫秒）
     * @return 下载内容字节数组
     * @throws IOException 连接失败、读取失败或响应非 2xx 时抛出
     */
    public static byte[] downloadBytes(String url, int connectTimeoutMs, int readTimeoutMs)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestMethod("GET");
        try {
            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("下载失败，HTTP 状态码: " + status);
            }
            try (InputStream is = conn.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[BUFFER_SIZE];
                int n;
                while ((n = is.read(buf)) != -1) {
                    bos.write(buf, 0, n);
                }
                return bos.toByteArray();
            }
        } finally {
            conn.disconnect();
        }
    }
}
