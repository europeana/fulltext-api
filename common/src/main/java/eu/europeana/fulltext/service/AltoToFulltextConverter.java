package eu.europeana.fulltext.service;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.edm.FullTextPackage;
import eu.europeana.edm.media.MediaReference;
import eu.europeana.edm.media.MediaResource;
import eu.europeana.edm.ocr.Alto2EDMTranslator;
import eu.europeana.edm.text.FullTextResource;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.alto.model.AltoPage;
import eu.europeana.fulltext.alto.parser.AltoParser;
import eu.europeana.fulltext.exception.LanguageMismatchException;
import eu.europeana.fulltext.exception.XmlParsingException;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.util.GeneralUtils;

import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class AltoToFulltextConverter extends AltoParser implements FulltextConverter {

    @Override
    public FullTextPackage convert(AnnotationPreview annotationPreview) throws EuropeanaApiException {
        try {
            // create Alto page
            MediaReference reference = new MediaResource(annotationPreview.getMedia());
            AltoPage altoPage = processPage(new StreamSource(new ByteArrayInputStream(annotationPreview.getAnnotationBody().getBytes(StandardCharsets.UTF_8))), reference);

            // convert alto page to EDM Fulltext
           return getAltoToEDM(altoPage, annotationPreview, reference);

        } catch (TransformerException e) {
            throw new XmlParsingException("Please provide proper data!! Text passed is not parseable.");
        }

    }

    /**
     * Converts Alto To EDM fulltext
     * generate the urls and Ids.
     * @param altoPage
     * @param annotationPreview
     * @param reference
     * @return
     */
    protected FullTextPackage getAltoToEDM(AltoPage altoPage, AnnotationPreview annotationPreview, MediaReference reference) throws LanguageMismatchException {
        // convert alto page to EDM Fulltext
        Alto2EDMTranslator alto2EDMTranslator = new Alto2EDMTranslator();
        FullTextPackage fullTextPackage = alto2EDMTranslator.processPage(altoPage, reference);

        // There should NOT be mismatch in the two lanaguges - language sent in the request and one identified by the alto parser
        if (fullTextPackage.getResource().getLang() != null && fullTextPackage.getResource().isLangOverriden(annotationPreview.getLanguage())) {
            throw new LanguageMismatchException("Mismatch in resource language while converting. " +
                    "Language sent - " +annotationPreview.getLanguage() +
                    ", Language obtained - " + fullTextPackage.getResource().getLang());
        }

        // In Alto the fulltext resource language is identified by the parser, hence generate the urls afterwards
        String fullTextResourceURI =
                GeneralUtils.getFullTextResourceURI(
                        annotationPreview.getRecordId(),
                        GeneralUtils.generateResourceId(annotationPreview.getRecordId(), annotationPreview.getLanguage(), annotationPreview.getMedia()));

        String uri = WebConstants.ITEM_BASE_URL + annotationPreview.getRecordId();

        // generate the fulltext resource
        FullTextResource resource =
                new FullTextResource(
                        fullTextResourceURI, fullTextPackage.getResource().getValue(), annotationPreview.getLanguage(), annotationPreview.getRights(), uri);

        // update fulltext
        fullTextPackage.setBaseUri(GeneralUtils.getAnnotationPageURI(annotationPreview.getRecordId()));
        fullTextPackage.setResource(resource);

        return fullTextPackage;
    }

}
