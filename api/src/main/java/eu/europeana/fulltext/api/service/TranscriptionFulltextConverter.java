package eu.europeana.fulltext.api.service;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.edm.EdmAnnotation;
import eu.europeana.fulltext.edm.EdmFullTextPackage;
import eu.europeana.fulltext.edm.EdmFullTextResource;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.util.GeneralUtils;

/**
 * @author Srishti Singh Created on 27-07-2022
 */
public class TranscriptionFulltextConverter implements FulltextConverter {

    @Override
    public EdmFullTextPackage convert(AnnotationPreview annotationPreview) {
        // get the uri
        String uri = WebConstants.ITEM_BASE_URL + annotationPreview.getRecordId();
        String annotationPageURI = GeneralUtils.getAnnotationPageURI(annotationPreview.getRecordId());
        String fullTextResourceURI =
                GeneralUtils.getFullTextResourceURI(
                        annotationPreview.getRecordId(),
                        GeneralUtils.generateResourceId(annotationPreview.getRecordId(), annotationPreview.getLanguage(), annotationPreview.getMedia()));

        // create resource
        EdmFullTextResource edmFullTextResource = new EdmFullTextResource(
                        fullTextResourceURI, annotationPreview.getAnnotationBody(), annotationPreview.getLanguage(), annotationPreview.getRights(), uri);

        // create fulltext with only one annotation Page
        EdmFullTextPackage edmFullTextPackage = new EdmFullTextPackage(annotationPageURI, edmFullTextResource);
        edmFullTextPackage.add(new EdmAnnotation(null, null, AnnotationType.PAGE, null, null));

        return edmFullTextPackage;
    }
}
