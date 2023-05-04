/**
 *
 */
package eu.europeana.fulltext.alto.utils;

import eu.europeana.fulltext.alto.model.AltoPage;
import eu.europeana.fulltext.alto.model.LanguageElement;
import eu.europeana.fulltext.alto.model.TextElement;
import eu.europeana.fulltext.alto.model.TextNode;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 19 Dec 2018
 *
 * Fix bad language codes or language codes that have been deprecated with the 
 * correct ones over the full alto file
 */
public class AltoLanguageCorrector implements AltoPageProcessor {
    private final LanguageCorrector _corrector = new LanguageCorrector();

    public void process(AltoPage page) {
        correct(page);
    }

    private void correct(TextNode<? extends TextElement> node) {
        if (node instanceof LanguageElement) {
            LanguageElement le = node;
            le.setLanguage(_corrector.correct(le.getLanguage()));
        }

        for (TextElement e : node) {
            if (e instanceof TextNode) {
                correct((TextNode) e);
            }
        }
    }
}