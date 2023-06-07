/**
 *
 */
package eu.europeana.fulltext.alto.utils;

import eu.europeana.fulltext.alto.model.AltoPage;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 19 Dec 2018
 */
public class AltoPageProcessorImpl extends ArrayList<AltoPageProcessor>
        implements AltoPageProcessor {
    public AltoPageProcessorImpl(AltoPageProcessor... processors) {
        super(Arrays.asList(processors));
    }

    public AltoPageProcessorImpl() {
        this(new ConfidenceProcessor(), new AltoLanguageProcessor()
                , new AltoLanguageCorrector());
    }

    public void process(AltoPage page) {
        for (AltoPageProcessor processor : this) {
            processor.process(page);
        }
    }
}
