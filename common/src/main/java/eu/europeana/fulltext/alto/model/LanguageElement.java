/**
 *
 */
package eu.europeana.fulltext.alto.model;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 20 Dec 2018
 */
public interface LanguageElement extends TextElement {
    String getLanguage();

    void setLanguage(String lang);
}
