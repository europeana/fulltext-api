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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * Created by luthien on 14/06/2018.
 */
public class AnnotationPage extends JsonLdId implements Serializable{

    private static final long serialVersionUID = 3852844091029376312L;

    private Annotation[] items;

    public AnnotationPage(String id) {
        super(id);
    }

    public Annotation[] getItems() {
        return items;
    }

    public void setItems(Annotation[] items) {
        this.items = items;
    }

}
