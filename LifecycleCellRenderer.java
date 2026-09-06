package newsaggre;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/** Renders the "Life Cycle" column as a colored pill: stage name + current score out of 100. */
public class LifecycleCellRenderer extends JLabel implements TableCellRenderer {

    public LifecycleCellRenderer() {
        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setFont(getFont().deriveFont(Font.BOLD, 11f));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        if (!(value instanceof LifecycleAnalyzer.LifecycleState)) {
            setText("");
            setToolTipText(null);
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
        LifecycleAnalyzer.LifecycleState state = (LifecycleAnalyzer.LifecycleState) value;
        setText(state.stage.label() + " " + Math.round(state.currentScore));
        setToolTipText(state.summary());
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            setBackground(Theme.lifecycleBackground(state.stage));
            setForeground(Theme.lifecycleForeground(state.stage));
        }
        return this;
    }
}
