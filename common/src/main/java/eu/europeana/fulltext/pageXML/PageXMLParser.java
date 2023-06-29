package eu.europeana.fulltext.pageXML;

import eu.europeana.edm.media.MediaReference;
import eu.europeana.fulltext.alto.model.AltoPage;
import eu.europeana.fulltext.alto.parser.AltoParser;
import eu.europeana.fulltext.exception.XmlParsingException;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Hugo
 * @since 4 Apr 2023
 */
public class PageXMLParser extends AltoParser {
    private static final String XSLT_PATH = "etc/PageToAltoV2WordLevel.xsl";
    private final Transformer transformer;

    public PageXMLParser() throws TransformerConfigurationException, IOException, XmlParsingException {
        URL file = PageXMLParser.class.getClassLoader().getResource(XSLT_PATH);
        if (file == null) {
            throw new XmlParsingException("Unable to find file " +  XSLT_PATH);
        }
        try (InputStream is = file.openStream()) {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            transformer = tf.newTransformer(new StreamSource(is));
            transformer.setParameter("splitIntoWords", true);
            transformer.setParameter("useWordLayer", false);
        }
    }

    public AltoPage processPage(InputSource source, MediaReference ref) {
        return processPage(new StreamSource(source.getByteStream()), ref);
    }

    public AltoPage processPage(Source source, MediaReference ref) {
        try {
            DOMResult result = new DOMResult();
            transformer.transform(source, result);
            return super.processPage(new DOMSource(result.getNode()), ref);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
