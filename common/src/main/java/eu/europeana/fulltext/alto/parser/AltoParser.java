package eu.europeana.fulltext.alto.parser;

import eu.europeana.edm.media.ImageBoundary;
import eu.europeana.edm.media.ImageDimension;
import eu.europeana.edm.media.MediaReference;
import eu.europeana.fulltext.alto.model.*;
import eu.europeana.fulltext.alto.model.TextStyle.TextType;
import eu.europeana.fulltext.alto.utils.AltoPageProcessor;
import eu.europeana.fulltext.alto.utils.AltoPageProcessorImpl;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import java.io.IOException;

import static eu.europeana.fulltext.alto.parser.EDMFullTextUtils.newImageBoundary;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 23 Feb 2018
 */
public class AltoParser extends DefaultHandler {

    private AltoContext context = null;
    private AltoPageProcessor processor = null;

    public AltoParser() {
        processor = new AltoPageProcessorImpl();
    }

    public AltoPage processPage(InputSource source, MediaReference ref)
            throws IOException, SAXException, ParserConfigurationException {
        try {
            AltoPage page = new AltoPage();
            context = new AltoContext(page, ref);
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser parser = parserFactory.newSAXParser();
            // XML parsers should not be vulnerable to XXE attacks
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(this);
            reader.parse(source);
            context.stack.clear();
            return page;
        } finally {
            context = null;
        }
    }

    public AltoPage processPage(Source source, MediaReference ref)
            throws TransformerException {
        try {
            AltoPage page = new AltoPage();
            context = new AltoContext(page, ref);
            TransformerFactory factory = TransformerFactory.newInstance();
            // XML parsers should not be vulnerable to XXE attacks
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            factory.newTransformer().transform(source, new SAXResult(this));
            context.stack.clear();
            return page;
        } finally {
            context = null;
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName
            , Attributes attr) throws SAXException {
        ImageDimension imageDimension = context.getPage().getDimension();
        if (StringUtils.equals(localName, "String")) {
            TextString word = context.newWord(attr.getValue("CONTENT")
                    , getLanguage(attr)
                    , toImageBoundary(attr), getConfidence(attr, "WC")
                    , buildStyle(attr), imageDimension, getCorrectionStatus(attr));
            context.setCurrentSubs(
                    getSubstitution(attr.getValue("SUBS_CONTENT")
                            , attr.getValue("SUBS_TYPE"), word));
            return;
        }
        if (StringUtils.equals(localName, "SP")) {
            context.newSpace();
            return;
        }
        if (StringUtils.equals(localName, "HYP")) {
            context.newHyphen(attr.getValue("CONTENT"));
            return;
        }
        if (StringUtils.equals(localName, "TextLine")) {
            context.newLine(toImageBoundary(attr), getLanguage(attr)
                    , buildStyle(attr), imageDimension, getCorrectionStatus(attr));
            return;
        }
        if (StringUtils.equals(localName, "TextBlock") || StringUtils.equals(localName, "ComposeBlock")) {
            context.newBlock(toImageBoundary(attr), getLanguage(attr)
                    , buildStyle(attr),imageDimension, getCorrectionStatus(attr));
            return;
        }
        if (StringUtils.equals(localName, "TextStyle")) {
            TextStyle style = context.newStyle(getID(attr), getStyleSize(attr));
            setStyleTypes(style, getStyleType(attr));
            return;
        }
        if (StringUtils.equals(localName, "ParagraphStyle")) {
            context.newParagraphStyle(getID(attr));
            return;
        }
        if (StringUtils.equals(localName, "Page")) {
            AltoPage page = context.getPage();
            page.setConfidence(getConfidence(attr, "PC"));
            page.setAccuracy(getAccuracy(attr));
            page.setStyle(buildStyle(attr));
            page.setDimension(getDimension(attr));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (StringUtils.equals(localName, "TextLine")
                || StringUtils.equals(localName, "TextBlock")
                || StringUtils.equals(localName, "ComposeBlock")) {
            context.stack.pop();
        }
    }

    @Override
    public void startDocument() {
        // empty
    }

    @Override
    public void endDocument() {
        processor.process(context.getPage());
    }


    private ImageDimension getDimension(Attributes attr) {
        try {
            String height = attr.getValue("HEIGHT");
            String width = attr.getValue("WIDTH");
            if (height == null || width == null) {
                return null;
            }

            return new ImageDimension(Integer.parseInt(width)
                    , Integer.parseInt(height));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getID(Attributes attr) {
        return attr.getValue("ID");
    }

    private SubstitutionHyphen getSubstitution(String subsText, String subsType
            , TextString w) {
        if (subsText == null) {
            return null;
        }

        if (subsType == null || StringUtils.equals(subsType, "HypPart1")) {
            SubstitutionHyphen subs = context.newSubstitution(subsText);
            subs.setWord1(w);
            w.setSubs(subs);
            return subs;
        }

        if (!StringUtils.equals(subsType, "HypPart2")) {
            return null;
        }

        SubstitutionHyphen subs = context.getCurrentSubs();
        if (subs == null) {
            subs = context.newSubstitution(subsText);
        }
        subs.setWord2(w);
        w.setSubs(subs);

        return null;
    }

    private TextStyle buildStyle(Attributes attr) {
        TextStyle newStyle = context.getCurrentStyle();
        setStyleRefs(newStyle, attr.getValue("STYLEREFS"));
        setStyleTypes(newStyle, attr.getValue("STYLE"));

        return newStyle;
    }

    private void setStyleRefs(TextStyle style, String str) {
        if (StringUtils.isEmpty(str)) {
            return;
        }

        for (String ref : str.split("\\s+")) {
            if (context.hasParagraphStyle(ref)) {
                continue;
            }

            TextStyle s = context.getStyle(ref);
            if (s != null) {
                style.copyStyle(s);
            }
        }
    }

    private void setStyleTypes(TextStyle style, String str) {
        if (StringUtils.isEmpty(str)) {
            return;
        }

        String[] types = str.split(" ");
        for (String type : types) {
            TextType textType = TextType.valueOf(type.trim().toLowerCase());
            if (textType != null) {
                style.addType(textType);
            }
        }
    }

    private String getStyleType(Attributes attr) {
        return attr.getValue("FONTSTYLE");
    }

    private Float getStyleSize(Attributes attr) {
        String value = attr.getValue("FONTSIZE");
        try {
            return (value != null ? Float.parseFloat(value) : null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getLanguage(Attributes attr) {
        String value = attr.getValue("LANG");
        return (value != null ? value : attr.getValue("language"));
    }

    private Float getConfidence(Attributes attrs, String attr) {
        return toFloat(attrs.getValue(attr));
    }

    private Float getAccuracy(Attributes attrs) {
        return toFloat(attrs.getValue("ACCURACY"));
    }

    private boolean getCorrectionStatus(Attributes attr) {
        return ("true".equalsIgnoreCase(attr.getValue("CS")));
    }

    private ImageBoundary toImageBoundary(Attributes attr) {
        return newImageBoundary(context.getReference()
                , toPixel(attr.getValue("HPOS"))
                , toPixel(attr.getValue("VPOS"))
                , toPixel(attr.getValue("WIDTH"))
                , toPixel(attr.getValue("HEIGHT")));
    }

    private Float toFloat(String value) {
        try {
            return (value == null ? null : Float.parseFloat(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toPixel(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new Integer(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
