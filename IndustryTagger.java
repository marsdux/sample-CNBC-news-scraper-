package newsaggre;

import java.util.*;

/**
 * Tags an article with the economic sector(s)/industries it relates to, so
 * you can see at a glance how a headline connects to broader global market
 * conditions (e.g. an OPEC story tags "Energy" + "Macro / Global Markets").
 * Pure keyword-rule based - no ML/API involved. Assigns up to 3 tags, ranked
 * by keyword-match strength.
 */
public class IndustryTagger {

    private static final Map<String, String[]> SECTOR_KEYWORDS = new LinkedHashMap<>();
    static {
        SECTOR_KEYWORDS.put("Technology", new String[]{
            "tech ", "technology", "software", "artificial intelligence", " ai ", "chip", "chips",
            "semiconductor", "cloud computing", "cybersecurity", "data center", "smartphone",
            "internet", "computing", "chatbot", "app store"
        });
        SECTOR_KEYWORDS.put("Financial Services", new String[]{
            "bank ", "banking", "lender", "credit ", "loan ", "mortgage rate", "insurer", "insurance",
            "fintech", "hedge fund", "private equity", "asset manager", "wall street", "brokerage"
        });
        SECTOR_KEYWORDS.put("Energy", new String[]{
            "oil ", "oil price", "gas price", "crude", "opec", "energy ", "renewable", "solar",
            "wind power", "pipeline", "drilling", "barrel", "electricity", "utility", "natural gas"
        });
        SECTOR_KEYWORDS.put("Healthcare & Pharma", new String[]{
            "health ", "hospital", "drug ", "pharma", "vaccine", "biotech", " fda ", "medicare",
            "clinical trial", "medical device", "insurer"
        });
        SECTOR_KEYWORDS.put("Retail & Consumer", new String[]{
            "retail", "retailer", "consumer spending", "shopper", "store ", "e-commerce",
            "holiday shopping", "brand ", "apparel"
        });
        SECTOR_KEYWORDS.put("Automotive", new String[]{
            "car ", "auto ", "vehicle", " ev ", "electric vehicle", "automaker", "tesla", "ford",
            "general motors", "toyota"
        });
        SECTOR_KEYWORDS.put("Real Estate", new String[]{
            "housing", "home price", "mortgage", "real estate", "property", "rent ", "homebuilder",
            "home sales"
        });
        SECTOR_KEYWORDS.put("Industrials & Manufacturing", new String[]{
            "factory", "manufacturing", "industrial", "supply chain", "steel", "factory output",
            "production line"
        });
        SECTOR_KEYWORDS.put("Telecom & Media", new String[]{
            "telecom", "wireless", "streaming", "media ", "broadcast", "advertising", "5g",
            "subscribers"
        });
        SECTOR_KEYWORDS.put("Agriculture & Commodities", new String[]{
            "crop", "farm ", "agriculture", "wheat", "corn ", "soybean", "commodity", "gold price",
            "silver", "metal price"
        });
        SECTOR_KEYWORDS.put("Transportation & Logistics", new String[]{
            "airline", "shipping", "freight", "logistics", "railroad", "flight delays", "cargo"
        });
        SECTOR_KEYWORDS.put("Macro / Global Markets", new String[]{
            "inflation", "interest rate", "federal reserve", "the fed", "gdp", "recession", "tariff",
            "trade war", "central bank", "currency", "dollar index", "treasury yield",
            "global economy", "jobs report", "unemployment"
        });
    }

    public List<String> tag(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        String lower = " " + text.toLowerCase(Locale.ROOT) + " ";
        List<Map.Entry<String, Integer>> scored = new ArrayList<>();
        for (Map.Entry<String, String[]> e : SECTOR_KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : e.getValue()) {
                if (lower.contains(kw)) score++;
            }
            if (score > 0) scored.add(Map.entry(e.getKey(), score));
        }
        scored.sort((a, b) -> b.getValue() - a.getValue());
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < Math.min(3, scored.size()); i++) tags.add(scored.get(i).getKey());
        return tags;
    }
}
