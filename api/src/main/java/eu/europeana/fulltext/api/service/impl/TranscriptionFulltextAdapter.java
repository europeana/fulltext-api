package eu.europeana.fulltext.api.service.impl;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.api.service.FulltextAdapter;
import eu.europeana.fulltext.edm.EdmFullTextPackage;
import eu.europeana.fulltext.subtitles.AnnotationPreview;


// TODO EA-3101 implementation will be part of this ticket
public class TranscriptionFulltextAdapter implements FulltextAdapter {

    @Override
    public EdmFullTextPackage adapt(AnnotationPreview annotationPreview) throws EuropeanaApiException {
        return null;
    }
}
