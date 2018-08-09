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

package eu.europeana.fulltext.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonldType(value = "oa:Annotation")
@JsonPropertyOrder({"context", "id"})
public class AnnotationV2 extends JsonLdId implements Serializable {

    private static final long serialVersionUID = 7120324589144279826L;

    @JsonProperty("@context")
    private String context;
    private String              motivation;
    private String              dcType;
    private AnnotationBodyV2    resource;
    private String[]            on;

    public AnnotationV2(String id) {
        super(id);
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

    public AnnotationBodyV2 getResource() {
        return resource;
    }

    public void setResource(AnnotationBodyV2 resource) {
        this.resource = resource;
    }

    public String[] getOn() {
        return on;
    }

    public void setOn(String[] on) {
        this.on = on;
    }
}

