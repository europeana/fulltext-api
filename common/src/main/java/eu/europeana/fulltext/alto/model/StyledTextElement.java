/**
 *
 */
package eu.europeana.fulltext.alto.model;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 */
public interface StyledTextElement extends TextElement {
    TextStyle getStyle();

    void setStyle(TextStyle style);
}
