package eu.europeana.fulltext.api.service;

import eu.europeana.fulltext.api.config.FTDefinitions;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.model.FTResource;
import eu.europeana.fulltext.api.model.v2.AnnotationBodyV2;
import eu.europeana.fulltext.api.model.v2.AnnotationFullBodyV2;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationBodyV3;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.exception.ResourceDoesNotExistException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

import static eu.europeana.fulltext.api.config.FTDefinitions.*;
import static eu.europeana.fulltext.util.NormalPlayTime.msToHHmmss;

/**
 * This class contains the methods for mapping Annotation / AnnoPage Mongo bean objects to IIIF v2 / v3 fulltext
 * resource objects.
 *
 * NOTE while a given value for the 'motivation' field in the Annotation class will be stored as such in Mongo, it is
 * for now not displayed in the output. Instead, the values as specified in the EDM 2 IIIF mapping document are used;
 * they are for now hard-coded in this class for both the V2 and V3 version of the output JSON.
 *
 * Created by luthien on 18/06/2018.
 */
@Component
public final class EDM2IIIFMapping {

    private static FTSettings fts;
    private static FTService  ftService;

    private static final String V2_MOTIVATION = "sc:painting";
    private static final String V3_MOTIVATION = "transcribing";

    private static final Logger LOG           = LogManager.getLogger(EDM2IIIFMapping.class);

    @Autowired
    private EDM2IIIFMapping(FTSettings fts, FTService ftService) {
        EDM2IIIFMapping.fts = fts;
        EDM2IIIFMapping.ftService = ftService;
    }

    static AnnotationPageV2 getAnnotationPageV2(AnnoPage annoPage, boolean derefResource){
        AnnotationPageV2 annPage = new AnnotationPageV2(getAnnoPageIdUrl(annoPage));
        annPage.setLanguage(annoPage.getLang());
        annPage.setSource(annoPage.getSource());
        annPage.setResources(getAnnotationV2Array(annoPage, derefResource));
        return annPage;
    }

    private static AnnotationV2[] getAnnotationV2Array(AnnoPage annoPage, boolean derefResource){
        ArrayList<AnnotationV2> annoArrayList = new ArrayList<>();
        for (Annotation ftAnno : annoPage.getAns()){
            addAnnotationV2(annoPage, ftAnno, derefResource, annoArrayList);
        }
        return annoArrayList.toArray(new AnnotationV2[0]);
    }

    private static void addAnnotationV2(AnnoPage annoPage, Annotation ftAnno, boolean derefResource, ArrayList<AnnotationV2> annoArrayList) {
        // make sure page annotations are listed first.
        if (ftAnno.isTopLevel()) {
            annoArrayList.add(0, getAnnotationV2(annoPage, ftAnno, false, derefResource ));
        } else {
            annoArrayList.add(getAnnotationV2(annoPage, ftAnno, false, false));
        }
    }

    public static AnnotationV2 getAnnotationV2(AnnoPage annoPage, Annotation annotation, boolean includeContext, boolean derefResource){
        String       resourceIdUrl  = getResourceIdUrl(annoPage, annotation);
        AnnotationV2 ann            = new AnnotationV2(getAnnotationIdUrl(annoPage, annotation));
        if (includeContext){
            ann.setContext(new String[]{MEDIA_TYPE_IIIF_V2, MEDIA_TYPE_EDM_JSONLD});
        }
        ann.setMotivation(StringUtils.isNotBlank(annotation.getMotiv()) ? annotation.getMotiv() : V2_MOTIVATION);
        ann.setDcType(expandDCType(annotation.getDcType()));
        ann.setOn(getFTTargetArray(annoPage, annotation));
        AnnotationBodyV2 anb;

        if (StringUtils.isNotBlank(annotation.getLang())){
            anb = new AnnotationFullBodyV2(resourceIdUrl);
            anb.setFull(getResourceIdBaseUrl(annoPage));
            anb.setLanguage(annotation.getLang());
        } else {
            anb = new AnnotationBodyV2(resourceIdUrl);
            // dereference Resource: because dereferenced annotations ONLY occur in top-level annotations
            // *AND* top-level annotations in practice never have a language set, this should be OK
            if (derefResource) {
                FTResource ftResource = fetchFTResource(annoPage);
                if (ftResource != null) {
                    anb.setType(ftResource.getType());
                    anb.setLanguage(ftResource.getLanguage());
                    anb.setValue(ftResource.getValue());
                    anb.setSource(ftResource.getSource());
                    anb.setRights(ftResource.getRights());
                }
            }
        }
        ann.setResource(anb);
        return ann;
    }

