package eu.europeana.fulltext.api.model.v2;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

/**
 * Created by luthien on 14/06/2018.
 */
@JsonPropertyOrder({"id"})
public class AnnotationBodyV2 extends JsonLdId implements Serializable{

    private static final long serialVersionUID = -814446825873060414L;

    public AnnotationBodyV2(String id) {
        super(id);
    }
}