package eu.europeana.fulltext.service;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.edm.FullTextPackage;

import eu.europeana.edm.media.MediaReference;
import eu.europeana.edm.media.MediaResource;
import eu.europeana.fulltext.alto.model.AltoPage;
import eu.europeana.fulltext.exception.XmlParsingException;
import eu.europeana.fulltext.pageXML.PageXMLParser;
import eu.europeana.fulltext.subtitles.AnnotationPreview;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;


public class PageXmlFulltextConverter  implements FulltextConverter {

    @Override
    public FullTextPackage convert(AnnotationPreview annotationPreview) throws EuropeanaApiException {

        try {
            PageXMLParser parser = new PageXMLParser();
            // create Alto page
            MediaReference reference = new MediaResource(annotationPreview.getMedia());
            AltoPage altoPage = parser.processPage(new StreamSource(new ByteArrayInputStream(annotationPreview.getAnnotationBody().getBytes(StandardCharsets.UTF_8))), reference);

            // page xml first converts the xslt into alto then we can convert alto to EDM
            return new AltoToFulltextConverter().getAltoToEDM(altoPage, annotationPreview, reference);

        } catch (TransformerConfigurationException e) {
            throw new XmlParsingException("Error configuring the transformer for type "+annotationPreview.getFulltextType());
        }

    }
}
