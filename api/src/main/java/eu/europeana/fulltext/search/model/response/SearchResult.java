package eu.europeana.fulltext.search.model.response;

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
    String getId();

    String getType();

    Debug getDebug();

    int itemSize();

    void addAnnotationHit(AnnoPage annoPage, Annotation annotation, Hit hit);

    List<Hit> getHits();
}

