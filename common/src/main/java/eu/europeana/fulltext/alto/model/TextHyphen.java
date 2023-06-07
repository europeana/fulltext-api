/**
 *
 */
package eu.europeana.fulltext.alto.model;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 */
public class TextHyphen implements TextElement {
    private final String _text;

    public TextHyphen(String text) {
        _text = text;
    }

    public String getText() {
        return _text;
    }

    public void visit(AltoVisitor visitor) {
        visitor.visit(this);
    }
}
