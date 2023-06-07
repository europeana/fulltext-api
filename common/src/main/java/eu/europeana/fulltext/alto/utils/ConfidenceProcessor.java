/**
 *
 */
package eu.europeana.fulltext.alto.utils;

import eu.europeana.fulltext.alto.model.*;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 19 Dec 2018
 */
public class ConfidenceProcessor implements AltoPageProcessor {
    private float _sum = 0f;
    private int _count = 0;

    public void process(AltoPage page) {
        try {
            for (TextBlock e : page) {
                process(e, e.getCorrection());
            }

            Float pc = (_count == 0 ? null : (_sum / (float) _count));
            if (!page.hasConfidence()) {
                page.setConfidence(pc);
            }
        } finally {
            _sum = 0;
            _count = 0;
        }
    }

    private void process(TextBlock block, boolean correction) {
        for (TextNode e : block) {
            if (e instanceof TextBlock) {
                TextBlock child = (TextBlock) e;
                process(child, correction || child.getCorrection());
            } else {
                TextLine child = (TextLine) e;
                process(child, correction || child.getCorrection());
            }
        }
    }

    private void process(TextLine line, boolean correction) {
        for (TextElement e : line) {
            if (!(e instanceof TextString)) {
                continue;
            }

            TextString w = (TextString) e;
            process(w, correction || w.getCorrection());
        }
    }

    private void process(TextString word, boolean correction) {
        if (correction) {
            word.setConfidence(1.0f);
        }

        Float c = word.getConfidence();
        if (c == null) {
            return;
        }

        _sum += c;
        _count++;
    }
}
