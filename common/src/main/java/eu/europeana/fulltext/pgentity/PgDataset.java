package eu.europeana.fulltext.pgentity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Created by luthien on 03/08/2021.
 */
@Entity(name = "PgDataset")
@Table(name = "dataset")
public class PgDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    @NotNull
    @Column(name = "value")
    private String value;

    /**
     * creates an dataset identifier (having multiple local documents)
     * @param value dataset identifier
     */
    public PgDataset(String value){
        this.value = value;
    }

    public PgDataset(){ }

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
        PgDataset pgDataset = (PgDataset) o;
        return getValue().equals(pgDataset.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}