    static AnnotationPageV3 getAnnotationPageV3(AnnoPage annoPage, boolean derefResource){
        AnnotationPageV3 annPage = new AnnotationPageV3(getAnnoPageIdUrl(annoPage));
        annPage.setLanguage(annoPage.getLang());
        annPage.setSource(annoPage.getSource());
        annPage.setItems(getAnnotationV3Array(annoPage, derefResource));
        return annPage;
    }

    private static AnnotationV3[] getAnnotationV3Array(AnnoPage annoPage, boolean derefResource){
        ArrayList<AnnotationV3> annoArrayList = new ArrayList<>();
        for (Annotation ftAnno : annoPage.getAns()){
            addAnnotationV3(annoPage, ftAnno, derefResource, annoArrayList);
        }
        return annoArrayList.toArray(new AnnotationV3[0]);
    }

    private static void addAnnotationV3(AnnoPage annoPage, Annotation ftAnno, boolean derefResource, ArrayList<AnnotationV3> annoArrayList) {
        // make sure page annotations are listed first.
        if (ftAnno.isTopLevel()) {
            annoArrayList.add(0, getAnnotationV3(annoPage, ftAnno, false, derefResource));

        } else {
            annoArrayList.add(getAnnotationV3(annoPage, ftAnno, false, false));
        }
    }

    public static AnnotationV3 getAnnotationV3(AnnoPage annoPage, Annotation annotation, boolean includeContext, boolean derefResource){
        String       body = getResourceIdUrl(annoPage, annotation);
        AnnotationV3 ann  = new AnnotationV3(getAnnotationIdUrl(annoPage, annotation));
        AnnotationBodyV3 anb;
        if (includeContext) {
            ann.setContext(new String[]{MEDIA_TYPE_IIIF_V3, MEDIA_TYPE_EDM_JSONLD});
        }

        ann.setMotivation(StringUtils.isNotBlank(annotation.getMotiv()) ? annotation.getMotiv() : V3_MOTIVATION);
        ann.setDcType(expandDCType(annotation.getDcType()));
        if (StringUtils.isNotBlank(annotation.getLang())){
            anb = new AnnotationBodyV3(body, V3_ANNO_BODY_TYPE);
            anb.setSource(getResourceIdBaseUrl(annoPage));
            anb.setLanguage(annotation.getLang());
        } else {
            anb = new AnnotationBodyV3(body);
            // dereference Resource: because dereferenced annotations ONLY occur in top-level annotations
            // *AND* top-level annotations in practice never have a language set, this should be OK
              if (derefResource) {
                  FTResource ftResource = fetchFTResource(annoPage);
                  if (ftResource != null) {
                    anb.setType(ftResource.getType());
                    anb.setLanguage(ftResource.getLanguage());
                    anb.setValue(ftResource.getValue());
                    anb.setSource(ftResource.getSource());
                    anb.setRights(ftResource.getRights());
                }
            }
        }

        ann.setBody(anb);
        ann.setTarget(getFTTargetArray(annoPage, annotation));
        return ann;
    }

    static AnnotationV3 getSingleAnnotationV3(AnnoPage annoPage, String annoId){
        Optional<Annotation> maybe = annoPage.getAns().stream().filter(o -> o.getAnId().equals(annoId)).findFirst();
        // NOTE this shouldn't fail because in that case the annoPage would not have been found in the first place
        return maybe.map(annotation1 -> getAnnotationV3(annoPage, annotation1, true, false)).orElse(null);
    }

    static AnnotationV2 getSingleAnnotationV2(AnnoPage annoPage, String annoId){
        Optional<Annotation> maybe = annoPage.getAns().stream().filter(o -> o.getAnId().equals(annoId)).findFirst();
        // NOTE this shouldn't fail because in that case the annoPage would not have been found in the first place
        return maybe.map(annotation1 -> getAnnotationV2(annoPage, annotation1, true,false)).orElse(null);
    }

