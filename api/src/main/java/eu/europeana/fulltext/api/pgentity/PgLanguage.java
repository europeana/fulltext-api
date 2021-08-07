package eu.europeana.fulltext.api.pgentity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Created by luthien on 03/08/2021.
 */
@Entity(name = "PgLanguage")
@Table(name = "language")
public class PgLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "value")
    private String value;

    /**
     * creates a language record
     * @param value language identifier
     */
    public PgLanguage(String value){
        this.value = ((null != value) ? value : "null");
    }

    public PgLanguage(){ }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PgLanguage pgLanguage = (PgLanguage) o;
        return getValue().equals(pgLanguage.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}
