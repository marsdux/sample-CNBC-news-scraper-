package newsaggre;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-article window: shows the full fetched page (text + lead image), lets
 * the user generate a categorized, PESTEL/market-aware outline from that
 * content on demand, and lets that outline be edited, saved/stored to the
 * local outline database, deleted, printed, or exported to Excel/Word -
 * all scoped to just this one article.
 */
public class FullArticleDialog extends JDialog {

    private final Article article;
    private final RssFetcher fetcher;
    private final OutlineService outlineService;
    private final OutlineDatabase db;
    private final OfficeExporter exporter;
    private final Runnable onArticleChanged; // notifies the main window to refresh table/detail
    private final LifecycleAnalyzer lifecycleAnalyzer = new LifecycleAnalyzer();

    private JLabel imageLabel;
    private JTextArea fullTextArea;
    private JEditorPane outlineView;
    private JTextArea editArea;
    private JLabel statusLabel;
    private JButton generateBtn;
    private JTabbedPane tabs;
    private LifecycleGauge lifecycleGauge;

    public FullArticleDialog(Frame owner, Article article, RssFetcher fetcher, OutlineService outlineService,
                              OutlineDatabase db, OfficeExporter exporter, Runnable onArticleChanged) {
        super(owner, "Full Article", false);
        this.article = article;
        this.fetcher = fetcher;
        this.outlineService = outlineService;
        this.db = db;
        this.exporter = exporter;
        this.onArticleChanged = onArticleChanged;

        setSize(820, 760);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(Theme.BG_SOFT);
        setLayout(new BorderLayout(8, 8));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        loadFullPageIfNeeded();
        loadImageAsync();
    }

    // ------------------------------------------------------------- build

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBorder(new EmptyBorder(10, 10, 4, 10));
        header.setBackground(Theme.CARD_BG);
        header.setOpaque(true);

        JTextArea title = new JTextArea(article.getTitle());
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setEditable(false);
        title.setOpaque(false);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        title.setForeground(Theme.PRIMARY_DARK);

        boolean savedInDb = db.containsTitle(article.getTitle());
        String meta = article.getCategory() + "  |  Published: " + article.getFormattedPubDate()
                + "  |  Fetched into app: " + article.getFormattedFetchedAt()
                + (savedInDb ? "  |  \u2713 outline saved in database" : "");
        JLabel metaLabel = new JLabel(meta);
        metaLabel.setForeground(Theme.TEXT_MUTED);
        metaLabel.setFont(metaLabel.getFont().deriveFont(11f));

