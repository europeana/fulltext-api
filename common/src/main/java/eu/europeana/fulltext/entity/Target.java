package eu.europeana.fulltext.entity;

import dev.morphia.annotations.Embedded;

/**
 * Created by luthien on 26/06/2018.
 */
@Embedded
public class Target {

    private Integer x;
    private Integer y;
    private Integer w;
    private Integer h;
    private Long    start;
    private Long    end;

    public Target(){}

    public Target(Integer x, Integer y, Integer w, Integer h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public Target(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getW() {
        return w;
    }

    public void setW(Integer w) {
        this.w = w;
    }

    public Integer getH() {
        return h;
    }

    public void setH(Integer h) {
        this.h = h;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }
}
