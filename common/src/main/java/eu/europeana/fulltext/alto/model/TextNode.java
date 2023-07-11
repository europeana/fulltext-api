/**
 *
 */
package eu.europeana.fulltext.alto.model;

import java.util.ArrayList;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 */
public abstract class TextNode<E extends TextElement>
        extends ArrayList<E>
        implements StyledTextElement, LanguageElement {
    protected String _lang;

    public String getLanguage() {
        return _lang;
    }

    public void setLanguage(String lang) {
        _lang = lang;
    }
}
