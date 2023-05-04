package eu.europeana.edm.media;


/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 17 Jun 2018
 */
public class ImageDimension {
    public int w;
    public int h;

    public ImageDimension(int w, int h) {
        this.w = w;
        this.h = h;
    }

    public String toString() {
        return ("{" + this.w + "," + this.h + "}");
    }

    public boolean isEquals(ImageDimension dim) {
        return ((this.w == dim.w) && (this.h == dim.h));
    }
}