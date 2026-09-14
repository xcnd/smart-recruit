package com.smartrecruit.recruitment.service.impl;

import com.smartrecruit.recruitment.exception.FileParseException;
import com.smartrecruit.recruitment.parser.ResumeDocumentParser;
import com.smartrecruit.recruitment.service.DocumentParserService;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文档解析服务门面 — 根据文件类型委托给对应的 {@link ResumeDocumentParser} 实现。
 *
 * <p>支持的解析器由 Spring 自动注入（{@code List<ResumeDocumentParser>}）：
 * <ul>
 *   <li>{@link com.smartrecruit.recruitment.parser.PdfResumeParser} — PDF 多策略解析</li>
 *   <li>{@link com.smartrecruit.recruitment.parser.WordResumeParser} — Word 多策略解析</li>
 * </ul>
 *
 * <p>新增文件格式只需实现 {@link ResumeDocumentParser} 并注册为 {@code @Component} 即可，
 * 无需修改本类。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class DocumentParserServiceImpl implements DocumentParserService {

    private static final int MAX_CHARACTERS = 15000;
    private static final Tika TIKA_DETECTOR = new Tika();

    private final List<ResumeDocumentParser> parsers;

    public DocumentParserServiceImpl(List<ResumeDocumentParser> parsers) {
        this.parsers = parsers;
        log.info("已加载 {} 个文档解析器: {}", parsers.size(),
                parsers.stream().map(p -> p.getClass().getSimpleName()).toList());
    }

    // ─── MultipartFile 适配 ───

    /** 从文件字节流中提取纯文本内容。 */
    @Override
    public String extractText(MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("上传文件为空，返回空文本");
            return "";
        }
        try {
            byte[] bytes = file.getBytes();
            return doExtractText(bytes, file.getOriginalFilename());
        } catch (IOException e) {
            log.error("读取上传文件失败: {}", file.getOriginalFilename(), e);
            throw new FileParseException("无法读取文件: " + file.getOriginalFilename(), e);
        }
    }

    /** 从文件字节流中提取纯文本内容。 */
    @Override
    public String extractText(byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("传入空字节数组，返回空文本");
            return "";
        }
        return doExtractText(fileBytes, fileName);
    }

    /** 判断文件是否为图片型简历。 */
    @Override
    public boolean isImageBased(MultipartFile file) {
        if (file.isEmpty()) return false;
        try {
            String contentType = TIKA_DETECTOR.detect(file.getBytes());
            return contentType != null && contentType.startsWith("image/");
        } catch (IOException e) {
            log.warn("检测文件内容类型失败: {}", file.getOriginalFilename(), e);
            return false;
        }
    }

    // ─── 核心委托逻辑 ───

    private String doExtractText(byte[] fileBytes, String fileName) {
        long totalStart = DateUtils.currentEpochMillis();
        log.info("[计时] 开始提取文本: file={}, size={} bytes", fileName, fileBytes.length);

        // 检测内容类型
        String contentType = detectContentType(fileBytes, fileName);

        // 按 supports() 匹配解析器
        for (ResumeDocumentParser parser : parsers) {
            if (parser.supports(contentType, fileName)) {
                log.info("[计时] 匹配到解析器 {}: file={}", parser.getClass().getSimpleName(), fileName);
                try {
                    String text = parser.extractText(fileBytes, fileName);
                    if (!text.isEmpty()) {
                        String result = truncate(text);
                        log.info("[计时] 文本提取总耗时 {}ms: file={}, chars={}",
                                DateUtils.currentEpochMillis() - totalStart, fileName, result.length());
                        return result;
                    }
                    // 解析器返回空 — 继续尝试下一个
                    log.warn("[计时] 解析器 {} 返回空文本: file={}", parser.getClass().getSimpleName(), fileName);
                } catch (Throwable t) {
                    log.error("[计时] 解析器 {} 抛出异常 (跳过): file={}, errorType={}",
                            parser.getClass().getSimpleName(), fileName,
                            t.getClass().getName(), t);
                    // 继续尝试下一个解析器
                }
            }
        }

        log.warn("所有解析器均返回空文本: file={}, size={}, contentType={}, 总耗时 {}ms",
                fileName, fileBytes.length, contentType, DateUtils.currentEpochMillis() - totalStart);
        return "";
    }

    /** 从简历文件中提取头像图片。 */
    @Override
    public byte[] extractProfileImage(byte[] fileBytes, String fileName) {
        long start = DateUtils.currentEpochMillis();
        String contentType = detectContentType(fileBytes, fileName);
        log.info("[计时] 开始头像提取: file={}, contentType={}", fileName, contentType);

        for (ResumeDocumentParser parser : parsers) {
            if (parser.supports(contentType, fileName)) {
                try {
                    byte[] result = parser.extractProfileImage(fileBytes, fileName);
                    if (result != null) {
                        log.info("[计时] 头像提取成功: file={}, parser={}, size={} bytes, 耗时 {}ms",
                                fileName, parser.getClass().getSimpleName(), result.length,
                                DateUtils.currentEpochMillis() - start);
                        return result;
                    }
                } catch (Throwable t) {
                    log.error("[计时] 解析器 {} 提取头像异常 (跳过): file={}, errorType={}",
                            parser.getClass().getSimpleName(), fileName,
                            t.getClass().getName(), t);
                }
            }
        }

        log.info("[计时] 头像提取结束(未找到): file={}, 耗时 {}ms",
                fileName, DateUtils.currentEpochMillis() - start);
        return null;
    }

    // ─── 工具方法 ───

    private String detectContentType(byte[] fileBytes, String fileName) {
        try {
            long detectStart = DateUtils.currentEpochMillis();
            String ct = TIKA_DETECTOR.detect(fileBytes, fileName);
            log.info("[计时] 检测到文件类型: {} -> {} (耗时 {}ms)", fileName, ct,
                    DateUtils.currentEpochMillis() - detectStart);
            return ct;
        } catch (Exception e) {
            log.warn("无法检测文件类型: {}", fileName, e);
            return "application/octet-stream";
        }
    }

    private String truncate(String text) {
        if (text.length() > MAX_CHARACTERS) {
            log.info("截断提取文本: {} -> {} 字符", text.length(), MAX_CHARACTERS);
            return text.substring(0, MAX_CHARACTERS);
        }
        return text;
    }
}
