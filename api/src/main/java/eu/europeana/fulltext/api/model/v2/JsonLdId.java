package eu.europeana.fulltext.api.model.v2;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldId;

import java.io.Serializable;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonPropertyOrder({"id"}) // make sure id always comes first, instead of last
public class JsonLdId implements Serializable {

    private static final long serialVersionUID = 8639654567514332458L;

    @JsonldId
    private String id;

    JsonLdId() {
        // empty constructor to make it also deserializable (see SonarQube squid:S2055)
    }

    JsonLdId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}

