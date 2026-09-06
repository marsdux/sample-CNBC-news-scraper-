package newsaggre;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Simple local persistence: everything lives in one file under the user's
 * home directory. No database/server required, so "save/store/retrieve"
 * work fully offline and survive restarts.
 */
public class ArticleStore {

    private final Path storeFile;

    public ArticleStore() {
        Path dir = Paths.get(System.getProperty("user.home"), ".newsaggre");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        this.storeFile = dir.resolve("articles.ser");
    }

    public Path getStoreFile() { return storeFile; }

    @SuppressWarnings("unchecked")
    public List<Article> retrieve() {
        if (!Files.exists(storeFile)) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(storeFile)))) {
            SerializationSafety.harden(ois);
            List<Article> loaded = (List<Article>) ois.readObject();
            for (Article a : loaded) a.ensureDefaults();
            return loaded;
        } catch (Exception e) {
            System.err.println("Could not read saved articles: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** Writes to a temp file then atomically renames it into place, so a crash mid-write can't leave a corrupt file. */
    public void save(List<Article> articles) throws IOException {
        Path tmp = storeFile.resolveSibling(storeFile.getFileName() + ".tmp");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            oos.writeObject(new ArrayList<>(articles));
        }
        Files.move(tmp, storeFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public void deleteAll() throws IOException {
        Files.deleteIfExists(storeFile);
    }
}