    private static String[] getFTTargetArray(AnnoPage annoPage, Annotation annotation){
        ArrayList<String> ftTargetURLList = new ArrayList<>();
        if (annotation.getTgs() != null) {
            // generate target if it exists
            for (Target target : annotation.getTgs()) {
                ftTargetURLList.add(annoPage.getTgtId() + generateTargetCoordinates(target, annotation.isMedia()));
            }
            return ftTargetURLList.toArray(new String[0]);
        } else if (annotation.isTopLevel()) {
            ftTargetURLList.add(annoPage.getTgtId());
            return ftTargetURLList.toArray(new String[0]);
        }
        return new String[0];
    }

    private static String generateTargetCoordinates(Target target, boolean isMedia){
        if (isMedia) {
            return "#t=" +
                   msToHHmmss(target.getStart().longValue()) + "," +
                   msToHHmmss(target.getEnd().longValue());

        } else {
            return "#xywh=" +
                   target.getX() + "," +
                   target.getY() + "," +
                   target.getW() + "," +
                   target.getH();
        }
    }

    static FTResource getFTResource(Resource resource){
        return new FTResource(fts.getResourceBaseUrl() +
                              resource.getDsId() + "/" +
                              resource.getLcId() + "/" +
                              resource.getId(),
                              resource.getLang(),
                              resource.getValue(),
                              resource.getSource(),
                              resource.getRights());
    }

    private static FTResource fetchFTResource(AnnoPage annoPage) {
        FTResource resource;
        try {
            resource = ftService.fetchFTResource(annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId(),
                annoPage.getLang());
        } catch (ResourceDoesNotExistException e) {
            LOG.info("Error retrieving fulltext resource for annoPage {}", annoPage, e);
            resource = null;
        }
        return resource;
    }

    private static String getResourceIdUrl(AnnoPage annoPage, Annotation annotation){
        StringBuilder s = new StringBuilder(getResourceIdBaseUrl(annoPage));
        if (annotation.getFrom() != null || annotation.getTo() != null) {
            s.append("#char=");
            s.append(annotation.getFrom());
            s.append(",");
            s.append(annotation.getTo());
        }
        return s.toString();
    }

  private static String getResourceIdBaseUrl(AnnoPage annoPage) {
    String url =
        fts.getResourceBaseUrl()
            + annoPage.getDsId()
            + "/"
            + annoPage.getLcId()
            + "/"
            + annoPage.getRes().getPgId();

    // EA-3073: Resource endpoint can return the original version without the lang parameter. Only append if this is a translation
    if (annoPage.isTranslation()) {
      url = url + "?" + LANGUAGE_PARAM + annoPage.getRes().getLang();
    }
    return url;
  }

    private static String getAnnoPageIdUrl(AnnoPage annoPage){
        return fts.getAnnoPageBaseUrl() + annoPage.getDsId() + "/" +
               annoPage.getLcId() + FTDefinitions.ANNOPAGE_PATH + "/" + annoPage.getPgId() + "?" + LANGUAGE_PARAM + annoPage.getLang();
    }

    private static String getAnnotationIdUrl(AnnoPage annoPage, Annotation annotation) {
        return fts.getAnnotationBaseUrl() + annoPage.getDsId() + "/" + annoPage.getLcId() +
               ANNOTATION_PATH + annotation.getAnId();
    }

    public static String getAnnotationIdUrl(String europeanaId, Annotation annotation) {
        StringBuilder s = new StringBuilder(fts.getAnnotationBaseUrl());
        if (europeanaId.startsWith("/")) {
            s.deleteCharAt(s.length() - 1);
        }
        s.append(europeanaId)
                .append(ANNOTATION_PATH)
                .append(annotation.getAnId());
        return s.toString();
    }

    private static String expandDCType(char dcTypeCode){
        AnnotationType dcType = AnnotationType.fromAbbreviation(dcTypeCode);
        if (dcType == null) {
            LOG.warn("Unknown dcType code '{}'", dcTypeCode);
            return "undefined";
        }
        return dcType.getDisplayName();
    }
}
