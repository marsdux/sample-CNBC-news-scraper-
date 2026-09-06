package newsaggre;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CNBC's own public RSS feeds, one per site section/category.
 * RSS is CNBC's sanctioned free/public content layer, and each feed already
 * maps 1:1 to a category CNBC itself uses on cnbc.com/rss-feeds/, so using
 * these both (a) keeps us on free content and (b) gives us the site's own
 * groupings for free, with no guesswork.
 *
 * If CNBC adds/retires a feed, just edit this map - nothing else changes.
 */
public final class FeedCatalog {

    private FeedCatalog() {}

    public static final Map<String, String> FEEDS = new LinkedHashMap<>();

    static {
        FEEDS.put("World News",         "https://www.cnbc.com/id/100727362/device/rss/rss.html");
        FEEDS.put("U.S. News",          "https://www.cnbc.com/id/15837362/device/rss/rss.html");
        FEEDS.put("Asia News",          "https://www.cnbc.com/id/19832390/device/rss/rss.html");
        FEEDS.put("Europe News",        "https://www.cnbc.com/id/19794221/device/rss/rss.html");
        FEEDS.put("Business News",      "https://www.cnbc.com/id/10001147/device/rss/rss.html");
        FEEDS.put("Earnings",           "https://www.cnbc.com/id/15839135/device/rss/rss.html");
        FEEDS.put("Commentary",         "https://www.cnbc.com/id/100370673/device/rss/rss.html");
        FEEDS.put("Economy",            "https://www.cnbc.com/id/20910258/device/rss/rss.html");
        FEEDS.put("Finance",            "https://www.cnbc.com/id/10000664/device/rss/rss.html");
        FEEDS.put("Technology News",    "https://www.cnbc.com/id/19854910/device/rss/rss.html");
        FEEDS.put("Politics",           "https://www.cnbc.com/id/10000113/device/rss/rss.html");
        FEEDS.put("Health and Science", "https://www.cnbc.com/id/10000108/device/rss/rss.html");
        FEEDS.put("Real Estate",        "https://www.cnbc.com/id/10000115/device/rss/rss.html");
        FEEDS.put("Wealth",             "https://www.cnbc.com/id/10001054/device/rss/rss.html");
        FEEDS.put("Autos",              "https://www.cnbc.com/id/10000101/device/rss/rss.html");
        FEEDS.put("Energy",             "https://www.cnbc.com/id/19836768/device/rss/rss.html");
        FEEDS.put("Media",              "https://www.cnbc.com/id/10000110/device/rss/rss.html");
        FEEDS.put("Retail",             "https://www.cnbc.com/id/10000116/device/rss/rss.html");
        FEEDS.put("Travel",             "https://www.cnbc.com/id/10000739/device/rss/rss.html");
        FEEDS.put("Small Business",     "https://www.cnbc.com/id/44877279/device/rss/rss.html");
        FEEDS.put("Investing",          "https://www.cnbc.com/id/15839069/device/rss/rss.html");
        FEEDS.put("Financial Advisors", "https://www.cnbc.com/id/100646281/device/rss/rss.html");
        FEEDS.put("Personal Finance",   "https://www.cnbc.com/id/21324812/device/rss/rss.html");
        FEEDS.put("Market Insider",     "https://www.cnbc.com/id/20409666/device/rss/rss.html?x=1");
        FEEDS.put("CEO Interviews",     "https://www.cnbc.com/id/100004032/device/rss/rss.html");
        FEEDS.put("Buffett Watch",      "https://www.cnbc.com/id/19206666/device/rss/rss.html");
        FEEDS.put("Mad Money w/ Cramer","https://www.cnbc.com/id/15838459/device/rss/rss.html");
        FEEDS.put("Trader Talk",        "https://www.cnbc.com/id/20398120/device/rss/rss.html");
        FEEDS.put("Squawk Box",         "https://www.cnbc.com/id/15838368/device/rss/rss.html");
        FEEDS.put("Squawk Box Asia",    "https://www.cnbc.com/id/15838831/device/rss/rss.html");
        FEEDS.put("Squawk Box Europe",  "https://www.cnbc.com/id/15838652/device/rss/rss.html");
        FEEDS.put("Options Action",     "https://www.cnbc.com/id/28282083/device/rss/rss.html");
        FEEDS.put("Net Net",            "https://www.cnbc.com/id/38818154/device/rss/rss.html");
        FEEDS.put("Kudlow's Corner",    "https://www.cnbc.com/id/15838446/device/rss/rss.html");
    }
}
