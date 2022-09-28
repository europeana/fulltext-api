package eu.europeana.fulltext.api.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.fulltext.api.model.AnnotationWrapper;

import java.io.Serializable;

import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_EDM_JSONLD;
import static eu.europeana.iiif.Definitions.MEDIA_TYPE_IIIF_V2;

/**
 * Created by luthien on 14/06/2018.
 */
//@JsonldType(value = "sc:AnnotationList") // commenting this out works for property ordering #EA-1310
@JsonPropertyOrder({"context", "id", "type"})
public class AnnotationPageV2 extends JsonLdId implements Serializable, AnnotationWrapper {

    private static final long serialVersionUID = -491589144458820254L;

    @JsonProperty("@context")
    private String[] context = new String[]{MEDIA_TYPE_IIIF_V2, MEDIA_TYPE_EDM_JSONLD};
    @JsonProperty("@type")
    private String type = "sc:AnnotationList";
    private String language;
    private String source;
    private AnnotationV2[] resources;

    public AnnotationPageV2(String id) {
        super(id);
    }

    @Override
    public String[] getContext() {
        return context;
    }

    @Override
    public void setContext(String[] context) {
        this.context = context;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language){
        this.language = language;
    }

    public AnnotationV2[] getResources() {
        return resources;
    }

    public void setResources(AnnotationV2[] resources) {
        this.resources = resources;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
