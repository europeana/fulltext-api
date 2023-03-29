package eu.europeana.fulltext.api.model.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.model.AnnotationWrapper;
import eu.europeana.fulltext.api.model.JsonLdIdType;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;

import static eu.europeana.fulltext.api.config.FTDefinitions.V3_ANNOTATION_TYPE;

/**
 * Created by luthien target 14/06/2018.
 */
@JsonPropertyOrder({"context", "id", "type", "motivation", "textGranularity", "body", "target"})
public class AnnotationV3 extends JsonLdIdType implements Serializable, AnnotationWrapper {

    private static final long serialVersionUID = 8849251970656404497L;

    @JsonProperty("@context")
    // note that we only set context for a single annotation and not for an array of annotations part of an annotationpage
    private String[] context;
    private String           motivation;
    private String           textGranularity;
    private AnnotationBodyV3 body;
    private String[]         target;

    public AnnotationV3(String id) {
        super(id, V3_ANNOTATION_TYPE);
    }

    @Override
    public String[] getContext() {
        return context;
    }

    @Override
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

    public AnnotationBodyV3 getBody() {
        return body;
    }

    public void setBody(AnnotationBodyV3 body) {
        this.body = body;
    }

    public String[] getTarget() {
        return target;
    }

    public void setTarget(String[] target) {
        this.target = target;
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

