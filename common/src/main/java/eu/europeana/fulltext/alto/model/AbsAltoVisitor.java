/**
 *
 */
package eu.europeana.fulltext.alto.model;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 */
public class AbsAltoVisitor implements AltoVisitor {
    public void visit(AltoPage page) {
        for (TextNode<?> node : page) {
            node.visit(this);
        }
    }

    public void visit(TextStyle style) {
    }

    public void visit(TextBlock block) {
        for (TextElement node : block) {
            node.visit(this);
        }
    }

    public void visit(TextLine line) {
        for (TextElement node : line) {
            node.visit(this);
        }
    }

    public void visit(TextString word) {
    }

    public void visit(TextSpace space) {
    }

    public void visit(TextHyphen hyphen) {
    }
}
