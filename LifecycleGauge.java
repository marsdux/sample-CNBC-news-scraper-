package newsaggre;

import javax.swing.*;
import java.awt.*;

/** Horizontal gauge showing an article's current lifecycle score (0-100), colored by stage, with a summary label. */
public class LifecycleGauge extends JComponent {

    private LifecycleAnalyzer.LifecycleState state;

    public LifecycleGauge() {
        setPreferredSize(new Dimension(260, 34));
        setOpaque(false);
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
    }

    public void setState(LifecycleAnalyzer.LifecycleState state) {
        this.state = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int barHeight = 12;
        int barY = 2;

        g2.setColor(new Color(0xE5E9F0));
        g2.fillRoundRect(0, barY, w, barHeight, barHeight, barHeight);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();

        if (state != null) {
            int fillW = (int) Math.round(w * Math.max(0.03, state.currentScore / 100.0));
            Color fg = Theme.lifecycleForeground(state.stage);
            g2.setColor(fg);
            g2.fillRoundRect(0, barY, Math.min(fillW, w), barHeight, barHeight, barHeight);

            g2.setColor(Theme.TEXT_DARK);
            g2.drawString(state.summary(), 0, barY + barHeight + fm.getAscent() + 2);
        } else {
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString("No lifecycle data yet", 0, barY + barHeight + fm.getAscent() + 2);
        }
        g2.dispose();
    }
}
