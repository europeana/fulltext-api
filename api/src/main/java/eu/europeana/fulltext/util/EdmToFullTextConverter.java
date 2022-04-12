package eu.europeana.fulltext.util;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.entity.TranslationResource;
import eu.europeana.fulltext.exception.SubtitleConversionException;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.edm.EdmAnnotation;
import eu.europeana.fulltext.subtitles.edm.EdmFullTextPackage;
import eu.europeana.fulltext.subtitles.edm.EdmFullTextResource;
import eu.europeana.fulltext.subtitles.edm.EdmTextBoundary;
import eu.europeana.fulltext.subtitles.edm.EdmTimeBoundary;
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
    public static TranslationAnnoPage getAnnoPage(
        String datasetId, String localId, AnnotationPreview request, EdmFullTextPackage fulltext)
        throws SubtitleConversionException {
      TranslationResource resource = getResource(fulltext.getResource(), request, datasetId, localId);
      TranslationAnnoPage annoPage = new TranslationAnnoPage();
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
      annoPage.setAns(getAnnotations(fulltext));
      annoPage.setSource(request.getSource());
      // fail-safe check
      if (annoPage.getAns().size() != fulltext.size()) {
        throw new SubtitleConversionException(
            "Mismatch in Annotations while converting from EDM to fulltext. "
                + "Annotations obtained - "
                + fulltext.size()
                + ". Annotations converted - "
                + annoPage.getAns().size());
      }
      return annoPage;
    }

    private static TranslationResource getResource(
        EdmFullTextResource ftResource, AnnotationPreview request, String datasetId, String localId) {
      TranslationResource resource = new TranslationResource();
      resource.setId(
          getFulltextResourceId(ftResource.getFullTextResourceURI(), request.getRecordId()));
      resource.setLang(request.getLanguage());
      resource.setValue(ftResource.getValue());
      resource.setRights(request.getRights());
      resource.setDsId(datasetId);
      resource.setLcId(localId);
      return resource;
    }

    private static List<Annotation> getAnnotations(EdmFullTextPackage fulltext) {
      List<Annotation> annotationList = new ArrayList<>();
      for (EdmAnnotation sourceAnnotation : fulltext) {
        EdmTextBoundary boundary = (EdmTextBoundary) sourceAnnotation.getTextReference();
        List<Target> targets = new ArrayList<>();
        if (sourceAnnotation.hasTargets()) {
          EdmTimeBoundary tB = sourceAnnotation.getTargets().get(0);
          targets.add(new Target(tB.getStart(), tB.getEnd()));
        }
        // for media don't add default to, from values
        if (sourceAnnotation.getType().equals(AnnotationType.MEDIA)) {
          Annotation annotation = new Annotation();
          annotation.setAnId(sourceAnnotation.getAnnoId());
          annotation.setDcType(sourceAnnotation.getType().getAbbreviation());
          annotationList.add(annotation);
        } else {
          annotationList.add(
              new Annotation(
                  sourceAnnotation.getAnnoId(),
                  sourceAnnotation.getType().getAbbreviation(),
                  boundary.getFrom(),
                  boundary.getTo(),
                  targets));
        }
      }
      return annotationList;
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
}
