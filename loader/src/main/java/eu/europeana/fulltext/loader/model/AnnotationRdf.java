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

package eu.europeana.fulltext.loader.model;

import java.util.List;

/**
 * Created by luthien on 18/07/2018.
 */
public class AnnotationRdf {

    private String       id;
    private String       dcType;
    private String       motiv;
    private String       lang;
    private Integer      from;
    private Integer      to;

    private List<TargetRdf> targetRdfList;

    public AnnotationRdf(String id,
                         String dcType) {
        this.id = id;
        this.dcType = dcType;
    }

    public AnnotationRdf(String id,
                         String dcType,
                         String lang) {
        this(id, dcType);
        this.lang = lang;
    }

    public AnnotationRdf(String  id,
                         String  dcType,
                         Integer from,
                         Integer to) {
        this(id, dcType);
        this.from = from;
        this.to   = to;
    }

    public AnnotationRdf(String id,
                         String dcType,
                         String motiv,
                         List   targetList) {
        this(id, dcType);
        this.motiv         = motiv;
        this.targetRdfList = targetList;
    }

    public AnnotationRdf(String id,
                         String dcType,
                         String motiv,
                         String lang,
                         List   targetList) {
        this(id, dcType, lang);
        this.motiv         = motiv;
        this.targetRdfList = targetList;
    }

    public AnnotationRdf(String  id,
                         String  dcType,
                         String  motiv,
                         Integer from,
                         Integer to) {
        this(id, dcType, from, to);
        this.motiv    = motiv;
    }

    public AnnotationRdf(String  id,
                         String  dcType,
                         String  motiv,
                         Integer from,
                         Integer to,
                         List    targetList) {
        this(id, dcType, motiv, from, to);
        this.targetRdfList = targetList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<TargetRdf> getTargetRdfList() {
        return targetRdfList;
    }

    public void setTargetRdfList(List<TargetRdf> targetRdfList) {
        this.targetRdfList = targetRdfList;
    }

}
