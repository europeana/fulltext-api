package eu.europeana.fulltext.util;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.edm.FullTextAnnotation;
import eu.europeana.edm.FullTextPackage;
import eu.europeana.edm.media.*;
import eu.europeana.edm.text.FullTextResource;
import eu.europeana.edm.text.TextBoundary;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.exception.MismatchInAnnotationException;
import eu.europeana.fulltext.subtitles.AnnotationPreview;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

  public class EdmToFullTextConverter {

    private EdmToFullTextConverter() {
      // private constructor to hide implicit one
    }

    /**
     * Converts FulltextPackage to AnnoPage class
     *
     * @param datasetId
     * @param localId
     * @param request
     * @param fulltext
     * @return
     */
    public static AnnoPage createAnnoPage(
            String datasetId, String localId, AnnotationPreview request, FullTextPackage fulltext, boolean isContributed)
        throws MismatchInAnnotationException {
      Resource resource = getResource(fulltext.getResource(), request, datasetId, localId, isContributed);
      AnnoPage annoPage = new AnnoPage();
      annoPage.setDsId(datasetId);
      annoPage.setLcId(localId);
      annoPage.setPgId(GeneralUtils.derivePageId(request.getMedia()));
      annoPage.setTgtId(request.getMedia());
      annoPage.setLang(request.getLanguage());

      // set the source if present
      if (!StringUtils.isEmpty(request.getSource())) {
        annoPage.setSource(request.getSource());
      }
      annoPage.setRes(resource);
      annoPage.setAns(getAnnotations(fulltext, request.getMedia(), request.getLanguage()));
      annoPage.setSource(request.getSource());
      annoPage.setTranslation(!request.isOriginalLang());
      // fail-safe check
      if (annoPage.getAns().size() != fulltext.size()) {
        throw new MismatchInAnnotationException(
            "Mismatch in Annotations while converting from EDM to fulltext. "
                + "Annotations obtained - "
                + fulltext.size()
                + ". Annotations converted - "
                + annoPage.getAns().size());
      }
      return annoPage;
    }

    private static Resource getResource(
            FullTextResource ftResource, AnnotationPreview request, String datasetId, String localId, boolean isContributed) {
      Resource resource = new Resource();
      resource.setContributed(isContributed);
      resource.setId(
          getFulltextResourceId(ftResource.getFullTextResourceURI(), request.getRecordId()));
      resource.setLang(ftResource.getLang());
      resource.setValue(ftResource.getValue());
      resource.setRights(request.getRights());
      resource.setDsId(datasetId);
      resource.setLcId(localId);
      resource.setPgId(GeneralUtils.derivePageId(request.getMedia()));
      resource.setTranslation(!request.isOriginalLang());
      return resource;
    }

    private static List<Annotation> getAnnotations(FullTextPackage fulltext,
        String mediaUrl, String language) {
      List<Annotation> annotationList = new ArrayList<>();
      for (FullTextAnnotation sourceAnnotation : fulltext) {
        TextBoundary boundary = (TextBoundary) sourceAnnotation.getTextReference();
        List<Target> targets = new ArrayList<>();
        // we have the multiple targets for newspapers and AlTO
        if (sourceAnnotation.hasTargets()) {
          sourceAnnotation.getTargets().stream().forEach(target -> addTarget(target, targets));
        }
        Annotation annotation = new Annotation();
        // for top level annotation MEDIA or PAGE, don't add default to, from values
        if (isTopLevel(sourceAnnotation)) {
          annotation.setDcType(sourceAnnotation.getType().getAbbreviation());
      } else {
        annotation.setDcType(sourceAnnotation.getType().getAbbreviation());
        annotation.setFrom(boundary.getFrom());
        annotation.setTo(boundary.getTo());
        annotation.setTgs(targets);
        }
      annotation.setAnId(GeneralUtils.createAnnotationHash(annotation, mediaUrl, language));
      annotationList.add(annotation);
      }
      return annotationList;
    }

    private static void addTarget(MediaReference reference, List<Target> targets) {
      if (reference instanceof TimeBoundary) {
        // add time boundary
        TimeBoundary tb = (TimeBoundary) reference;
        targets.add(new Target(tb.getStart(), tb.getEnd()));
      } else if (reference instanceof ImageBoundary) {
        // add image boundary
        ImageBoundary iB = (ImageBoundary) reference;
        targets.add(new Target(iB.x, iB.y, iB.w, iB.h));
      }
    }

    /**
     * Extracts fulltext Resource ID from the url. url ex :
     * http://data.europeana.eu/fulltext/456-test/data_euscreenXL_EUS_test/161d895530ccefd51e08611fde992c7e
     *
     * @param fulltextResourceUri
     * @param itemID
     * @return
     */
    private static String getFulltextResourceId(String fulltextResourceUri, String itemID) {
      return StringUtils.substringAfter(
          fulltextResourceUri, WebConstants.FULLTEXT_BASE_URL + itemID + "/");
    }

    public static boolean isTopLevel(FullTextAnnotation annotation) {
      return (annotation.getType().equals(AnnotationType.MEDIA) ||
              annotation.getType().equals(AnnotationType.PAGE));
    }
}
