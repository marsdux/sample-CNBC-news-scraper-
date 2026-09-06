package newsaggre;

import java.util.*;

/**
 * Groups articles that are likely covering the same underlying story.
 * Runs per-category (CNBC's own grouping is the first-level bucket; this is
 * the second-level "these headlines are about the same event" bucket).
 *
 * Approach: union-find over pairwise Jaccard similarity of each article's
 * top keyword set. No ML/API - just word overlap - so it's a heuristic, not
 * perfect entity resolution, but it catches same-day follow-ups, updates,
 * and multiple angles on one event.
 */
public class StoryGrouper {

    private static final int KEYWORDS_PER_ARTICLE = 12;
    private static final double CONNECT_THRESHOLD = 0.22;

    /** Assigns groupId on each article in place. Returns count of distinct groups formed. */
    public int assignGroups(List<Article> articles) {
        Map<String, List<Article>> byCategory = new LinkedHashMap<>();
        for (Article a : articles) {
            byCategory.computeIfAbsent(a.getCategory(), k -> new ArrayList<>()).add(a);
        }
        int nextGroupId = 0;
        for (List<Article> bucket : byCategory.values()) {
            nextGroupId = clusterBucket(bucket, nextGroupId);
        }
        return nextGroupId;
    }

    private int clusterBucket(List<Article> bucket, int startId) {
        int n = bucket.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        List<Set<String>> keywordSets = new ArrayList<>();
        for (Article a : bucket) {
            String basis = a.getTitle() + ". " + a.getRawDescription();
            keywordSets.add(TextUtils.topKeywords(basis, KEYWORDS_PER_ARTICLE));
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double sim = TextUtils.jaccard(keywordSets.get(i), keywordSets.get(j));
                if (sim >= CONNECT_THRESHOLD) {
                    union(parent, i, j);
                }
            }
        }

        Map<Integer, Integer> rootToGroupId = new HashMap<>();
        int nextId = startId;
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            Integer gid = rootToGroupId.get(root);
            if (gid == null) {
                gid = nextId++;
                rootToGroupId.put(root, gid);
            }
            bucket.get(i).setGroupId(gid);
        }
        return nextId;
    }

    private int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private void union(int[] parent, int a, int b) {
        int ra = find(parent, a), rb = find(parent, b);
        if (ra != rb) parent[ra] = rb;
    }

    /** Number of articles currently sharing this group id (1 if standalone or ungrouped). */
    public static int clusterSize(List<Article> allArticles, int groupId) {
        if (groupId < 0) return 1;
        int n = 0;
        for (Article a : allArticles) if (a.getGroupId() == groupId) n++;
        return n;
    }

    /** True if this group has more than one article, i.e. it's a "connected stories" cluster. */
    public static boolean isConnectedCluster(List<Article> allArticles, int groupId) {
        return clusterSize(allArticles, groupId) > 1;
    }
}
