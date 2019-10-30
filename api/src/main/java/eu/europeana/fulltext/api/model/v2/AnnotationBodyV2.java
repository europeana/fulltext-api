package eu.europeana.fulltext.api.model.v2;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonPropertyOrder({"id"})
public class AnnotationBodyV2 extends JsonLdId implements Serializable{

    private static final long serialVersionUID = -814446825873060414L;

    private String full;
    private String language;
    private String type;
    private String value;

    public AnnotationBodyV2(String id) {
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

    public String getType() { return this.type; }

    public void setType(String type) { this.type = type; }

    public String getValue() { return this.value; }

    public void setValue(String value) { this.value = value; }
}