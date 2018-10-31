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
import org.mongodb.morphia.annotations.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Created by luthien on 31/05/2018.
 * Namespace assumptions (see the FTDefinitions class):
 * IIIF Api base URL: https://iiif.europeana.eu/presentation/
 * Resource base URL: https://www.europeana.eu/api/fulltext/
 *
 */
@Entity(value = "AnnoPage")
@Indexes(@Index(fields = { @Field("dsId"), @Field("lcId"), @Field("pgId") }, options = @IndexOptions(unique = true)))
public class AnnoPage {

    @Id
    private ObjectId         _id;   // Mongo ObjectId
    private String           dsId;  // IIIF_API_BASE_URL/{dsId}/      /annopage/
    private String           lcId;  // IIIF_API_BASE_URL/      /{lcId}/annopage/
    private String           pgId;  // IIIF_API_BASE_URL/      /      /annopage/{pgId}
    private String           tgtId; // IIIF_API_BASE_URL/      /      /canvas/{tgtId} USE WHOLE URL!!
    private List<Annotation> ans;   // List of Annotations
    private Date             modified = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

    @Reference
    private Resource res;           // RESOURCE_BASE_URL/      /      /{resId} (= resource)


    public AnnoPage() {}

    public AnnoPage(String dsId, String lcId, String pgId, String tgtId, Resource res) {
        this.dsId  = dsId;
        this.lcId  = lcId;
        this.pgId  = pgId;
        this.tgtId = tgtId;
        this.res   = res;
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

    public Resource getRes() {
        return res;
    }

    public void setRes(Resource res) {
        this.res = res;
    }

    public String getTgtId() {
        return tgtId;
    }

    public void setTgtId(String tgtId) {
        this.tgtId = tgtId;
    }

    public List<Annotation> getAns() {
        return ans;
    }

    public void setAns(List<Annotation> ans) {
        this.ans = ans;
    }

    public Date getModified() {
        return modified;
    }
}
