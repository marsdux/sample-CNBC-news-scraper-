package newsaggre;

import java.awt.Color;

/** Shared color palette so the app reads as one coherent, colorful design rather than default gray Swing. */
public final class Theme {

    private Theme() {}

    public static final Color PRIMARY_DARK  = new Color(0x1B2A4A);
    public static final Color PRIMARY       = new Color(0x2E5EAA);
    public static final Color PRIMARY_LIGHT = new Color(0x5B8DEF);
    public static final Color ACCENT        = new Color(0xFF6B6B);
    public static final Color ACCENT_2      = new Color(0xFFC857);
    public static final Color ACCENT_3      = new Color(0x2EC4B6);
    public static final Color VIOLET        = new Color(0x9B5DE5);

    public static final Color BG_SOFT    = new Color(0xF4F6FB);
    public static final Color CARD_BG    = Color.WHITE;
    public static final Color TEXT_DARK  = new Color(0x111827);
    public static final Color TEXT_MUTED = new Color(0x6B7280);

    // Pastel tints for buttons: colorful but pair with black text at good contrast (per readability request)
    public static final Color PASTEL_BLUE   = new Color(0xB7C9EA);
    public static final Color PASTEL_VIOLET = new Color(0xD9C9F0);
    public static final Color PASTEL_TEAL   = new Color(0xB7EFE8);
    public static final Color PASTEL_CORAL  = new Color(0xFFD1CC);
    public static final Color PASTEL_GOLD   = new Color(0xFFE6A7);

    public static final Color CONNECTED  = new Color(0x2EC4B6);
    public static final Color STANDALONE = new Color(0x9AA3B2);

    private static final Color[] TAG_PALETTE = {
        new Color(0x5B8DEF), new Color(0xFF6B6B), new Color(0x2EC4B6), new Color(0xE0A100),
        new Color(0x9B5DE5), new Color(0xF15BB5), new Color(0x0092C7), new Color(0xFB8500),
        new Color(0x06A77D), new Color(0xEF476F), new Color(0x118AB2), new Color(0x8338EC)
    };

    /** Deterministic color per tag name, so the same industry always renders the same color. */
    public static Color colorForTag(String tag) {
        if (tag == null || tag.isBlank()) return PRIMARY_LIGHT;
        int hash = Math.abs(tag.hashCode());
        return TAG_PALETTE[hash % TAG_PALETTE.length];
    }

    public static Color outlineCategoryColor(String category) {
        if (category == null) return PRIMARY;
        switch (category) {
            case OutlineBuilder.CAT_EVENT:   return ACCENT;
            case OutlineBuilder.CAT_MARKET:  return ACCENT_3;
            case OutlineBuilder.CAT_FIGURES: return new Color(0xC98A00);
            case OutlineBuilder.CAT_OUTLOOK: return VIOLET;
            default:                          return PRIMARY;
        }
    }

    public static String toHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static Color lifecycleBackground(LifecycleAnalyzer.Stage stage) {
        switch (stage) {
            case BREAKING:   return new Color(0xFFE1DE);
            case DEVELOPING: return new Color(0xFFF2CC);
            case ACTIVE:     return new Color(0xE3ECFF);
            case COOLING:    return new Color(0xEDEFF3);
            case ARCHIVED:   return new Color(0xF1F2F4);
            default:         return new Color(0xF1F2F4);
        }
    }

    public static Color lifecycleForeground(LifecycleAnalyzer.Stage stage) {
        switch (stage) {
            case BREAKING:   return ACCENT.darker();
            case DEVELOPING: return new Color(0xB07C00);
            case ACTIVE:     return PRIMARY.darker();
            case COOLING:    return TEXT_MUTED;
            case ARCHIVED:   return STANDALONE.darker();
            default:         return TEXT_MUTED;
        }
    }
}
