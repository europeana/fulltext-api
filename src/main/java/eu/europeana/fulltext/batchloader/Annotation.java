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

package eu.europeana.fulltext.batchloader;

import java.util.List;

/**
 * Created by luthien on 18/07/2018.
 */
public class Annotation {

    private String       id;
    private String       dcType;
    private String       motiv;
    private String       lang;
    private Integer      from;
    private Integer      to;

    private List<Target> targetList;
    private String       resource;

    public Annotation(String id, String dcType) {
        this.id = id;
        this.dcType = dcType;
    }

    public Annotation(String id, String dcType, Integer from, Integer to) {
        this(id, dcType);
        this.from = from;
        this.to = to;
    }

    public Annotation(String id, String dcType, String motiv, String resource, List targetList) {
        this(id, dcType);
        this.motiv = motiv;
        this.resource = resource;
        this.targetList = targetList;
    }

    public Annotation(String id, String dcType, String motiv, Integer from, Integer to, String resource) {
        this(id, dcType, from, to);
        this.motiv = motiv;
        this.resource = resource;
    }

    public Annotation(String id, String dcType, String motiv, Integer from, Integer to, String resource, List targetList) {
        this(id, dcType, motiv, from, to, resource);
        this.targetList = targetList;
    }

    public Annotation(String id, String dcType, String motiv, String lang, Integer from, Integer to, String resource) {
        this(id, dcType, motiv, from, to, resource);
        this.lang = lang;
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

    public List<Target> getTargetList() {
        return targetList;
    }

    public void setTargetList(List<Target> targetList) {
        this.targetList = targetList;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }
}
