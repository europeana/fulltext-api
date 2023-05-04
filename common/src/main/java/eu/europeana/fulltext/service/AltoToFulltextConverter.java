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
import eu.europeana.fulltext.exception.AltoParsingException;
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
            // generate url
            String uri = WebConstants.ITEM_BASE_URL + annotationPreview.getRecordId();
            String annotationPageURI = GeneralUtils.getAnnotationPageURI(annotationPreview.getRecordId());

            // create Alto page
            MediaReference reference = new MediaResource(annotationPreview.getMedia());
            AltoPage altoPage = processPage(new StreamSource(new ByteArrayInputStream(annotationPreview.getAnnotationBody().getBytes(StandardCharsets.UTF_8))), reference);

            // convert alto page to EDM Fulltext
            Alto2EDMTranslator alto2EDMTranslator = new Alto2EDMTranslator();
            FullTextPackage fullTextPackage = alto2EDMTranslator.processPage(altoPage, reference);

            // In Alto the fulltext resource language is identified as well, hence generate the urls afterwards
            // TODO check if we can rely on this because if not identified weel it will override the existing resources in DB
            String lang = fullTextPackage.getResource().getLang();
            String fullTextResourceURI =
                    GeneralUtils.getFullTextResourceURI(
                            annotationPreview.getRecordId(),
                            GeneralUtils.generateResourceId(annotationPreview.getRecordId(), lang, annotationPreview.getMedia()));

            // generate the fulltext resource
            FullTextResource resource =
                    new FullTextResource(
                            fullTextResourceURI, fullTextPackage.getResource().getValue(), lang, annotationPreview.getRights(), uri);

            // update fulltext
            fullTextPackage.setBaseUri(annotationPageURI);
            fullTextPackage.setResource(resource);

            return fullTextPackage;

        } catch (TransformerException e) {
            throw new AltoParsingException("Please provide proper data!! Text passed is not parseable.");
        }

    }

}
