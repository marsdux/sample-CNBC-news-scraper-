package newsaggre;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JTable;
import java.awt.*;

/** Colors the Sectors column text by its (first) industry tag so scanning the table is faster. */
public class SectorsCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setFont(getFont().deriveFont(Font.PLAIN, 12f));
        if (!isSelected) {
            String text = value == null ? "" : value.toString();
            if (text.isBlank() || text.equals("\u2014")) {
                setForeground(Theme.TEXT_MUTED);
            } else {
                String firstTag = text.split(",")[0].trim();
                setForeground(Theme.colorForTag(firstTag).darker());
            }
        }
        return this;
    }
}
