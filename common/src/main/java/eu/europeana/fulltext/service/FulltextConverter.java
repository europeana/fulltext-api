package eu.europeana.fulltext.service;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.edm.FullTextPackage;
import eu.europeana.fulltext.subtitles.AnnotationPreview;

public interface FulltextConverter {

    FullTextPackage convert(AnnotationPreview annotationPreview) throws EuropeanaApiException;
}
