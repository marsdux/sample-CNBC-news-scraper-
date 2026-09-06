package newsaggre;

import java.util.*;
import java.util.regex.*;

/**
 * Estimates how significant a story is right now: a base "how big a deal is
 * this" severity score derived from its content, decayed over time since
 * publication (real-world urgency naturally fades if nothing new happens),
 * unless the story's connected-article cluster keeps growing - which instead
 * signals a still-developing/escalating event and offsets the decay.
 *
 * Pure keyword/heuristic rule-based - no ML/API - so this is a rough proxy
 * for real-world impact, not a verified measurement of it. Treat the score
 * as a scanning aid, not a ground truth.
 */
public class LifecycleAnalyzer {

    /** Hours after which base severity has decayed to half its original value, absent new developments. */
    private static final double HALF_LIFE_HOURS = 36.0;

    private static final String[] HIGH_SEVERITY_TERMS = {
        "crisis", "collapse", "crash", "war", "invasion", "attack", "explosion", "disaster",
        "bankruptcy", "recession", "emergency", "historic", "unprecedented", "record high",
        "record low", "plunge", "plunged", "surge", "surged", "shutdown", "resign", "resigned",
        "fired", "sanctions", "default", "meltdown", "catastrophe", "scandal", "fraud", "hack",
        "breach", "outage", "recall"
    };
    private static final String[] MODERATE_SEVERITY_TERMS = {
        "warns", "warning", "concern", "risk", "volatil", "uncertain", "tension", "dispute",
        "investigation", "lawsuit", "layoffs", "job cuts", "decline", "downgrade", "strike", "delay"
    };

    private static final Pattern PERCENT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s?%");

    /** Content-only severity, 0-100, independent of how much time has passed. */
    public double computeBaseSeverity(Article a) {
        String text = (a.getTitle() + ". " + a.bestAvailableText()).toLowerCase(Locale.ROOT);
        double score = 20; // every published story has some baseline newsworthiness

        for (String kw : HIGH_SEVERITY_TERMS) if (text.contains(kw)) score += 10;
        for (String kw : MODERATE_SEVERITY_TERMS) if (text.contains(kw)) score += 5;

        double maxPercent = 0;
        Matcher m = PERCENT.matcher(text);
        while (m.find()) {
            try { maxPercent = Math.max(maxPercent, Double.parseDouble(m.group(1))); } catch (Exception ignored) {}
        }
        if (maxPercent >= 10) score += 20;
        else if (maxPercent >= 5) score += 12;
        else if (maxPercent >= 2) score += 6;

        if (a.getOutline() != null && !a.getOutline().isEmpty()) {
            Set<String> categoriesTouched = new HashSet<>();
            for (OutlineItem oi : a.getOutline()) categoriesTouched.add(oi.getCategory());
            if (categoriesTouched.size() >= 4) score += 10; // touches many PESTEL/market angles at once
        }
        if (a.getIndustryTags() != null && a.getIndustryTags().contains("Macro / Global Markets")) {
            score += 8;
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Live snapshot combining stored base severity, time decay since publish, and cluster growth.
     * Always recomputed fresh (never cached) since it depends on the current moment.
     */
    public LifecycleState evaluate(Article a, int currentClusterSize) {
        double severity = a.getSeverityScore() == null ? computeBaseSeverity(a) : a.getSeverityScore();

        Long publishedMillis = DateUtils.parsePubDateMillis(a.getPubDate());
        long referenceMillis = publishedMillis != null ? publishedMillis : a.getFetchedAt();
        double hoursElapsed = Math.max(0, (System.currentTimeMillis() - referenceMillis) / 3_600_000.0);

        int baseline = a.getFirstClusterSize() == null ? currentClusterSize : a.getFirstClusterSize();
        boolean growing = currentClusterSize > baseline;

        // A story that keeps generating new connected coverage is still "alive" in the real world,
        // so it effectively ages more slowly - the more it has grown past its baseline, the more its
        // decay clock is slowed, rather than just tacking a flat multiplier onto an already-stale score.
        double growthFactor = growing ? Math.max(0.15, 1.0 - 0.15 * (currentClusterSize - baseline)) : 1.0;
        double effectiveHours = hoursElapsed * growthFactor;
        double decay = Math.pow(0.5, effectiveHours / HALF_LIFE_HOURS);

        double current = Math.max(0, Math.min(100, severity * decay));

        Stage stage;
        if (growing && current >= 25) stage = Stage.DEVELOPING;
        else if (current >= 65) stage = Stage.BREAKING;
        else if (current >= 35) stage = Stage.ACTIVE;
        else if (current >= 12) stage = Stage.COOLING;
        else stage = Stage.ARCHIVED;

        return new LifecycleState(severity, current, growing, currentClusterSize, baseline,
                Math.round(hoursElapsed), stage);
    }

    /**
     * Convenience overload for contexts without access to the live, fully-grouped article list
     * (e.g. the Full Article window, or an outline reopened from the saved-outlines database).
     * Falls back to the article's own recorded baseline cluster size, so it never falsely
     * reports "growing" without real current data to compare against.
     */
    public LifecycleState evaluate(Article a) {
        int assumedClusterSize = a.getFirstClusterSize() == null ? 1 : a.getFirstClusterSize();
        return evaluate(a, assumedClusterSize);
    }

    public enum Stage {
        BREAKING("Breaking"), DEVELOPING("Developing"), ACTIVE("Active"),
        COOLING("Cooling"), ARCHIVED("Archived");

        private final String label;
        Stage(String label) { this.label = label; }
        public String label() { return label; }
    }

    /** Immutable point-in-time lifecycle read for one article. */
    public static final class LifecycleState implements Comparable<LifecycleState> {
        public final double baseSeverity;
        public final double currentScore;
        public final boolean growing;
        public final int clusterSize;
        public final int baselineClusterSize;
        public final long hoursSincePublished;
        public final Stage stage;

        public LifecycleState(double baseSeverity, double currentScore, boolean growing, int clusterSize,
                               int baselineClusterSize, long hoursSincePublished, Stage stage) {
            this.baseSeverity = baseSeverity;
            this.currentScore = currentScore;
            this.growing = growing;
            this.clusterSize = clusterSize;
            this.baselineClusterSize = baselineClusterSize;
            this.hoursSincePublished = hoursSincePublished;
            this.stage = stage;
        }

        public String ageText() {
            if (hoursSincePublished < 1) return "just published";
            if (hoursSincePublished < 24) return hoursSincePublished + "h old";
            return (hoursSincePublished / 24) + "d old";
        }

        public String summary() {
            String growthNote = growing
                    ? String.format(" \u2014 growing (%d related, up from %d)", clusterSize, baselineClusterSize)
                    : "";
            return String.format("%s \u2022 %.0f/100 \u2022 %s%s", stage.label(), currentScore, ageText(), growthNote);
        }

        @Override
        public int compareTo(LifecycleState other) {
            return Double.compare(this.currentScore, other.currentScore);
        }
    }
}
