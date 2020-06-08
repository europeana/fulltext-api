package eu.europeana.fulltext.search.model.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.api.service.EDM2IIIFMapping;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Root object for serializing search response
 *
 * @author Patrick Ehlert
 * Created on 2 June 2020
 */
@JsonPropertyOrder({"id", "type", "items","hits"})
public class SearchResult implements Serializable {

    private static final long serialVersionUID = -5643549600050178321L;

    private static final String TYPE = "Annotationpage";

    private String id;
    private List<AnnotationV3> items = new ArrayList<>();
    private List<Hit> hits = new ArrayList<>();

    public SearchResult(String searchId) {
        this.id = searchId;
    }

    public String getType() {
        return SearchResult.TYPE;
    }

    public String getId() {
        return id;
    }

    public List<AnnotationV3> getItems() {
        return items;
    }

    public List<Hit> getHits() {
        return hits;
    }

    /**
     * Add an Annotation that matches to a hit
     * @param annoPage, the annotation page where the hit was found
     * @param annotation the annotation that matches/overlaps with the hit
     * @param hit the found hit
     */
    public void addAnnotationHit(AnnoPage annoPage, Annotation annotation, Hit hit) {
        AnnotationV3 annoV3 = EDM2IIIFMapping.getAnnotationV3(annoPage, annotation, false, false);
        hit.addAnnotationId(annoV3.getId());
        // TODO check if we already have this hit from another annotation?
        hits.add(hit);
        items.add(annoV3);
    }
}
