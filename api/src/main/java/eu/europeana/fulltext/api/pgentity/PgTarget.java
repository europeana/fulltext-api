package eu.europeana.fulltext.api.pgentity;

import javax.persistence.*;
import java.util.Objects;

/**
 * Created by luthien on 03/08/2021.
 */
@Entity(name = "PgTarget")
@Table(name = "target")
public class PgTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annotation_id", referencedColumnName = "id")
    private PgAnnotation pgAnnotation;

    @Column(name = "x_start")
    private Integer xStart;

    @Column(name = "y_end")
    private Integer yEnd;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    /**
     * creates a Target for a subtitle / transcription annotation
     * @param pgAnnotation  the annotation this Target belongs to
     * @param start         start index of annotation in subtitle text
     * @param end           end index of annotation in subtitle text
     */
    public PgTarget(PgAnnotation pgAnnotation, Integer start, Integer end){
        this.pgAnnotation = pgAnnotation;
        this.xStart = start;
        this.yEnd = end;
    }

    /**
     * creates a Target for a newspaper annotation
     * @param pgAnnotation  FK of the annotation this Target belongs to
     * @param x             x coordinate of target area
     * @param y             y coordinate of target area
     * @param width         width of target area
     * @param height        height of target area
     */
    public PgTarget(PgAnnotation pgAnnotation, Integer x, Integer y, Integer width, Integer height){
        this(pgAnnotation, x, y);
        this.width = width;
        this.height = height;
    }

    public PgTarget(){ }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PgAnnotation getPgAnnotation() {
        return pgAnnotation;
    }

    public void setPgAnnotation(PgAnnotation pgAnnotation) {
        this.pgAnnotation = pgAnnotation;
    }

    public Integer getxStart() {
        return xStart;
    }

    public void setxStart(int xStart) {
        this.xStart = xStart;
    }

    public Integer getyEnd() {
        return yEnd;
    }

    public void setyEnd(int yEnd) {
        this.yEnd = yEnd;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PgTarget pgTarget = (PgTarget) o;

        if (null != height && null != width){
            return getxStart().equals(pgTarget.getxStart())
                   && getyEnd().equals(pgTarget.getyEnd())
                   && getWidth().equals(pgTarget.getWidth())
                   && getHeight().equals(pgTarget.getHeight());
        } else {
            return getxStart().equals(pgTarget.getxStart())
                   && getyEnd().equals(pgTarget.getyEnd());
        }
    }

    @Override
    public int hashCode() {
        if (null != height && null != width){
            return Objects.hash(getxStart(), getyEnd(), getWidth(), getHeight());
        } else {
            return Objects.hash(getxStart(), getyEnd());
        }
    }

    @Override
    public String toString() {
        return "PgTarget{"
               + "xStart="
               + xStart
               + ", yEnd="
               + yEnd
               + ", width="
               + ((null != width) ? width : "null")
               + ", height="
               + ((null != height) ? height : "null")
               + '}';
    }
}
