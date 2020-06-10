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
public class HitSelector implements Serializable {

    private static final long serialVersionUID = -2157026156195054409L;

    private static final String TYPE = "TextQuoteSelector";

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

    public String getType() {
        return HitSelector.TYPE;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getExact() {
        return exact;
    }

    public String getSuffix() {
        return suffix;
    }

    /**
     * @return string presentation of a hit, including the prefix and postfix
     */
    public String toString() {
        return getPrefix() + getExact() + getSuffix();
    }

}
