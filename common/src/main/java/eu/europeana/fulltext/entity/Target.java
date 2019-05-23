package eu.europeana.fulltext.entity;

import org.mongodb.morphia.annotations.Embedded;

/**
 * Created by luthien on 26/06/2018.
 */
@Embedded
public class Target {

    private Integer x;
    private Integer y;
    private Integer w;
    private Integer h;

    public Target(){}

    public Target(Integer x, Integer y, Integer w, Integer h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
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

}
