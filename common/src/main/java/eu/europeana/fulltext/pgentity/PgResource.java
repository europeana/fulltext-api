package eu.europeana.fulltext.pgentity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Created by luthien on 03/08/2021.
 */
@Entity(name = "PgResource")
@Table(name = "resource")
public class PgResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lang_id", referencedColumnName = "id")
    private PgLanguage pgLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rights_id", referencedColumnName = "id")
    private PgRights pgRights;

    @Column(name = "original")
    private boolean original;

    @NotNull
    @Column(name = "value")
    private String value;

    @NotNull
    @Column(name = "source")
    private String source;

    /**
     * Creates a Fulltext Resource
     * @param pgLanguage    language the resource is in
     * @param pgRights      Rights that apply to the resource
     * @param value         resource text
     */
    public PgResource(PgLanguage pgLanguage, PgRights pgRights, String value){
        this.pgLanguage = pgLanguage;
        this.pgRights = pgRights;
        this.value = value;
    }

    /**
     * Creates a Fulltext Resource
     * @param pgLanguage    language the resource is in
     * @param pgRights      Rights that apply to the resource
     * @param original      if the resource is the original (as opposed to translated)
     * @param value         resource text
     */
    public PgResource(PgLanguage pgLanguage, PgRights pgRights, boolean original, String value){
        this(pgLanguage, pgRights, value);
        this.original = original;
    }

    public PgResource(){ }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PgLanguage getPgLanguage() {
        return pgLanguage;
    }

    public void setPgLanguage(PgLanguage pgLanguage) {
        this.pgLanguage = pgLanguage;
    }

    public PgRights getPgRights() {
        return pgRights;
    }

    public void setPgRights(PgRights pgRights) {
        this.pgRights = pgRights;
    }

    public boolean isOriginal() {
        return original;
    }

    public void setOriginal(boolean original) {
        this.original = original;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PgResource that = (PgResource) o;
        return isOriginal() == that.isOriginal()
               && getPgLanguage().getId().equals(that.getPgLanguage().getId())
               && getPgRights().getId().equals(that.getPgRights().getId())
               && getValue().equals(that.getValue())
               && getSource().equals(that.getSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPgLanguage().getId(), getPgRights().getId(), isOriginal(), getValue(), getSource());
    }

    @Override
    public String toString() {
        return "PgResource{"
               + "pgLanguage="
               + pgLanguage.getValue()
               + ", pgRights="
               + pgRights.getValue()
               + ", original="
               + original
               + ", value='"
               + value
               + '\''
               + ", source='"
               + source
               + '\''
               + '}';
    }
}
