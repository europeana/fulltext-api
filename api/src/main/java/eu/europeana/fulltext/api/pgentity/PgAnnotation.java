package eu.europeana.fulltext.api.pgentity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by luthien on 03/08/2021.
 */
@Entity(name = "PgAnnotation")
@Table(name = "annotation")
public class PgAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annopage_id", referencedColumnName = "id")
    private PgAnnopage pgAnnopage;

    @NotNull
    @Column(name = "dc_type")
    private String dcType;

    @Column(name = "from_index")
    private Integer fromIndex;

    @Column(name = "to_index")
    private Integer toIndex;

    @OneToMany(
            mappedBy = "pgAnnotation",
            cascade = CascadeType.ALL
    )
    private List<PgTarget> pgTargets = new ArrayList<>();

    /**
     * creates an Annotation object
     * @param pgAnnopage    the annotation page this Annotation is part of
     * @param dcType        type of the Annotation (word, block, page, etc.)
     * @param fromIndex     from where the Annotation applies (in character position (textual) or milliseconds (video)
     * @param toIndex       to where the Annotation applies (in character position (textual) or milliseconds (video)
     */
    public PgAnnotation(PgAnnopage pgAnnopage, char dcType, Integer fromIndex, Integer toIndex) {
        this.pgAnnopage = pgAnnopage;
        this.dcType = Character.toString(dcType);
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /**
     * creates a top level Annotation object without indexes
     * @param pgAnnopage    the annotation page this Annotation is part of
     * @param dcType        type of the Annotation (word, block, page, etc.)
     */
    public PgAnnotation(PgAnnopage pgAnnopage, char dcType) {
        this.pgAnnopage = pgAnnopage;
        this.dcType = Character.toString(dcType);
    }
    public PgAnnotation(){ }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PgAnnopage getPgAnnopage() {
        return pgAnnopage;
    }

    public void setPgAnnopage(PgAnnopage pgAnnopage) {
        this.pgAnnopage = pgAnnopage;
    }

    public String getDcType() {
        return dcType;
    }

    public void setDcType(String dcType) {
        this.dcType = dcType;
    }

    public Integer getFromIndex() {
        return fromIndex;
    }

    public void setFromIndex(Integer fromIndex) {
        this.fromIndex = fromIndex;
    }

    public Integer getToIndex() {
        return toIndex;
    }

    public void setToIndex(Integer toIndex) {
        this.toIndex = toIndex;
    }

    public List<PgTarget> getPgTargets() {
        return pgTargets;
    }

    public void setPgTargets(List<PgTarget> pgTargets) {
        this.pgTargets = pgTargets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PgAnnotation that = (PgAnnotation) o;
        if (null != getFromIndex() || null != getToIndex()){
            return getFromIndex() == that.getFromIndex()
                   && getToIndex() == that.getToIndex()
                   && getPgAnnopage().getId().equals(that.getPgAnnopage().getId())
                   && getDcType().equals(that.getDcType());
        } else {
            return getPgAnnopage().getId().equals(that.getPgAnnopage().getId())
                   && getDcType().equals(that.getDcType());
        }
    }

    @Override
    public int hashCode() {
        if (null != getFromIndex() || null != getToIndex()){
            return Objects.hash(getPgAnnopage().getId(), getDcType(), getFromIndex(), getToIndex());
        } else {
            return Objects.hash(getPgAnnopage().getId(), getDcType());
        }
    }

    @Override
    public String toString() {
        return "PgAnnotation{"
               + "pgAnnopage="
               + pgAnnopage.getPgDataset().getValue() + "/"
               + pgAnnopage.getPgLocaldoc().getValue() + "/"
               + pgAnnopage.getPage() + "/lang="
               + pgAnnopage.getPgResource().getPgLanguage().getValue()
               + ", dcType='"
               + dcType
               + '\''
               + ", fromIndex="
               + ((null != fromIndex) ? fromIndex : "null")
               + ", toIndex="
               + ((null != toIndex) ? toIndex : "null")
               + '}';
    }
}