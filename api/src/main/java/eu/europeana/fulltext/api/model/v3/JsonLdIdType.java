/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.api.model.v3;

import java.io.Serializable;

/**
 * Id and Type are common fields for Fulltext v3 types as well
 * After the IIIF version by Patrick Ehlert
 *
 * @author Luthien
 * Created on 02-07-2018
 */

public class JsonLdIdType implements Serializable{

    private static final long serialVersionUID = 509628293285922978L;

    private String id;
    private String type;

    public JsonLdIdType() {
        // empty constructor to make it also deserializable (see SonarQube squid:S2055)
    }

    public JsonLdIdType(String id) {
        this.id = id;
    }

    public JsonLdIdType(String id, String type) {
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

