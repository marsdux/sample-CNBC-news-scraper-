package newsaggre;

import javax.swing.*;
import java.awt.*;

/** Abstract-gradient title banner shown at the top of the app for a bit of visual identity. */
public class GradientHeaderPanel extends JPanel {

    public GradientHeaderPanel(String title, String subtitle) {
        setPreferredSize(new Dimension(0, 72));
        setLayout(new BorderLayout());
        setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(new Color(255, 255, 255, 210));
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 0));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subtitleLabel);

        add(textPanel, BorderLayout.WEST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY_DARK, w, h, Theme.PRIMARY));
        g2.fillRect(0, 0, w, h);

        // abstract translucent circles for a bit of visual flair
        g2.setColor(new Color(255, 255, 255, 22));
        g2.fillOval(w - 170, -50, 220, 220);
        g2.setColor(new Color(255, 255, 255, 16));
        g2.fillOval(w - 280, 10, 130, 130);
        Color accent = Theme.ACCENT_2;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
        g2.fillOval(w - 95, 28, 55, 55);
        Color violet = Theme.VIOLET;
        g2.setColor(new Color(violet.getRed(), violet.getGreen(), violet.getBlue(), 55));
        g2.fillOval(w - 340, -20, 90, 90);

        g2.dispose();
        super.paintComponent(g);
    }
}
