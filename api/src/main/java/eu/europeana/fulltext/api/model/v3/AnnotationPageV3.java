package eu.europeana.fulltext.api.model.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_EDM_JSONLD;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_IIIF_V3;
import static eu.europeana.fulltext.api.config.FTDefinitions.V3_ANNO_PAGE_TYPE;

import eu.europeana.fulltext.api.model.AnnotationWrapper;
import eu.europeana.fulltext.api.model.JsonLdIdType;

/**
 * Created by luthien on 14/06/2018.
 */

@JsonPropertyOrder({"context", "id", "type"})
public class AnnotationPageV3 extends JsonLdIdType implements Serializable, AnnotationWrapper {

    private static final long serialVersionUID = 3567695991809278386L;

    @JsonProperty("@context")
    private String[] context = new String[]{MEDIA_TYPE_IIIF_V3, MEDIA_TYPE_EDM_JSONLD};
    private AnnotationV3[] items;

    public AnnotationPageV3(String id) {
        super(id, V3_ANNO_PAGE_TYPE);
    }

    public AnnotationV3[] getItems() {
        return items;
    }

    public void setItems(AnnotationV3[] items) {
        this.items = items;
    }

    @Override
    public String[] getContext() {
        return context;
    }

    @Override
    public void setContext(String[] context) {
        this.context = context;
    }
}
