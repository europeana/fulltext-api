package eu.europeana.fulltext.search.model.response;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.search.model.response.v2.HitV2;
import eu.europeana.fulltext.search.model.response.v3.HitV3;

import static eu.europeana.fulltext.util.HttpUtils.REQUEST_VERSION_3;

public final class HitFactory {

    private HitFactory() {
        // avoid instantiation of this class
    }

    /**
     * Instantiates a Hit implementation based on the version provided
     *
     * @param start      start index of hit
     * @param end        end index of hit
     * @param annoPage   the annotation page object for this hit
     * @param annotation the annotation object for this hit
     * @param version    request version. "2" by default
     * @return Hit instance
     */
    public static Hit createHit(int start, int end, AnnoPage annoPage, Annotation annotation, String version) {
        if (REQUEST_VERSION_3.equals(version)) {
            return new HitV3().addAnnotation(start, end, annoPage, annotation);
        }
        return new HitV2().addAnnotation(start, end, annoPage, annotation);
    }


}
