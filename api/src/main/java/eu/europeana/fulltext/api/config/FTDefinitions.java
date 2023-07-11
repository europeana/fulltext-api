package eu.europeana.fulltext.api.config;

import eu.europeana.fulltext.AnnotationType;
import java.util.EnumSet;
import java.util.Set;

/**
 * Fulltext specific definitions. For common definitions shared between IIIF Manifest and
 * Fulltext @see eu.europeana.iiif.IIIFDefinitions class
 *
 * Created by luthien on 14/06/2018.
 */
public final class FTDefinitions {

    public static final  String V3_ANNO_PAGE_TYPE         = "AnnotationPage";
    public static final  String V3_ANNOTATION_TYPE        = "Annotation";
    public static final  String V3_ANNO_BODY_TYPE         = "SpecificResource";
    public static final  String EDM_FULLTEXTRESOURCE_TYPE = "FullTextResource";
    public static final  String INFO_CANVAS_TYPE          = "Canvas";
    public static final  String INFO_ANNOPAGE_TYPE        = "AnnotationPage";

    public static final String ANNOTATION_PATH           = "/";
    public static final String CANVAS_PATH               = "/canvas";
    public static final String LANGUAGE_PARAM            = "lang=";

    public static final Set<AnnotationType> TEXT_ANNOTATION_TYPES = EnumSet.of(
        AnnotationType.BLOCK, AnnotationType.LINE, AnnotationType.WORD, AnnotationType.PAGE);

    public static final Set<AnnotationType> MEDIA_ANNOTATION_TYPES = EnumSet.of(
        AnnotationType.MEDIA, AnnotationType.CAPTION);

    private FTDefinitions(){
        // empty constructor to prevent initialization
    }

}
