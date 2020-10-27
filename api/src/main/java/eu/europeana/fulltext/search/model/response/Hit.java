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
public abstract class Hit implements Serializable {

    private static final long serialVersionUID = -3280544584499568202L;

    private Integer startIndex; // for processing purposes only
    private Integer endIndex; // for processing purposes only
    private List<String> annotations = new ArrayList<>(); // even though this is a list, it will contain only 1 annotation id
    private List<HitSelector> selectors = new ArrayList<>(); // even though this is a list, it will contain only 1 hitselector

    public abstract String getType();

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
     * This adds an annotation to the hit and also sets the prefix and suffix. Note that if multiple annotations
     * are added the first annotation determines the prefix, and the last one determines the suffix
     *
     * @param annoPage   the annotation page where the hit was found
     * @param annotation the annotation that was found
     */
    public Hit addAnnotation(int start, int end, AnnoPage annoPage, Annotation annotation) {
        String fulltext = annoPage.getRes().getValue();
        HitSelector hs;

        if (this.selectors.isEmpty()) {
            // first annotation so set prefix
            this.startIndex = start;
            this.endIndex = end;
            hs = createSelector(fulltext.substring(start, end));
            this.selectors.add(hs);
            if ((annotation.getFrom() < this.startIndex)) {
                hs.setPrefix(fulltext.substring(annotation.getFrom(), this.startIndex));
            }
        } else {
            // second or later annotation, so reuse selector to set new suffix
            hs = this.selectors.get(0);
        }
        if (this.endIndex < annotation.getTo()) {
            hs.setSuffix(fulltext.substring(this.endIndex, annotation.getTo()));
        }

        // add annotation itself
        EuropeanaId id = new EuropeanaId(annoPage.getDsId(), annoPage.getLcId());
        this.annotations.add(EDM2IIIFMapping.getAnnotationIdUrl(id.toString(), annotation));
        return this;
    }

    protected abstract HitSelector createSelector(String exact);
}
