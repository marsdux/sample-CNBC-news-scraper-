package newsaggre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/**
 * Writes real, valid .xlsx and .docx files by hand-assembling the minimal
 * required OOXML parts and zipping them - no Apache POI / third-party jar
 * needed, so the app compiles and runs with nothing but the JDK.
 */
public class OfficeExporter {

    // ---------------------------------------------------------------- XLSX

    public void exportExcel(List<Article> articles, File out) throws IOException {
        String[] headers = {"Category", "Title", "Published", "Sectors", "Link", "Image URL", "Connected Story?", "Key Takeaways"};

        StringBuilder sheet = new StringBuilder();
        sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
             .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
             .append("<cols><col min=\"1\" max=\"1\" width=\"18\"/><col min=\"2\" max=\"2\" width=\"45\"/>")
             .append("<col min=\"3\" max=\"3\" width=\"22\"/><col min=\"4\" max=\"4\" width=\"26\"/>")
             .append("<col min=\"5\" max=\"5\" width=\"40\"/><col min=\"6\" max=\"6\" width=\"40\"/>")
             .append("<col min=\"7\" max=\"7\" width=\"16\"/><col min=\"8\" max=\"8\" width=\"70\"/></cols>")
             .append("<sheetData>");

        int rowNum = 1;
        sheet.append(rowXml(rowNum++, headers));

        for (Article a : articles) {
            boolean connected = StoryGrouper.isConnectedCluster(articles, a.getGroupId());
            String takeaways = String.join(" | ", a.getTakeaways());
            String sectors = a.getIndustryTags() == null ? "" : String.join(", ", a.getIndustryTags());
            sheet.append(rowXml(rowNum++, new String[]{
                    a.getCategory(), a.getTitle(), a.getFormattedPubDate(), sectors, a.getLink(),
                    a.getImageUrl() == null ? "" : a.getImageUrl(),
                    connected ? "Yes" : "No", takeaways
            }));
        }
        sheet.append("</sheetData></worksheet>");

        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(out))) {
            writeEntry(zip, "[Content_Types].xml", XLSX_CONTENT_TYPES);
            writeEntry(zip, "_rels/.rels", PACKAGE_RELS);
            writeEntry(zip, "xl/workbook.xml", XLSX_WORKBOOK);
            writeEntry(zip, "xl/_rels/workbook.xml.rels", XLSX_WORKBOOK_RELS);
            writeEntry(zip, "xl/worksheets/sheet1.xml", sheet.toString());
        }
    }

    private String rowXml(int rowNum, String[] cells) {
        StringBuilder row = new StringBuilder("<row r=\"" + rowNum + "\">");
        char col = 'A';
        for (String cell : cells) {
            row.append("<c r=\"").append(col).append(rowNum).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
               .append(TextUtils.escapeXml(cell == null ? "" : cell))
               .append("</t></is></c>");
            col++;
        }
        row.append("</row>");
        return row.toString();
    }

    // ---------------------------------------------------------------- DOCX

    public void exportWord(List<Article> articles, File out) throws IOException {
        StringBuilder body = new StringBuilder();
        body.append(heading("CNBC News Digest", 40));
        body.append(paragraph("Generated " + new Date() + " \u2014 " + articles.size() + " article(s)", false));

        Map<String, List<Article>> byCategory = new LinkedHashMap<>();
        for (Article a : articles) {
            byCategory.computeIfAbsent(a.getCategory(), k -> new ArrayList<>()).add(a);
        }

        for (Map.Entry<String, List<Article>> entry : byCategory.entrySet()) {
            body.append(heading(entry.getKey(), 30));
            Map<Integer, List<Article>> byGroup = new LinkedHashMap<>();
            for (Article a : entry.getValue()) {
                byGroup.computeIfAbsent(a.getGroupId(), k -> new ArrayList<>()).add(a);
            }
            for (List<Article> group : byGroup.values()) {
                if (group.size() > 1) {
                    body.append(paragraph("Connected stories (" + group.size() + ")", true));
                }
                for (Article a : group) {
                    body.append(heading(a.getTitle(), 24));
                    body.append(paragraph(a.getFormattedPubDate() + "   " + a.getLink(), false));
                    if (a.getImageUrl() != null && !a.getImageUrl().isBlank()) {
                        body.append(paragraph("Image: " + a.getImageUrl(), false));
                    }
                    if (a.getIndustryTags() != null && !a.getIndustryTags().isEmpty()) {
                        body.append(paragraph("Sectors: " + String.join(", ", a.getIndustryTags()), true));
                    }
                    List<OutlineItem> outline = a.getOutline();
                    if (outline != null && !outline.isEmpty()) {
                        for (String cat : OutlineBuilder.DISPLAY_ORDER) {
                            List<OutlineItem> inCat = new ArrayList<>();
                            for (OutlineItem oi : outline) if (cat.equals(oi.getCategory())) inCat.add(oi);
                            if (inCat.isEmpty()) continue;
                            body.append(paragraph(cat + ":", true));
                            for (OutlineItem oi : inCat) body.append(bullet(oi.getText()));
                        }
                    } else if (!a.getTakeaways().isEmpty()) {
                        for (String t : a.getTakeaways()) body.append(bullet(t));
                    } else {
                        body.append(paragraph("(No takeaways generated)", false));
                    }
                }
            }
        }

        String documentXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
                "<w:body>" + body + "<w:sectPr/></w:body></w:document>";

        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(out))) {
            writeEntry(zip, "[Content_Types].xml", DOCX_CONTENT_TYPES);
            writeEntry(zip, "_rels/.rels", DOCX_PACKAGE_RELS);
            writeEntry(zip, "word/document.xml", documentXml);
        }
    }

    private String heading(String text, int halfPoints) {
        return "<w:p><w:pPr><w:spacing w:before=\"120\" w:after=\"60\"/></w:pPr>" +
               "<w:r><w:rPr><w:b/><w:sz w:val=\"" + halfPoints + "\"/></w:rPr><w:t xml:space=\"preserve\">" +
               TextUtils.escapeXml(text) + "</w:t></w:r></w:p>";
    }

    private String paragraph(String text, boolean bold) {
        String rpr = bold ? "<w:rPr><w:b/></w:rPr>" : "";
        return "<w:p><w:r>" + rpr + "<w:t xml:space=\"preserve\">" + TextUtils.escapeXml(text) + "</w:t></w:r></w:p>";
    }

    private String bullet(String text) {
        return "<w:p><w:pPr><w:ind w:left=\"360\"/></w:pPr><w:r><w:t xml:space=\"preserve\">\u2022 " +
               TextUtils.escapeXml(text) + "</w:t></w:r></w:p>";
    }

    // ---------------------------------------------------------------- zip helper

    private void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // ---------------------------------------------------------------- static OOXML boilerplate

    private static final String PACKAGE_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
        "</Relationships>";

    private static final String XLSX_CONTENT_TYPES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
        "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
        "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
        "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
        "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
        "</Types>";

    private static final String XLSX_WORKBOOK =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
        "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
        "<sheets><sheet name=\"Articles\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";

    private static final String XLSX_WORKBOOK_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
        "</Relationships>";

    private static final String DOCX_CONTENT_TYPES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
        "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
        "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
        "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
        "</Types>";

    private static final String DOCX_PACKAGE_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
        "</Relationships>";
}
