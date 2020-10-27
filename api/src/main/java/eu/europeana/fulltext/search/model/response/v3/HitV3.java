package eu.europeana.fulltext.search.model.response.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.fulltext.search.model.response.Hit;
import eu.europeana.fulltext.search.model.response.HitSelector;

public class HitV3 extends Hit {

    private static final String TYPE = "Hit";
    private static final long serialVersionUID = 2790503975059781515L;

    @Override
    @JsonProperty("type")
    public String getType() {
        return TYPE;
    }

    protected HitSelector createSelector(String exact) {
        return new HitSelectorV3("", exact, "");
    }
}
