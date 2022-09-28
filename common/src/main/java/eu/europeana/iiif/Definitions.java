package eu.europeana.iiif;

/**
 * Common definitions used by IIIF Manifest and Fulltext API
 */
public class Definitions {

    public static final  String UTF_8                     = "charset=UTF-8";
    private static final String PROFILE_IS                = ";profile=http://\""; // always return with http, never https

    public static final  String MEDIA_TYPE_JSONLD         = "application/ld+json";
    public static final  String MEDIA_TYPE_JSON           = "application/json";

    /**
     * Media type for IIIF version 2
     */
    public static final String MEDIA_TYPE_IIIF_V2  = "iiif.io/api/presentation/2/context.json";
    /**
     * Media type for IIIF version 3
     */
    public static final String MEDIA_TYPE_IIIF_V3  = "iiif.io/api/presentation/3/context.json";

    /**
     * JSON Content-type returned on manifest requests for version 2
     */
    public static final String MEDIA_TYPE_IIIF_JSON_V2 = MEDIA_TYPE_JSON
                                                           + PROFILE_IS
                                                           + MEDIA_TYPE_IIIF_V2
                                                           + "\";" + UTF_8;
    /**
     * Default Content-type with JSON-LD returned on manifest requests for version 2
     */
    public static final String MEDIA_TYPE_IIIF_JSONLD_V2 = MEDIA_TYPE_JSONLD
                                                           + PROFILE_IS
                                                           + MEDIA_TYPE_IIIF_V2
                                                           + "\";" + UTF_8;

    /**
     * JSON Content-type returned on manifest requests for version 3
     */
    public static final String MEDIA_TYPE_IIIF_JSON_V3 = MEDIA_TYPE_JSON
                                                           + PROFILE_IS
                                                           + MEDIA_TYPE_IIIF_V3
                                                           + "\";" + UTF_8;
    /**
     * Default Content-type returned on manifest requests for version 3
     */
    public static final String MEDIA_TYPE_IIIF_JSONLD_V3 = MEDIA_TYPE_JSONLD
                                                           + PROFILE_IS
                                                           + MEDIA_TYPE_IIIF_V3
                                                           + "\";" + UTF_8;

}
