package com.smartrecruit.recruitment.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import com.smartrecruit.recruitment.service.WordPreviewService;

/**
 * Word 文档在线预览服务 — 将 .docx / .doc 文件转换为 HTML。
 *
 * <p>.docx 通过 XWPFDocument 手工构建 HTML，保留段落样式、文本格式、表格、图片。
 * .doc 通过 HWPFDocument + WordToHtmlConverter 转换。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class WordPreviewServiceImpl implements WordPreviewService {

    private static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body {
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif;
                  font-size: 14px; line-height: 1.8; color: #333;
                  max-width: 800px; margin: 0 auto; padding: 40px 32px;
                  background: #fff;
                }
                p { margin: 0 0 8px; min-height: 1em; }
                p.align-right { text-align: right; }
                p.align-center { text-align: center; }
                p.indent { text-indent: 2em; }
                b, strong { font-weight: 600; }
                u { text-decoration: underline; }
                i, em { font-style: italic; }
                h1, h2, h3 { margin: 16px 0 8px; font-weight: 600; }
                h1 { font-size: 22px; }
                h2 { font-size: 18px; }
                h3 { font-size: 16px; }
                table { border-collapse: collapse; width: 100%; margin: 8px 0 16px; }
                td, th { border: 1px solid #d0d0d0; padding: 6px 10px; }
                img { max-width: 100%; height: auto; margin: 8px 0; }
              </style>
            </head>
            <body>
            %s
            </body>
            </html>""";

    /**
     * 将 Word 文件字节转换为完整 HTML 页面。
     *
     * @param fileBytes Word 文件字节
     * @param fileName  原始文件名（用于判断 .doc / .docx）
     * @return 完整 HTML 字符串
     */
    public String convertToHtml(byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length == 0) {
            return wrapHtml("<p style='color:#999;text-align:center;padding:60px 0;'>文件为空，无法预览</p>");
        }

        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        try {
            // 优先按文件名判断，但任何格式解析失败都会尝试另一种格式
            // 因为用户可能把 .docx 文件改名成 .doc
            if (lowerName.endsWith(".docx")) {
                return tryConvert(fileBytes, true);  // .docx 优先，回退 .doc
            } else if (lowerName.endsWith(".doc")) {
                return tryConvert(fileBytes, false); // .doc 优先，回退 .docx
            } else {
                // 无后缀，自动检测
                return tryConvert(fileBytes, true);  // .docx 优先，回退 .doc
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Word 转 HTML 失败: fileName={}", fileName, e);
            return wrapHtml("<p style='color:#e74c3c;text-align:center;padding:60px 0;'>"
                    + "文档预览失败，请尝试下载后查看<br>"
                    + "<small style='color:#999;'>错误: " + e.getMessage() + "</small></p>");
        }
    }

    /**
     * 尝试用两种格式解析，一种失败则回退到另一种。
     *
     * @param docxFirst true=先试 .docx 再试 .doc, false=反过来
     */
    private String tryConvert(byte[] fileBytes, boolean docxFirst) throws Exception {
        try {
            if (docxFirst) {
                return convertDocxToHtml(fileBytes);
            } else {
                return convertDocToHtml(fileBytes);
            }
        } catch (Exception firstError) {
            log.info("首选格式解析失败，尝试回退格式: {}", firstError.getMessage());
            if (docxFirst) {
                return convertDocToHtml(fileBytes);
            } else {
                return convertDocxToHtml(fileBytes);
            }
        }
    }

    // ─── .docx → HTML ───

    private String convertDocxToHtml(byte[] fileBytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            StringBuilder html = new StringBuilder();

            // 遍历文档 body 元素（段落 + 表格，保持原始顺序）
            for (IBodyElement element : doc.getBodyElements()) {
                switch (element.getElementType()) {
                    case PARAGRAPH -> html.append(convertParagraph((XWPFParagraph) element));
                    case TABLE -> html.append(convertTable((XWPFTable) element));
                }
            }

            if (html.length() == 0) {
                return wrapHtml("<p style='color:#999;text-align:center;padding:60px 0;'>"
                        + "文档内容为空，无法预览</p>");
            }
            return wrapHtml(html.toString());
        }
    }

    private String convertParagraph(XWPFParagraph para) {
        String alignment = getParagraphAlignment(para);
        String styleClass = getParagraphStyleClass(para);

        StringBuilder sb = new StringBuilder();
        sb.append("<p");

        // 字号检测 — 大字号可能是标题
        String fontSize = getParaFontSize(para);
        if (fontSize != null && Integer.parseInt(fontSize.replace("px", "")) >= 18) {
            // 大字号段落视为标题
        }

        if (!styleClass.isEmpty()) {
            sb.append(" class=\"").append(styleClass).append("\"");
        }
        if (!alignment.isEmpty()) {
            sb.append(" style=\"text-align:").append(alignment).append("\"");
        }
        sb.append(">");

        List<XWPFRun> runs = para.getRuns();
        if (runs.isEmpty()) {
            sb.append("&nbsp;");
        } else {
            for (XWPFRun run : runs) {
                sb.append(convertRun(run));
            }
        }

        sb.append("</p>\n");
        return sb.toString();
    }

    private String convertRun(XWPFRun run) {
        String text = run.getText(0);
        if (text == null || text.isEmpty()) return "";

        // 转义 HTML 特殊字符
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br>");

        // 无需任何格式 → 直接返回
        if (!run.isBold() && !run.isItalic() && !isUnderline(run) && run.getFontSize() == -1) {
            return escaped;
        }

        StringBuilder sb = new StringBuilder();
        StringBuilder styles = new StringBuilder();

        // 字号
        if (run.getFontSize() != -1) {
            // getFontSize() 返回 half-points，除以 2 得到 points
            int points = run.getFontSize() / 2;
            styles.append("font-size:").append(points).append("px;");
        }

        // 颜色
        String color = run.getColor();
        if (color != null && !color.isEmpty() && !"000000".equals(color)) {
            styles.append("color:#").append(color).append(";");
        }

        // 文本装饰
        if (run.isBold()) sb.append("<b>");
        if (run.isItalic()) sb.append("<i>");
        if (isUnderline(run)) sb.append("<u>");

        if (styles.length() > 0) {
            sb.append("<span style=\"").append(styles).append("\">");
        }

        sb.append(escaped);

        if (styles.length() > 0) sb.append("</span>");
        if (isUnderline(run)) sb.append("</u>");
        if (run.isItalic()) sb.append("</i>");
        if (run.isBold()) sb.append("</b>");

        return sb.toString();
    }

    private boolean isUnderline(XWPFRun run) {
        UnderlinePatterns u = run.getUnderline();
        return u != null && u != UnderlinePatterns.NONE;
    }

    private String convertTable(XWPFTable table) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");

        // 检测合并单元格 (gridSpan / vMerge)
        List<XWPFTableRow> rows = table.getRows();
        for (int i = 0; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            sb.append("<tr>");
            List<XWPFTableCell> cells = row.getTableCells();
            for (XWPFTableCell cell : cells) {
                // 跳过被垂直合并的单元格
                CTTcPr tcPr = cell.getCTTc().getTcPr();
                if (tcPr != null && tcPr.getVMerge() != null
                        && STMerge.RESTART != tcPr.getVMerge().getVal()) {
                    continue;
                }

                int colspan = 1;
                if (tcPr != null && tcPr.getGridSpan() != null) {
                    BigInteger span = tcPr.getGridSpan().getVal();
                    if (span != null) colspan = span.intValue();
                }

                if (colspan > 1) {
                    sb.append("<td colspan=\"").append(colspan).append("\">");
                } else {
                    sb.append("<td>");
                }

                // 渲染单元格内的段落
                for (XWPFParagraph para : cell.getParagraphs()) {
                    for (XWPFRun run : para.getRuns()) {
                        sb.append(convertRun(run));
                    }
                    if (para.getRuns().isEmpty()) {
                        sb.append("&nbsp;");
                    }
                }

                sb.append("</td>");
            }
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    // ─── 段落样式辅助 ───

    private String getParagraphAlignment(XWPFParagraph para) {
        ParagraphAlignment align = para.getAlignment();
        if (align == null) return "";
        return switch (align) {
            case CENTER -> "center";
            case RIGHT -> "right";
            case BOTH -> "justify";
            default -> "";
        };
    }

    private String getParagraphStyleClass(XWPFParagraph para) {
        String style = para.getStyle();
        if (style == null) return "";

        // Word 内置标题样式
        if (style.matches("(?i).*(heading|Heading|标题).*1.*")) return "h1-class";
        if (style.matches("(?i).*(heading|Heading|标题).*2.*")) return "h2-class";
        return "";
    }

    private String getParaFontSize(XWPFParagraph para) {
        for (XWPFRun run : para.getRuns()) {
            if (run.getFontSize() != -1) {
                return (run.getFontSize() / 2) + "px";
            }
        }
        return null;
    }

    // ─── .doc → HTML (使用 POI 内置转换器) ───

    private String convertDocToHtml(byte[] fileBytes) throws Exception {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(fileBytes))) {
            WordToHtmlConverter converter = new WordToHtmlConverter(
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());

            converter.processDocument(doc);

            // 序列化为 HTML 字符串
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(converter.getDocument()), new StreamResult(out));

            String htmlBody = out.toString("UTF-8");
            if (htmlBody.isEmpty()) {
                return wrapHtml("<p style='color:#999;text-align:center;padding:60px 0;'>"
                        + "文档内容为空，无法预览</p>");
            }

            // 提取 <body> 内的内容
            int bodyStart = htmlBody.indexOf("<body>");
            int bodyEnd = htmlBody.indexOf("</body>");
            if (bodyStart >= 0 && bodyEnd > bodyStart) {
                htmlBody = htmlBody.substring(bodyStart + 6, bodyEnd);
            }

            // .doc 转换器可能把图片写成本地文件路径，需要转换为 base64
            // 提取文档中的图片并转为 base64
            org.apache.poi.hwpf.model.PicturesTable picturesTable = doc.getPicturesTable();
            if (picturesTable != null) {
                List<?> allPictures = picturesTable.getAllPictures();
                for (Object pic : allPictures) {
                    if (pic instanceof org.apache.poi.hwpf.usermodel.Picture picture) {
                        String mime = picture.getMimeType();
                        if (mime == null || mime.isEmpty()) mime = "image/png";
                        String base64 = "data:" + mime + ";base64,"
                                + Base64.getEncoder().encodeToString(picture.getContent());
                        // 尝试替换 HTML 中对应的图片引用
                        String picFileName = picture.suggestFullFileName();
                        if (picFileName != null && !picFileName.isEmpty()) {
                            htmlBody = htmlBody.replace(picFileName, base64);
                        }
                    }
                }
            }

            return wrapHtml(htmlBody);
        }
    }

    // ─── 包装完整 HTML ───

    private String wrapHtml(String body) {
        return String.format(HTML_TEMPLATE, body);
    }
}
