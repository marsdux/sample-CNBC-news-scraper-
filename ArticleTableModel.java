package newsaggre;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class ArticleTableModel extends AbstractTableModel {

    private static final String[] COLS = {"", "Category", "Title", "Published", "Sectors", "Connected", "Life Cycle", "Edited"};

    private final LifecycleAnalyzer lifecycleAnalyzer = new LifecycleAnalyzer();

    private List<Article> rows;
    private final List<Article> allArticlesRef; // full master list, needed to compute cluster sizes

    public ArticleTableModel(List<Article> rows, List<Article> allArticlesRef) {
        this.rows = rows;
        this.allArticlesRef = allArticlesRef;
    }

    public void setRows(List<Article> rows) {
        this.rows = rows;
        fireTableDataChanged();
    }

    public Article getArticleAt(int row) { return rows.get(row); }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return COLS.length; }
    @Override public String getColumnName(int c) { return COLS[c]; }

    @Override
    public Class<?> getColumnClass(int c) {
        if (c == 0) return Boolean.class;
        if (c == 6) return LifecycleAnalyzer.LifecycleState.class;
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 0;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (col == 0) {
            rows.get(row).setSelected(Boolean.TRUE.equals(value));
            fireTableCellUpdated(row, col);
        }
    }

    @Override
    public Object getValueAt(int row, int col) {
        Article a = rows.get(row);
        switch (col) {
            case 0: return a.isSelected();
            case 1: return a.getCategory();
            case 2: return a.getTitle();
            case 3: return a.getFormattedPubDate();
            case 4:
                List<String> tags = a.getIndustryTags();
                return (tags == null || tags.isEmpty()) ? "\u2014" : String.join(", ", tags);
            case 5:
                int count = clusterSize(a.getGroupId());
                return count > 1 ? "Yes (" + count + ")" : "No";
            case 6:
                return lifecycleAnalyzer.evaluate(a, clusterSize(a.getGroupId()));
            case 7: return a.isEdited() ? "\u270E edited" : "";
            default: return "";
        }
    }

    private int clusterSize(int groupId) {
        if (groupId < 0) return 1;
        int n = 0;
        for (Article a : allArticlesRef) if (a.getGroupId() == groupId) n++;
        return n;
    }
}
