/**
 *
 */
package eu.europeana.fulltext.resize;

import eu.europeana.edm.media.*;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 17 Jun 2018
 */
public class ImageScale implements MediaReferenceVisitor {
    public float x;
    public float y;
    public ImageScale(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static ImageScale project(ImageDimension d1, ImageDimension d2) {
        return new ImageScale((float) d2.w / d1.w, (float) d2.h / d1.h);
    }

    @Override
    public void visit(ImageBoundary img) {
        if (img == null) {
            return;
        }

        img.h = Math.round(((float) img.h * this.y));
        img.y = Math.round(((float) img.y * this.y));
        img.w = Math.round(((float) img.w * this.x));
        img.x = Math.round(((float) img.x * this.x));
    }

    @Override
    public void visit(TimeBoundary time) {
    }

    @Override
    public void visit(MediaResource media) {
    }
}