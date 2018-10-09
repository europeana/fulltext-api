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

package eu.europeana.fulltext.api.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.fulltext.api.model.AnnotationWrapper;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_EDM_JSONLD;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_IIIF_V2;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonldType(value = "sc:AnnotationList")
@JsonPropertyOrder({"context", "id"})
public class AnnotationPageV2 extends JsonLdId implements Serializable, AnnotationWrapper {

    private static final long serialVersionUID = -491589144458820254L;

    @JsonProperty("@context")
    private String[] context = new String[]{MEDIA_TYPE_IIIF_V2, MEDIA_TYPE_EDM_JSONLD};
    private AnnotationV2[] resources;

    public AnnotationPageV2(String id) {
        super(id);
    }

    public AnnotationV2[] getResources() {
        return resources;
    }

    public void setResources(AnnotationV2[] resources) {
        this.resources = resources;
    }

    @Override
    public String[] getContext() {
        return new String[0];
    }

    @Override
    public void setContext(String[] context) {

    }
}
