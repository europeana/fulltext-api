package eu.europeana.fulltext.api.model.v2;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonPropertyOrder({"id", "type", "edmRights", "source", "language", "value"})
public class AnnotationBodyV2 extends JsonLdId implements Serializable{

    private static final long serialVersionUID = -814446825873060414L;

    private String full;
    private String language;
    private String type;
    private String value;
    private String source;
    private String edmRights;

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEdmRights() { return edmRights; }

    public void setEdmRights(String edmRights) { this.edmRights = edmRights; }
}
