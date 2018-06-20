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
import eu.europeana.fulltext.entity.FTPage;
import eu.europeana.fulltext.model.v2.AnnotationBodyV2;
import eu.europeana.fulltext.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.model.v2.AnnotationV2;
import eu.europeana.fulltext.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.model.v3.AnnotationV3;
import eu.europeana.fulltext.model.v3.AnnotationBodyV3;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * This class contains the methods for mapping FTAnnotation / FTPage Mongo bean objects to IIIF v2 / v3 fulltext
 * resource objects
 *
 * Created by luthien on 18/06/2018.
 */
public class EDM2IIIFMapping {

    private static final Logger LOG = LogManager.getLogger(EDM2IIIFMapping.class);

    private EDM2IIIFMapping() {
        // private constructor to prevent initialization
    }

    public static AnnotationPageV2 getAnnotationPageV2(FTPage ftPage){
        AnnotationPageV2 annPage = new AnnotationPageV2(ftPage.getId());
        annPage.setItems(getAnnotationV2Array(ftPage));
        return annPage;
    }

    public static AnnotationV2 getAnnotationV2(FTAnnotation ftAnno, FTPage ftPpage){
        String       resource  = ftPpage.getSourceUrl() + "#char=" + ftAnno.getTextStart() + "," + ftAnno.getTextEnd();
        AnnotationV2 ann       = new AnnotationV2(ftAnno.getId());
        ann.setMotivation(ftAnno.getMotivation());
        ann.setDcType(ftAnno.getDcType());
        if (StringUtils.isNotBlank(ftAnno.getLanguage())){
            AnnotationBodyV2 anb = new AnnotationBodyV2(resource);
            anb.setFull(ftPpage.getSourceUrl());
            anb.setLanguage(ftAnno.getLanguage());
            ann.setAnnotationBodyV2(anb);
        } else {
            ann.setResource(resource);
        }
        ann.setOn(ftPpage.getTargetUrl()
                  + "#xywh="    + ftAnno.getTargetX()
                  + ","         + ftAnno.getTargetY()
                  + ","         + ftAnno.getTargetW()
                  + ","         + ftAnno.getTargetH());
        return ann;
    }

    public static AnnotationPageV3 getAnnotationPageV3(FTPage ftPage){
        AnnotationPageV3 annPage = new AnnotationPageV3(ftPage.getId());
        annPage.setItems(getAnnotationV3Array(ftPage));
        return annPage;
    }

    public static AnnotationV3 getAnnotationV3(FTAnnotation ftAnno, FTPage ftPpage){
        String       body = ftPpage.getSourceUrl() + "#char=" + ftAnno.getTextStart() + "," + ftAnno.getTextEnd();
        AnnotationV3 ann  = new AnnotationV3(ftAnno.getId());
        ann.setMotivation(ftAnno.getMotivation());
        ann.setDcType(ftAnno.getDcType());
        if (StringUtils.isNotBlank(ftAnno.getLanguage())){
            AnnotationBodyV3 anb = new AnnotationBodyV3(body);
            anb.setSource(ftPpage.getSourceUrl());
            anb.setLanguage(ftAnno.getLanguage());
            ann.setAnnotationBodyV3(anb);
        } else {
            ann.setBody(body);
        }
        ann.setTarget(ftPpage.getTargetUrl()
                      + "#xywh="    + ftAnno.getTargetX()
                      + ","         + ftAnno.getTargetY()
                      + ","         + ftAnno.getTargetW()
                      + ","         + ftAnno.getTargetH());
        return ann;
    }

    private static AnnotationV2[] getAnnotationV2Array(FTPage ftPage){
        ArrayList<AnnotationV2> annoArrayList = new ArrayList<>();
        for (FTAnnotation ftAnno : ftPage.getFTAnnotations()){
            annoArrayList.add(getAnnotationV2(ftAnno, ftPage));
        }
        return annoArrayList.toArray(new AnnotationV2[0]);
    }

    private static AnnotationV3[] getAnnotationV3Array(FTPage ftPage){
        ArrayList<AnnotationV3> annoArrayList = new ArrayList<>();
        for (FTAnnotation ftAnno : ftPage.getFTAnnotations()){
            annoArrayList.add(getAnnotationV3(ftAnno, ftPage));
        }
        return annoArrayList.toArray(new AnnotationV3[0]);
    }

}
