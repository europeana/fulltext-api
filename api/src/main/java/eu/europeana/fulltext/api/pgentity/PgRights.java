package eu.europeana.fulltext.api.pgentity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Created by luthien on 03/08/2021.
 */
@Entity(name = "PgRights")
@Table(name = "rights")
public class PgRights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "value")
    private String value;

    /**
     * Creates a Rights record
     * @param value description
     */
    public PgRights(String value){
        this.value = ((null != value) ? value : "null");
    }

    public PgRights(){ }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
        PgRights pgRights = (PgRights) o;
        return getValue().equals(pgRights.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}
