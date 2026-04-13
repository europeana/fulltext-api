package eu.europeana.fulltext.api.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Created by luthien on 14/06/2018.
 */

@JsonPropertyOrder({"id","type"})
public class AnnotationFullBodyV2 extends AnnotationBodyV2{

    private static final String TYPE = "oa:SpecificResource";

    private static final long serialVersionUID = 3894020068338298481L;

    public AnnotationFullBodyV2(String id) {
        super(id);
    }

    @Override
    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }

}
