package newsaggre;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainFrame extends JFrame {

    private final List<Article> allArticles = new CopyOnWriteArrayList<>();
    private List<Article> visibleArticles = new ArrayList<>();

    private final ArticleStore store = new ArticleStore();
    private final OutlineDatabase outlineDb = new OutlineDatabase();
    private final RssFetcher fetcher = new RssFetcher();
    private final StoryGrouper grouper = new StoryGrouper();
    private final Summarizer summarizer = new Summarizer();
    private final OutlineService outlineService = new OutlineService();
    private final OfficeExporter exporter = new OfficeExporter();
    private final LifecycleAnalyzer lifecycleAnalyzer = new LifecycleAnalyzer();

    private DefaultListModel<String> categoryListModel;
    private JList<String> categoryList;
    private JTable table;
    private ArticleTableModel tableModel;
    private TableRowSorter<ArticleTableModel> sorter;
    private JTextField searchField;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JCheckBox fetchFullTextCheck;

    private JTextArea titleArea;
    private JLabel metaLabel;
    private JPanel tagPanel;
    private LifecycleGauge lifecycleGauge;
    private JLabel imageLabel;
    private JTextArea previewArea;
    private Article currentlyEditing;

    public MainFrame() {
        super("newsaggre \u2014 CNBC Free-Article Digest");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1360, 860);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_SOFT);

        setJMenuBar(buildMenuBar());

        JPanel north = new JPanel(new BorderLayout());
        north.add(new GradientHeaderPanel("newsaggre", "CNBC free-article digest \u2014 outlines, sectors & connected stories"), BorderLayout.NORTH);
        north.add(buildToolBar(), BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        add(buildMainSplit(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        loadFromDisk();
    }

    // ------------------------------------------------------------- UI build

    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(menuItem("Fetch latest from CNBC", e -> doFetch()));
        file.add(menuItem("Save / Store", e -> doSave()));
        file.add(menuItem("Retrieve from disk", e -> loadFromDisk()));
        file.addSeparator();
        file.add(menuItem("Export selected to Excel...", e -> doExport(true)));
        file.add(menuItem("Export all to Excel...", e -> doExport(false)));
        file.add(menuItem("Export selected to Word...", e -> doExportWord(true)));
        file.add(menuItem("Export all to Word...", e -> doExportWord(false)));
        file.addSeparator();
        file.add(menuItem("Print selected", e -> doPrint()));
        file.addSeparator();
        file.add(menuItem("Exit", e -> System.exit(0)));
        mb.add(file);

        JMenu edit = new JMenu("Edit");
        edit.add(menuItem("Delete selected", e -> doDelete()));
        edit.add(menuItem("Select all visible", e -> setAllVisibleSelected(true)));
        edit.add(menuItem("Deselect all", e -> setAllVisibleSelected(false)));
        mb.add(edit);

        JMenu articleMenu = new JMenu("Article");
        articleMenu.add(menuItem("View full article (checked/selected)", e -> viewFullArticle(getCheckedOrSelected())));
        mb.add(articleMenu);

        JMenu dbMenu = new JMenu("Database");
        dbMenu.add(menuItem("Browse saved outlines...", e -> browseSavedOutlines()));
        mb.add(dbMenu);
        return mb;
    }

    private JMenuItem menuItem(String text, ActionListener l) {
        JMenuItem mi = new JMenuItem(text);
        mi.addActionListener(l);
        return mi;
    }

    private JToolBar buildToolBar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(new EmptyBorder(6, 8, 6, 8));
        tb.setBackground(Theme.CARD_BG);
        tb.setOpaque(true);

        JButton fetchBtn = pastelButton("Fetch latest (free articles)", Theme.PASTEL_BLUE);
        fetchBtn.addActionListener(e -> doFetch());
        tb.add(fetchBtn);

        fetchFullTextCheck = new JCheckBox("Try full-article text on fetch (slower, best-effort)");
        fetchFullTextCheck.setOpaque(false);
        tb.add(fetchFullTextCheck);

        tb.addSeparator();
        JButton viewFullBtn = pastelButton("View full article & outline", Theme.PASTEL_VIOLET);
        viewFullBtn.setToolTipText("Check one or more boxes (or select rows), then click to open the Full Article window");
        viewFullBtn.addActionListener(e -> viewFullArticle(getCheckedOrSelected()));
        tb.add(viewFullBtn);

        tb.addSeparator();
        JButton saveBtn = new JButton("Save / Store");
        saveBtn.addActionListener(e -> doSave());
        tb.add(saveBtn);

        JButton loadBtn = new JButton("Retrieve");
        loadBtn.addActionListener(e -> loadFromDisk());
        tb.add(loadBtn);

        JButton delBtn = new JButton("Delete selected");
        delBtn.addActionListener(e -> doDelete());
        tb.add(delBtn);

        tb.addSeparator();
        JButton printBtn = new JButton("Print selected");
        printBtn.addActionListener(e -> doPrint());
        tb.add(printBtn);

        JButton xlsBtn = new JButton("Export Excel");
        xlsBtn.addActionListener(e -> doExport(true));
        tb.add(xlsBtn);

        JButton docBtn = new JButton("Export Word");
        docBtn.addActionListener(e -> doExportWord(true));
        tb.add(docBtn);

        tb.add(Box.createHorizontalGlue());
        tb.add(new JLabel("Search: "));
        searchField = new JTextField(18);
        searchField.getDocument().addDocumentListener((SimpleDocListener) e -> applyFilter());
        tb.add(searchField);

        return tb;
    }

    /** Pastel background + black-ish text: colorful but reliably readable (per request). */
    private JButton pastelButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Theme.TEXT_DARK);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return b;
    }

    private JSplitPane buildMainSplit() {
        tableModel = new ArticleTableModel(visibleArticles, allArticles);
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionBackground(Theme.PRIMARY_LIGHT);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(0xE5E9F0));
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(360);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(160);
        table.getColumnModel().getColumn(4).setCellRenderer(new SectorsCellRenderer());
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setCellRenderer(new ConnectedCellRenderer());
        table.getColumnModel().getColumn(6).setPreferredWidth(150);
        table.getColumnModel().getColumn(6).setCellRenderer(new LifecycleCellRenderer());
        table.getTableHeader().setBackground(Theme.PRIMARY_DARK);
        table.getTableHeader().setForeground(Theme.ACCENT_2);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onTableSelectionChanged();
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && currentlyEditing != null) {
                    viewFullArticle(List.of(currentlyEditing));
                }
            }
        });
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        categoryListModel = new DefaultListModel<>();
        categoryList = new JList<>(categoryListModel);
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.setBackground(Theme.CARD_BG);
        categoryList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) applyFilter(); });
        refreshCategoryList();
        JScrollPane catScroll = new JScrollPane(categoryList);
        catScroll.setPreferredSize(new Dimension(220, 0));
        catScroll.setBorder(titledBorder("CNBC Sections"));

        JSplitPane leftCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, catScroll, tableScroll);
        leftCenter.setDividerLocation(220);
        leftCenter.setBorder(null);

        JPanel detail = buildDetailPanel();
        JSplitPane full = new JSplitPane(JSplitPane.VERTICAL_SPLIT, leftCenter, detail);
        full.setDividerLocation(440);
        full.setResizeWeight(0.55);
        full.setBorder(null);
        return full;
    }

    private javax.swing.border.Border titledBorder(String title) {
        javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder(title);
        tb.setTitleColor(Theme.PRIMARY_DARK);
        tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD, 12f));
        return tb;
    }

    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(titledBorder("Article detail"));
        panel.setBackground(Theme.CARD_BG);

        titleArea = new JTextArea(2, 60);
        titleArea.setLineWrap(true);
        titleArea.setWrapStyleWord(true);
        titleArea.setEditable(false);
        titleArea.setOpaque(false);
        titleArea.setFont(titleArea.getFont().deriveFont(Font.BOLD, 16f));
        titleArea.setForeground(Theme.PRIMARY_DARK);

        metaLabel = new JLabel(" ");
        metaLabel.setForeground(Theme.TEXT_MUTED);
        metaLabel.setFont(metaLabel.getFont().deriveFont(11f));

        tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        tagPanel.setOpaque(false);

        lifecycleGauge = new LifecycleGauge();

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        titleArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        lifecycleGauge.setAlignmentX(Component.LEFT_ALIGNMENT);
        textCol.add(titleArea);
        textCol.add(metaLabel);
        textCol.add(tagPanel);
        textCol.add(lifecycleGauge);

        imageLabel = new JLabel("No image", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(200, 120));
        imageLabel.setForeground(Theme.TEXT_MUTED);
        imageLabel.setBorder(BorderFactory.createLineBorder(new Color(0xE5E9F0)));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Theme.BG_SOFT);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(textCol, BorderLayout.CENTER);
        top.add(imageLabel, BorderLayout.EAST);

        previewArea = new JTextArea();
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        previewArea.setBorder(new EmptyBorder(6, 6, 6, 6));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        JButton openFullBtn = pastelButton("Open full article & outline", Theme.PASTEL_VIOLET);
        openFullBtn.addActionListener(e -> {
            if (currentlyEditing != null) viewFullArticle(List.of(currentlyEditing));
        });
        JButton openLinkBtn = new JButton("Open article link");
        openLinkBtn.addActionListener(e -> openCurrentLink());
        buttons.add(openFullBtn);
        buttons.add(openLinkBtn);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(previewArea), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBorder(new EmptyBorder(4, 8, 4, 8));
        bar.setBackground(Theme.PRIMARY_DARK);
        statusLabel = new JLabel("Ready. Load saved articles from the toolbar, or fetch the latest from CNBC.");
        statusLabel.setForeground(Color.WHITE);
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(220, 16));
        progressBar.setVisible(false);
        bar.add(statusLabel, BorderLayout.CENTER);
        bar.add(progressBar, BorderLayout.EAST);
        return bar;
    }

    // ------------------------------------------------------------- fetch & derive

    private void doFetch() {
        boolean fullText = fetchFullTextCheck.isSelected();
        progressBar.setVisible(true);
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(FeedCatalog.FEEDS.size());

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                List<Article> fetched = fetcher.fetchAll(new RssFetcher.FetchProgressListener() {
                    @Override public void onProgress(int index, int total, String category) {
                        publish("Fetching " + category + " (" + index + "/" + total + ")");
                        SwingUtilities.invokeLater(() -> progressBar.setValue(index));
                    }
                    @Override public void onError(String category, String message) {
                        publish("Skipped " + category + ": " + message);
                    }
                });

                Set<String> existingLinks = new HashSet<>();
                for (Article a : allArticles) existingLinks.add(a.getLink());
                List<Article> brandNew = new ArrayList<>();
                for (Article a : fetched) {
                    if (a.getLink() != null && !a.getLink().isBlank() && existingLinks.add(a.getLink())) {
                        brandNew.add(a);
                    }
                }

                if (fullText) {
                    int i = 0;
                    for (Article a : brandNew) {
                        i++;
                        publish("Reading full article " + i + "/" + brandNew.size() + ": " + a.getTitle());
                        RssFetcher.PageContent pc = fetcher.tryFetchFullPage(a.getLink());
                        if (!pc.text.isBlank()) a.setFullText(pc.text);
                        if (pc.imageUrl != null && !pc.imageUrl.isBlank()) a.setImageUrl(pc.imageUrl);
                    }
                }

                allArticles.addAll(brandNew);

                publish("Grouping connected stories...");
                grouper.assignGroups(new ArrayList<>(allArticles));
                captureLifecycleBaselines();

                publish("Generating key-takeaway outlines and sector tags...");
                regenerateSummaries();

                publish("Done: " + brandNew.size() + " new article(s), " + allArticles.size() + " total.");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) statusLabel.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                refreshCategoryList();
                applyFilter();
            }
        };
        worker.execute();
    }

    /** Records each article's connected-cluster size the first time we see it, so later growth is detectable. */
    private void captureLifecycleBaselines() {
        Map<Integer, Integer> clusterSizes = new HashMap<>();
        for (Article a : allArticles) clusterSizes.merge(a.getGroupId(), 1, Integer::sum);
        for (Article a : allArticles) {
            if (a.getFirstClusterSize() == null) {
                a.setFirstClusterSize(clusterSizes.getOrDefault(a.getGroupId(), 1));
            }
        }
    }

    /** Recompute takeaways/outline/tags for every non-edited article, merging connected clusters' outlines. */
    private void regenerateSummaries() {
        Map<Integer, List<Article>> byGroup = new LinkedHashMap<>();
        for (Article a : allArticles) {
            byGroup.computeIfAbsent(a.getGroupId(), k -> new ArrayList<>()).add(a);
        }
        for (List<Article> group : byGroup.values()) {
            if (group.size() > 1) {
                List<String> merged = summarizer.summarizeGroup(group);
                outlineService.quickGenerateGroup(group, merged);
            } else {
                outlineService.quickGenerate(group.get(0));
            }
        }
    }

    // ------------------------------------------------------------- full article window

    private void viewFullArticle(List<Article> targets) {
        if (targets == null || targets.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Check the box next to (or select) at least one article first.");
            return;
        }
        for (Article a : targets) {
            FullArticleDialog dialog = new FullArticleDialog(this, a, fetcher, outlineService, outlineDb, exporter,
                    () -> { tableModel.fireTableDataChanged(); if (a == currentlyEditing) refreshDetailPanel(); });
            dialog.setVisible(true);
        }
    }

    private void browseSavedOutlines() {
        new SavedOutlinesDialog(this, outlineDb, fetcher, outlineService, exporter).setVisible(true);
    }

    // ------------------------------------------------------------- persistence & bulk actions

    private void doSave() {
        try {
            store.save(allArticles);
            statusLabel.setText("Saved " + allArticles.size() + " article(s) to " + store.getStoreFile());
        } catch (Exception ex) {
            showError("Could not save", ex);
        }
    }

    private void loadFromDisk() {
        List<Article> loaded = store.retrieve();
        for (Article a : loaded) {
            if (a.getOutline().isEmpty() || a.getIndustryTags().isEmpty()) {
                outlineService.quickGenerate(a);
            }
        }
        allArticles.clear();
        allArticles.addAll(loaded);
        captureLifecycleBaselines();
        refreshCategoryList();
        applyFilter();
        statusLabel.setText("Retrieved " + loaded.size() + " article(s) from " + store.getStoreFile());
    }

    private void doDelete() {
        List<Article> toDelete = getCheckedOrSelected();
        if (toDelete.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Check the box next to (or select) at least one article to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + toDelete.size() + " article(s)? This also removes them from storage on next Save.",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        allArticles.removeAll(toDelete);
        refreshCategoryList();
        applyFilter();
        statusLabel.setText("Deleted " + toDelete.size() + " article(s). Click Save/Store to persist.");
    }

    private void doExport(boolean selectedOnly) {
        List<Article> toExport = selectedOnly ? getSelectionOrAllVisible() : new ArrayList<>(visibleArticles);
        if (toExport.isEmpty()) { JOptionPane.showMessageDialog(this, "Nothing to export."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("cnbc-digest.xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = ensureExtension(chooser.getSelectedFile(), ".xlsx");
        try {
            exporter.exportExcel(toExport, out);
            statusLabel.setText("Exported " + toExport.size() + " article(s) to " + out);
        } catch (Exception ex) {
            showError("Export to Excel failed", ex);
        }
    }

    private void doExportWord(boolean selectedOnly) {
        List<Article> toExport = selectedOnly ? getSelectionOrAllVisible() : new ArrayList<>(visibleArticles);
        if (toExport.isEmpty()) { JOptionPane.showMessageDialog(this, "Nothing to export."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("cnbc-digest.docx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = ensureExtension(chooser.getSelectedFile(), ".docx");
        try {
            exporter.exportWord(toExport, out);
            statusLabel.setText("Exported " + toExport.size() + " article(s) to " + out);
        } catch (Exception ex) {
            showError("Export to Word failed", ex);
        }
    }

    private void doPrint() {
        List<Article> toPrint = getSelectionOrAllVisible();
        if (toPrint.isEmpty()) { JOptionPane.showMessageDialog(this, "Nothing to print."); return; }
        PrintUtil.print(this, toPrint);
    }

    /** Checked boxes (across all loaded articles) if any, otherwise the highlighted table rows. */
    private List<Article> getCheckedOrSelected() {
        List<Article> sel = new ArrayList<>();
        for (Article a : allArticles) if (a.isSelected()) sel.add(a);
        if (!sel.isEmpty()) return sel;
        for (int viewRow : table.getSelectedRows()) {
            Article a = tableModel.getArticleAt(table.convertRowIndexToModel(viewRow));
            if (!sel.contains(a)) sel.add(a);
        }
        return sel;
    }

    /** Checked/selected articles, or everything currently visible if nothing is picked (used by export/print). */
    private List<Article> getSelectionOrAllVisible() {
        List<Article> sel = getCheckedOrSelected();
        return sel.isEmpty() ? new ArrayList<>(visibleArticles) : sel;
    }

    private void setAllVisibleSelected(boolean value) {
        for (Article a : visibleArticles) a.setSelected(value);
        tableModel.fireTableDataChanged();
    }

    // ------------------------------------------------------------- detail panel

    private void onTableSelectionChanged() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            currentlyEditing = null;
            titleArea.setText("");
            metaLabel.setText(" ");
            tagPanel.removeAll();
            tagPanel.revalidate();
            tagPanel.repaint();
            previewArea.setText("");
            imageLabel.setIcon(null);
            imageLabel.setText("No image");
            lifecycleGauge.setState(null);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        currentlyEditing = tableModel.getArticleAt(modelRow);
        refreshDetailPanel();
    }

    private void refreshDetailPanel() {
        if (currentlyEditing == null) return;
        Article a = currentlyEditing;
        titleArea.setText(a.getTitle());

        boolean connected = StoryGrouper.isConnectedCluster(allArticles, a.getGroupId());
        metaLabel.setText("<html>" + esc(a.getCategory()) + "  |  Published: " + esc(a.getFormattedPubDate())
                + "  |  Fetched into app: " + esc(a.getFormattedFetchedAt())
                + (connected ? "  |  <b style='color:#2EC4B6'>Connected story</b>" : "")
                + (a.isEdited() ? "  |  <b>\u270E manually edited</b>" : "")
                + "</html>");

        tagPanel.removeAll();
        if (a.getIndustryTags() != null) {
            for (String tag : a.getIndustryTags()) tagPanel.add(new TagChip(tag, Theme.colorForTag(tag)));
        }
        tagPanel.revalidate();
        tagPanel.repaint();

        lifecycleGauge.setState(lifecycleAnalyzer.evaluate(a, StoryGrouper.clusterSize(allArticles, a.getGroupId())));

        previewArea.setText(a.getTakeaways().isEmpty()
                ? (a.getRawDescription().isBlank() ? "No summary available yet. Fetch, or open the full article to generate one."
                                                    : a.getRawDescription())
                : a.takeawaysAsBulletText());
        previewArea.setCaretPosition(0);

        imageLabel.setIcon(null);
        imageLabel.setText(a.getImageUrl() == null || a.getImageUrl().isBlank() ? "No image" : "Loading image...");
        if (a.getImageUrl() != null && !a.getImageUrl().isBlank()) {
            String url = a.getImageUrl();
            SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
                @Override protected ImageIcon doInBackground() { return ImageLoader.loadScaled(url, 200, 120); }
                @Override protected void done() {
                    if (currentlyEditing != a) return; // selection moved on while loading
                    try {
                        ImageIcon icon = get();
                        if (icon != null) {
                            imageLabel.setIcon(icon);
                            imageLabel.setText(null);
                        } else {
                            imageLabel.setText("No image");
                        }
                    } catch (Exception ex) {
                        imageLabel.setText("No image");
                    }
                }
            };
            worker.execute();
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void openCurrentLink() {
        if (currentlyEditing == null || currentlyEditing.getLink().isBlank()) return;
        if (!UrlSafety.isSafeToOpen(currentlyEditing.getLink())) {
            JOptionPane.showMessageDialog(this, "This article's link isn't a valid http/https URL, so it can't be opened.");
            return;
        }
        try {
            Desktop.getDesktop().browse(java.net.URI.create(currentlyEditing.getLink()));
        } catch (Exception ex) {
            showError("Could not open link", ex);
        }
    }

    // ------------------------------------------------------------- filtering

    private void refreshCategoryList() {
        String previouslySelected = categoryList.getSelectedValue();
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("All Sections", allArticles.size());
        for (String cat : FeedCatalog.FEEDS.keySet()) counts.put(cat, 0);
        for (Article a : allArticles) counts.merge(a.getCategory(), 1, Integer::sum);

        categoryListModel.clear();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            categoryListModel.addElement(e.getKey() + "  (" + e.getValue() + ")");
        }
        if (previouslySelected != null) {
            for (int i = 0; i < categoryListModel.size(); i++) {
                if (categoryListModel.get(i).startsWith(stripCount(previouslySelected))) {
                    categoryList.setSelectedIndex(i);
                    return;
                }
            }
        }
        categoryList.setSelectedIndex(0);
    }

    private String stripCount(String labelWithCount) {
        int idx = labelWithCount.lastIndexOf("  (");
        return idx > 0 ? labelWithCount.substring(0, idx) : labelWithCount;
    }

    private void applyFilter() {
        if (tableModel == null) return; // guard against events firing during initial UI construction
        String selectedLabel = categoryList.getSelectedValue();
        String selectedCategory = selectedLabel == null ? "All Sections" : stripCount(selectedLabel);
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        visibleArticles = new ArrayList<>();
        for (Article a : allArticles) {
            boolean categoryOk = selectedCategory.equals("All Sections") || selectedCategory.equals(a.getCategory());
            boolean queryOk = query.isEmpty()
                    || a.getTitle().toLowerCase(Locale.ROOT).contains(query)
                    || a.getRawDescription().toLowerCase(Locale.ROOT).contains(query);
            if (categoryOk && queryOk) visibleArticles.add(a);
        }
        tableModel.setRows(visibleArticles);
    }

    // ------------------------------------------------------------- helpers

    private File ensureExtension(File f, String ext) {
        if (f.getName().toLowerCase(Locale.ROOT).endsWith(ext)) return f;
        return new File(f.getParentFile(), f.getName() + ext);
    }

    private void showError(String context, Exception ex) {
        JOptionPane.showMessageDialog(this, context + ":\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface SimpleDocListener extends DocumentListener {
        void update(DocumentEvent e);
        @Override default void insertUpdate(DocumentEvent e) { update(e); }
        @Override default void removeUpdate(DocumentEvent e) { update(e); }
        @Override default void changedUpdate(DocumentEvent e) { update(e); }
    }
}
