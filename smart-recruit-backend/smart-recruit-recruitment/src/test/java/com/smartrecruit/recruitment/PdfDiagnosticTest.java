package com.smartrecruit.recruitment;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Quick diagnostic to understand why a specific PDF can't be parsed.
 * Run: mvn test-compile exec:java -pl smart-recruit-recruitment \\
 *      -Dexec.classpathScope=test -Dexec.mainClass=com.smartrecruit.recruitment.PdfDiagnosticTest
 */
public class PdfDiagnosticTest {
    public static void main(String[] args) throws Exception {
        String pdfPath = args.length > 0 ? args[0]
                : "/Users/mac/Downloads/苏三_简历模版.pdf";
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            System.err.println("File not found: " + pdfPath);
            System.exit(1);
        }

        byte[] fileBytes = Files.readAllBytes(Paths.get(pdfPath));
        System.out.println("=== File: " + pdfFile.getName() + ", size: " + pdfFile.length() + " bytes ===");

        // 1. Detect content type via Tika
        String contentType = new org.apache.tika.Tika().detect(fileBytes, pdfFile.getName());
        System.out.println("Tika detected type: " + contentType);

        // 2. Try PDFBox with full diagnostics
        try (PDDocument doc = PDDocument.load(fileBytes)) {
            int pageCount = doc.getNumberOfPages();
            boolean encrypted = doc.isEncrypted();
            System.out.println("\n--- PDFBox Basic Info ---");
            System.out.println("Pages: " + pageCount);
            System.out.println("Encrypted: " + encrypted);
            System.out.println("PDF Version: " + doc.getVersion());

            // 3. Check each page for fonts and content
            for (int i = 0; i < pageCount && i < 5; i++) {
                PDPage page = doc.getPage(i);
                System.out.println("\n--- Page " + (i + 1) + " ---");
                System.out.println("Has content stream: " + page.hasContents());

                // Check resources
                var resources = page.getResources();
                if (resources != null) {
                    int fontCount = 0;
                    for (COSName key : resources.getFontNames()) {
                        fontCount++;
                        try {
                            PDFont font = resources.getFont(key);
                            System.out.println("  Font '" + key.getName() + "': "
                                    + "name=" + (font != null ? font.getName() : "null")
                                    + ", subtype=" + (font != null ? font.getSubType() : "null")
                                    + ", class=" + (font != null ? font.getClass().getSimpleName() : "null"));
                        } catch (Exception e) {
                            System.out.println("  Font '" + key.getName() + "': ERROR - " + e.getMessage());
                        }
                    }
                    System.out.println("  Total fonts: " + fontCount);

                    // Check for XObjects (images)
                    int xobjCount = 0;
                    for (COSName key : resources.getXObjectNames()) {
                        xobjCount++;
                    }
                    System.out.println("  XObjects (images etc): " + xobjCount);
                }
            }

            // 4. Try text extraction with different settings
            System.out.println("\n--- Text Extraction Tests ---");

            // 4a. Default stripper
            PDFTextStripper s1 = new PDFTextStripper();
            s1.setSortByPosition(true);
            s1.setSuppressDuplicateOverlappingText(true);
            String t1 = s1.getText(doc);
            System.out.println("Strategy A (sort+suppress): " + t1.length() + " chars");
            System.out.println("  Preview: '" + (t1.length() > 200 ? t1.substring(0, 200) + "..." : t1) + "'");

            // 4b. Without sort
            PDFTextStripper s2 = new PDFTextStripper();
            s2.setSortByPosition(false);
            s2.setSuppressDuplicateOverlappingText(true);
            String t2 = s2.getText(doc);
            System.out.println("Strategy B (no-sort+suppress): " + t2.length() + " chars");
            if (t2.length() > 0) {
                System.out.println("  Preview: '" + (t2.length() > 200 ? t2.substring(0, 200) + "..." : t2) + "'");
            }

            // 4c. No special settings
            PDFTextStripper s3 = new PDFTextStripper();
            String t3 = s3.getText(doc);
            System.out.println("Strategy C (default): " + t3.length() + " chars");
            if (t3.length() > 0) {
                System.out.println("  Preview: '" + (t3.length() > 200 ? t3.substring(0, 200) + "..." : t3) + "'");
            }

            // 4d. With addMoreFormatting
            PDFTextStripper s4 = new PDFTextStripper();
            s4.setSortByPosition(true);
            s4.setAddMoreFormatting(true);
            String t4 = s4.getText(doc);
            System.out.println("Strategy D (formatting): " + t4.length() + " chars");
            if (t4.length() > 0) {
                System.out.println("  Preview: '" + (t4.length() > 200 ? t4.substring(0, 200) + "..." : t4) + "'");
            }

            // 4e. Page by page
            System.out.println("\n--- Page-by-page Extraction ---");
            for (int i = 0; i < pageCount && i < 5; i++) {
                PDFTextStripper sp = new PDFTextStripper();
                sp.setSortByPosition(true);
                sp.setStartPage(i + 1);
                sp.setEndPage(i + 1);
                String tp = sp.getText(doc).trim();
                System.out.println("  Page " + (i + 1) + ": " + tp.length() + " chars"
                        + (tp.length() > 0 ? " '" + tp.substring(0, Math.min(100, tp.length())) + "'" : ""));
            }

            // 5. Check raw content operators in first page
            System.out.println("\n--- Raw Content Stream Check (Page 1) ---");
            if (pageCount > 0) {
                PDPage p1 = doc.getPage(0);
                var contents = p1.getContents();
                if (contents != null) {
                    byte[] contentBytes = org.apache.commons.io.IOUtils.toByteArray(contents);
                    System.out.println("Content stream size: " + contentBytes.length + " bytes");
                    // Check if text operators exist (Tj, TJ, ', ")
                    String rawOps = new String(contentBytes, 0, Math.min(contentBytes.length, 5000));
                    boolean hasTj = rawOps.contains("Tj") || rawOps.contains("TJ");
                    boolean hasQuote = rawOps.contains("' ") || rawOps.contains("\" ");
                    boolean hasBT = rawOps.contains("BT");
                    boolean hasBTend = rawOps.contains("ET");
                    boolean hasDo = rawOps.contains("Do"); // XObject (image) operator
                    System.out.println("Has BT (begin text): " + hasBT);
                    System.out.println("Has ET (end text): " + hasBTend);
                    System.out.println("Has Tj/TJ (text output): " + hasTj);
                    System.out.println("Has '/\" (text output): " + hasQuote);
                    System.out.println("Has Do (draw XObject/image): " + hasDo);
                    System.out.println("Content stream preview (first 500 chars):");
                    System.out.println(new String(contentBytes, 0, Math.min(contentBytes.length, 500)));
                } else {
                    System.out.println("No content stream!");
                }
            }

            // 6. Try to find CJK/ToUnicode issues
            System.out.println("\n--- CJK/Encoding Analysis ---");
            for (int i = 0; i < pageCount && i < 5; i++) {
                PDPage page = doc.getPage(i);
                var res = page.getResources();
                if (res != null) {
                    for (COSName key : res.getFontNames()) {
                        PDFont font = res.getFont(key);
                        if (font != null && "Type0".equals(font.getSubType())) {
                            System.out.println("Page " + (i+1) + " Type0 font '" + font.getName() + "':");
                            try {
                                // Try to check ToUnicode via font's COS dictionary
                                var cosFont = font.getCOSObject();
                                var toUnicode = cosFont.getDictionaryObject(COSName.TO_UNICODE);
                                System.out.println("  ToUnicode: " + (toUnicode != null ? "PRESENT (" + toUnicode.getClass().getSimpleName() + ")" : "MISSING"));
                                var encoding = cosFont.getDictionaryObject(COSName.ENCODING);
                                System.out.println("  Encoding: " + (encoding != null ? encoding.toString() : "MISSING"));
                                var descendantFonts = cosFont.getDictionaryObject(COSName.DESCENDANT_FONTS);
                                if (descendantFonts != null) {
                                    System.out.println("  DescendantFonts: " + descendantFonts);
                                }
                            } catch (Exception e) {
                                System.out.println("  Error inspecting: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            System.out.println("\n=== CONCLUSION ===");
            String best = "";
            if (!t1.isEmpty()) best = t1;
            else if (!t2.isEmpty()) best = t2;
            else if (!t3.isEmpty()) best = t3;
            else if (!t4.isEmpty()) best = t4;

            if (!best.isEmpty()) {
                System.out.println("SUCCESS: Text extraction works! Got " + best.length() + " chars.");
                System.out.println("Best result preview: " + best.substring(0, Math.min(500, best.length())));
            } else {
                System.out.println("FAILURE: All extraction strategies returned empty.");
                System.out.println("Check the content stream analysis above for BT/ET/Tj operators.");
                System.out.println("If BT/ET/Tj are present but text is empty, font encoding/ToUnicode issue.");
                System.out.println("If BT/ET/Tj are absent, text is rendered as paths (vector graphics).");
            }
        } catch (Exception e) {
            System.err.println("PDFBox load FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
