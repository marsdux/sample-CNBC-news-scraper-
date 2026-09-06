package newsaggre;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads a stream into memory but refuses to hold more than a fixed byte budget - defense against a
 * pathological or malformed response (huge page, huge image, etc.) causing unbounded memory growth.
 */
public final class BoundedReader {

    private BoundedReader() {}

    /**
     * Reads up to maxBytes from in. Returns null if the stream exceeds that limit, rather than
     * silently truncating - a partial page or image is generally useless and easy to mistake for a
     * short-but-valid one, so callers should treat null the same as "couldn't be read."
     */
    public static byte[] readBounded(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(maxBytes, 1 << 16));
        byte[] chunk = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(chunk)) != -1) {
            total += n;
            if (total > maxBytes) return null;
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
    }
}
