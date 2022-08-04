package eu.europeana.fulltext.indexing.testutils;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntegrationTestUtils {

    public static final String EN = "en";
    public static final String RIGHTS = "http://creativecommons.org/publicdomain/zero/1.0/";
    public static final String ANN_1= "ann1";
    public static final String ANN_2= "ann2";
    public static final String RESOURCE_ID_1= "res1";
    public static final String RESOURCE_ID_2= "res2";
    public static final String RESOURCE_ID_3= "res3";

    public static final String TRANSCRIPTION_DSID = "125";
    public static final String TRANSCRIPTION_LCID = "contributions_88b844a0";
    public static final String TRANSCRIPTION_PGID = "15643";
    public static final String TRANSCRIPTION_TGID = "http://contribute.europeana.eu/media/fa675800-9793-0138-75ad-6eee0af68290?page=15";
    public static final String TRANSCRIPTION_SOURCE = "http://annotation-acceptance.europeana.eu/annotation/50716";

    public static final String SUBTITLE_DSID = "8604";
    public static final String SUBTITLE_LCID = "EAAE870171E24F05A64CE364D750631A";
    public static final String SUBTITLE_PGID = "6c60c01";
    public static final String SUBTITLE_TGID = "https://www.filmportal.de/sites/default/files/video/Salem06_x264.mp4";
    public static final String SUBTITLE_SOURCE = "http://annotation-acceptance.europeana.eu/annotation/53675";


    public static AnnoPage createTranscriptionAnnoPage() {
        Resource resource = new Resource(RESOURCE_ID_1, EN,  "transcription value", RIGHTS, TRANSCRIPTION_DSID, TRANSCRIPTION_LCID, "");
        AnnoPage annoPage = new AnnoPage(TRANSCRIPTION_DSID, TRANSCRIPTION_LCID, TRANSCRIPTION_PGID, TRANSCRIPTION_TGID, EN, resource);
        annoPage.setSource(TRANSCRIPTION_SOURCE);
        Annotation annotation = new Annotation();
        annotation.setAnId(ANN_1);
        annotation.setDcType(AnnotationType.PAGE.getAbbreviation());
        annoPage.setAns(Collections.singletonList(annotation));
        return annoPage;
    }

    public static AnnoPage createSubtitleAnnoPage() {
        Resource resource = new Resource(RESOURCE_ID_2, EN,  "subtitle value", RIGHTS, SUBTITLE_DSID, SUBTITLE_LCID, "");
        AnnoPage annoPage = new AnnoPage(SUBTITLE_DSID, SUBTITLE_LCID, SUBTITLE_PGID, SUBTITLE_TGID, EN, resource);
        annoPage.setSource(SUBTITLE_SOURCE);
        List<Annotation> annotations = new ArrayList<>();

        Annotation annotation = new Annotation();
        annotation.setAnId(ANN_1);
        annotation.setDcType(AnnotationType.MEDIA.getAbbreviation());
        annotations.add(annotation);

        annotation = new Annotation(ANN_2, AnnotationType.CAPTION.getAbbreviation(), 0, 20);
        annotation.setTgs(Collections.singletonList(new Target(6000, 8200)));
        annotations.add(annotation);

        annoPage.setAns(annotations);
        return annoPage;
    }
}
