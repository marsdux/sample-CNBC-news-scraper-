package newsaggre;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Downloads and scales an article's lead image. Synchronous - always call from a background thread, never the EDT. */
public final class ImageLoader {

    private ImageLoader() {}

    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0 Safari/537.36";

    // Hard cap on image size held in memory - defense against a pathological/huge response.
    private static final int MAX_IMAGE_BYTES = 8_000_000; // 8 MB

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Returns a scaled ImageIcon that fits within maxWidth x maxHeight, or null if the image can't
     * be loaded. url comes from parsed feed/page content, so it's validated before fetching -
     * defense-in-depth against a compromised feed pointing the app somewhere unexpected.
     */
    public static ImageIcon loadScaled(String url, int maxWidth, int maxHeight) {
        if (!UrlSafety.isSafeToFetch(url)) return null;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<InputStream> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() != 200) return null;
            byte[] bytes = BoundedReader.readBounded(resp.body(), MAX_IMAGE_BYTES);
            if (bytes == null) return null; // response too large - skip rather than risk OOM
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) return null;

            int w = img.getWidth(), h = img.getHeight();
            double scale = Math.min((double) maxWidth / w, (double) maxHeight / h);
            if (scale < 1.0) {
                int nw = Math.max(1, (int) (w * scale));
                int nh = Math.max(1, (int) (h * scale));
                Image scaled = img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
            return new ImageIcon(img);
        } catch (Exception e) {
            return null;
        }
    }
}
