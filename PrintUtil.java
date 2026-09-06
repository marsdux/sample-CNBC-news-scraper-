package newsaggre;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.util.List;

/** Prints one or more articles (title, meta, takeaways) via the system print dialog. */
public class PrintUtil {

    public static void print(Component parent, List<Article> articles) {
        StringBuilder sb = new StringBuilder();
        for (Article a : articles) {
            sb.append(a.getCategory()).append("\n");
            sb.append(a.getTitle()).append("\n");
            sb.append(a.getFormattedPubDate()).append("   ").append(a.getLink()).append("\n");
            if (a.getIndustryTags() != null && !a.getIndustryTags().isEmpty()) {
                sb.append("Sectors: ").append(String.join(", ", a.getIndustryTags())).append("\n");
            }
            sb.append("\n");
            List<OutlineItem> outline = a.getOutline();
            if (outline != null && !outline.isEmpty()) {
                for (String cat : OutlineBuilder.DISPLAY_ORDER) {
                    boolean wroteHeader = false;
                    for (OutlineItem oi : outline) {
                        if (!cat.equals(oi.getCategory())) continue;
                        if (!wroteHeader) { sb.append(cat).append(":\n"); wroteHeader = true; }
                        sb.append("  \u2022 ").append(oi.getText()).append("\n");
                    }
                }
            } else {
                sb.append("Key takeaways:\n");
                for (String t : a.getTakeaways()) sb.append("  \u2022 ").append(t).append("\n");
            }
            sb.append("\n----------------------------------------\n\n");
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.SERIF, Font.PLAIN, 12));
        area.setSize(new Dimension(560, Integer.MAX_VALUE));

        try {
            boolean printed = area.print();
            if (!printed) {
                JOptionPane.showMessageDialog(parent, "Print cancelled.");
            }
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(parent, "Could not print: " + e.getMessage(),
                    "Print error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
