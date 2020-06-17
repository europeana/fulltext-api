package eu.europeana.fulltext.search.model.query;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Class used to temporarily store highlighted words retrieved from Solr snippets
 */
public class SolrHit {

    @NotNull
    private String prefix;
    @NotNull
    private String exact;
    @NotNull
    private String suffix;

    private int hlStartPos; // start of the start hl tag
    private int hlEndPos; // end of the end hl tag

    public SolrHit(Character prefix, String exact, Character suffix, int hlStartPos, int hlEndPos) {
        if (prefix == null) {
            this.prefix = "";
        } else {
            this.prefix = prefix.toString();
        }
        this.exact = exact;
        if (suffix == null) {
            this.suffix = "";
        } else {
            this.suffix = suffix.toString();
        }
        this.hlStartPos = hlStartPos;
        this.hlEndPos = hlEndPos;
    }

    public SolrHit(String prefix, String exact, String suffix, int hlStartPos, int hlEndPos) {
        this.prefix = prefix;
        this.exact = exact;
        this.suffix = suffix;
        this.hlStartPos = hlStartPos;
        this.hlEndPos = hlEndPos;
    }

    /**
     * @return empty string or string of length 1
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return exact keyword
     */
    public String getExact() {
        return exact;
    }


    /**
     * @return empty string or string of length 1
     */
    public String getSuffix() {
        return suffix;
    }

    public int getHlStartPos() {
        return hlStartPos;
    }

    public int getHlEndPos() {
        return hlEndPos;
    }

    public String toString() {
        return prefix + exact + suffix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SolrHit solrHit = (SolrHit) o;
        return prefix.equals(solrHit.prefix) &&
                exact.equals(solrHit.exact) &&
                suffix.equals(solrHit.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, exact, suffix);
    }
}
