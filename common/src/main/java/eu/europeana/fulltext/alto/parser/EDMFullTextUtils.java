/**
 *
 */
package eu.europeana.fulltext.alto.parser;

import eu.europeana.edm.media.ImageBoundary;
import eu.europeana.edm.media.MediaReference;
import eu.europeana.edm.text.TextBoundary;
import eu.europeana.edm.text.TextReference;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 */
public class EDMFullTextUtils {
    public static String EUROPEANA_DATA_NS = "http://data.europeana.eu/item";

    public static TextBoundary newTextBoundary(TextReference ref, int s, int e) {
        return (s < 0 || e < 0 || s > e ? null : new TextBoundary(ref, s, e));
    }

    public static ImageBoundary newImageBoundary(MediaReference ref
            , Integer x, Integer y
            , Integer w, Integer h) {
        return ((x == null || y == null || w == null || h == null)
                ? null : new ImageBoundary(ref, x, y, w, h));
    }

    public static String getItemID(String itemURI) {
        return (itemURI == null ? null : itemURI.replace(EUROPEANA_DATA_NS, ""));
    }
}
