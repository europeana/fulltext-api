package eu.europeana.fulltext.alto.parser;

import eu.europeana.edm.media.ImageBoundary;
import eu.europeana.edm.media.ImageDimension;
import eu.europeana.edm.media.MediaReference;
import eu.europeana.fulltext.alto.model.*;
import eu.europeana.fulltext.alto.model.TextStyle.TextType;
import eu.europeana.fulltext.alto.utils.AltoPageProcessor;
import eu.europeana.fulltext.alto.utils.AltoPageProcessorImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Stack;

import static eu.europeana.fulltext.alto.parser.EDMFullTextUtils.newImageBoundary;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 23 Feb 2018
 */
public class AltoParser extends DefaultHandler {
    private static final Logger LOG = LogManager.getLogger(AltoParser.class);

    private Context _context = null;
    private AltoPageProcessor _processor = null;

    public AltoParser() {
        _processor = new AltoPageProcessorImpl();
    }

    public AltoPage processPage(InputSource source, MediaReference ref)
            throws IOException, SAXException {
        try {
            AltoPage page = new AltoPage();
            _context = new Context(page, ref);
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(this);
            xr.parse(source);
            _context.clear();
            return page;
        } finally {
            _context = null;
        }
    }

    public AltoPage processPage(Source source, MediaReference ref)
            throws TransformerException {
        try {
            AltoPage page = new AltoPage();
            _context = new Context(page, ref);
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.newTransformer().transform(source, new SAXResult(this));
            _context.clear();
            return page;
        } finally {
            _context = null;
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName
            , Attributes attr) throws SAXException {
        if (localName.equals("String")) {
            TextString word = _context.newWord(attr.getValue("CONTENT")
                    , getLanguage(attr)
                    , toImageBoundary(attr), getConfidence(attr, "WC")
                    , buildStyle(attr), getCorrectionStatus(attr));
            _context.setCurrentSubs(
                    getSubstitution(attr.getValue("SUBS_CONTENT")
                            , attr.getValue("SUBS_TYPE"), word));
            return;
        }
        if (localName.equals("SP")) {
            _context.newSpace();
            return;
        }
        if (localName.equals("HYP")) {
            _context.newHyphen(attr.getValue("CONTENT"));
            return;
        }
        if (localName.equals("TextLine")) {
            _context.newLine(toImageBoundary(attr), getLanguage(attr)
                    , buildStyle(attr), getCorrectionStatus(attr));
            return;
        }
        if (localName.equals("TextBlock") || localName.equals("ComposeBlock")) {
            _context.newBlock(toImageBoundary(attr), getLanguage(attr)
                    , buildStyle(attr), getCorrectionStatus(attr));
            return;
        }
        if (localName.equals("TextStyle")) {
            TextStyle style = _context.newStyle(getID(attr), getStyleSize(attr));
            setStyleTypes(style, getStyleType(attr));
            return;
        }
        if (localName.equals("ParagraphStyle")) {
            _context.newParagraphStyle(getID(attr));
            return;
        }
        if (localName.equals("Page")) {
            AltoPage page = _context.getPage();
            page.setConfidence(getConfidence(attr, "PC"));
            page.setAccuracy(getAccuracy(attr));
            page.setStyle(buildStyle(attr));
            page.setDimension(getDimension(attr));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (localName.equals("TextLine")
                || localName.equals("TextBlock")
                || localName.equals("ComposeBlock")) {
            _context.pop();
        }
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void endDocument() {
        _processor.process(_context.getPage());
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

        if (subsType == null || subsType.equals("HypPart1")) {
            SubstitutionHyphen subs = _context.newSubstitution(subsText);
            subs.setWord1(w);
            w.setSubs(subs);
            return subs;
        }

        if (!subsType.equals("HypPart2")) {
            return null;
        }

        SubstitutionHyphen subs = _context.getCurrentSubs();
        if (subs == null) {
            subs = _context.newSubstitution(subsText);
        }
        subs.setWord2(w);
        w.setSubs(subs);

        return null;
    }


    private TextStyle buildStyle(Attributes attr) {
        TextStyle newStyle = _context.getCurrentStyle();

        setStyleRefs(newStyle, attr.getValue("STYLEREFS"));
        setStyleTypes(newStyle, attr.getValue("STYLE"));

        return newStyle;
    }

    private void setStyleRefs(TextStyle style, String str) {
        if (StringUtils.isEmpty(str)) {
            return;
        }

        for (String ref : str.split("\\s+")) {
            if (_context.hasParagraphStyle(ref)) {
                continue;
            }

            TextStyle s = _context.getStyle(ref);
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
        return newImageBoundary(_context.getReference()
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

    @SuppressWarnings("serial")
    private class Context extends Stack<TextNode<? super TextElement>> {
        private final Collection _parStyles = new HashSet();
        private final AltoPage _page;
        private SubstitutionHyphen _subs;
        private final MediaReference _ref;

        public Context(AltoPage page, MediaReference ref) {
            _page = page;
            _ref = ref;
            push((TextNode) page);
        }

        protected MediaReference getReference() {
            return _ref;
        }

        protected void newParagraphStyle(String style) {
            _parStyles.add(style);
        }

        protected boolean hasParagraphStyle(String style) {
            return _parStyles.contains(style);
        }

        protected TextStyle getCurrentStyle() {
            TextNode node = _context.peek();
            if (node instanceof AltoPage) {
                return new TextStyle();
            }

            return new TextStyle(_context.peek().getStyle());
        }

        protected TextStyle getStyle(String id) {
            if (id == null) {
                return null;
            }

            TextStyle style = _page.getStyle(id);
            if (style == null) {
                LOG.warn("Unknown style: " + id);
            }

            return style;
        }

        protected AltoPage getPage() {
            return _page;
        }

        protected SubstitutionHyphen getCurrentSubs() {
            return _subs;
        }

        protected void setCurrentSubs(SubstitutionHyphen subs) {
            _subs = subs;
        }

        protected TextStyle newStyle(String id, float size) {
            TextStyle style = new TextStyle(size);
            _page.addStyle(id, style);
            return style;
        }

        protected void newSpace() {
            peek().add(TextSpace.SINGLETON);
        }

        protected TextHyphen newHyphen(String text) {
            TextHyphen hyphen = new TextHyphen(text);
            peek().add(hyphen);
            if (_subs != null) {
                _subs.setHyphen(hyphen);
            }
            return hyphen;
        }

        protected SubstitutionHyphen newSubstitution(String subsText) {
            return new SubstitutionHyphen(subsText);
        }

        protected TextString newWord(String text, String lang
                , ImageBoundary ib, Float confidence
                , TextStyle style, boolean cs) {
            TextString w = new TextString(text, null, lang, process(ib)
                    , confidence, style, cs);
            peek().add(w);
            return w;
        }

        protected TextLine newLine(ImageBoundary ib, String lang
                , TextStyle style, boolean cs) {
            TextLine line = new TextLine(process(ib), lang, style, cs);
            peek().add(line);
            push(line);
            return line;
        }

        protected TextBlock newBlock(ImageBoundary ib, String lang
                , TextStyle style, boolean cs) {
            TextBlock block = new TextBlock(process(ib), lang, style, cs);
            peek().add(block);
            push((TextNode) block);
            return block;
        }

        protected ImageBoundary process(ImageBoundary ib) {
            if (ib == null) {
                return ib;
            }

            ib = ib.clip(_context.getPage().getDimension());
            return (ib.isValid() ? ib : null);
        }
    }
}
