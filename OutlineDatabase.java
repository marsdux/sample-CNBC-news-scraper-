package newsaggre;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Persists individually-generated article outlines (from the Full Article
 * window), keyed by the article's title, so a previously generated outline
 * can be searched for and reloaded later by title - independent of whether
 * the article is still in the main fetched list.
 *
 * This is a simple embedded file-based store rather than a SQL database:
 * the app is built with zero third-party dependencies (no JDBC driver to
 * download), so this is the "database" available without adding a build
 * tool. It still gives real CRUD semantics keyed by title.
 */
public class OutlineDatabase {

    private final Path dbFile;

    public OutlineDatabase() {
        Path dir = Paths.get(System.getProperty("user.home"), ".newsaggre");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        this.dbFile = dir.resolve("outline_database.ser");
    }

    public Path getDbFile() { return dbFile; }

    private String key(String title) {
        return title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private synchronized Map<String, Article> loadIndex() {
        if (!Files.exists(dbFile)) return new LinkedHashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(dbFile)))) {
            SerializationSafety.harden(ois);
            Map<String, Article> map = (Map<String, Article>) ois.readObject();
            for (Article a : map.values()) a.ensureDefaults();
            return map;
        } catch (Exception e) {
            System.err.println("Could not read outline database: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /** Writes to a temp file then atomically renames it into place, so a crash mid-write can't leave a corrupt file. */
    private synchronized void writeIndex(Map<String, Article> index) throws IOException {
        Path tmp = dbFile.resolveSibling(dbFile.getFileName() + ".tmp");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            oos.writeObject(new LinkedHashMap<>(index));
        }
        Files.move(tmp, dbFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** Saves/overwrites the stored outline for this article, keyed by its (trimmed, case-insensitive) title. */
    public synchronized void save(Article article) throws IOException {
        Map<String, Article> index = loadIndex();
        index.put(key(article.getTitle()), article);
        writeIndex(index);
    }

    public Article retrieveByTitle(String title) {
        return loadIndex().get(key(title));
    }

    /** Case-insensitive "contains" search over stored titles; blank query returns everything. */
    public List<Article> searchByTitle(String query) {
        String q = key(query);
        List<Article> results = new ArrayList<>();
        for (Map.Entry<String, Article> e : loadIndex().entrySet()) {
            if (q.isBlank() || e.getKey().contains(q)) results.add(e.getValue());
        }
        return results;
    }

    public List<Article> retrieveAll() {
        return new ArrayList<>(loadIndex().values());
    }

    public synchronized void deleteByTitle(String title) throws IOException {
        Map<String, Article> index = loadIndex();
        index.remove(key(title));
        writeIndex(index);
    }

    public boolean containsTitle(String title) {
        return loadIndex().containsKey(key(title));
    }
}
