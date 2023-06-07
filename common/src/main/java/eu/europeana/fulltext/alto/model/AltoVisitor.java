/**
 *
 */
package eu.europeana.fulltext.alto.model;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 */
public interface AltoVisitor {
    void visit(AltoPage page);

    void visit(TextStyle style);

    void visit(TextBlock block);

    void visit(TextLine line);

    void visit(TextString word);

    void visit(TextSpace space);

    void visit(TextHyphen hyphen);
}
