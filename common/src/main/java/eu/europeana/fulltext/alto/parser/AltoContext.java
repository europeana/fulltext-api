package eu.europeana.fulltext.alto.parser;

import eu.europeana.edm.media.ImageBoundary;
import eu.europeana.edm.media.ImageDimension;
import eu.europeana.edm.media.MediaReference;
import eu.europeana.fulltext.alto.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class AltoContext {
    private static final Logger LOG = LogManager.getLogger(AltoContext.class);

    private final Collection<String> parStyles = new HashSet<>();
    private final AltoPage page;
    private SubstitutionHyphen subs;
    private final MediaReference ref;

    protected Deque<TextNode<? super TextElement>> stack = new ArrayDeque<>();

    /**
     * to create the alto context
     * @param page alto page
     * @param ref media url
     */
    public AltoContext(AltoPage page, MediaReference ref) {
        this.page = page;
        this.ref = ref;
        this.stack.push((TextNode) page);
    }

    protected MediaReference getReference() {
        return ref;
    }

    protected void newParagraphStyle(String style) {
        parStyles.add(style);
    }

    protected boolean hasParagraphStyle(String style) {
        return parStyles.contains(style);
    }

    protected TextStyle getStyle(String id) {
        if (id == null) {
            return null;
        }

        TextStyle style = page.getStyle(id);
        if (style == null) {
            LOG.warn("Unknown style: {}",  id);
        }

        return style;
    }

    protected AltoPage getPage() {
        return page;
    }

    protected SubstitutionHyphen getCurrentSubs() {
        return subs;
    }

    protected void setCurrentSubs(SubstitutionHyphen subs) {
        this.subs = subs;
    }

    public TextStyle getCurrentStyle() {
        TextNode node = stack.peek();
        if (node instanceof AltoPage) {
            return new TextStyle();
        }
        return new TextStyle(stack.peek().getStyle());
    }

    protected TextStyle newStyle(String id, float size) {
        TextStyle style = new TextStyle(size);
        page.addStyle(id, style);
        return style;
    }

    protected void newSpace() {
        stack.peek().add(TextSpace.SINGLETON);
    }

    protected TextHyphen newHyphen(String text) {
        TextHyphen hyphen = new TextHyphen(text);
        stack.peek().add(hyphen);
        if (subs != null) {
            subs.setHyphen(hyphen);
        }
        return hyphen;
    }

    protected SubstitutionHyphen newSubstitution(String subsText) {
        return new SubstitutionHyphen(subsText);
    }

    protected TextString newWord(String text, String lang
            , ImageBoundary ib, Float confidence
            , TextStyle style, ImageDimension imageDimension,  boolean cs) {
        TextString w = new TextString(text, null, lang, process(ib, imageDimension)
                , confidence, style, cs);
        stack.peek().add(w);
        return w;
    }

    protected TextLine newLine(ImageBoundary ib, String lang
            , TextStyle style, ImageDimension imageDimension, boolean cs) {
        TextLine line = new TextLine(process(ib, imageDimension), lang, style, cs);
        stack.peek().add(line);
        stack.push(line);
        return line;
    }

    protected TextBlock newBlock(ImageBoundary ib, String lang
            , TextStyle style, ImageDimension imageDimension, boolean cs) {
        TextBlock block = new TextBlock(process(ib, imageDimension), lang, style, cs);
        stack.peek().add(block);
        stack.push((TextNode) block);
        return block;
    }

    protected ImageBoundary process(ImageBoundary ib, ImageDimension imageDimension) {
        if (ib == null) {
            return ib;
        }
        ib = ib.clip(imageDimension);
        return (ib.isValid() ? ib : null);
    }
}
