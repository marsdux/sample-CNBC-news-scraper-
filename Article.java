package newsaggre;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single CNBC article pulled from a free/public RSS feed, plus everything
 * the app derives or the user edits about it.
 */
public class Article implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id = UUID.randomUUID().toString();
    private String title = "";
    private String link = "";
    private String rawDescription = "";
    private String fullText = "";          // best-effort scraped body text, may be empty
    private String pubDate = "";
    private String category = "Uncategorized"; // CNBC's own feed grouping
    private List<String> takeaways = new ArrayList<>(); // rule-based outline bullets
    private List<OutlineItem> outline = new ArrayList<>(); // takeaways tagged by type (Event/Market/Figures/Outlook/Fact)
    private List<String> industryTags = new ArrayList<>(); // economic sector tags (Technology, Energy, Macro, ...)
    private String imageUrl = "";          // lead image, from RSS media tag or the page's og:image
    private Double severityScore = null;   // content-based "how big a deal is this" score, 0-100; null = not yet computed
    private Integer firstClusterSize = null; // connected-cluster size the first time we saw this story; null = not yet captured
    private int groupId = -1;              // cluster id assigned by StoryGrouper; -1 = ungrouped
    private boolean edited = false;        // user manually touched the takeaways
    private boolean selected = false;      // checkbox state in the table
    private long fetchedAt = System.currentTimeMillis();

    public Article() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getRawDescription() { return rawDescription; }
    public void setRawDescription(String rawDescription) { this.rawDescription = rawDescription; }

    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }

    public String getPubDate() { return pubDate; }
    public void setPubDate(String pubDate) { this.pubDate = pubDate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTakeaways() { return takeaways; }
    public void setTakeaways(List<String> takeaways) { this.takeaways = takeaways; }

    public List<OutlineItem> getOutline() { return outline; }
    public void setOutline(List<OutlineItem> outline) { this.outline = outline; }

    public List<String> getIndustryTags() { return industryTags; }
    public void setIndustryTags(List<String> industryTags) { this.industryTags = industryTags; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Double getSeverityScore() { return severityScore; }
    public void setSeverityScore(Double severityScore) { this.severityScore = severityScore; }

    public Integer getFirstClusterSize() { return firstClusterSize; }
    public void setFirstClusterSize(Integer firstClusterSize) { this.firstClusterSize = firstClusterSize; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }

    /** Human-friendly published date/time, parsed from the RSS pubDate; falls back to the raw string. */
    public String getFormattedPubDate() { return DateUtils.formatPubDate(pubDate); }

    /** When this app last pulled/updated this article - useful to judge how fresh the data is. */
    public String getFormattedFetchedAt() { return DateUtils.formatMillis(fetchedAt); }

    /** Fills in any fields missing after deserializing an older saved file (new fields default to null). */
    public void ensureDefaults() {
        if (takeaways == null) takeaways = new ArrayList<>();
        if (outline == null) outline = new ArrayList<>();
        if (industryTags == null) industryTags = new ArrayList<>();
        if (imageUrl == null) imageUrl = "";
        if (title == null) title = "";
        if (link == null) link = "";
        if (rawDescription == null) rawDescription = "";
        if (fullText == null) fullText = "";
        if (pubDate == null) pubDate = "";
        if (category == null) category = "Uncategorized";
    }

    /** Text used as the source for summarization: prefer scraped body, fall back to RSS blurb. */
    public String bestAvailableText() {
        if (fullText != null && fullText.trim().length() > rawDescription.trim().length()) {
            return fullText;
        }
        return rawDescription;
    }

    public String takeawaysAsBulletText() {
        StringBuilder sb = new StringBuilder();
        for (String t : takeaways) {
            sb.append("\u2022 ").append(t).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return title;
    }
}
