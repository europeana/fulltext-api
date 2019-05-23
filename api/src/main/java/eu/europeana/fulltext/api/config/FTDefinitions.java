package eu.europeana.fulltext.api.config;

/**
 * Created by luthien on 14/06/2018.
 */
public class FTDefinitions {

    public static final String MEDIA_TYPE_JSONLD          = "application/ld+json";
    public static final String MEDIA_TYPE_JSON            = "application/json";
    public static final String V3_ANNO_PAGE_TYPE          = "AnnotationPage";
    public static final String V3_ANNOTATION_TYPE         = "Annotation";
    public static final String V3_ANNO_BODY_TYPE          = "SpecificResource";
    public static final String EDM_FULLTESTRESOURCE_TYPE  = "FullTextResource";

    /**
     * Media type for IIIF version 2
     */
    public static final String MEDIA_TYPE_IIIF_V2  = "http://iiif.io/api/presentation/2/context.json";

    /**
     * Media type for IIIF version 3
     */
    public static final String MEDIA_TYPE_IIIF_V3  = "http://iiif.io/api/presentation/3/context.json";


    /**
     * Media type used in @context tag of Fulltext Resource
     */
    public static final String MEDIA_TYPE_EDM_JSONLD  = "https://www.europeana.eu/schemas/context/edm.jsonld";

    /**
     * Default Content-type returned on manifest requests for version 2
     */
    public static final String MEDIA_TYPE_IIIF_JSONLD_V2 = MEDIA_TYPE_JSONLD
                                                           + ";profile=\""
                                                           + MEDIA_TYPE_IIIF_V2
                                                           + "\""
                                                           + ";charset=UTF-8";

    /**
     * Default Content-type returned on manifest requests for version 3
     */
    public static final String MEDIA_TYPE_IIIF_JSONLD_V3 = MEDIA_TYPE_JSONLD
                                                           + ";profile=\""
                                                           + MEDIA_TYPE_IIIF_V3
                                                           + "\""
                                                           + ";charset=UTF-8";

    /**
     * JSON Content-type returned on manifest requests for version 2
     */
    public static final String MEDIA_TYPE_IIIF_JSON_V2 = MEDIA_TYPE_JSON
                                                           + ";profile=\""
                                                           + MEDIA_TYPE_IIIF_V2
                                                           + "\""
                                                           + ";charset=UTF-8";

    /**
     * JSON Content-type returned on manifest requests for version 3
     */
    public static final String MEDIA_TYPE_IIIF_JSON_V3 = MEDIA_TYPE_JSON
                                                           + ";profile=\""
                                                           + MEDIA_TYPE_IIIF_V3
                                                           + "\""
                                                           + ";charset=UTF-8";

}
