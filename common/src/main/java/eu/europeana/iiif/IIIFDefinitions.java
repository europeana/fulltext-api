package eu.europeana.iiif;

import static eu.europeana.iiif.AcceptUtils.*;

/**
 * Common definitions used by IIIF Manifest and Fulltext API
 */
public class IIIFDefinitions {

    public static final String IIIF_EUROPENA_BASE_URL = "https://iiif.europeana.eu";
    public static final String PRESENTATION_PATH = "/presentation";

    public static final String FULLTEXT_ANNOPAGE_PATH = "/annopage";
    public static final String FULLTEXT_SUMMARY_PATH = FULLTEXT_ANNOPAGE_PATH;
    public static final String FULLTEXT_SEARCH_PATH =  "/search";

    public static final String QUOTE                     = "\"";

    public static final String PROFILE_IS                = "profile=";

    /**
     * Media type used in @context tag of Fulltext Resource
     */
    public static final String MEDIA_TYPE_EDM_JSONLD  = "https://www.europeana.eu/schemas/context/edm.jsonld";

    /**
     * Media type used in @context tag of Fulltext Summary
     */
    public static final String MEDIA_TYPE_W3ORG_JSONLD  = "https://www.w3.org/ns/anno.jsonld";


    /**
     * Media type for IIIF version 2. Should always use http!
     */
    public static final String MEDIA_TYPE_IIIF_V2  = "http://iiif.io/api/presentation/2/context.json";
    /**
     * Media type for IIIF version 3 Should always use http!
     */
    public static final String MEDIA_TYPE_IIIF_V3  = "http://iiif.io/api/presentation/3/context.json";

    /**
     * JSON Content-type returned on manifest requests for version 2
     */
    public static final String MEDIA_TYPE_IIIF_JSON_V2 = MEDIA_TYPE_JSON + ACCEPT_DELIMITER
            + PROFILE_IS + QUOTE + MEDIA_TYPE_IIIF_V2 + QUOTE + ACCEPT_DELIMITER
            + CHARSET_UTF_8;
    /**
     * Default Content-type with JSON-LD returned on manifest requests for version 2
     */
    public static final String MEDIA_TYPE_IIIF_JSONLD_V2 = MEDIA_TYPE_JSONLD + ACCEPT_DELIMITER
            + PROFILE_IS + QUOTE + MEDIA_TYPE_IIIF_V2 + QUOTE + ACCEPT_DELIMITER
            + CHARSET_UTF_8;

    /**
     * JSON Content-type returned on manifest requests for version 3
     */
    public static final String MEDIA_TYPE_IIIF_JSON_V3 = MEDIA_TYPE_JSON + ACCEPT_DELIMITER
            + PROFILE_IS + QUOTE + MEDIA_TYPE_IIIF_V3 + QUOTE + ACCEPT_DELIMITER
            + CHARSET_UTF_8;
    /**
     * Default Content-type returned on manifest requests for version 3
     */
    public static final String MEDIA_TYPE_IIIF_JSONLD_V3 = MEDIA_TYPE_JSONLD + ACCEPT_DELIMITER
            + PROFILE_IS + QUOTE + MEDIA_TYPE_IIIF_V3 + QUOTE + ACCEPT_DELIMITER
            + CHARSET_UTF_8;

    private IIIFDefinitions() {
        // hide constructor
    }

}
