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

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonldType(value = "oa:Annotation")
public class Annotation extends JsonLdId implements Serializable {

    private static final long serialVersionUID = -7091618924397220872L;

    private String          motivation = "sc:painting";
    private String          dcType;
    private AnnotationBody  annotationBody;
    private String          resource;
    private String          on;

    public Annotation(String id) {
        super(id);
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
        this.on = motivation;
    }

    public AnnotationBody getAnnotationBody() {
        return annotationBody;
    }

    public void setAnnotationBody(AnnotationBody annotationBody) {
        this.annotationBody = annotationBody;
    }

    public String getResource() {
        return this.resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getOn() {
        return on;
    }

    public void setOn(String on) {
        this.on = on;
    }
}

