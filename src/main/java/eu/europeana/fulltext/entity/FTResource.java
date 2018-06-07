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

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Set;

/**
 * Created by luthien on 31/05/2018.
 */
@Document(collection = "fulltextResource")
public class FTResource {

    @Id
    private String             id;
    private String             language;
    private String             target;
    private String             text;
    private String             annotationId;
    @DBRef
    private List<FTAnnotation> FTAnnotations;


    // No args Constructor

    public FTResource(String id, String language, String target, String text,
                      String annotationId) {
        this.id = id;
        this.language = language;
        this.target = target;
        this.text = text;
        this.annotationId = annotationId;
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

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(String annotationId) {
        this.annotationId = annotationId;
    }

    public List<FTAnnotation> getFTAnnotations() {
        return FTAnnotations;
    }

    public void setFTAnnotations(List<FTAnnotation> FTAnnotations) {
        this.FTAnnotations = FTAnnotations;
    }
}
