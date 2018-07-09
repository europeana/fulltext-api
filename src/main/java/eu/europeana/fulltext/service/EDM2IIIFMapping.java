/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.service;

import eu.europeana.fulltext.entity.FTAnnotation;
import eu.europeana.fulltext.entity.FTAnnoPage;
import eu.europeana.fulltext.entity.FTTarget;
import eu.europeana.fulltext.model.v2.*;
import eu.europeana.fulltext.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.model.v3.AnnotationV3;
import eu.europeana.fulltext.model.v3.AnnotationBodyV3;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Optional;

import static eu.europeana.fulltext.config.FTDefinitions.*;

/**
 * This class contains the methods for mapping FTAnnotation / FTAnnoPage Mongo bean objects to IIIF v2 / v3 fulltext
 * resource objects.
 *
 * NOTE while a given value for the 'motivation' field in the Annotation class will be stored as such in Mongo, it is
 * for now not displayed in the output. Instead, the values as specified in the EDM 2 IIIF mapping document are used;
 * they are for now hard-coded in this class for both the V2 and V3 version of the output JSON.
 *
 * Created by luthien on 18/06/2018.
 */
public class EDM2IIIFMapping {

    private static final String V2_MOTIVATION = "oa:Annotation";
    private static final String V3_MOTIVATION = "transcribing";

    private static final Logger LOG = LogManager.getLogger(EDM2IIIFMapping.class);

    private EDM2IIIFMapping() {
        // private constructor to prevent initialization
    }

    public static AnnotationPageV2 getAnnotationPageV2(FTAnnoPage ftAnnoPage){
        AnnotationPageV2 annPage = new AnnotationPageV2(getAnnoPageIdUrl(ftAnnoPage));
        annPage.setResources(getAnnotationV2Array(ftAnnoPage));
        return annPage;
    }

    private static AnnotationV2[] getAnnotationV2Array(FTAnnoPage ftAnnoPage){
        ArrayList<AnnotationV2> annoArrayList = new ArrayList<>();
        for (FTAnnotation ftAnno : ftAnnoPage.getAns()){
            annoArrayList.add(getAnnotationV2(ftAnnoPage, ftAnno, false));
        }
        return annoArrayList.toArray(new AnnotationV2[0]);
    }

    public static AnnotationV2 getAnnotationV2(FTAnnoPage ftAnnoPage, FTAnnotation ftAnno, boolean addContext){
        String       resourceIdUrl  = getResourceIdUrl(ftAnnoPage, ftAnno);
        AnnotationV2 ann            = new AnnotationV2(getAnnotationIdUrl(ftAnnoPage, ftAnno));
        if (addContext){
            ann.setContext(MEDIA_TYPE_IIIF_V2);
        }
        ann.setMotivation(StringUtils.isNotBlank(ftAnno.getMotiv()) ? ftAnno.getMotiv() : V2_MOTIVATION);
        ann.setDcType(ftAnno.getDcType());
        ann.setOn(getFTTargetArray(ftAnnoPage, ftAnno));
        if (StringUtils.isNotBlank(ftAnno.getLang())){
            AnnotationFullBodyV2 anb = new AnnotationFullBodyV2(resourceIdUrl);
            anb.setFull(getResourceIdBaseUrl(ftAnnoPage, ftAnno));
            anb.setLanguage(ftAnno.getLang());
            ann.setResource(anb);
        } else {
            AnnotationBodyV2 anb = new AnnotationBodyV2(resourceIdUrl);
            ann.setResource(anb);
        }
        return ann;
    }

    public static AnnotationPageV3 getAnnotationPageV3(FTAnnoPage ftAnnoPage){
        AnnotationPageV3 annPage = new AnnotationPageV3(getAnnoPageIdUrl(ftAnnoPage));
        annPage.setItems(getAnnotationV3Array(ftAnnoPage));
        return annPage;
    }

    private static AnnotationV3[] getAnnotationV3Array(FTAnnoPage ftAnnoPage){
        ArrayList<AnnotationV3> annoArrayList = new ArrayList<>();
        for (FTAnnotation ftAnno : ftAnnoPage.getAns()){
            annoArrayList.add(getAnnotationV3(ftAnnoPage, ftAnno, false));
        }
        return annoArrayList.toArray(new AnnotationV3[0]);
    }

