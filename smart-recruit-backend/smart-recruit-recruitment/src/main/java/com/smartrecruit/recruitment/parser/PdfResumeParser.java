package com.smartrecruit.recruitment.parser;

import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * PDF 简历解析器 — 多策略文本提取 + 头像提取。
 *
 * <h3>解析策略</h3>
 * <ol>
 *   <li>Tika AutoDetectParser（主引擎，配置 PDFParserConfig 处理嵌入字体和非标准编码）</li>
 *   <li>PDFBox 整文档提取（回退）</li>
 *   <li>PDFBox 逐页提取（回退，绕过某些页面的字体冲突）</li>
 *   <li>PDF 结构诊断（全部失败时输出字体/内容流信息辅助排查）</li>
 * </ol>
 *
 * @since 1.0.0
 */
@Component
@Slf4j
public class PdfResumeParser implements ResumeDocumentParser {

    private static final Parser TIKA_PARSER;

    static {
        long start = DateUtils.currentEpochMillis();
        log.info("[预热] PdfResumeParser 开始初始化 Tika AutoDetectParser...");
        TIKA_PARSER = new AutoDetectParser();
        log.info("[预热] PdfResumeParser Tika AutoDetectParser 初始化完成, 耗时 {}ms",
                DateUtils.currentEpochMillis() - start);
    }

    @Override
    public boolean supports(String contentType, String fileName) {
        if (contentType != null) {
            if (contentType.equals("application/pdf") || contentType.contains("pdf")) {
                return true;
            }
        }
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String extractText(byte[] fileBytes, String fileName) {
        long totalStart = DateUtils.currentEpochMillis();
        log.info("[计时] PdfResumeParser 开始提取文本: file={}, size={} bytes", fileName, fileBytes.length);

        // 策略1: Tika AutoDetectParser
        long tikaStart = DateUtils.currentEpochMillis();
        String text = extractWithTika(fileBytes, fileName);
        log.info("[计时] PDF Tika 执行完成: file={}, chars={}, 耗时 {}ms",
                fileName, text.length(), DateUtils.currentEpochMillis() - tikaStart);
        if (!text.isEmpty()) {
            log.info("[计时] PdfResumeParser 文本提取总耗时 {}ms (Tika 成功)",
                    DateUtils.currentEpochMillis() - totalStart);
            return text;
        }

        // 策略2: PDFBox 整文档
        long pdfStart = DateUtils.currentEpochMillis();
        log.info("[计时] Tika 为空，尝试 PDFBox: {}", fileName);
        text = extractWithPDFBox(fileBytes, fileName);
        if (!text.isEmpty()) {
            log.info("[计时] PDFBox 成功: file={}, 耗时 {}ms", fileName, DateUtils.currentEpochMillis() - pdfStart);
            return text;
        }

        // 策略3: PDFBox 逐页
        text = extractWithPDFBoxByPage(fileBytes, fileName);
        if (!text.isEmpty()) {
            log.info("[计时] PDFBox 逐页成功: file={}, 耗时 {}ms", fileName, DateUtils.currentEpochMillis() - pdfStart);
            return text;
        }

        // 全部失败 — 诊断
        diagnosePDFStructure(fileBytes, fileName);
        log.warn("[计时] PDF 所有策略失败: file={}, 总耗时 {}ms",
                fileName, DateUtils.currentEpochMillis() - totalStart);
        return "";
    }

    @Override
    public byte[] extractProfileImage(byte[] fileBytes, String fileName) {
        long start = DateUtils.currentEpochMillis();
        log.info("[计时] PdfResumeParser 开始头像提取: file={}", fileName);

        try (PDDocument document = PDDocument.load(fileBytes)) {
            if (document.isEncrypted() || document.getNumberOfPages() == 0) {
                return null;
            }

            PDPage firstPage = document.getPage(0);
            PDResources resources = firstPage.getResources();
            if (resources == null) return null;

            PDImageXObject bestImage = null;
            int bestScore = 0;

            for (COSName name : resources.getXObjectNames()) {
                PDXObject xobj = resources.getXObject(name);
                if (!(xobj instanceof PDImageXObject img)) continue;

                int w = img.getWidth();
                int h = img.getHeight();

                if (w < 80 || h < 80) continue;

                float ratio = (float) h / w;
                if (ratio < 0.5f || ratio > 3.0f) continue;

                int score = w * h;
                if (h > w && ratio >= 1.0f && ratio <= 1.6f) score += 50000;

                if (score > bestScore) {
                    bestScore = score;
                    bestImage = img;
                }
            }

            if (bestImage == null) {
                log.info("[计时] PDF 首页未找到合适头像: file={}, 耗时 {}ms",
                        fileName, DateUtils.currentEpochMillis() - start);
                return null;
            }

            BufferedImage image = bestImage.getImage();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "JPEG", bos);
            byte[] jpegBytes = bos.toByteArray();

            log.info("[计时] PDF 头像提取成功: file={}, size={}x{}, bytes={}, 耗时 {}ms",
                    fileName, bestImage.getWidth(), bestImage.getHeight(), jpegBytes.length,
                    DateUtils.currentEpochMillis() - start);
            return jpegBytes;
        } catch (Throwable t) {
            log.warn("[计时] PDF 头像提取失败 (已耗时 {}ms): {} - {}",
                    DateUtils.currentEpochMillis() - start, fileName, t.getMessage());
            return null;
        }
    }

    // ─── Tika 提取 ───

