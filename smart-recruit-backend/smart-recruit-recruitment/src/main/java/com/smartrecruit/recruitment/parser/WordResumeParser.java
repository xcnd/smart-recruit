package com.smartrecruit.recruitment.parser;

import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Word 简历解析器 — 多策略文本提取 + 头像提取。
 *
 * <h3>解析策略</h3>
 * <ol>
 *   <li>Tika AutoDetectParser（主引擎，自动检测 .doc/.docx）</li>
 *   <li>POI XWPF 直接提取 .docx（回退，含段落和表格内容）</li>
 *   <li>POI HWPF 直接提取 .doc（回退）</li>
 *   <li>Word 结构诊断（全部失败时输出段落/表格/图片统计）</li>
 * </ol>
 *
 * <h3>头像提取</h3>
 * <p>支持 .docx（XWPF 嵌入式图片）和 .doc（HWPF PicturesTable），
 * 按人像比例评分选择最佳候选。</p>
 *
 * @since 1.0.0
 */
@Component
@Slf4j
public class WordResumeParser implements ResumeDocumentParser {

    private static final Parser TIKA_PARSER;

    static {
        long start = DateUtils.currentEpochMillis();
        log.info("[预热] WordResumeParser 开始初始化 Tika AutoDetectParser...");
        TIKA_PARSER = new AutoDetectParser();
        log.info("[预热] WordResumeParser Tika AutoDetectParser 初始化完成, 耗时 {}ms",
                DateUtils.currentEpochMillis() - start);
    }

