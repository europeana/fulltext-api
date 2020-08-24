package eu.europeana.fulltext.search.model.response;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;

import java.util.List;

public interface SearchResult {
    String getId();

    String getType();

    Debug getDebug();

    int itemSize();

    void addAnnotationHit(AnnoPage annoPage, Annotation annotation, Hit hit);

    List<Hit> getHits();
}

