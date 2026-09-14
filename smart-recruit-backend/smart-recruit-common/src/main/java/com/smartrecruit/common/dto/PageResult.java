package com.smartrecruit.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 通用分页结果 DTO。
 *
 * <p>用于所有支持分页的列表接口。</p>
 *
 * @param <T> 分页中的条目类型
 * @since 1.0.0
 */
@JsonPropertyOrder({"records", "total", "size", "current", "pages"})
public record PageResult<T>(
        @JsonProperty("records")
        List<T> records,

        @JsonProperty("total")
        long total,

        @JsonProperty("size")
        long size,

        @JsonProperty("current")
        long current,

        @JsonProperty("pages")
        long pages
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public PageResult {
        records = records != null ? List.copyOf(records) : Collections.emptyList();
        if (size < 1) size = 10;
        if (current < 1) current = 1;
        if (total < 0) total = 0;
        // 根据 total 和 size 计算总页数
        pages = total == 0 ? 0 : (total + size - 1) / size;
    }

    /**
     * 从 MyBatis-Plus 的 IPage 创建 PageResult。
     */
    public static <T> PageResult<T> from(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                page.getSize(),
                page.getCurrent(),
                page.getPages()
        );
    }

    /**
     * 从 Spring Data 的 Page 创建 PageResult。
     */
    public static <T> PageResult<T> from(org.springframework.data.domain.Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber() + 1,
                page.getTotalPages()
        );
    }

    /**
     * 创建一个空的分页结果。
     */
    public static <T> PageResult<T> empty(long size, long current) {
        return new PageResult<>(Collections.emptyList(), 0, size, current, 0);
    }

    /**
     * 手动创建 PageResult。
     */
    public static <T> PageResult<T> of(List<T> records, long total, long size, long current) {
        return new PageResult<>(records, total, size, current, 0);
    }

    /**
     * 使用映射函数转换分页记录。
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = records.stream().map(mapper).collect(java.util.stream.Collectors.toList());
        return new PageResult<>(mapped, total, size, current, pages);
    }

    /**
     * 返回本页是否包含任何记录。
     */
    public boolean isEmpty() {
        return records.isEmpty();
    }

    /**
     * 返回本页之后是否还有更多页。
     */
    public boolean hasNext() {
        return current < pages;
    }

    /**
     * 返回本页之前是否还有前页。
     */
    public boolean hasPrevious() {
        return current > 1;
    }
}
