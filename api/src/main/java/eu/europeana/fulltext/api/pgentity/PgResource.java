package eu.europeana.fulltext.api.pgentity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

import static eu.europeana.fulltext.api.service.Tools.nvl;

/**
 * Created by luthien on 03/08/2021.
 */
@Entity(name = "PgResource")
@Table(name = "resource")
public class PgResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_id_gen")
    @SequenceGenerator(name = "resource_id_gen", sequenceName = "resource_id_seq", allocationSize = 50)
    private Long id;

    @NotNull
    private String language;

    @NotNull
    private String rights;

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
     *
     * @param language language the resource is in
     * @param rights   Rights that apply to the resource
     * @param value    resource text
     */
    public PgResource(String language, String rights, String value, String source) {
        this.language = language;
        this.rights = nvl(rights);
        this.value = value;
        this.source = nvl(source);
    }

    /**
     * Creates a Fulltext Resource
     *
     * @param rights   language the resource is in
     * @param rights   Rights that apply to the resource
     * @param original if the resource is the original (as opposed to translated)
     * @param value    resource text
     */
    public PgResource(String language, String rights, boolean original, String value, String source) {
        this(language, rights, value, source);
        this.original = original;
    }

    public PgResource() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
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
               && language.equalsIgnoreCase(that.getLanguage())
               && rights.equalsIgnoreCase(that.getRights())
               && getValue().equals(that.getValue())
               && getSource().equals(that.getSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, rights, isOriginal(), getValue(), getSource());
    }

    @Override
    public String toString() {
        return "PgResource{"
               + "pgLanguage="
               + language
               + ", pgRights="
               + rights
               + ", original="
               + original
               + ", value="
               + value
               + ", source="
               + source
               + '}';
    }
}
