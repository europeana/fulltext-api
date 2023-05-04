/**
 *
 */
package eu.europeana.fulltext.pageXML;

import eu.europeana.edm.media.MediaReference;
import eu.europeana.fulltext.alto.model.AltoPage;
import eu.europeana.fulltext.alto.parser.AltoParser;
import org.xml.sax.InputSource;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

/**
 * @author Hugo
 * @since 4 Apr 2023
 */
public class PageXMLParser extends AltoParser {
    private static final String XSLT_PATH = "etc/PageToAlto.xsl";
    private final Transformer _transformer;

    public PageXMLParser() throws TransformerConfigurationException {
        InputStream is = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(XSLT_PATH);
        if (is == null) {
            is = this.getClass().getResourceAsStream(XSLT_PATH);
        }
        TransformerFactory tf = TransformerFactory.newInstance();
        _transformer = tf.newTransformer(new StreamSource(is));
    }

    public AltoPage processPage(InputSource source, MediaReference ref) {
        return processPage(new StreamSource(source.getByteStream()), ref);
    }

    public AltoPage processPage(Source source, MediaReference ref) {
        try {
            DOMResult result = new DOMResult();
            _transformer.transform(source, result);
            return super.processPage(new DOMSource(result.getNode()), ref);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}