package newsaggre;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** A small rounded, colored "pill" label - used for industry tags and status badges. */
public class TagChip extends JComponent {

    private final String text;
    private final Color background;

    public TagChip(String text, Color background) {
        this.text = text;
        this.background = background;
        setOpaque(false);
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

        FontMetrics fm = measureFont(getFont());
        int w = fm.stringWidth(text) + 20;
        int h = fm.getHeight() + 8;
        Dimension d = new Dimension(w, h);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    private FontMetrics measureFont(Font f) {
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tmp.createGraphics();
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        g2.dispose();
        return fm;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(text)) / 2;
        int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, textX, textY);
        g2.dispose();
    }
}
