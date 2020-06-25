package eu.europeana.fulltext.search.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.fulltext.api.service.EDM2IIIFMapping;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.search.model.query.EuropeanaId;

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

    private static final long serialVersionUID = -3280544584499568202L;

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

    public Hit(Integer startIndex, Integer endIndex, String exact) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.selectors.add(new HitSelector("", exact, ""));
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

    /**
     * This adds a new annotation to the hit and also sets the prefix and suffix. Note that the first added
     * annotation determines the prefix, and the last added annotation the suffix
     * @param annoPage the annotation page where the hit was found
     * @param annotation the annotation that was found
     */
    public void addAnnotation(AnnoPage annoPage, Annotation annotation) {
        String fulltext = annoPage.getRes().getValue();
        HitSelector hs = this.selectors.get(0); // should be only 1 hitSelector per hit

        if (this.getAnnotations().isEmpty() && (annotation.getFrom() < this.startIndex)) {
            hs.setPrefix(fulltext.substring(annotation.getFrom(), this.startIndex));
        }

        if (this.endIndex < annotation.getTo()) {
            hs.setSuffix(fulltext.substring(this.endIndex, annotation.getTo()));
        } else {
            hs.setSuffix("");
        }

        EuropeanaId id = new EuropeanaId(annoPage.getDsId(), annoPage.getLcId());
        this.annotations.add(EDM2IIIFMapping.getAnnotationIdUrl(id.toString(), annotation));
    }

}
