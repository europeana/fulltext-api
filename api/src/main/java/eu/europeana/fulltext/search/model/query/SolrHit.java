package eu.europeana.fulltext.search.model.query;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Class used to temporarily store snippets and offsets retrieved from Solr
 * and for generating debug output
 */
public class SolrHit implements Serializable {

    private static final long serialVersionUID = -2586948963638483178L;

    @NotNull
    private String imageId;
    @NotNull
    private String snippet;
    private int start;
    private int end;

    /**
     * Create a new object containing the snippet and offset data from Solr
     */
    public SolrHit(String imageId, String snippet, int start, int end) {
        this.imageId = imageId;
        this.snippet = snippet;
        this.start = start;
        this.end = end;
    }

    public String getImageId() {
        return imageId;
    }

    public String getSnippet() {
        return snippet;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    /**
     * Generate a textual description of this hit
     */
    @JsonIgnore
    public String getDebugInfo() {
        return "hit " + start + "," + end + " with text '"+ snippet.substring(start, end) + "'";
    }

}
