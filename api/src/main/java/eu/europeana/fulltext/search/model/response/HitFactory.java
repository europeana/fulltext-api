package eu.europeana.fulltext.search.model.response;

import eu.europeana.fulltext.search.model.response.v2.HitV2;
import eu.europeana.fulltext.search.model.response.v3.HitV3;

import static eu.europeana.fulltext.RequestUtils.REQUEST_VERSION_3;

public class HitFactory {

    /**
     * Instantiates a Hit implementation based on the version provided
     *
     * @param startIndex start index of hit
     * @param endIndex   end index of hit
     * @param exact      exact string contained in hit
     * @param version    request version. "2" by default
     * @return Hit instance
     */
    public static Hit createHit(Integer startIndex, Integer endIndex, String exact, String version) {
        if (REQUEST_VERSION_3.equals(version)) {
            return new HitV3(startIndex, endIndex, exact);
        }
        return new HitV2(startIndex, endIndex, exact);
    }
}
