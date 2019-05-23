package eu.europeana.fulltext.api.model;

import java.io.Serializable;

/**
 * Id and Type are common fields for Fulltext v3 types as well
 * After the IIIF version by Patrick Ehlert
 *
 * @author Luthien
 * Created on 02-07-2018
 */

public class JsonLdIdType implements Serializable{

    private static final long serialVersionUID = -2465231545266868944L;

    private String id;
    private String type;

    protected JsonLdIdType() {
        // empty constructor to make it also deserializable (see SonarQube squid:S2055)
    }

    protected JsonLdIdType(String id) {
        this.id = id;
    }

    protected JsonLdIdType(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}

