package newsaggre;

import java.util.*;

/**
 * Turns a flat list of takeaway sentences into a categorized outline:
 *  - Key Event: what actually happened
 *  - Market Analysis & Impact: how it moved/affects markets, investors, valuations
 *  - Figures & Data: hard numbers
 *  - Political / Economic / Social / Technological / Environmental / Legal:
 *    a lightweight PESTEL read on which broader forces the story touches
 *  - Outlook & Analysts: forward-looking statements
 *  - Key Fact: anything left over
 * ("Notable Quotes" is a valid category too, but populated separately by
 * QuoteExtractor directly from the source text, not by classify() here.)
 *
 * Pure keyword-rule based - no ML/API involved. This can only surface PESTEL
 * angles that are actually present in the source text; it can't invent
 * analysis the article doesn't contain.
 */
public class OutlineBuilder {

    public static final String CAT_EVENT       = "Key Event";
    public static final String CAT_QUOTE       = "Notable Quotes";
    public static final String CAT_MARKET      = "Market Analysis & Impact";
    public static final String CAT_FIGURES     = "Figures & Data";
    public static final String CAT_POLITICAL   = "Political";
    public static final String CAT_ECONOMIC    = "Economic";
    public static final String CAT_LEGAL       = "Legal";
    public static final String CAT_TECH        = "Technological";
    public static final String CAT_ENVIRONMENT = "Environmental";
    public static final String CAT_SOCIAL      = "Social";
    public static final String CAT_OUTLOOK     = "Outlook & Analysts";
    public static final String CAT_FACT        = "Key Fact";

    /** Order categories should be displayed/grouped in. */
    public static final List<String> DISPLAY_ORDER = List.of(
            CAT_EVENT, CAT_QUOTE, CAT_MARKET, CAT_FIGURES,
            CAT_POLITICAL, CAT_ECONOMIC, CAT_LEGAL, CAT_TECH, CAT_ENVIRONMENT, CAT_SOCIAL,
            CAT_OUTLOOK, CAT_FACT
    );

    // Order here also breaks ties when a sentence scores equally in two categories - more specific/narrow
    // vocabularies are checked first, with the generic "Key Event" bucket checked last among these.
    private static final Map<String, String[]> KEYWORDS = new LinkedHashMap<>();
    static {
        KEYWORDS.put(CAT_MARKET, new String[]{
            "shares", "stock", "stocks", "rose", "fell", "dropped", "rallied", "surged", "tumbled",
            "gained", "lost", "index", "dow", "s&p", "nasdaq", "rally", "sell-off", "selloff",
            "climbed", "slid", "plunged", "closed higher", "closed lower", "investor", "valuation",
            "market cap", "impact on", "weighed on", "weigh on", "boosted", "dragged"
        });
        KEYWORDS.put(CAT_OUTLOOK, new String[]{
            "expects", "expected", "forecast", "outlook", "analysts", "estimate", "estimates",
            "projected", "guidance", "likely", "could", "may ", "anticipate", "predicts", "project"
        });
        KEYWORDS.put(CAT_LEGAL, new String[]{
            "lawsuit", "court", "litigation", "compliance", "legal action", "antitrust", "ruling",
            "judge", "settlement", "sec filing", "investigation", "subpoena", "indictment", "regulator"
        });
        KEYWORDS.put(CAT_POLITICAL, new String[]{
            "government", "congress", "senate", "white house", "president", "election", "policy",
            "regulation", "lawmakers", "administration", "geopolitical", "sanctions", "diplomatic",
            "parliament", "governor", "vote in", "legislation"
        });
        KEYWORDS.put(CAT_ECONOMIC, new String[]{
            "gdp", "inflation", "interest rate", "recession", "economy", "unemployment", "fiscal",
            "monetary policy", "trade deficit", "economic growth", "consumer price index", " cpi ",
            "tariff", "trade war", "central bank", "the fed", "federal reserve"
        });
        KEYWORDS.put(CAT_TECH, new String[]{
            "technology", "innovation", "artificial intelligence", " ai ", "software", "digital",
            "automation", "cybersecurity", "data breach", "semiconductor", "chip", "cloud computing"
        });
        KEYWORDS.put(CAT_ENVIRONMENT, new String[]{
            "climate", "emissions", "carbon", "renewable", "sustainability", "environment",
            "pollution", "green energy", "esg", "clean energy"
        });
        KEYWORDS.put(CAT_SOCIAL, new String[]{
            "consumer spending", "public opinion", "society", "workers", "employees", "labor union",
            "labor group", "demographic", "social media", "workforce", "job cuts", "layoffs", "hiring"
        });
        KEYWORDS.put(CAT_EVENT, new String[]{
            "announced", "launched", "reported", "filed", "agreed", "signed", "acquired",
            "acquisition", "merger", "cut ", "raised", "hiked", "fired", "resigned", "appointed",
            "plans to", "said it will", "unveiled", "introduced", "confirmed", "approved", "rejected",
            "proposed", "warned"
        });
    }

    public List<OutlineItem> build(List<String> sentences) {
        List<OutlineItem> items = new ArrayList<>();
        if (sentences == null) return items;
        for (String s : sentences) {
            items.add(new OutlineItem(classify(s), s));
        }
        return items;
    }

    private String classify(String sentence) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        String best = null;
        int bestScore = 0;
        for (Map.Entry<String, String[]> e : KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : e.getValue()) {
                if (lower.contains(kw)) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                best = e.getKey();
            }
        }
        if (best != null) return best;
        if (looksNumeric(lower)) return CAT_FIGURES;
        return CAT_FACT;
    }

    private boolean looksNumeric(String lower) {
        boolean hasDigit = lower.chars().anyMatch(Character::isDigit);
        return hasDigit && (lower.contains("%") || lower.contains("$") || lower.contains("percent")
                || lower.contains("billion") || lower.contains("million") || lower.contains("trillion"));
    }
}
