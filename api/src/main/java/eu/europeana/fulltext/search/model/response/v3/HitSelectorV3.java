package eu.europeana.fulltext.search.model.response.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.fulltext.search.model.response.HitSelector;

public class HitSelectorV3 extends HitSelector {
    private static final String TYPE = "TextQuoteSelector";
    private static final long serialVersionUID = 6723803512710231616L;

    /**
     * Create a new HitSelector.
     *
     * @param prefix can be null, but this is stored as empty String
     * @param exact  cannot be null
     * @param suffix can be null, but this is stored as empty String
     */
    public HitSelectorV3(String prefix, String exact, String suffix) {
        super(prefix, exact, suffix);
    }


    @Override
    @JsonProperty("type")
    public String getType() {
        return TYPE;
    }
}
