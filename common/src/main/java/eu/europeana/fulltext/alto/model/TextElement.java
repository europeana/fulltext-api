/**
 *
 */
package eu.europeana.fulltext.alto.model;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 17 Jun 2018
 */
public interface TextElement {
    void visit(AltoVisitor visitor);
}
