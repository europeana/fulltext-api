package eu.europeana.fulltext.api.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.model.AnnotationWrapper;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by luthien on 14/06/2018.
 */
//@JsonldType(value = "oa:Annotation") // commenting this out works for property ordering #EA-1310
@JsonPropertyOrder({"context", "id", "type", "motivation", "textGranularity", "resource", "on"})
public class AnnotationV2 extends JsonLdId implements Serializable, AnnotationWrapper {

    private static final long serialVersionUID = 7120324589144279826L;

    @JsonProperty("@context")
    // note that we only set context for a single annotation and not for an array of annotations part of an annotationpage
    private String[] context;

    @JsonProperty("@type")
    private String type = "oa:Annotation";

    private String           motivation;
    private String           textGranularity;
    private AnnotationBodyV2 resource;
    private String[]            on;

    public AnnotationV2(String id) {
        super(id);
    }

    public String[] getContext() {
        return context;
    }

    public void setContext(String[] context) {
        this.context = context;
    }

    public String getMotivation() {
        return motivation;
    }

    public String getTextGranularity() {
        return textGranularity;
    }

    public void setTextGranularity(String textGranularity) {
        this.textGranularity = textGranularity;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public AnnotationBodyV2 getResource() {
        return resource;
    }

    public void setResource(AnnotationBodyV2 resource) {
        this.resource = resource;
    }

    public String[] getOn() {
        return on;
    }

    public void setOn(String[] on) {
        this.on = on;
    }

    @JsonIgnore
    public boolean isMedia() {
        return (StringUtils.equalsAnyIgnoreCase(getTextGranularity(),
                                                AnnotationType.MEDIA.getDisplayName(),
                                                AnnotationType.CAPTION.getDisplayName()));
    }

    @JsonIgnore
    public AnnotationType getAnnotationType(){
        return AnnotationType.fromName(this.textGranularity);
    }
}

