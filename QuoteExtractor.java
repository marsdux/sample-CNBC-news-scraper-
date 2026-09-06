package newsaggre;

import java.util.*;
import java.util.regex.*;

/**
 * Pulls direct quotations out of the full article text exactly as written -
 * unlike the extractive summarizer, quotes are never paraphrased, reworded,
 * or cut short. Handles both straight and curly double quotes.
 */
public class QuoteExtractor {

    private static final Pattern QUOTE =
            Pattern.compile("[\"\u201C]([^\"\u201C\u201D]{20,500})[\"\u201D]");

    private static final int MAX_QUOTES = 6;

    public List<String> extract(String text) {
        List<String> quotes = new ArrayList<>();
        if (text == null || text.isBlank()) return quotes;
        Matcher m = QUOTE.matcher(text);
        Set<String> seen = new HashSet<>();
        while (m.find() && quotes.size() < MAX_QUOTES) {
            String q = m.group(1).trim();
            if (q.isEmpty() || !seen.add(q.toLowerCase(Locale.ROOT))) continue;
            quotes.add("\u201C" + q + "\u201D");
        }
        return quotes;
    }
}
