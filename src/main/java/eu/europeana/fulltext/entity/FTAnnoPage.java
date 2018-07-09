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

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Created by luthien on 31/05/2018.
 * Namespace assumptions (see the FTDefinitions class):
 * IIIF Api base URL: https://iiif.europeana.eu/presentation/
 * Resource base URL: https://www.europeana.eu/api/fulltext/
 *
 */
@Document(collection = "ftAnnoPage")
public class FTAnnoPage {

    @Id
    private ObjectId           _id;   // Mongo ObjectId
    // these combined must be unique
    private String             dsId;  // IIIF_API_BASE_URL/{dsId}/      /annopage/
    private String             lcId;  // IIIF_API_BASE_URL/      /{lcId}/annopage/
    private String             pgId;  // IIIF_API_BASE_URL/      /      /annopage/{pgId}

    private String             resId; // RESOURCE_BASE_URL/      /      /{resId}
    private String             tgtId; // IIIF_API_BASE_URL/      /      /canvas/{tgtId}
    private String             lang;
    private FTAnnotation       pgAn;  // FTAnnotation
    private List<FTAnnotation> ans;   // List of Annotations

    public FTAnnoPage(String dsId, String lcId,  String pgId,
                      String lang, String resId, String tgtId) {
        this.dsId  = dsId;
        this.lcId  = lcId;
        this.pgId  = pgId;
        this.resId = resId;
        this.tgtId = tgtId;
        this.lang  = lang;
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

    public String getPgId() {
        return pgId;
    }

    public void setPgId(String pgId) {
        this.pgId = pgId;
    }

    public String getResId() {
        return resId;
    }

    public void setResId(String resId) {
        this.resId = resId;
    }

    public String getTgtId() {
        return tgtId;
    }

    public void setTgtId(String tgtId) {
        this.tgtId = tgtId;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public FTAnnotation getPgAn() {
        return pgAn;
    }

    public void setPgAn(FTAnnotation pgAn) {
        this.pgAn = pgAn;
    }

    public List<FTAnnotation> getAns() {
        return ans;
    }

    public void setAns(List<FTAnnotation> ans) {
        this.ans = ans;
    }
}
