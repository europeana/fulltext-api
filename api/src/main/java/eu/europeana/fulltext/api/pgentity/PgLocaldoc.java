package eu.europeana.fulltext.api.pgentity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Created by luthien on 03/08/2021.
 */
@Entity(name = "PgLocaldoc")
@Table(name = "localdoc")
public class PgLocaldoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "value")
    private String value;

    /**
     * creates an local identifier for a document (having multiple pages)
     * @param value local identifier
     */
    public PgLocaldoc(String value){
        this.value = value;
    }

    public PgLocaldoc(){ }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PgLocaldoc pgLocaldoc = (PgLocaldoc) o;
        return getValue().equals(pgLocaldoc.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}
