package eu.europeana.fulltext.search.model.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Contains the prefix, exact, and suffix fields retrieved from Solr, used for searching in fulltext and for
 * serializing search response
 *
 * @author Patrick Ehlert
 * Created on 2 June 2020
 */
@JsonPropertyOrder({"type", "prefix", "exact", "postfix"})
public abstract class HitSelector implements Serializable {

    private static final long serialVersionUID = -2157026156195054409L;

    @NotNull
    private String prefix;
    @NotNull
    private String exact;
    @NotNull
    private String suffix;

    /**
     * Create a new HitSelector.
     * @param prefix can be null, but this is stored as empty String
     * @param exact cannot be null
     * @param suffix can be null, but this is stored as empty String
     */
    public HitSelector(String prefix, String exact, String suffix) {
        if (prefix == null) {
            this.prefix = "";
        } else {
            this.prefix = prefix;
        }

        this.exact = exact;

        if (suffix == null) {
            this.suffix = "";
        } else {
            this.suffix = suffix;
        }
    }

    abstract public String getType();

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getExact() {
        return exact;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * @return string presentation of a hit, including the prefix and postfix
     */
    public String toString() {
        return getPrefix() + getExact() + getSuffix();
    }
}
