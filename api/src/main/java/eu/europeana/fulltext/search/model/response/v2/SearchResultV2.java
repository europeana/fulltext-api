package eu.europeana.fulltext.search.model.response.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.service.EDM2IIIFMapping;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.search.model.response.Debug;
import eu.europeana.fulltext.search.model.response.Hit;
import eu.europeana.fulltext.search.model.response.SearchResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({"id", "type", "debug", "resources", "hits"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultV2 implements Serializable, SearchResult {

    private static final long serialVersionUID = 5755904077393708504L;

    private static final String TYPE = "sc:AnnotationList";

    private final String id;
    private Debug debug;
    private final List<AnnotationV2> items = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();


    public SearchResultV2(String searchId, boolean debug) {
        this.id = searchId;
        if (debug) {
            this.debug = new Debug();
        }
    }

    @Override
    @JsonProperty("@id")
    public String getId() {
        return id;
    }

    @Override
    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }

    @Override
    public Debug getDebug() {
        return debug;
    }

    @Override
    public int itemSize() {
        return items.size();
    }

    @JsonProperty("resources")
    public List<AnnotationV2> getItems(){
        return items;
    }

    @Override
    public void addAnnotationHit(AnnoPage annoPage, Annotation annotation, Hit hit) {
        AnnotationV2 annoV2 = EDM2IIIFMapping.getAnnotationV2(annoPage, annotation, false, false);
        items.add(annoV2);

        if (hit != null) {
            hits.add(hit);
        }
    }

    @Override
    public List<Hit> getHits() {
        return hits;
    }
}
