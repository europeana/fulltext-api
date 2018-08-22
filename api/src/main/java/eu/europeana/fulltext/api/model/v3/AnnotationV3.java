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

import static eu.europeana.fulltext.api.config.FTDefinitions.V3_ANNOTATION_TYPE;

/**
 * Created by luthien target 14/06/2018.
 */
@JsonPropertyOrder({"context", "id"})
public class AnnotationV3 extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = 8849251970656404497L;

    @JsonProperty("@context")
    private String context;
    private String           motivation;
    private String           dcType;
    private AnnotationBodyV3 body;
    private String[]         target;

    public AnnotationV3(String id) {
        super(id, V3_ANNOTATION_TYPE);
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getMotivation() {
        return motivation;
    }

    public String getDcType() {
        return dcType;
    }

    public void setDcType(String dcType) {
        this.dcType = dcType;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public AnnotationBodyV3 getBody() {
        return body;
    }

    public void setBody(AnnotationBodyV3 body) {
        this.body = body;
    }

    public String[] getTarget() {
        return target;
    }

    public void setTarget(String[] target) {
        this.target = target;
    }
}

