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
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.List;

/**
 * Created by luthien on 31/05/2018.
 */
//@Document(collection = "ftAnnotation")
public class FTAnnotation {

//    @Id
//    private ObjectId       _id;    // Mongo ObjectId
    private String         anId;   // IIIF_API_BASE_URL/               /            /annotation/{anId}

    private String         dcType;
    private String         motiv;  // can be stored but is initially not used for output
    private String         lang;   // optional
    @DBRef
    private FTResource     res;    // implements the AnnotationBody
    private Integer        from;
    private Integer        to;
    private List<FTTarget> tgs;    // Only the coordinates. Can be multiple e.g. in case of abbreviated words

    // parameters below are only used when the Annotation's datasetId and /or localId differ from
    // the other Annotations for this AnnoPage. It is unknown if this occurs, but providing the possibility in any case.
    private String anDsId;         // IIIF_API_BASE_URL/{anDsId}/        /annotation/..
    private String anLcId;         // IIIF_API_BASE_URL/        /{anLcId}/annotation/..

    // to provide a way to define a Resource Base URL that does not conform to the regular namespace, eg for external resources
    private String anResUrl;

    // to provide a way to define a Target URL that does not conform to the regular namespace, eg for external targets
    private String anTgUrl;

    public FTAnnotation(){}

    public FTAnnotation(String anId, String dcType, Integer from, Integer to) {
        this.anId   = anId;
        this.dcType = dcType;
        this.from   = from;
        this.to     = to;
    }

    public FTAnnotation(String anId, String dcType, Integer from, Integer to,
                        FTResource res) {
        this(anId, dcType, from, to);
        this.res = res;
    }

    public FTAnnotation(String anId, String dcType, Integer from, Integer to,
                        FTResource res, List<FTTarget> tgs) {
        this(anId, dcType, from, to, res);
        this.tgs = tgs;
    }

    public FTAnnotation(String anId, String dcType, Integer from, Integer to,
                        FTResource res, List<FTTarget> tgs, String lang) {
        this(anId, dcType, from, to, res, tgs);
        this.lang = lang;
    }

    public String getAnId() {
        return anId;
    }

    public void setAnId(String anId) {
        this.anId = anId;
    }

    public String getDcType() {
        return dcType;
    }

    public void setDcType(String dcType) {
        this.dcType = dcType;
    }

    public String getMotiv() {
        return motiv;
    }

    public void setMotiv(String motiv) {
        this.motiv = motiv;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public FTResource getRes() {
        return res;
    }

    public void setRes(FTResource res) {
        this.res = res;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }

    public List<FTTarget> getTgs() {
        return tgs;
    }

    public void setTgs(List<FTTarget> tgs) {
        this.tgs = tgs;
    }

    public String getAnDsId() {
        return anDsId;
    }

    public void setAnDsId(String anDsId) {
        this.anDsId = anDsId;
    }

    public String getAnLcId() {
        return anLcId;
    }

    public void setAnLcId(String anLcId) {
        this.anLcId = anLcId;
    }

    public String getAnResUrl() {
        return anResUrl;
    }

    public void setAnResUrl(String anResUrl) {
        this.anResUrl = anResUrl;
    }

    public String getAnTgUrl() {
        return anTgUrl;
    }

    public void setAnTgUrl(String anTgUrl) {
        this.anTgUrl = anTgUrl;
    }
}
