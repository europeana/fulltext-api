package eu.europeana.fulltext.search.model.response.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.fulltext.search.model.response.HitSelector;

public class HitSelectorV2 extends HitSelector {

    private static final String TYPE = "oa:TextQuoteSelector";
    private static final long serialVersionUID = 5601968867137790383L;

    /**
     * Create a new HitSelector.
     *
     * @param prefix can be null, but this is stored as empty String
     * @param exact  cannot be null
     * @param suffix can be null, but this is stored as empty String
     */
    public HitSelectorV2(String prefix, String exact, String suffix) {
        super(prefix, exact, suffix);
    }

    @Override
    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }
}
