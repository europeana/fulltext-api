package eu.europeana.fulltext.search.model.response.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.fulltext.search.model.response.Hit;
import eu.europeana.fulltext.search.model.response.HitSelector;

public class HitV2 extends Hit {

    private static final String TYPE = "search:Hit";
    private static final long serialVersionUID = -5695711109165049290L;

    public HitV2(Integer startIndex, Integer endIndex, String exact) {
        super(startIndex, endIndex, exact);
    }

    @Override
    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }

    protected HitSelector createSelector(String exact){
        return new HitSelectorV2("", exact, "");
    }
}
