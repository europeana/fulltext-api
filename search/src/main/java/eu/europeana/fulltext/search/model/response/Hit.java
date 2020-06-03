package eu.europeana.fulltext.search.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A highlight retrieved from Solr and the corresponding annotation from Mongo, used for serializing search response
 *
 * @author Patrick Ehlert
 * Created on 2 June 2020
 */
@JsonPropertyOrder({"type", "annotations", "selectors"})
public class Hit implements Serializable {

    private static final String TYPE = "Hit";

    private Integer startIndex; // for processing purposes only
    private Integer endIndex; // for processing purposes only
    private List<String> annotations = new ArrayList<>();// even though this is a list, it will contain only 1 annotation id
    private List<HitSelector> selectors = new ArrayList<>(); // even though this is a list, it will contain only 1 hitselector

    public Hit(Integer startIndex, Integer endIndex, HitSelector selector) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.selectors.add(selector);
    }

    public String getType() {
        return Hit.TYPE;
    }

    @JsonIgnore
    public Integer getStartIndex() {
        return this.startIndex;
    }

    @JsonIgnore
    public Integer getEndIndex() {
        return this.endIndex;
    }

    public List<String> getAnnotations() {
        return this.annotations;
    }

    public List<HitSelector> getSelectors() {
        return this.selectors;
    }

    public void addAnnotationId(String annotationId) {
        this.annotations.add(annotationId);
    }

}
