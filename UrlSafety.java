package newsaggre;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Defense-in-depth checks before this app fetches or opens a URL that came from parsed feed/page
 * content (article links, image URLs) rather than its own hardcoded feed list. Feeds are fixed,
 * trusted CNBC HTTPS URLs, so this is a backstop against a compromised feed delivery trying to point
 * the app somewhere unexpected - not a defense the app depends on day to day.
 */
public final class UrlSafety {

    private UrlSafety() {}

    /** For network fetches (HttpClient): http/https only, and refuses private/loopback/link-local hosts. */
    public static boolean isSafeToFetch(String url) {
        URI uri = parseHttpUri(url);
        if (uri == null) return false;
        String host = uri.getHost();
        if (host == null || host.isBlank()) return false;
        return !isPrivateOrLocal(host);
    }

    /** For opening in the user's default browser (Desktop.browse): just requires http/https. */
    public static boolean isSafeToOpen(String url) {
        return parseHttpUri(url) != null;
    }

    private static URI parseHttpUri(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null) return null;
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return null;
            return uri;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isPrivateOrLocal(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        if (h.equals("localhost") || h.endsWith(".local")) return true;
        try {
            for (InetAddress addr : InetAddress.getAllByName(host)) {
                if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()
                        || addr.isAnyLocalAddress() || addr.isMulticastAddress()) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            return true; // fail closed: if we can't resolve it, don't fetch it
        }
        return false;
    }
}
