/**
 *
 */
package eu.europeana.fulltext.alto.model;

import eu.europeana.edm.media.ImageBoundary;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 17 Jun 2018
 */
public class TextBlock extends TextNode<TextNode>
        implements StyledTextElement {
    private final ImageBoundary _ib;
    private TextStyle _style;
    private final boolean _correction;
    private String _type;

    public TextBlock(ImageBoundary ib, String lang
            , TextStyle style, boolean correction) {
        _ib = ib;
        _lang = lang;
        _style = style;
        _correction = correction;
    }

    public ImageBoundary getImageBoundary() {
        return _ib;
    }

    public TextStyle getStyle() {
        return _style;
    }

    public void setStyle(TextStyle style) {
        _style = style;
    }

    public boolean getCorrection() {
        return _correction;
    }

    public void visit(AltoVisitor visitor) {
        visitor.visit(this);
    }

    public boolean hasLines() {
        for (TextElement e : this) {
            if (e instanceof TextLine) {
                return true;
            }
        }
        return false;
    }
}
