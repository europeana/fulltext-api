package eu.europeana.fulltext.search.model.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

/**
 * Contains the prefix, exact, and suffix fields retrieved from Solr, used for serializing search response
 *
 * @author Patrick Ehlert
 * Created on 2 June 2020
 */
@JsonPropertyOrder({"type", "prefix", "exact", "postfix"})
public class HitSelector implements Serializable {

    private static final long serialVersionUID = -2157026156195054409L;

    private static final String TYPE = "TextQuoteSelector";

    private String prefix;
    private String exact;
    private String suffix;

    public HitSelector(String prefix, String exact, String suffix) {
        this.prefix = prefix;
        this.exact = exact;
        this.suffix = suffix;
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

    public String toString() {
        return getPrefix() + getExact() + getSuffix();
    }

}
