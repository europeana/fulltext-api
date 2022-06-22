package eu.europeana.fulltext.api.service;

import static eu.europeana.fulltext.AppConstants.defaultSubtitleConfig;
import static eu.europeana.fulltext.subtitles.SubtitleType.SRT;
import static eu.europeana.fulltext.subtitles.SubtitleType.WEB_VTT;
import static eu.europeana.fulltext.util.GeneralUtils.getDsId;
import static eu.europeana.fulltext.util.GeneralUtils.getLocalId;

import com.dotsub.converter.exception.FileFormatException;
import com.dotsub.converter.importer.SubtitleImportHandler;
import com.dotsub.converter.importer.impl.QtTextImportHandler;
import com.dotsub.converter.importer.impl.WebVttImportHandler;
import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.exception.InvalidFormatException;
import eu.europeana.fulltext.exception.SubtitleConversionException;
import eu.europeana.fulltext.exception.SubtitleParsingException;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.SubtitleType;
import eu.europeana.fulltext.subtitles.edm.EdmAnnotation;
import eu.europeana.fulltext.subtitles.edm.EdmFullTextPackage;
import eu.europeana.fulltext.subtitles.edm.EdmFullTextResource;
import eu.europeana.fulltext.subtitles.edm.EdmTextBoundary;
import eu.europeana.fulltext.subtitles.edm.EdmTimeBoundary;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import eu.europeana.fulltext.util.EdmToFullTextConverter;
import eu.europeana.fulltext.util.GeneralUtils;
import eu.europeana.fulltext.util.SubtitleContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class SubtitleService {
  private static final Logger logger = LogManager.getLogger(SubtitleService.class);
  private static final Pattern PATTERN = Pattern.compile("[<][/]?[^<]+[/]?[>]");

  private static final Map<SubtitleType, SubtitleImportHandler> subtitleHandlerMapping =
      Map.of(WEB_VTT, new WebVttImportHandler(), SRT, new QtTextImportHandler());

  public EdmFullTextPackage convert(AnnotationPreview preview) {
    String uri = WebConstants.ITEM_BASE_URL + preview.getRecordId();
    String annotationPageURI = GeneralUtils.getAnnotationPageURI(preview.getRecordId());
    // generate resource id - hash of recordId and lang(this is to avoid any override of the
    // resource based on same recordId but different lang)
    String fullTextResourceURI =
        GeneralUtils.getFullTextResourceURI(
            preview.getRecordId(),
            GeneralUtils.generateResourceId(preview.getRecordId(), preview.getLanguage(), preview.getMedia()));

    EdmFullTextPackage page = new EdmFullTextPackage(annotationPageURI, null);

    // generate Fulltext Resource
    EdmFullTextResource resource =
        new EdmFullTextResource(
            fullTextResourceURI, null, preview.getLanguage(), preview.getRights(), uri);
    // add first annotation of type Media - this will not have any targets or text boundary
    EdmTextBoundary tb = new EdmTextBoundary(fullTextResourceURI);
    page.add(new EdmAnnotation(null, tb, null, AnnotationType.MEDIA, null, null));

    // add the subtitles as annotations
    SubtitleContext subtitleContext = new SubtitleContext();
    subtitleContext.start(fullTextResourceURI);
    int i = 0;
    for (SubtitleItem item : preview.getSubtitleItems()) {
      if (i++ != 0) {
        subtitleContext.separator();
      }
      int start = item.getStartTime();
      int end = start + item.getDuration();
      EdmTimeBoundary mr = new EdmTimeBoundary(preview.getMedia(), start, end);
      EdmTextBoundary tr = subtitleContext.newItem(processSubtitle(item.getContent()));
      page.add(new EdmAnnotation(null, tr, mr, AnnotationType.CAPTION, null, null));
    }
    // ADD the resource in Fulltext page
    resource.setValue(subtitleContext.end());
    page.setResource(resource);
    if (logger.isTraceEnabled()) {
      logger.trace(
          "Successfully converted SRT to EDM for record {}. Processed Annotations - {}",
          preview.getRecordId(),
          page.size());
    }
    return page;
  }

  private String processSubtitle(String text) {
    return PATTERN.matcher(text).replaceAll("");
  }

  /** parses the text to Subtitle Item */
  public List<SubtitleItem> parseSubtitle(InputStream text, SubtitleType subtitleType)
      throws InvalidFormatException, SubtitleParsingException {
    SubtitleImportHandler subtitleImportHandler = subtitleHandlerMapping.get(subtitleType);
    if (subtitleImportHandler == null) {
      throw new InvalidFormatException("Format not supported : " + subtitleType.getMimeType());
    }
    try {
      return subtitleImportHandler.importFile(text, defaultSubtitleConfig);
    } catch (FileFormatException e) {
      throw new InvalidFormatException(
          "Please provide proper format!! File does not match the expected format - "
              + subtitleType.getMimeType());
    } catch (IOException e) {
      throw new SubtitleParsingException(e.getMessage());
    }
  }

  public AnnotationPreview createAnnotationPreview(AnnotationItem item)
      throws SubtitleParsingException, InvalidFormatException {
    SubtitleType subtitleType = SubtitleType.getValueByMimetype(item.getBody().getFormat());

    if (subtitleType == null) {
      throw new SubtitleParsingException(
          String.format(
              "Unsupported mimeType in Annotation id=%s body.format=%s",
              item.getId(), item.getBody().getFormat()));
    }

    List<SubtitleItem> subtitleItems =
        parseSubtitle(
            new ByteArrayInputStream(item.getBody().getValue().getBytes(StandardCharsets.UTF_8)),
            subtitleType);

    return new AnnotationPreview.Builder(
        GeneralUtils.getRecordIdFromUri(item.getTarget().getScope()),
        subtitleType,
        subtitleItems)
        .setSource(item.getId())
        .setMedia(item.getTarget().getSource())
        .setLanguage(item.getBody().getLanguage())
        .setRights(item.getBody().getEdmRights())
        .build();
  }

  public AnnoPage createAnnoPage(AnnotationPreview annotationPreview, boolean isContributed)
      throws SubtitleConversionException {
    EdmFullTextPackage fulltext = convert(annotationPreview);
    String recordId = annotationPreview.getRecordId();
    return EdmToFullTextConverter.getAnnoPage(
        getDsId(recordId), getLocalId(recordId), annotationPreview, fulltext, isContributed);
  }

  /**
   * Checks if AnnoPage should be updated. AnnoPage will only be updated if source field is passed
   * OR if the new SRT was uploaded ie; the new subtitles were processed
   */
  public boolean isAnnoPageUpdateRequired(AnnotationPreview preview) {
    return (StringUtils.isNotEmpty(preview.getSource()) || !preview.getSubtitleItems().isEmpty());
  }
}