    public static AnnotationV3 getAnnotationV3(FTAnnoPage ftAnnoPage, FTAnnotation ftAnno, boolean addContext){
        String       body = getResourceIdUrl(ftAnnoPage, ftAnno);
        AnnotationV3 ann  = new AnnotationV3(getAnnotationIdUrl(ftAnnoPage, ftAnno));
        AnnotationBodyV3 anb;
        if (addContext){
            ann.setContext(MEDIA_TYPE_IIIF_V3);
        }
        ann.setMotivation(StringUtils.isNotBlank(ftAnno.getMotiv()) ? ftAnno.getMotiv() : V3_MOTIVATION);
        ann.setDcType(ftAnno.getDcType());
        if (StringUtils.isNotBlank(ftAnno.getLang())){
            anb = new AnnotationBodyV3(body, V3_ANNO_BODY_TYPE);
            anb.setSource(getResourceIdBaseUrl(ftAnnoPage, ftAnno));
            anb.setLanguage(ftAnno.getLang());
        } else {
            anb = new AnnotationBodyV3(body);
        }
        ann.setBody(anb);
        ann.setTarget(getFTTargetArray(ftAnnoPage, ftAnno));
        return ann;
    }

    public static AnnotationV3 getSingleAnnotationV3(FTAnnoPage ftAnnoPage, String annoId){
        FTAnnotation ftAnno;
        Optional<FTAnnotation> maybe = ftAnnoPage.getAns().stream().filter(o -> o.getAnId().equals(annoId)).findFirst();
        if (maybe.isPresent()){
            return getAnnotationV3(ftAnnoPage, maybe.get(), true);
        } else {
            return null; // TODO handle this better
        }
    }

    public static AnnotationV2 getSingleAnnotationV2(FTAnnoPage ftAnnoPage, String annoId){
        FTAnnotation ftAnno;
        Optional<FTAnnotation> maybe = ftAnnoPage.getAns().stream().filter(o -> o.getAnId().equals(annoId)).findFirst();
        if (maybe.isPresent()){
            return getAnnotationV2(ftAnnoPage, maybe.get(), true);
        } else {
            return null; // TODO handle this better
        }
    }

    private static String[] getFTTargetArray(FTAnnoPage ftAnnoPage, FTAnnotation ftAnno){
        ArrayList<String> ftTargetURLList = new ArrayList<>();
        for (FTTarget ftTarget : ftAnno.getTgs()){
            ftTargetURLList.add(getTargetIdBaseUrl(ftAnnoPage, ftAnno) + "#xywh="
                                + ftTarget.getX() + ","
                                + ftTarget.getY() + ","
                                + ftTarget.getW() + ","
                                + ftTarget.getH());
        }
        return ftTargetURLList.toArray(new String[0]);
    }

    private static String getResourceIdUrl(FTAnnoPage ftAnnoPage, FTAnnotation ftAnno){
        return getResourceIdBaseUrl(ftAnnoPage, ftAnno) + "#char=" + ftAnno.getFrom() + "," + ftAnno.getTo();
    }

    private static String getResourceIdBaseUrl(FTAnnoPage ftAnnoPage, FTAnnotation ftAnno){
        if (StringUtils.isNotBlank(ftAnno.getAnResUrl())){
            return ftAnno.getAnResUrl();
        } else {
            return RESOURCE_BASE_URL + ftAnnoPage.getDsId() + "/" + ftAnnoPage.getLcId() + "/" + ftAnnoPage.getResId();
        }
    }

    private static String getAnnoPageIdUrl(FTAnnoPage ftAnnoPage){
        return IIIF_API_BASE_URL + ftAnnoPage.getDsId() + "/" +
               ftAnnoPage.getLcId() + ANNOPAGE_DIR + ftAnnoPage.getPgId();
    }

    private static String getAnnotationIdUrl(FTAnnoPage ftAnnoPage, FTAnnotation ftAnno){
        String datasetId = StringUtils.isNotBlank(ftAnno.getAnDsId()) ? ftAnno.getAnDsId() : ftAnnoPage.getDsId();
        String localId = StringUtils.isNotBlank(ftAnno.getAnLcId()) ? ftAnno.getAnLcId() : ftAnnoPage.getLcId();
        return IIIF_API_BASE_URL + datasetId + "/" + localId + ANNOTATION_DIR + ftAnno.getAnId();
    }

    private static String getTargetIdBaseUrl(FTAnnoPage ftAnnoPage, FTAnnotation ftAnno){
        if (StringUtils.isNotBlank(ftAnno.getAnTgUrl())){
            return ftAnno.getAnTgUrl();
        } else {
            return IIIF_API_BASE_URL + ftAnnoPage.getDsId() + "/" + ftAnnoPage.getLcId() + TARGET_DIR + ftAnnoPage.getTgtId();
        }
    }
}
