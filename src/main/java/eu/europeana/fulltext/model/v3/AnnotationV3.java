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

package eu.europeana.fulltext.model.v3;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * Created by luthien target 14/06/2018.
 */
@JsonldType(value = "Annotation")
public class AnnotationV3 extends JsonLdId implements Serializable {

    private static final long serialVersionUID = -7091618924397220872L;

    private String          motivation = "transcribing";
    private String           dcType;
    private AnnotationBodyV3 annotationBodyV3;
    private String           body;
    private String           target;

    public AnnotationV3(String id) {
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
        this.target = motivation;
    }

    public AnnotationBodyV3 getAnnotationBodyV3() {
        return annotationBodyV3;
    }

    public void setAnnotationBodyV3(AnnotationBodyV3 annotationBodyV3) {
        this.annotationBodyV3 = annotationBodyV3;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}

