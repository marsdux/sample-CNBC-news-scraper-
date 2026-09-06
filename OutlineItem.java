package newsaggre;

import java.io.Serializable;

/** One takeaway sentence tagged with what kind of information it is. */
public class OutlineItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String category;
    private String text;

    public OutlineItem() {}

    public OutlineItem(String category, String text) {
        this.category = category;
        this.text = text;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
