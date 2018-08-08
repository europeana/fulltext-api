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

package eu.europeana.fulltext.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by luthien on 31/05/2018.
 */
@Document(collection = "Resource")
//@Entity(noClassnameStored = true)
@CompoundIndexes({
                         @CompoundIndex(name = "dataset_local_res",
                                        unique = true,
                                        def = "{'dsId' : 1, 'lcId': 1, '_id': 1}")
                 })
public class Resource {

    @Id
    private String id;    // custom Mongo ID
    private String dsId;  // IIIF_API_BASE_URL/{dsId}/      /annopage/
    private String lcId;  // IIIF_API_BASE_URL/      /{lcId}/annopage/
    private String lang;
    private String value;

    public Resource() {
    }

    public Resource(String id, String lang, String value) {
        this.id = id;
        this.lang = lang;
        this.value = value;
    }

    public Resource(String id, String lang, String value, String dsId, String lcId) {
        this(id, lang, value);
        this.dsId = dsId;
        this.lcId = lcId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getDsId() {
        return dsId;
    }

    public void setDsId(String dsId) {
        this.dsId = dsId;
    }

    public String getLcId() {
        return lcId;
    }

    public void setLcId(String lcId) {
        this.lcId = lcId;
    }

}
