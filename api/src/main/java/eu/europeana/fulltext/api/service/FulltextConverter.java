package eu.europeana.fulltext.api.service;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.edm.EdmFullTextPackage;
import eu.europeana.fulltext.subtitles.AnnotationPreview;

public interface FulltextConverter {

    EdmFullTextPackage convert(AnnotationPreview annotationPreview) throws EuropeanaApiException;
}
