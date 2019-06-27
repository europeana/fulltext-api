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
    private String  start;
    private String  end;

    public Target(){}

    public Target(Integer x, Integer y, Integer w, Integer h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public Target(String start, String end) {
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

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}
