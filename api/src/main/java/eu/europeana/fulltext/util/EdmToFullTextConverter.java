package eu.europeana.fulltext.util;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
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
    public static AnnoPage getAnnoPage(
        String datasetId, String localId, AnnotationPreview request, EdmFullTextPackage fulltext, boolean isContributed)
        throws SubtitleConversionException {
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

    private static Resource getResource(
        EdmFullTextResource ftResource, AnnotationPreview request, String datasetId, String localId, boolean isContributed) {
      Resource resource = new Resource();
      resource.setContributed(isContributed);
      resource.setId(
          getFulltextResourceId(ftResource.getFullTextResourceURI(), request.getRecordId()));
      resource.setLang(request.getLanguage());
      resource.setValue(ftResource.getValue());
      resource.setRights(request.getRights());
      resource.setDsId(datasetId);
      resource.setLcId(localId);
      resource.setPgId(GeneralUtils.derivePageId(request.getMedia()));
      return resource;
    }

    private static List<Annotation> getAnnotations(EdmFullTextPackage fulltext,
        String mediaUrl, String language) {
      List<Annotation> annotationList = new ArrayList<>();
      for (EdmAnnotation sourceAnnotation : fulltext) {
        EdmTextBoundary boundary = (EdmTextBoundary) sourceAnnotation.getTextReference();
        List<Target> targets = new ArrayList<>();
        if (sourceAnnotation.hasTargets()) {
          EdmTimeBoundary tB = sourceAnnotation.getTargets().get(0);
          targets.add(new Target(tB.getStart(), tB.getEnd()));
        }
        Annotation annotation = new Annotation();
        // for media don't add default to, from values
        if (sourceAnnotation.getType().equals(AnnotationType.MEDIA)) {
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
