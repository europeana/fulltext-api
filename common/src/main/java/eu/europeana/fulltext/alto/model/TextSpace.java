/**
 *
 */
package eu.europeana.fulltext.alto.model;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 */
public class TextSpace implements TextElement {
    public static TextSpace SINGLETON = new TextSpace();

    public void visit(AltoVisitor visitor) {
        visitor.visit(this);
    }
}
