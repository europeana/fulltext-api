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
 * Created by luthien on 10/07/2018.
 */

public class AnnoPageRdf {
    private String              resourceId;
    private String              datasetId;
    private String              localId;
    private String              pageId;
    private String              ftText;
    private String              ftLang;
    private String              imgTargetBase;
    private AnnotationRdf pageAnnotationRdf;
    private List<AnnotationRdf> annotationRdfList;

    public AnnoPageRdf(String datasetId, String localId, String resourceId, String pageId) {
        this.datasetId  = datasetId;
        this.localId    = localId;
        this.resourceId = resourceId;
        this.pageId = pageId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getFtText() {
        return ftText;
    }

    public void setFtText(String ftText) {
        this.ftText = ftText;
    }

    public String getFtLang() {
        return ftLang;
    }

    public void setFtLang(String ftLang) {
        this.ftLang = ftLang;
    }

    public List<AnnotationRdf> getAnnotationRdfList() {
        return annotationRdfList;
    }

    public void setAnnotationRdfList(List<AnnotationRdf> annotationRdfList) {
        this.annotationRdfList = annotationRdfList;
    }

    public AnnotationRdf getPageAnnotationRdf() {
        return pageAnnotationRdf;
    }

    public void setPageAnnotationRdf(AnnotationRdf pageAnnotationRdf) {
        this.pageAnnotationRdf = pageAnnotationRdf;
    }

    public String getImgTargetBase() {
        return imgTargetBase;
    }

    public void setImgTargetBase(String imgTargetBase) {
        this.imgTargetBase = imgTargetBase;
    }
}
