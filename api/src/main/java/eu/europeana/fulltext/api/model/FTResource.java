package eu.europeana.fulltext.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static eu.europeana.fulltext.api.config.FTDefinitions.EDM_FULLTEXTRESOURCE_TYPE;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_EDM_JSONLD;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonPropertyOrder({"context", "id", "type", "rights", "language", "value"})
public class FTResource extends JsonLdIdType {

    private static final long serialVersionUID = -2460385486748326124L;

    @JsonProperty("@context")
    private String context = MEDIA_TYPE_EDM_JSONLD;
    private String language;
    private String value;
    private String source;
    private String rights;


    private FTResource(String id) {
        super(id, EDM_FULLTEXTRESOURCE_TYPE);
    }

    public FTResource(String id, String language, String value, String rights) {
        this(id);
        this.language   = language;
        this.value      = value;
        this.rights     = rights;
    }

    public FTResource(String id, String language, String value, String source, String rights) {
        this(id);
        this.language   = language;
        this.value      = value;
        this.source     = source;
        this.rights     = rights;
    }

    public String getLanguage() {
        return language;
    }

    public String getValue() {
        return value;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRights() { return rights; }

}

