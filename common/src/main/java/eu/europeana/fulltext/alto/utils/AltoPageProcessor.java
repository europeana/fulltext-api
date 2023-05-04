/**
 *
 */
package eu.europeana.fulltext.alto.utils;

import eu.europeana.fulltext.alto.model.AltoPage;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 19 Dec 2018
 */
public interface AltoPageProcessor {
    void process(AltoPage page);
}
