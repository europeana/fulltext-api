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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_EDM_JSONLD;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_IIIF_V3;
import static eu.europeana.fulltext.api.config.FTDefinitions.V3_ANNO_PAGE_TYPE;

import eu.europeana.fulltext.api.model.JsonLdIdType;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonPropertyOrder({"context", "id"})
public class AnnotationPageV3 extends JsonLdIdType implements Serializable{

    private static final long serialVersionUID = 3567695991809278386L;

    @JsonProperty("@context")
    private String[] context = new String[]{MEDIA_TYPE_IIIF_V3, MEDIA_TYPE_EDM_JSONLD};
    private AnnotationV3[] items;

    private AnnotationPageV3(String id) {
        super(id, V3_ANNO_PAGE_TYPE);
    }

    public AnnotationPageV3(String id, boolean includeContext) {
        this(id);
        if (!includeContext){
            context = null;
        }
    }

    public AnnotationV3[] getItems() {
        return items;
    }

    public void setItems(AnnotationV3[] items) {
        this.items = items;
    }

}