    @Override
    public boolean supports(String contentType, String fileName) {
        if (contentType != null) {
            if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    || contentType.equals("application/msword")
                    || contentType.contains("word")
                    || contentType.contains("officedocument.wordprocessingml")) {
                return true;
            }
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            return lower.endsWith(".docx") || lower.endsWith(".doc");
        }
        return false;
    }

    @Override
    public String extractText(byte[] fileBytes, String fileName) {
        long totalStart = DateUtils.currentEpochMillis();
        log.info("[计时] WordResumeParser 开始提取文本: file={}, size={} bytes", fileName, fileBytes.length);

        // 策略1: Tika AutoDetectParser
        long tikaStart = DateUtils.currentEpochMillis();
        String text = extractWithTika(fileBytes, fileName);
        log.info("[计时] Word Tika 执行完成: file={}, chars={}, 耗时 {}ms",
                fileName, text.length(), DateUtils.currentEpochMillis() - tikaStart);
        if (!text.isEmpty()) {
            log.info("[计时] WordResumeParser 文本提取总耗时 {}ms (Tika 成功)",
                    DateUtils.currentEpochMillis() - totalStart);
            return text;
        }

        // 策略2: XWPF 直接提取 .docx
        if (isDocxFile(fileName)) {
            long xwpfStart = DateUtils.currentEpochMillis();
            log.info("[计时] Tika 为空，尝试 XWPF: {}", fileName);
            text = extractTextFromDocx(fileBytes, fileName);
            if (!text.isEmpty()) {
                log.info("[计时] XWPF 成功: file={}, 耗时 {}ms", fileName, DateUtils.currentEpochMillis() - xwpfStart);
                return text;
            }
        }

        // 策略3: HWPF 提取 .doc（旧格式）
        if (isDocFile(fileName)) {
            long hwpfStart = DateUtils.currentEpochMillis();
            log.info("[计时] Tika 为空，尝试 HWPF: {}", fileName);
            text = extractTextFromDoc(fileBytes, fileName);
            if (!text.isEmpty()) {
                log.info("[计时] HWPF 成功: file={}, 耗时 {}ms", fileName, DateUtils.currentEpochMillis() - hwpfStart);
                return text;
            }
        }

        // 全部失败 — 诊断
        diagnoseWordStructure(fileBytes, fileName);
        log.warn("[计时] Word 所有策略失败: file={}, 总耗时 {}ms",
                fileName, DateUtils.currentEpochMillis() - totalStart);
        return "";
    }

    @Override
    public byte[] extractProfileImage(byte[] fileBytes, String fileName) {
        if (isDocxFile(fileName)) {
            return extractProfileImageFromDocx(fileBytes, fileName);
        }
        if (isDocFile(fileName)) {
            return extractProfileImageFromDoc(fileBytes, fileName);
        }
        return null;
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

    // ─── XWPF .docx 直接提取 ───

    private String extractTextFromDocx(byte[] fileBytes, String fileName) {
        long start = DateUtils.currentEpochMillis();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
             XWPFDocument doc = new XWPFDocument(bis)) {

            log.info("[计时] XWPFDocument 加载完成: file={}, 耗时 {}ms",
                    fileName, DateUtils.currentEpochMillis() - start);

            StringBuilder sb = new StringBuilder();

            // 段落
            long paraStart = DateUtils.currentEpochMillis();
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            int nonEmptyCount = 0;
            for (XWPFParagraph p : paragraphs) {
                String text = p.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text.trim()).append("\n");
                    nonEmptyCount++;
                }
            }
            log.info("[计时] XWPF 段落提取: file={}, 总{}段, 非空{}段, 耗时 {}ms",
                    fileName, paragraphs.size(), nonEmptyCount, DateUtils.currentEpochMillis() - paraStart);

            // 表格
            long tableStart = DateUtils.currentEpochMillis();
            List<XWPFTable> tables = doc.getTables();
            if (!tables.isEmpty()) {
                int cellCount = 0;
                for (XWPFTable table : tables) {
                    for (XWPFTableRow row : table.getRows()) {
                        StringBuilder rowText = new StringBuilder();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            String cellText = cell.getText();
                            if (cellText != null && !cellText.isBlank()) {
                                rowText.append(cellText.trim()).append("  ");
                                cellCount++;
                            }
                        }
                        if (!rowText.isEmpty()) {
                            sb.append(rowText.toString().trim()).append("\n");
                        }
                    }
                }
                log.info("[计时] XWPF 表格提取: file={}, {}个表格, {}个非空单元格, 耗时 {}ms",
                        fileName, tables.size(), cellCount, DateUtils.currentEpochMillis() - tableStart);
            }

            // XWPFWordExtractor 补充
            long extractorStart = DateUtils.currentEpochMillis();
            try (XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                String extractorText = extractor.getText();
                if (extractorText != null && !extractorText.isBlank()
                        && extractorText.length() > sb.length()) {
                    log.info("[计时] XWPFWordExtractor 内容更丰富 ({} vs {}), 采用 extractor 结果",
                            extractorText.length(), sb.length());
                    sb.setLength(0);
                    sb.append(extractorText);
                }
            }
            log.info("[计时] XWPFWordExtractor 补充: 耗时 {}ms", DateUtils.currentEpochMillis() - extractorStart);

            String result = sb.toString().trim();
            log.info("[计时] XWPF 提取完成: file={}, chars={}, paragraphs={}, tables={}, 总耗时 {}ms",
                    fileName, result.length(), paragraphs.size(), tables.size(), DateUtils.currentEpochMillis() - start);
            return result;

        } catch (Exception e) {
            log.warn("[计时] XWPF 提取失败 (已耗时 {}ms): {} - {}",
                    DateUtils.currentEpochMillis() - start, fileName, e.getMessage(), e);
            return "";
        }
    }

    // ─── HWPF .doc 直接提取 ───

    private String extractTextFromDoc(byte[] fileBytes, String fileName) {
        long start = DateUtils.currentEpochMillis();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
             HWPFDocument doc = new HWPFDocument(bis)) {

            log.info("[计时] HWPFDocument 加载完成: file={}, 耗时 {}ms",
                    fileName, DateUtils.currentEpochMillis() - start);

            WordExtractor extractor = new WordExtractor(doc);
            String text = extractor.getText();
            String result = text != null ? text.trim() : "";

            log.info("[计时] HWPF 提取完成: file={}, chars={}, 总耗时 {}ms",
                    fileName, result.length(), DateUtils.currentEpochMillis() - start);
            return result;

        } catch (Exception e) {
            log.warn("[计时] HWPF 提取失败 (已耗时 {}ms): {} - {}",
                    DateUtils.currentEpochMillis() - start, fileName, e.getMessage(), e);
            return "";
        }
    }

    // ─── Word 结构诊断 ───

    private void diagnoseWordStructure(byte[] fileBytes, String fileName) {
        try {
            log.warn("=== Word 结构诊断: {} ===", fileName);

            if (isDocxFile(fileName)) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
                     XWPFDocument doc = new XWPFDocument(bis)) {

                    List<XWPFParagraph> paragraphs = doc.getParagraphs();
                    int nonEmpty = 0;
                    for (XWPFParagraph p : paragraphs) {
                        String text = p.getText();
                        if (text != null && !text.isBlank()) nonEmpty++;
                    }
                    List<XWPFTable> tables = doc.getTables();
                    int totalImages = 0;
                    for (XWPFPictureData pd : doc.getAllPictures()) totalImages++;

                    log.warn("  总段落数: {} (非空: {}), 表格数: {}, 嵌入式图片: {}",
                            paragraphs.size(), nonEmpty, tables.size(), totalImages);

                    if (nonEmpty == 0 && tables.isEmpty()) {
                        log.warn("  诊断结论: 文档无文本段落和表格 — 可能是扫描件或纯图片");
                    } else if (nonEmpty == 0 && !tables.isEmpty()) {
                        log.warn("  诊断结论: 文档仅含表格，纯段落为空");
                    } else {
                        log.warn("  诊断结论: 文档包含文本但提取为空 — 文件可能加密或损坏");
                    }
                }
            } else if (isDocFile(fileName)) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
                     HWPFDocument doc = new HWPFDocument(bis)) {

                    int rangeLength = doc.getRange().text().length();
                    PicturesTable picsTable = doc.getPicturesTable();
                    int picCount = picsTable != null ? picsTable.getAllPictures().size() : 0;

                    log.warn("  文本长度(字符): {}, 图片数: {}", rangeLength, picCount);

                    if (rangeLength <= 1) {
                        log.warn("  诊断结论: .doc 文档无有效文本 — 可能是扫描件或纯图片");
                    } else {
                        log.warn("  诊断结论: 文档包含 {} 字符文本但提取为空 — 文件可能加密或损坏", rangeLength);
                    }
                }
            }

            log.warn("=== Word 结构诊断结束 ===");
        } catch (Exception e) {
            log.error("Word 结构诊断失败: {} - {}", fileName, e.getMessage(), e);
        }
    }

    // ─── Word 头像提取 ───

    private byte[] extractProfileImageFromDocx(byte[] fileBytes, String fileName) {
        long start = DateUtils.currentEpochMillis();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
             XWPFDocument doc = new XWPFDocument(bis)) {

            List<XWPFPictureData> allPictures = doc.getAllPictures();
            log.info("[计时] .docx 图片扫描: file={}, 总数={}, 耗时 {}ms",
                    fileName, allPictures.size(), DateUtils.currentEpochMillis() - start);
            if (allPictures.isEmpty()) return null;

            XWPFPictureData bestPicture = null;
            int bestScore = 0;
            int candidatesChecked = 0;

            for (XWPFPictureData pic : allPictures) {
                try {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(pic.getData()));
                    if (image == null) continue;

                    int w = image.getWidth();
                    int h = image.getHeight();
                    if (w < 80 || h < 80) continue;

                    float ratio = (float) h / w;
                    if (ratio < 0.5f || ratio > 3.0f) continue;

                    candidatesChecked++;
                    int score = w * h;
                    if (h > w && ratio >= 1.0f && ratio <= 1.6f) score += 50000;

                    if (score > bestScore) {
                        bestScore = score;
                        bestPicture = pic;
                    }
                } catch (Exception e) {
                    log.debug("跳过无法解码的图片: {}", e.getMessage());
                }
            }

            if (bestPicture == null) {
                log.info("[计时] .docx 未找到合适头像 (扫描{}个, 耗时 {}ms)",
                        allPictures.size(), DateUtils.currentEpochMillis() - start);
                return null;
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bestPicture.getData()));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "JPEG", bos);
            byte[] jpegBytes = bos.toByteArray();

            log.info("[计时] .docx 头像提取成功: file={}, size={}x{}, bytes={}, 候选{}个, 总耗时 {}ms",
                    fileName, image.getWidth(), image.getHeight(), jpegBytes.length,
                    candidatesChecked, DateUtils.currentEpochMillis() - start);
            return jpegBytes;

        } catch (Exception e) {
            log.warn("[计时] .docx 头像提取失败 (已耗时 {}ms): {} - {}",
                    DateUtils.currentEpochMillis() - start, fileName, e.getMessage());
            return null;
        }
    }

    private byte[] extractProfileImageFromDoc(byte[] fileBytes, String fileName) {
        long start = DateUtils.currentEpochMillis();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
             HWPFDocument doc = new HWPFDocument(bis)) {

            PicturesTable picsTable = doc.getPicturesTable();
            if (picsTable == null) {
                log.info("[计时] .doc 无图片表: file={}, 耗时 {}ms", fileName, DateUtils.currentEpochMillis() - start);
                return null;
            }

            List<Picture> allPictures = picsTable.getAllPictures();
            log.info("[计时] .doc 图片扫描: file={}, 总数={}, 耗时 {}ms",
                    fileName, allPictures.size(), DateUtils.currentEpochMillis() - start);
            if (allPictures.isEmpty()) return null;

            Picture bestPicture = null;
            int bestScore = 0;
            int candidatesChecked = 0;

            for (Picture pic : allPictures) {
                try {
                    byte[] picBytes = pic.getContent();
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(picBytes));
                    if (image == null) continue;

                    int w = image.getWidth();
                    int h = image.getHeight();
                    if (w < 80 || h < 80) continue;

                    float ratio = (float) h / w;
                    if (ratio < 0.5f || ratio > 3.0f) continue;

                    candidatesChecked++;
                    int score = w * h;
                    if (h > w && ratio >= 1.0f && ratio <= 1.6f) score += 50000;

                    if (score > bestScore) {
                        bestScore = score;
                        bestPicture = pic;
                    }
                } catch (Exception e) {
                    log.debug("跳过无法解码的 .doc 图片: {}", e.getMessage());
                }
            }

            if (bestPicture == null) {
                log.info("[计时] .doc 未找到合适头像: file={}, 扫描{}个, 耗时 {}ms",
                        fileName, allPictures.size(), DateUtils.currentEpochMillis() - start);
                return null;
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bestPicture.getContent()));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "JPEG", bos);
            byte[] jpegBytes = bos.toByteArray();

            log.info("[计时] .doc 头像提取成功: file={}, size={}x{}, bytes={}, 候选{}个, 总耗时 {}ms",
                    fileName, image.getWidth(), image.getHeight(), jpegBytes.length,
                    candidatesChecked, DateUtils.currentEpochMillis() - start);
            return jpegBytes;

        } catch (Exception e) {
            log.warn("[计时] .doc 头像提取失败 (已耗时 {}ms): {} - {}",
                    DateUtils.currentEpochMillis() - start, fileName, e.getMessage());
            return null;
        }
    }

    // ─── 文件类型判断 ───

    private boolean isDocxFile(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".docx");
    }

    private boolean isDocFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".doc") && !lower.endsWith(".docx");
    }
}
