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

package eu.europeana.fulltext.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

import static eu.europeana.fulltext.api.config.FTDefinitions.*;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonPropertyOrder({"context", "id"})
public class FullTextResource extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = -2460385486748326124L;

    @JsonProperty("@context")
    private String context = MEDIA_TYPE_EDM_JSONLD;
    private String language;
    private String value;


    public FullTextResource(String id) {
        super(id, EDM_FULLTESTRESOURCE_TYPE);
    }

    public FullTextResource(String id, String language, String value) {
        this(id);
        this.language = language;
        this.value = value;
    }

    public String getLanguage() {
        return language;
    }


    public String getValue() {
        return value;
    }
}

