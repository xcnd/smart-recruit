package com.smartrecruit.recruitment.exception;

import java.io.Serial;

/**
 * 简历文件解析失败时抛出的运行时异常。
 *
 * @since 1.0.0
 */
public class FileParseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FileParseException(String message) {
        super(message);
    }

    public FileParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
