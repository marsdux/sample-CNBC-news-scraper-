package newsaggre;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/** Renders the "Connected" column as a small colored pill instead of plain text. */
public class ConnectedCellRenderer extends JLabel implements TableCellRenderer {

    public ConnectedCellRenderer() {
        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setFont(getFont().deriveFont(Font.BOLD, 11f));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        String text = value == null ? "" : value.toString();
        boolean connected = text.startsWith("Yes");
        setText(text);
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            setBackground(connected ? new Color(0xE3FBF8) : new Color(0xF1F2F4));
            setForeground(connected ? Theme.CONNECTED.darker() : Theme.TEXT_MUTED);
        }
        return this;
    }
}
