package newsaggre;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats RSS pubDate strings and internal timestamps into readable, locale-friendly text. */
public final class DateUtils {

    private DateUtils() {}

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a zzz", Locale.ENGLISH);

    /** CNBC RSS pubDate is standard RFC-822/1123 (e.g. "Wed, 02 Jul 2025 10:15:00 -0400"). */
    public static String formatPubDate(String rawPubDate) {
        if (rawPubDate == null || rawPubDate.isBlank()) return "Publish date unavailable";
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(rawPubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.withZoneSameInstant(ZoneId.systemDefault()).format(DISPLAY);
        } catch (Exception e) {
            return rawPubDate; // fall back to the raw string rather than hiding it
        }
    }

    public static String formatMillis(long millis) {
        try {
            ZonedDateTime zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
            return zdt.format(DISPLAY);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /** Parses the RSS pubDate to epoch millis, or null if it can't be parsed. Used for time-decay math. */
    public static Long parsePubDateMillis(String rawPubDate) {
        if (rawPubDate == null || rawPubDate.isBlank()) return null;
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(rawPubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }
}
