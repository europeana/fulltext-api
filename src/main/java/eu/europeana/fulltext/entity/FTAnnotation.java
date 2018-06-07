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
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by luthien on 31/05/2018.
 */
@Document(collection = "fulltextAnnotation")
public class FTAnnotation {

    @Id
    private String  id;
    private String  types;
    private String  language;
    private Integer textStart;
    private Integer textEnd;
    private Integer targetX;

    private Integer targetY;
    private Integer targetW;
    private Integer targetH;


    // No args Constructor

    public FTAnnotation(String id, String types, String language,
                        Integer textStart, Integer textEnd, Integer targetX,
                        Integer targetY, Integer targetW, Integer targetH) {
        this.id = id;
        this.types = types;
        this.language = language;
        this.textStart = textStart;
        this.textEnd = textEnd;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetW = targetW;
        this.targetH = targetH;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypes() {
        return types;
    }

    public void setTypes(String types) {
        this.types = types;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getTextStart() {
        return textStart;
    }

    public void setTextStart(Integer textStart) {
        this.textStart = textStart;
    }

    public Integer getTextEnd() {
        return textEnd;
    }

    public void setTextEnd(Integer textEnd) {
        this.textEnd = textEnd;
    }

    public Integer getTargetX() {
        return targetX;
    }

    public void setTargetX(Integer targetX) {
        this.targetX = targetX;
    }

    public Integer getTargetY() {
        return targetY;
    }

    public void setTargetY(Integer targetY) {
        this.targetY = targetY;
    }

    public Integer getTargetW() {
        return targetW;
    }

    public void setTargetW(Integer targetW) {
        this.targetW = targetW;
    }

    public Integer getTargetH() {
        return targetH;
    }

    public void setTargetH(Integer targetH) {
        this.targetH = targetH;
    }
}