        JLabel linkLabel = new JLabel("<html><a href=''>" + esc(article.getLink()) + "</a></html>");
        linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkLabel.setFont(linkLabel.getFont().deriveFont(11f));
        linkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!UrlSafety.isSafeToOpen(article.getLink())) return;
                try { Desktop.getDesktop().browse(java.net.URI.create(article.getLink())); } catch (Exception ignored) {}
            }
        });

        JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        tagRow.setOpaque(false);
        if (article.getIndustryTags() != null) {
            for (String tag : article.getIndustryTags()) tagRow.add(new TagChip(tag, Theme.colorForTag(tag)));
        }

        lifecycleGauge = new LifecycleGauge();
        lifecycleGauge.setState(lifecycleAnalyzer.evaluate(article));

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        linkLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        lifecycleGauge.setAlignmentX(Component.LEFT_ALIGNMENT);
        textCol.add(title);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(metaLabel);
        textCol.add(linkLabel);
        textCol.add(tagRow);
        textCol.add(lifecycleGauge);

        imageLabel = new JLabel("No image", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(220, 140));
        imageLabel.setForeground(Theme.TEXT_MUTED);
        imageLabel.setBorder(BorderFactory.createLineBorder(new Color(0xE5E9F0)));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Theme.BG_SOFT);

        header.add(textCol, BorderLayout.CENTER);
        header.add(imageLabel, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        fullTextArea = new JTextArea("Loading full article text...");
        fullTextArea.setLineWrap(true);
        fullTextArea.setWrapStyleWord(true);
        fullTextArea.setEditable(false);
        fullTextArea.setMargin(new Insets(10, 10, 10, 10));
        fullTextArea.setFont(new Font(Font.SERIF, Font.PLAIN, 14));

        outlineView = new JEditorPane();
        outlineView.setContentType("text/html");
        outlineView.setEditable(false);
        outlineView.setBorder(new EmptyBorder(6, 10, 6, 10));
        outlineView.setText(buildOutlineHtml());

        editArea = new JTextArea(article.takeawaysAsBulletText());
        editArea.setLineWrap(true);
        editArea.setWrapStyleWord(true);
        editArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        editArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        tabs = new JTabbedPane();
        tabs.addTab("Full Article", new JScrollPane(fullTextArea));
        tabs.addTab("Outline (by topic)", new JScrollPane(outlineView));
        tabs.addTab("Edit outline", new JScrollPane(editArea));
        return tabs;
    }

    private JPanel buildButtons() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.CARD_BG);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        row1.setOpaque(false);
        generateBtn = pastelButton("Generate outline from this article", Theme.PASTEL_GOLD);
        generateBtn.addActionListener(e -> generateOutline());
        row1.add(generateBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row2.setOpaque(false);
        JButton saveEditBtn = new JButton("Save edit");
        saveEditBtn.setToolTipText("Apply the text in \"Edit outline\" to this article's outline");
        saveEditBtn.addActionListener(e -> saveEdit());
        JButton storeBtn = pastelButton("Save / Store to database", Theme.PASTEL_TEAL);
        storeBtn.addActionListener(e -> storeToDatabase());
        JButton deleteBtn = pastelButton("Delete saved outline", Theme.PASTEL_CORAL);
        deleteBtn.addActionListener(e -> deleteFromDatabase());
        JButton printBtn = new JButton("Print");
        printBtn.addActionListener(e -> PrintUtil.print(this, List.of(article)));
        JButton xlsBtn = new JButton("Export Excel");
        xlsBtn.addActionListener(e -> exportExcel());
        JButton docBtn = new JButton("Export Word");
        docBtn.addActionListener(e -> exportWord());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());

        for (JButton b : new JButton[]{saveEditBtn, storeBtn, deleteBtn, printBtn, xlsBtn, docBtn, closeBtn}) {
            row2.add(b);
        }

        statusLabel = new JLabel(" ");
        statusLabel.setBorder(new EmptyBorder(0, 10, 6, 0));
        statusLabel.setForeground(Theme.TEXT_MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(row1);
        rows.add(row2);
        wrap.add(rows, BorderLayout.CENTER);
        wrap.add(statusLabel, BorderLayout.SOUTH);
        return wrap;
    }

    private JButton pastelButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Theme.TEXT_DARK); // black-ish text for readability on pastel backgrounds
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return b;
    }

    // ------------------------------------------------------------- actions

    private void loadFullPageIfNeeded() {
        if (!article.getFullText().isBlank()) {
            fullTextArea.setText(article.getFullText());
            return;
        }
        SwingWorker<RssFetcher.PageContent, Void> worker = new SwingWorker<>() {
            @Override protected RssFetcher.PageContent doInBackground() { return fetcher.tryFetchFullPage(article.getLink()); }
            @Override protected void done() {
                RssFetcher.PageContent pc;
                try { pc = get(); } catch (Exception ex) { pc = new RssFetcher.PageContent("", null); }
                if (pc.text.isBlank()) {
                    fullTextArea.setText("(Could not read the full article - the page may be blocked, or the "
                            + "story may require a subscription. Showing the free RSS summary instead.)\n\n"
                            + article.getRawDescription());
                } else {
                    article.setFullText(pc.text);
                    fullTextArea.setText(pc.text);
                }
                if (pc.imageUrl != null && !pc.imageUrl.isBlank()) {
                    article.setImageUrl(pc.imageUrl);
                    loadImageAsync();
                }
            }
        };
        worker.execute();
    }

    private void loadImageAsync() {
        String url = article.getImageUrl();
        if (url == null || url.isBlank()) return;
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override protected ImageIcon doInBackground() { return ImageLoader.loadScaled(url, 220, 140); }
            @Override protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        imageLabel.setIcon(icon);
                        imageLabel.setText(null);
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void generateOutline() {
        if (article.isEdited()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "This article has manual edits. Overwrite them with a fresh outline generated from the full article?",
                    "Overwrite manual edits?", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        generateBtn.setEnabled(false);
        statusLabel.setText("Reading full article and generating outline...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (article.getFullText().isBlank()) {
                    RssFetcher.PageContent pc = fetcher.tryFetchFullPage(article.getLink());
                    if (!pc.text.isBlank()) article.setFullText(pc.text);
                    if (pc.imageUrl != null && !pc.imageUrl.isBlank()) article.setImageUrl(pc.imageUrl);
                }
                outlineService.generateFromFullArticle(article);
                return null;
            }
            @Override
            protected void done() {
                generateBtn.setEnabled(true);
                fullTextArea.setText(article.getFullText().isBlank() ? article.getRawDescription() : article.getFullText());
                outlineView.setText(buildOutlineHtml());
                outlineView.setCaretPosition(0);
                editArea.setText(article.takeawaysAsBulletText());
                lifecycleGauge.setState(lifecycleAnalyzer.evaluate(article));
                tabs.setSelectedIndex(1);
                statusLabel.setText("Outline generated from the full article text.");
                if (onArticleChanged != null) onArticleChanged.run();
            }
        };
        worker.execute();
    }

    private void saveEdit() {
        List<String> lines = new ArrayList<>();
        for (String line : editArea.getText().split("\\R")) {
            String cleaned = line.replaceFirst("^[\\s\u2022*-]+", "").trim();
            if (!cleaned.isEmpty()) lines.add(cleaned);
        }
        outlineService.applyManualEdit(article, lines);
        outlineView.setText(buildOutlineHtml());
        outlineView.setCaretPosition(0);
        lifecycleGauge.setState(lifecycleAnalyzer.evaluate(article));
        statusLabel.setText("Edit applied to this article's outline.");
        if (onArticleChanged != null) onArticleChanged.run();
    }

    private void storeToDatabase() {
        try {
            db.save(article);
            statusLabel.setText("Saved outline to database, keyed by title: \u201C" + article.getTitle() + "\u201D");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteFromDatabase() {
        if (!db.containsTitle(article.getTitle())) {
            JOptionPane.showMessageDialog(this, "No saved outline found in the database for this title.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete the saved outline for this article from the database?\n(The article stays in your list; only the stored outline record is removed.)",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            db.deleteByTitle(article.getTitle());
            statusLabel.setText("Deleted saved outline from database.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not delete: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(safeFileName(article.getTitle()) + ".xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = ensureExtension(chooser.getSelectedFile(), ".xlsx");
        try {
            exporter.exportExcel(List.of(article), out);
            statusLabel.setText("Exported to " + out);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportWord() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(safeFileName(article.getTitle()) + ".docx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = ensureExtension(chooser.getSelectedFile(), ".docx");
        try {
            exporter.exportWord(List.of(article), out);
            statusLabel.setText("Exported to " + out);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------- helpers

    private String buildOutlineHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:sans-serif;font-size:12px;color:#111827;padding:4px;'>");
        List<OutlineItem> outline = article.getOutline();
        if (outline == null || outline.isEmpty()) {
            html.append("<i>No outline yet - click \"Generate outline from this article\" above.</i>");
        } else {
            for (String cat : OutlineBuilder.DISPLAY_ORDER) {
                List<OutlineItem> inCat = new ArrayList<>();
                for (OutlineItem oi : outline) if (cat.equals(oi.getCategory())) inCat.add(oi);
                if (inCat.isEmpty()) continue;
                String hex = Theme.toHex(Theme.outlineCategoryColor(cat));
                html.append("<h4 style='color:").append(hex).append(";margin-bottom:2px;'>").append(esc(cat)).append("</h4>");
                if (cat.equals(OutlineBuilder.CAT_QUOTE)) {
                    html.append("<ul style='margin-top:0;'>");
                    for (OutlineItem oi : inCat) {
                        html.append("<li><i>").append(esc(oi.getText())).append("</i></li>");
                    }
                    html.append("</ul>");
                } else {
                    html.append("<ul style='margin-top:0;'>");
                    for (OutlineItem oi : inCat) html.append("<li>").append(esc(oi.getText())).append("</li>");
                    html.append("</ul>");
                }
            }
        }
        html.append("</body></html>");
        return html.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private File ensureExtension(File f, String ext) {
        if (f.getName().toLowerCase().endsWith(ext)) return f;
        return new File(f.getParentFile(), f.getName() + ext);
    }

    private String safeFileName(String title) {
        String base = title == null || title.isBlank() ? "article" : title;
        base = base.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
        return base.length() > 80 ? base.substring(0, 80) : base;
    }
}
