package eu.europeana.fulltext.pgentity;

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
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annopage_id", referencedColumnName = "id")
    private PgAnnopage pgAnnopage;

    @NotNull
    @Column(name = "dc_type")
    private String dcType;

    @NotNull
    @Column(name = "from_index")
    private int fromIndex;

    @NotNull
    @Column(name = "to_index")
    private int toIndex;

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
    public PgAnnotation(PgAnnopage pgAnnopage, char dcType, int fromIndex, int toIndex) {
        this.pgAnnopage = pgAnnopage;
        this.dcType = Character.toString(dcType);
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
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

    public int getFromIndex() {
        return fromIndex;
    }

    public void setFromIndex(int fromIndex) {
        this.fromIndex = fromIndex;
    }

    public int getToIndex() {
        return toIndex;
    }

    public void setToIndex(int toIndex) {
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
        return getFromIndex() == that.getFromIndex()
               && getToIndex() == that.getToIndex()
               && getPgAnnopage().getId().equals(that.getPgAnnopage().getId())
               && getDcType().equals(that.getDcType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPgAnnopage().getId(), getDcType(), getFromIndex(), getToIndex());
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
               + fromIndex
               + ", toIndex="
               + toIndex
               + '}';
    }
}
