package eu.europeana.fulltext.service;

import static eu.europeana.fulltext.subtitles.FulltextType.SUB_RIP;
import static eu.europeana.fulltext.subtitles.FulltextType.TTML;
import static eu.europeana.fulltext.subtitles.FulltextType.WEB_VTT;

import com.dotsub.converter.exception.FileFormatException;
import com.dotsub.converter.importer.SubtitleImportHandler;
import com.dotsub.converter.importer.impl.DfxpImportHandler;
import com.dotsub.converter.importer.impl.SrtImportHandler;
import com.dotsub.converter.importer.impl.WebVttImportHandler;
import com.dotsub.converter.model.Configuration;
import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.edm.FullTextAnnotation;
import eu.europeana.edm.FullTextPackage;
import eu.europeana.edm.media.MediaResource;
import eu.europeana.edm.media.TimeBoundary;
import eu.europeana.edm.text.FullTextResource;
import eu.europeana.edm.text.TextBoundary;
import eu.europeana.edm.text.TextReference;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.exception.InvalidFormatException;
import eu.europeana.fulltext.exception.SubtitleParsingException;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.FulltextType;
import eu.europeana.fulltext.util.GeneralUtils;
import eu.europeana.fulltext.util.SubtitleContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SubtitleFulltextConverter implements FulltextConverter {

  private static final Configuration defaultSubtitleConfig = new Configuration();

  private static final Logger LOGGER = LogManager.getLogger(SubtitleFulltextConverter.class);
  private static final Pattern PATTERN = Pattern.compile("[<][/]?[^<]+[/]?[>]");

  private static final Map<FulltextType, SubtitleImportHandler> subtitleImportHandlerMap = Map.of(WEB_VTT, new WebVttImportHandler(),
          SUB_RIP, new SrtImportHandler(), TTML, new DfxpImportHandler());


  @Override
  public FullTextPackage convert(AnnotationPreview annotationPreview) throws InvalidFormatException, SubtitleParsingException {
    // get the subtitles
    List<SubtitleItem> subtitleItems =
            parseSubtitle(
                    new ByteArrayInputStream(annotationPreview.getAnnotationBody().getBytes(StandardCharsets.UTF_8)),
                    annotationPreview.getFulltextType());

    // generate url
    String uri = WebConstants.ITEM_BASE_URL + annotationPreview.getRecordId();
    String annotationPageURI = GeneralUtils.getAnnotationPageURI(annotationPreview.getRecordId());

    // generate resource id - hash of recordId and lang(this is to avoid any override of the
    // resource based on same recordId but different lang)
    String fullTextResourceURI =
            GeneralUtils.getFullTextResourceURI(
                    annotationPreview.getRecordId(),
                    GeneralUtils.generateResourceId(annotationPreview.getRecordId(), annotationPreview.getLanguage(), annotationPreview.getMedia()));

    FullTextPackage page = new FullTextPackage(annotationPageURI, null);

    // generate Fulltext Resource
    FullTextResource resource =
            new FullTextResource(
                    fullTextResourceURI, null, annotationPreview.getLanguage(), annotationPreview.getRights(), uri);
    // add first annotation of type Media - this will not have any targets or text boundary
    TextReference tb = new TextBoundary(resource);
    page.add(new FullTextAnnotation(null, tb, null, AnnotationType.MEDIA, null, null));

    // add the subtitles as annotations
    SubtitleContext subtitleContext = new SubtitleContext();
    subtitleContext.start(fullTextResourceURI);
    int i = 0;
    for (SubtitleItem item : subtitleItems) {
      if (i++ != 0) {
        subtitleContext.separator();
      }
      int start = item.getStartTime();
      int end = start + item.getDuration();
      TimeBoundary mr = new TimeBoundary(new MediaResource(annotationPreview.getMedia()), start, end);
      TextBoundary tr = subtitleContext.newItem(processSubtitle(item.getContent()), resource);
      page.add(new FullTextAnnotation(null, tr, mr, AnnotationType.CAPTION, null, null));
    }
    // ADD the resource in Fulltext page
    resource.setValue(subtitleContext.end());
    page.setResource(resource);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(
              "Successfully converted SRT to EDM for record {}. Processed Annotations - {}",
              annotationPreview.getRecordId(),
              page.size());
    }
    return page;
  }

  /**
   * parses the text to Subtitle Item
   * @param text
   * @param fulltextType
   * @return
   * @throws InvalidFormatException
   * @throws SubtitleParsingException
   */
  public List<SubtitleItem> parseSubtitle(InputStream text, FulltextType fulltextType)
      throws InvalidFormatException, SubtitleParsingException {
    try {
      List<SubtitleItem> result = subtitleImportHandlerMap.get(fulltextType).importFile(text, defaultSubtitleConfig);
      // if the result is null, then the data was not parsed and is invalid
      // This is to avoid empty Fulltext being created
      if (result.isEmpty()) {
        throw new SubtitleParsingException("Please provide proper data!! Text passed is not parseable.");
      }
      return result;
    } catch (FileFormatException e) {
      throw new InvalidFormatException(
          "Please provide proper format!! File does not match the expected format - "
              + fulltextType.getMimeType());
    } catch (IOException e) {
      throw new SubtitleParsingException(e.getMessage());
    }
  }

  private String processSubtitle(String text) {
    return PATTERN.matcher(text).replaceAll("");
  }

}
