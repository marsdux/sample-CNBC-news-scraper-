package newsaggre;

import java.util.*;

/**
 * Single place that combines the summarizer, PESTEL/market outline
 * classifier, verbatim quote extractor, and industry tagger into the
 * "everything derived about this article" pipeline - used both for the
 * lightweight pass right after an RSS fetch and the richer pass triggered
 * from the Full Article window.
 */
public class OutlineService {

    private final Summarizer summarizer = new Summarizer();
    private final OutlineBuilder outlineBuilder = new OutlineBuilder();
    private final QuoteExtractor quoteExtractor = new QuoteExtractor();
    private final IndustryTagger industryTagger = new IndustryTagger();
    private final LifecycleAnalyzer lifecycleAnalyzer = new LifecycleAnalyzer();

    /** Quick pass right after fetching (RSS blurb quality). Leaves manually-edited articles' takeaways untouched. */
    public void quickGenerate(Article a) {
        if (!a.isEdited()) {
            applyTakeaways(a, summarizer.summarize(a), false);
        } else {
            refreshDerived(a);
        }
    }

    /** Same as quickGenerate, but for a cluster of connected articles sharing one merged, deduped takeaway list. */
    public void quickGenerateGroup(List<Article> group, List<String> mergedTakeaways) {
        for (Article a : group) {
            if (!a.isEdited()) {
                applyTakeaways(a, new ArrayList<>(mergedTakeaways), false);
            } else {
                refreshDerived(a);
            }
        }
    }

    /**
     * Rich pass meant to run against the full article page text (called from the Full Article
     * window's "Generate Outline" button). Always regenerates, clearing any manual-edit flag -
     * callers should confirm with the user first if the article was previously edited.
     */
    public List<String> generateFromFullArticle(Article a) {
        applyTakeaways(a, summarizer.summarizeFull(a), false);
        return a.getTakeaways();
    }

    /** Applies a user-edited takeaway list (from the Edit tab), marking the article as manually edited. */
    public void applyManualEdit(Article a, List<String> takeaways) {
        applyTakeaways(a, takeaways, true);
    }

    private void applyTakeaways(Article a, List<String> takeaways, boolean markEdited) {
        a.setTakeaways(takeaways);
        a.setEdited(markEdited);
        refreshDerived(a);
    }

    /** Rebuilds the categorized outline (incl. verbatim quotes), industry tags, and severity score from the article's current state. */
    private void refreshDerived(Article a) {
        List<OutlineItem> items = new ArrayList<>(outlineBuilder.build(a.getTakeaways()));
        for (String q : quoteExtractor.extract(a.bestAvailableText())) {
            items.add(new OutlineItem(OutlineBuilder.CAT_QUOTE, q));
        }
        a.setOutline(items);
        a.setIndustryTags(industryTagger.tag(a.getTitle() + ". " + a.bestAvailableText()));
        a.setSeverityScore(lifecycleAnalyzer.computeBaseSeverity(a));
    }
}
