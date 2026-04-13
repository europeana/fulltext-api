package eu.europeana.fulltext.search.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;

import java.util.List;

/**
 * Root object for serializing search response
 *
 * @author Patrick Ehlert
 * Created on 2 June 2020
 */
public interface SearchResult {

    @JsonProperty("@context")
    // note that we only set context for a single annotation and not for an array of annotations part of an annotationpage
    String[] getContext();
    String getId();

    String getType();

    Debug getDebug();

    int itemSize();

    void addAnnotationHit(AnnoPage annoPage, Annotation annotation, Hit hit);

    List<Hit> getHits();

    void setContext(String[] context);
}

