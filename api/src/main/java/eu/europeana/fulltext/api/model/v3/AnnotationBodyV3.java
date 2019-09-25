package eu.europeana.fulltext.api.model.v3;

import java.io.Serializable;
import eu.europeana.fulltext.api.model.JsonLdIdType;

/**
 * Created by luthien on 14/06/2018.
 */
public class AnnotationBodyV3 extends JsonLdIdType implements Serializable{

    private static final long serialVersionUID = 481686784002335472L;
    private String source;
    private String language;
    private String type;
    private String value;

    public AnnotationBodyV3(String id) {
        super(id);
    }

    public AnnotationBodyV3(String id, String type) {
        super(id, type);
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
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