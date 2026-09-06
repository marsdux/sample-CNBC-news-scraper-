package newsaggre;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

/**
 * Pulls items from CNBC's public RSS feeds (free content only - RSS never
 * includes CNBC Pro / paywalled stories) and, best-effort, tries to fetch the
 * full article page - both its readable body text and its lead image
 * (og:image) - so summaries/outlines aren't limited to the short RSS blurb.
 *
 * Full-page fetching is opportunistic: news sites frequently run bot
 * detection in front of scrapers. If a fetch is blocked or fails, we simply
 * fall back to the RSS title+description (and RSS-provided thumbnail, if
 * any), which are always available.
 */
public class RssFetcher {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0 Safari/537.36";

    // Hard caps on how much of any single response we'll hold in memory, so a pathological or
    // misbehaving response can't cause unbounded memory growth. Both are far larger than any
    // legitimate RSS feed or article page needs.
    private static final int MAX_FEED_BYTES = 5_000_000;   // 5 MB
    private static final int MAX_PAGE_BYTES = 3_000_000;   // 3 MB

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Fetch and parse every configured CNBC feed. Network/parse errors on one feed don't stop the rest. */
    public List<Article> fetchAll(FetchProgressListener listener) {
        List<Article> all = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, String> feed : FeedCatalog.FEEDS.entrySet()) {
            i++;
            String category = feed.getKey();
            String url = feed.getValue();
            if (listener != null) listener.onProgress(i, FeedCatalog.FEEDS.size(), category);
            try {
                all.addAll(fetchFeed(category, url));
            } catch (Exception e) {
                if (listener != null) listener.onError(category, e.getMessage());
            }
        }
        return all;
    }

    /** feedUrl always comes from our own hardcoded FeedCatalog, not parsed content, so no URL validation needed here. */
    public List<Article> fetchFeed(String category, String feedUrl) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(feedUrl))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/rss+xml, application/xml, text/xml")
                .timeout(Duration.ofSeconds(15))
                .GET().build();
        HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " for " + feedUrl);
        }
        byte[] bytes = BoundedReader.readBounded(resp.body(), MAX_FEED_BYTES);
        if (bytes == null) {
            throw new IOException("Feed response exceeded size limit for " + feedUrl);
        }
        return parseRss(new ByteArrayInputStream(bytes), category);
    }

    private List<Article> parseRss(InputStream in, String category) throws Exception {
        List<Article> items = new ArrayList<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(in);
        NodeList itemNodes = doc.getElementsByTagName("item");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element el = (Element) itemNodes.item(i);
            Article a = new Article();
            a.setCategory(category);
            a.setTitle(TextUtils.decodeEntities(text(el, "title")));
            a.setLink(text(el, "link").trim());
            a.setRawDescription(TextUtils.stripHtml(text(el, "description")));
            a.setPubDate(text(el, "pubDate"));
            String img = extractItemImage(el);
            if (img != null) a.setImageUrl(img);
            if (!a.getTitle().isBlank()) items.add(a);
        }
        return items;
    }

    private String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        Node n = nl.item(0);
        return n.getTextContent() == null ? "" : n.getTextContent();
    }

    /** Looks for a usable lead image in the RSS item: &lt;enclosure&gt;, media:content, or media:thumbnail. */
    private String extractItemImage(Element item) {
        NodeList enclosures = item.getElementsByTagName("enclosure");
        for (int i = 0; i < enclosures.getLength(); i++) {
            Element enc = (Element) enclosures.item(i);
            String type = enc.getAttribute("type");
            String url = enc.getAttribute("url");
            if (!url.isBlank() && (type.isBlank() || type.startsWith("image"))) return url;
        }
        NodeList mediaContent = item.getElementsByTagName("media:content");
        for (int i = 0; i < mediaContent.getLength(); i++) {
            String url = ((Element) mediaContent.item(i)).getAttribute("url");
            if (!url.isBlank()) return url;
        }
        NodeList mediaThumb = item.getElementsByTagName("media:thumbnail");
        for (int i = 0; i < mediaThumb.getLength(); i++) {
            String url = ((Element) mediaThumb.item(i)).getAttribute("url");
            if (!url.isBlank()) return url;
        }
        return null;
    }

    /**
     * Best-effort fetch of an article page's readable text plus its lead image URL.
     * Never throws - returns an empty PageContent if the page can't be read, so
     * callers can fall back to the RSS description/thumbnail.
     *
     * articleUrl comes from parsed feed content (not our own hardcoded list), so unlike fetchFeed()
     * this validates the URL before fetching - defense-in-depth against a compromised feed pointing
     * the app somewhere unexpected.
     */
    public PageContent tryFetchFullPage(String articleUrl) {
        if (!UrlSafety.isSafeToFetch(articleUrl)) return new PageContent("", null);
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(articleUrl))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html")
                    .timeout(Duration.ofSeconds(12))
                    .GET().build();
            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() != 200) return new PageContent("", null);
            byte[] bytes = BoundedReader.readBounded(resp.body(), MAX_PAGE_BYTES);
            if (bytes == null) return new PageContent("", null); // response too large - skip rather than risk OOM
            String html = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            return new PageContent(extractParagraphs(html), extractOgImage(html));
        } catch (Exception e) {
            return new PageContent("", null);
        }
    }

    private static final Pattern P_TAG = Pattern.compile("(?is)<p[^>]*>(.*?)</p>");

    private String extractParagraphs(String html) {
        // Drop whole navigation/header/footer/sidebar blocks up front - site chrome (menus, sign-in
        // prompts, watchlist widgets, etc.) is often marked up with its own <p> tags too, so this
        // needs to happen before we even start collecting paragraphs.
        String cleaned = html
                .replaceAll("(?is)<nav.*?</nav>", " ")
                .replaceAll("(?is)<header.*?</header>", " ")
                .replaceAll("(?is)<footer.*?</footer>", " ")
                .replaceAll("(?is)<aside.*?</aside>", " ");

        StringBuilder sb = new StringBuilder();
        Matcher m = P_TAG.matcher(cleaned);
        while (m.find()) {
            String para = TextUtils.stripHtml(m.group(1));
            if (para.length() > 40
                    && !para.toLowerCase(Locale.ROOT).startsWith("watch:")
                    && !looksLikeSiteChrome(para)) {
                sb.append(para).append(" ");
            }
        }
        return sb.toString().trim();
    }

    // Phrases from CNBC's own nav/header widgets (livestream ticker, sign-in prompt, watchlist,
    // section menu, etc.) that real article prose essentially never contains together.
    private static final String[] CHROME_MARKERS = {
        "sign in", "create free account", "livestream", "watchlist", "search quotes, news",
        "investing club", "make it", "closed captioning", "sign up for", "newsletters",
        "market data by", "data is a real-time snapshot", "terms of service", "privacy policy"
    };

    /** Catches leftover site-chrome text (menus, sign-in bars, tickers) that slipped past the block strip above. */
    private boolean looksLikeSiteChrome(String para) {
        String lower = para.toLowerCase(Locale.ROOT);
        int markerHits = 0;
        for (String marker : CHROME_MARKERS) {
            if (lower.contains(marker)) markerHits++;
        }
        if (markerHits >= 2) return true;

        // Nav/menu strings tend to be short runs of Capitalized Words with no sentence punctuation
        // ("Markets Business Investing Tech Politics & Policy Video Watchlist"), unlike real prose.
        long sentenceEnders = para.chars().filter(c -> c == '.' || c == '!' || c == '?').count();
        if (para.length() < 300 && sentenceEnders == 0) {
            String[] words = para.trim().split("\\s+");
            int capWords = 0;
            for (String w : words) {
                if (!w.isEmpty() && Character.isUpperCase(w.charAt(0))) capWords++;
            }
            if (words.length >= 4 && (double) capWords / words.length > 0.6) return true;
        }
        return false;
    }

    private static final Pattern OG_IMAGE_1 =
            Pattern.compile("(?is)<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']");
    private static final Pattern OG_IMAGE_2 =
            Pattern.compile("(?is)<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']og:image[\"']");

    private String extractOgImage(String html) {
        Matcher m1 = OG_IMAGE_1.matcher(html);
        if (m1.find()) return TextUtils.decodeEntities(m1.group(1));
        Matcher m2 = OG_IMAGE_2.matcher(html);
        if (m2.find()) return TextUtils.decodeEntities(m2.group(1));
        return null;
    }

    /** Result of fetching an article page: its readable body text and its lead image URL (either may be missing). */
    public static class PageContent {
        public final String text;
        public final String imageUrl;
        public PageContent(String text, String imageUrl) {
            this.text = text == null ? "" : text;
            this.imageUrl = imageUrl;
        }
    }

    public interface FetchProgressListener {
        void onProgress(int index, int total, String category);
        void onError(String category, String message);
    }
}
