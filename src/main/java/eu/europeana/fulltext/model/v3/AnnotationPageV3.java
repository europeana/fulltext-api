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
 * Created by luthien on 14/06/2018.
 */
@JsonldType("AnnotationPage")
public class AnnotationPageV3 extends JsonLdId implements Serializable{

    private static final long serialVersionUID = 3852844091029376312L;
    private AnnotationV3[] items;

    public AnnotationPageV3(String id) {
        super(id);
    }

    public AnnotationV3[] getItems() {
        return items;
    }

    public void setItems(AnnotationV3[] items) {
        this.items = items;
    }

}
