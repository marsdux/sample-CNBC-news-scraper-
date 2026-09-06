package newsaggre;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Lets the user search saved outlines by title and reopen, delete, print, or export them. */
public class SavedOutlinesDialog extends JDialog {

    private final OutlineDatabase db;
    private final RssFetcher fetcher;
    private final OutlineService outlineService;
    private final OfficeExporter exporter;

    private DefaultListModel<String> listModel;
    private JList<String> titleList;
    private List<Article> currentResults;
    private JTextField searchField;

    public SavedOutlinesDialog(Frame owner, OutlineDatabase db, RssFetcher fetcher,
                                OutlineService outlineService, OfficeExporter exporter) {
        super(owner, "Saved Outlines Database", true);
        this.db = db;
        this.fetcher = fetcher;
        this.outlineService = outlineService;
        this.exporter = exporter;

        setSize(560, 480);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Theme.BG_SOFT);

        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setBorder(new EmptyBorder(10, 10, 4, 10));
        top.add(new JLabel("Search by title: "), BorderLayout.WEST);
        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
        top.add(searchField, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        titleList = new JList<>(listModel);
        add(new JScrollPane(titleList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buttons.setBorder(new EmptyBorder(0, 6, 6, 6));
        JButton openBtn = new JButton("Open");
        openBtn.addActionListener(e -> openSelected());
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> deleteSelected());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        buttons.add(openBtn);
        buttons.add(deleteBtn);
        buttons.add(closeBtn);
        add(buttons, BorderLayout.SOUTH);

        refresh();
    }

    private void refresh() {
        currentResults = db.searchByTitle(searchField.getText());
        listModel.clear();
        for (Article a : currentResults) listModel.addElement(a.getTitle());
    }

    private void openSelected() {
        int idx = titleList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Select a saved outline first.");
            return;
        }
        Article a = currentResults.get(idx);
        new FullArticleDialog((Frame) getOwner(), a, fetcher, outlineService, db, exporter, null).setVisible(true);
    }

    private void deleteSelected() {
        int idx = titleList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Select a saved outline first.");
            return;
        }
        Article a = currentResults.get(idx);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete the saved outline for \u201C" + a.getTitle() + "\u201D from the database?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            db.deleteByTitle(a.getTitle());
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not delete: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
