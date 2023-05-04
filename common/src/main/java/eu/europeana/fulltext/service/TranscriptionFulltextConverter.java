package eu.europeana.fulltext.service;

import eu.europeana.edm.FullTextAnnotation;
import eu.europeana.edm.FullTextPackage;
import eu.europeana.edm.text.FullTextResource;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.util.GeneralUtils;

/**
 * @author Srishti Singh Created on 27-07-2022
 */
public class TranscriptionFulltextConverter implements FulltextConverter {

    @Override
    public FullTextPackage convert(AnnotationPreview annotationPreview) {
        // get the uri
        String uri = WebConstants.ITEM_BASE_URL + annotationPreview.getRecordId();
        String annotationPageURI = GeneralUtils.getAnnotationPageURI(annotationPreview.getRecordId());
        String fullTextResourceURI =
                GeneralUtils.getFullTextResourceURI(
                        annotationPreview.getRecordId(),
                        GeneralUtils.generateResourceId(annotationPreview.getRecordId(), annotationPreview.getLanguage(), annotationPreview.getMedia()));

//         create resource
        FullTextResource edmFullTextResource = new FullTextResource(
                        fullTextResourceURI, annotationPreview.getAnnotationBody(), annotationPreview.getLanguage(), annotationPreview.getRights(), uri);

        // create fulltext with only one annotation Page
        FullTextPackage fullTextPackage = new FullTextPackage(annotationPageURI, edmFullTextResource);
        fullTextPackage.add(new FullTextAnnotation(null, null,null, AnnotationType.PAGE, null, null));

        return fullTextPackage;
    }
}