    private String extractWithTika(byte[] fileBytes, String fileName) {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        metadata.set("resourceName", fileName);

        ParseContext context = new ParseContext();
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setSortByPosition(true);
        pdfConfig.setSuppressDuplicateOverlappingText(true);
        pdfConfig.setExtractAnnotationText(true);
        context.set(PDFParserConfig.class, pdfConfig);

        log.info("[计时] Tika 开始 TikaInputStream 包装: file={}", fileName);
        try (TikaInputStream tis = TikaInputStream.get(fileBytes)) {
            long parseStart = DateUtils.currentEpochMillis();
            log.info("[计时] Tika 开始 parser.parse(): file={}", fileName);
            TIKA_PARSER.parse(tis, handler, metadata, context);
            log.info("[计时] Tika parser.parse() 完成: file={}, chars={}, 耗时 {}ms",
                    fileName, handler.toString().length(), DateUtils.currentEpochMillis() - parseStart);
            String raw = handler.toString();
            String text = raw.trim();
            if (text.isEmpty() && !raw.isEmpty()) {
                log.info("Tika 提取内容仅含空白字符: file={}", fileName);
            }
            return text;
        } catch (IOException | SAXException | TikaException e) {
            log.warn("Tika 解析异常: {} - {} ({})", fileName, e.getMessage(), e.getClass().getSimpleName(), e);
            return "";
        } catch (Exception e) {
            log.warn("Tika 解析未知异常: {} - {} ({})", fileName, e.getMessage(), e.getClass().getSimpleName(), e);
            return "";
        }
    }

    // ─── PDFBox 整文档 ───

    private String extractWithPDFBox(byte[] fileBytes, String fileName) {
        try (PDDocument document = PDDocument.load(fileBytes)) {
            if (document.isEncrypted()) {
                log.warn("PDF 已加密: {}", fileName);
                return "";
            }
            int pageCount = document.getNumberOfPages();
            log.info("PDFBox 整文档: file={}, pages={}", fileName, pageCount);
            if (pageCount == 0) return "";

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setSuppressDuplicateOverlappingText(true);

            String raw = stripper.getText(document);
            log.info("[计时] PDFBox 整文档提取: file={}, pages={}, chars={}",
                    fileName, pageCount, raw.length());
            return raw.trim();
        } catch (Throwable t) {
            log.error("PDFBox 整文档失败: {} - {}", fileName, t.getMessage(), t);
            return "";
        }
    }

    // ─── PDFBox 逐页 ───

    private String extractWithPDFBoxByPage(byte[] fileBytes, String fileName) {
        try (PDDocument document = PDDocument.load(fileBytes)) {
            int pageCount = document.getNumberOfPages();
            if (pageCount <= 1) return "";

            StringBuilder allText = new StringBuilder();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setSuppressDuplicateOverlappingText(true);

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();
                if (!pageText.isEmpty()) {
                    allText.append(pageText).append("\n");
                }
            }

            String text = allText.toString().trim();
            log.info("[计时] PDFBox 逐页: file={}, pages={}, chars={}",
                    fileName, pageCount, text.length());
            return text;
        } catch (Throwable t) {
            log.warn("PDFBox 逐页失败: {} - {}", fileName, t.getMessage());
            return "";
        }
    }

    // ─── PDF 结构诊断 ───

    private void diagnosePDFStructure(byte[] fileBytes, String fileName) {
        try (PDDocument document = PDDocument.load(fileBytes)) {
            int pageCount = document.getNumberOfPages();
            log.warn("=== PDF 结构诊断: {} ({} pages) ===", fileName, pageCount);

            Set<String> fontNames = new HashSet<>();
            Set<String> fontSubTypes = new HashSet<>();
            boolean hasType3Font = false;
            boolean hasCidFont = false;

            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);
                Iterable<COSName> fontKeys = page.getResources().getFontNames();
                for (COSName key : fontKeys) {
                    try {
                        PDFont font = page.getResources().getFont(key);
                        if (font != null) {
                            String name = font.getName();
                            String subType = font.getSubType();
                            fontNames.add(name != null ? name : "unnamed");
                            fontSubTypes.add(subType != null ? subType : "unknown");
                            if (font instanceof PDType3Font) hasType3Font = true;
                            if ("Type0".equals(subType)) {
                                hasCidFont = true;
                                log.info("  CID字体(Type0) '{}' (页{}): 中文PDF可能缺少ToUnicode CMap",
                                        name, i + 1);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            log.warn("  PDF字体种类: {} 个, 字体: {}", fontNames.size(), fontNames);
            log.warn("  字体子类型: {}", fontSubTypes);
            log.warn("  含Type3字体(位图路径): {}, 含CID字体: {}", hasType3Font, hasCidFont);

            if (hasType3Font && !hasCidFont) {
                log.warn("  诊断结论: PDF使用Type3字体，文本被渲染为路径/位图");
            } else if (hasCidFont) {
                log.warn("  诊断结论: PDF使用CID字体(中文)，缺少ToUnicode CMap的字体将无法提取文本");
            } else if (fontNames.isEmpty()) {
                log.warn("  诊断结论: PDF未使用任何字体 — 文本可能为矢量路径(图形)");
            }

            if (pageCount > 0) {
                PDPage firstPage = document.getPage(0);
                boolean hasContents = firstPage.hasContents();
                log.warn("  首页含内容流: {}", hasContents);
                if (!hasContents) {
                    log.warn("  诊断结论: PDF页面无内容流 — 可能为扫描件或纯图片");
                }
            }

            log.warn("=== PDF 结构诊断结束 ===");
        } catch (Throwable t) {
            log.error("PDF 结构诊断失败: {} - {}", fileName, t.getMessage(), t);
        }
    }
}
