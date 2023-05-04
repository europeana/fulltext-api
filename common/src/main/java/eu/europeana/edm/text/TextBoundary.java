package eu.europeana.edm.text;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 17 Jun 2018
 */
public class TextBoundary implements TextReference {
    private TextReference reference;
    public int from;
    public int to;


    public TextBoundary(TextReference reference) {
        this.reference = reference;
    }

    public TextBoundary(TextReference reference, int from, int to) {
        this.reference = reference;
        this.from = from;
        this.to = to;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public String getFragment() {
        if (from == 0 && to == 0) {
            return "";
        } else {
            return ("#char=" + from + "," + to);
        }
    }

    public void shift(int chars) {
        this.from += chars;
        this.to += chars;
    }

    public FullTextResource getResource() {
        return reference.getResource();
    }

    public String getResourceURL() {
        return reference.getResourceURL();
    }

    public String getURL() {
        return reference.getURL() + getFragment();
    }
}

