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

package eu.europeana.fulltext.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import static eu.europeana.fulltext.config.FTDefinitions.EDM_WEBRESOURCE_TYPE;
import static eu.europeana.fulltext.config.FTDefinitions.EDM_FULLTESTRESOURCE_TYPE;

import java.io.Serializable;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonPropertyOrder({"context", "id"})
public class FullTextResource extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = -7091618924397220872L;

    private String value;


    public FullTextResource(String id) {
        super(id, new String[] {EDM_WEBRESOURCE_TYPE, EDM_FULLTESTRESOURCE_TYPE});
    }

    public FullTextResource(String id, String value) {
        this(id);
        this.value = value;
    }

}

