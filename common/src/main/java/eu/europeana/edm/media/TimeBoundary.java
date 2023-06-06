package eu.europeana.edm.media;

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 17 Jun 2018
 */
public class TimeBoundary extends MediaBoundary {

    private static String FORMAT = "HH:mm:ss.SSS";

    public int start;
    public int end;

    public TimeBoundary(MediaReference ref, int start, int end) {
        super(ref);
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getFragment() {
        String startOfFragment = DurationFormatUtils.formatDuration(this.start, FORMAT);
        String endOfFragment = DurationFormatUtils.formatDuration(this.end, FORMAT);
        return ("#t=" + startOfFragment + "," + endOfFragment);
    }

    public boolean isValid() {
        return (this.start >= 0 && this.end >= 0 && this.start < this.end);
    }

    @Override
    public void visit(MediaReferenceVisitor visitor) {
        visitor.visit(this);
    }
}