package newsaggre;

import java.util.*;
import java.util.regex.*;

public final class TextUtils {

    private TextUtils() {}

    public static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "a","an","the","and","or","but","if","then","so","of","in","on","at","by","for","with",
        "about","against","between","into","through","during","before","after","above","below",
        "to","from","up","down","out","off","over","under","again","further","once","is","are",
        "was","were","be","been","being","have","has","had","having","do","does","did","doing",
        "will","would","shall","should","can","could","may","might","must","this","that","these",
        "those","i","you","he","she","it","we","they","them","his","her","its","our","their",
        "as","not","no","nor","too","very","s","t","just","don","now","also","said","says",
        "cnbc","new","its","it's","he's","she's","-","–","—"
    ));

    /** Strip HTML tags and decode the handful of entities RSS/HTML commonly use. */
    public static String stripHtml(String html) {
        if (html == null) return "";
        String noTags = html.replaceAll("(?is)<script.*?</script>", " ")
                             .replaceAll("(?is)<style.*?</style>", " ")
                             .replaceAll("(?is)<br\\s*/?>", ". ")
                             .replaceAll("(?is)</p>", ". ")
                             .replaceAll("(?is)<[^>]+>", " ");
        return decodeEntities(noTags).replaceAll("\\s+", " ").trim();
    }

    public static String decodeEntities(String s) {
        if (s == null) return "";
        String named = s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&rsquo;", "\u2019")
                .replace("&lsquo;", "\u2018")
                .replace("&rdquo;", "\u201D")
                .replace("&ldquo;", "\u201C")
                .replace("&mdash;", "\u2014")
                .replace("&ndash;", "\u2013");
        return decodeNumericEntities(named);
    }

    private static final Pattern HEX_ENTITY = Pattern.compile("&#[xX]([0-9a-fA-F]+);");
    private static final Pattern DEC_ENTITY = Pattern.compile("&#([0-9]+);");

    /**
     * Decodes any remaining numeric character references (e.g. "&#x27;" or "&#8217;") into the
     * actual character. HTML/RSS from real sites uses far more of these than any fixed named-entity
     * list can cover, so without this step stray literal "&#x27;"-style text leaks into outlines.
     */
    private static String decodeNumericEntities(String s) {
        s = replaceEntities(s, HEX_ENTITY, 16);
        s = replaceEntities(s, DEC_ENTITY, 10);
        return s;
    }

    private static String replaceEntities(String s, Pattern pattern, int radix) {
        Matcher m = pattern.matcher(s);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(s, last, m.start());
            try {
                sb.appendCodePoint(Integer.parseInt(m.group(1), radix));
            } catch (Exception e) {
                sb.append(m.group()); // leave malformed references untouched rather than dropping text
            }
            last = m.end();
        }
        sb.append(s.substring(last));
        return sb.toString();
    }

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z0-9\"\u201C])");

    public static List<String> splitSentences(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        for (String s : SENTENCE_SPLIT.split(text.trim())) {
            String trimmed = s.trim();
            if (trimmed.length() >= 15) { // skip fragments/junk
                out.add(trimmed);
            }
        }
        return out;
    }

    private static final Pattern WORD = Pattern.compile("[A-Za-z']{2,}");

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) return tokens;
        Matcher m = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) {
            String w = m.group();
            if (!STOPWORDS.contains(w) && w.length() > 2) {
                tokens.add(w);
            }
        }
        return tokens;
    }

    /** Top-N significant keywords by raw frequency, used for both scoring and clustering. */
    public static Set<String> topKeywords(String text, int n) {
        Map<String, Integer> freq = new HashMap<>();
        for (String w : tokenize(text)) {
            freq.merge(w, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freq.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());
        Set<String> out = new LinkedHashSet<>();
        for (int i = 0; i < Math.min(n, entries.size()); i++) {
            out.add(entries.get(i).getKey());
        }
        return out;
    }

    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    /** Word-set overlap between two sentences, used to drop near-duplicate takeaways. */
    public static double sentenceSimilarity(String a, String b) {
        Set<String> wa = new HashSet<>(tokenize(a));
        Set<String> wb = new HashSet<>(tokenize(b));
        return jaccard(wa, wb);
    }

    public static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
