package eu.europeana.fulltext.api.model.v2;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonldType("oa:SpecificResource")
@JsonPropertyOrder({"id"})
public class AnnotationFullBodyV2 extends AnnotationBodyV2{

    private static final long serialVersionUID = 3894020068338298481L;

    private String full;
    private String language;

    public AnnotationFullBodyV2(String id) {
        super(id);
    }

    public String getFull() {
        return this.full;
    }

    public void setFull(String full) {
        this.full = full;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}