package com.smartrecruit.common.util;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式 ID 生成器，灵感来源于 Twitter Snowflake 算法。
 *
 * <p>结构（64 位）：</p>
 * <pre>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
 * |                         时间戳                        | 数据中心  | 工作节点 | 序列号
 * </pre>
 *
 * @since 1.0.0
 */
public final class IdGenerator {

    /** 自定义起始时间：2024-01-01 00:00:00 UTC（毫秒） */
    private static final long EPOCH = 1704067200000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;

    private final AtomicLong sequence = new AtomicLong(0L);
    private volatile long lastTimestamp = -1L;

    /** 单例实例持有者。 */
    private static final class Holder {
        static final IdGenerator INSTANCE = new IdGenerator(getDefaultWorkerId(), getDefaultDatacenterId());
    }

    public IdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    "workerId 必须在 0 到 " + MAX_WORKER_ID + " 之间");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    "datacenterId 必须在 0 到 " + MAX_DATACENTER_ID + " 之间");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 返回单例实例。
     */
    public static IdGenerator getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 生成下一个唯一 ID。
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "时钟回拨。拒绝生成 ID，已回拨 " + (lastTimestamp - timestamp) + " 毫秒");
        }

        if (timestamp == lastTimestamp) {
            long seq = sequence.incrementAndGet() & MAX_SEQUENCE;
            if (seq == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence.set(0L);
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence.get();
    }

    /**
     * 生成字符串类型的 ID（适用于偏好字符串 ID 的场景）。
     */
    public String nextStringId() {
        return String.valueOf(nextId());
    }

    /**
     * 从 Snowflake ID 中提取时间戳。
     */
    public static long extractTimestamp(long id) {
        return (id >> TIMESTAMP_LEFT_SHIFT) + EPOCH;
    }

    // ================================================================
    // 私有辅助方法
    // ================================================================

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    private static long currentTimeMillis() {
        return DateUtils.currentEpochMillis();
    }

    private static long getDefaultWorkerId() {
        long workerId;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        sb.append(String.format("%02x", b));
                    }
                }
            }
            workerId = (long) (sb.toString().hashCode() & 0x1F); // 取 5 位
        } catch (SocketException e) {
            workerId = new SecureRandom().nextInt(32);
        }
        return workerId;
    }

    private static long getDefaultDatacenterId() {
        return 1L;
    }
}
