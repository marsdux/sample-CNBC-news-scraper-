package newsaggre;

import java.util.*;

/**
 * Pure rule-based (no LLM/API) extractive summarizer:
 *  1. Split article text into sentences.
 *  2. Score each sentence by summed word-frequency of its non-stopword tokens
 *     (classic Luhn-style scoring), normalized by sentence length and boosted
 *     for sentences near the top (lede bias, since news puts key facts first).
 *  3. Take the top-scoring sentences as "key takeaways", preserving their
 *     original order for readability.
 *  4. When multiple connected articles are combined into one outline (see
 *     StoryGrouper), redundant/near-duplicate sentences across those articles
 *     are dropped so the same fact isn't repeated.
 */
public class Summarizer {

    private static final int MAX_TAKEAWAYS_PER_ARTICLE = 4;
    private static final int MAX_TAKEAWAYS_FULL_ARTICLE = 10;
    private static final double DUPLICATE_THRESHOLD = 0.5; // jaccard word overlap - lower = stricter de-duplication

    public List<String> summarize(Article article) {
        return summarize(article, MAX_TAKEAWAYS_PER_ARTICLE);
    }

    /** Richer pass intended to run against a full article page rather than the short RSS blurb. */
    public List<String> summarizeFull(Article article) {
        return summarize(article, MAX_TAKEAWAYS_FULL_ARTICLE);
    }

    private List<String> summarize(Article article, int maxTakeaways) {
        String text = article.bestAvailableText();
        List<String> sentences = TextUtils.splitSentences(text);
        if (sentences.isEmpty()) {
            // last resort: use the title itself as the single takeaway
            return article.getTitle().isBlank()
                    ? Collections.emptyList()
                    : new ArrayList<>(List.of(article.getTitle()));
        }
        List<String> ranked = rankSentences(sentences);
        List<String> top = ranked.subList(0, Math.min(maxTakeaways, ranked.size()));
        return dedupeInOriginalOrder(top, sentences);
    }

    /** Build one merged, deduplicated outline for a cluster of connected articles. */
    public List<String> summarizeGroup(List<Article> group) {
        List<String> merged = new ArrayList<>();
        for (Article a : group) {
            for (String t : (a.isEdited() ? a.getTakeaways() : summarize(a))) {
                if (!isRedundant(t, merged)) {
                    merged.add(t);
                }
            }
        }
        return merged;
    }

    private boolean isRedundant(String candidate, List<String> existing) {
        for (String e : existing) {
            if (TextUtils.sentenceSimilarity(candidate, e) >= DUPLICATE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private List<String> rankSentences(List<String> sentences) {
        Map<String, Integer> freq = new HashMap<>();
        for (String s : sentences) {
            for (String w : TextUtils.tokenize(s)) {
                freq.merge(w, 1, Integer::sum);
            }
        }
        List<double[]> scored = new ArrayList<>(); // [index, score]
        for (int i = 0; i < sentences.size(); i++) {
            List<String> words = TextUtils.tokenize(sentences.get(i));
            double score = 0;
            for (String w : words) score += freq.getOrDefault(w, 0);
            if (!words.isEmpty()) score /= Math.sqrt(words.size()); // avoid rewarding run-ons
            score *= ledeBoost(i, sentences.size());
            scored.add(new double[]{i, score});
        }
        scored.sort((a, b) -> Double.compare(b[1], a[1]));
        List<String> ranked = new ArrayList<>();
        for (double[] s : scored) ranked.add(sentences.get((int) s[0]));
        return ranked;
    }

    /** News ledes front-load facts; give earlier sentences a mild multiplier. */
    private double ledeBoost(int index, int total) {
        if (total <= 1) return 1.0;
        double position = (double) index / total;
        return 1.15 - (0.15 * position);
    }

    private List<String> dedupeInOriginalOrder(List<String> chosen, List<String> originalOrder) {
        List<String> deduped = new ArrayList<>();
        for (String s : originalOrder) {
            if (chosen.contains(s) && !isRedundant(s, deduped)) {
                deduped.add(s);
            }
        }
        return deduped;
    }
}
