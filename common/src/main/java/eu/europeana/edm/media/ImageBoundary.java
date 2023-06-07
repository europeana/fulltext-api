package eu.europeana.edm.media;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 17 Jun 2018
 */
public class ImageBoundary extends MediaBoundary {
    public int x;
    public int y;
    public int w;
    public int h;

    public ImageBoundary(MediaReference ref, int x, int y, int w, int h) {
        super(ref);
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    @Override
    public String getFragment() {
        return ("#xywh=" + x + "," + y + "," + w + "," + h);
    }

    /**
     * Clips the positions outside Image boundaries
     * @param d image dimension
     * @return image boundary
     */
    public ImageBoundary clip(ImageDimension d) {
        //left boundary
        if (this.x < 0) {
            this.w += this.x;
            this.x = 0;
        }
        //top boundary
        if (this.y < 0) {
            this.h += this.y;
            this.y = 0;
        }

        //right boundary
        int rightBound = this.x + this.w;
        if (rightBound > d.w) {
            this.w = this.w - (rightBound - d.w);
        }

        //bottom boundary
        int bottomBound = this.y + this.h;
        if (bottomBound > d.h) {
            this.h = this.h - (bottomBound - d.h);
        }

        return this;
    }

    public boolean isValid() {
        return (this.x >= 0 && this.y >= 0 && this.w > 0 && this.h > 0);
    }

    @Override
    public void visit(MediaReferenceVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "ImageBoundary{" +
                "x=" + x +
                ", y=" + y +
                ", w=" + w +
                ", h=" + h +
                '}';
    }
}
