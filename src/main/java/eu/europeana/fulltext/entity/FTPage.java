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

import org.mongodb.morphia.annotations.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Created by luthien on 31/05/2018.
 */
@Document(collection = "fulltextPage")
public class FTPage {

    @Id
    private String             id;              // {datasetId}/{recordId}/{FulltextResourceAbout}
    private String             language;
    private String             targetUrl;
    private String             sourceUrl;
    private String             value;
    private FTAnnotation       pageAnnotation;  // FTAnnotation
    @DBRef
    private List<FTAnnotation> FTAnnotations;   // List of FTAnnotations


    // No args Constructor

    public FTPage(String id, String language, String targetUrl, String sourceUrl, String value) {
        this.id = id;
        this.language = language;
        this.targetUrl = targetUrl;
        this.sourceUrl = sourceUrl;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public FTAnnotation getPageAnnotation() {
        return pageAnnotation;
    }

    public void setPageAnnotation(FTAnnotation pageAnnotation) {
        this.pageAnnotation = pageAnnotation;
    }

    public List<FTAnnotation> getFTAnnotations() {
        return FTAnnotations;
    }

    public void setFTAnnotations(List<FTAnnotation> FTAnnotations) {
        this.FTAnnotations = FTAnnotations;
    }
}
