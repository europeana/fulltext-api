package eu.europeana.fulltext.api.web;

import static eu.europeana.fulltext.WebConstants.MOTIVATION_SUBTITLING;
import static eu.europeana.fulltext.WebConstants.REQUEST_VALUE_SOURCE;
import static eu.europeana.fulltext.util.GeneralUtils.isValidAnnotationId;

import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.service.AnnotationApiRestService;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.SubtitleService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.exception.InvalidFormatException;
import eu.europeana.fulltext.exception.InvalidUriException;
import eu.europeana.fulltext.exception.MediaTypeNotSupportedException;
import eu.europeana.fulltext.exception.SubtitleParsingException;
import eu.europeana.fulltext.exception.UnsupportedAnnotationException;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.AnnotationPreview.Builder;
import eu.europeana.fulltext.subtitles.DeleteAnnoSyncResponse;
import eu.europeana.fulltext.subtitles.DeleteAnnoSyncResponse.Status;
import eu.europeana.fulltext.subtitles.SubtitleType;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import eu.europeana.fulltext.util.GeneralUtils;
import io.swagger.annotations.ApiOperation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@Validated
public class FTWriteController extends BaseRest {

  private final FTSettings appSettings;
  private final SubtitleService subtitleService;
  private final AnnotationApiRestService annotationsApiRestService;

  private final FTService ftService;
  private final Predicate<String> annotationIdPattern;

  private static final Logger LOG = LogManager.getLogger(FTWriteController.class);

  public FTWriteController(
      FTSettings appSettings,
      SubtitleService subtitleHandlerService,
      AnnotationApiRestService annotationsApiRestService,
      FTService ftService) {
    this.appSettings = appSettings;
    this.subtitleService = subtitleHandlerService;
    this.annotationsApiRestService = annotationsApiRestService;
    this.ftService = ftService;
    annotationIdPattern =
        Pattern.compile(
                String.format(
                    GeneralUtils.ANNOTATION_ID_REGEX, appSettings.getAnnotationIdHostsPattern()))
            .asMatchPredicate();
  }

  @ApiOperation(value = "Propagate and synchronise with Annotations API")
  @PostMapping(
      value = "/fulltext/annosync",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> syncAnnotations(
      @RequestParam(value = REQUEST_VALUE_SOURCE) String source, HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException, IOException {
    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.UPDATE, request);
    }

    // check that sourceUrl is valid, and points to a europeana.eu domain
    if (!isValidAnnotationId(source, annotationIdPattern)) {
      throw new InvalidUriException(
          String.format(
              "'%s' request parameter must be a valid annotation id", REQUEST_VALUE_SOURCE));
    }

    Optional<AnnotationItem> itemOptional = annotationsApiRestService.retrieveAnnotation(source);
    if (itemOptional.isEmpty()) {
      // annotationItem not present, meaning 410 returned by Annotation API - so it has been deleted

      TranslationAnnoPage annoPage = ftService.getShellAnnoPageBySource(source);
      long count = ftService.deleteAnnoPagesWithSources(Collections.singletonList(source));

      DeleteAnnoSyncResponse response =
          new DeleteAnnoSyncResponse(
              source, count > 0 ? Status.DELETED.getValue() : Status.NOOP.getValue(), annoPage);

      return generateResponse(request, serializeResponse(response), HttpStatus.ACCEPTED);
    }

    AnnotationItem item = itemOptional.get();
    // motivation must be subtitling

    if (!MOTIVATION_SUBTITLING.equals(item.getMotivation())) {
      throw new UnsupportedAnnotationException(
          String.format(
              "Annotation motivation '%s' not supported for sync. Only subtitles are supported",
              item.getMotivation()));
    }

    AnnotationPreview annotationPreview =
        subtitleService.createAnnotationPreview(itemOptional.get());
    TranslationAnnoPage annoPage = subtitleService.createAnnoPage(annotationPreview);

    // Morphia creates a new _id value if none exists, so we can't directly call save() – as this
    // could be an update.
    ftService.upsertAnnoPage(List.of(annoPage));

    return generateResponse(request, serializeJsonLd(annoPage), HttpStatus.ACCEPTED);
  }

  @ApiOperation(
      value = "Submits a new fulltext document for a given Europeana ID (dataset + localID)")
  @PostMapping(
      value = "/presentation/{datasetId}/{localId}/annopage",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> submitNewFulltext(
      @PathVariable(value = WebConstants.REQUEST_VALUE_DATASET_ID) String datasetId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_LOCAL_ID) String localId,
      @RequestParam(value = WebConstants.REQUEST_VALUE_MEDIA) String media,
      @RequestParam(value = WebConstants.REQUEST_VALUE_LANG) String lang,
      @RequestParam(
              value = WebConstants.REQUEST_VALUE_ORIGINAL_LANG,
              required = false,
              defaultValue = "false")
          boolean originalLang,
      @RequestParam(value = WebConstants.REQUEST_VALUE_RIGHTS) String rights,
      @RequestParam(value = WebConstants.REQUEST_VALUE_SOURCE, required = false) String source,
      @RequestBody String content,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException, IOException,
          URISyntaxException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.CREATE, request);
    }
    return addNewFulltext(
        datasetId, localId, media, lang, originalLang, rights, source, content, request);
  }

  private ResponseEntity<String> addNewFulltext(
      String datasetId,
      String localId,
      String media,
      String lang,
      boolean originalLang,
      String rights,
      String source,
      String content,
      HttpServletRequest request)
      throws EuropeanaApiException, IOException {
    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the media URL, if so then return a HTTP 301 with the URL of the Annotation Page
     */
    if (ftService.annoPageExistsByTgtId(datasetId, localId, media, lang)) {
      String redirectPath =
          String.format(
              "/presentation/%s/%s/%s", datasetId, localId, GeneralUtils.derivePageId(media));
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "AnnoPage already exists for subtitle. Redirecting to {}?lang={}", redirectPath, lang);
      }
      // return 301 redirect
      return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
          .location(
              UriComponentsBuilder.newInstance()
                  .path(redirectPath)
                  .query(WebConstants.REQUEST_VALUE_LANG + "=" + lang)
                  .build()
                  .toUri())
          .build();
    }

    SubtitleType type = SubtitleType.getValueByMimetype(request.getContentType());
    if (type == null) {
      throw new MediaTypeNotSupportedException(
          "The content type " + request.getContentType() + " is not supported");
    }
    AnnotationPreview annotationPreview =
        createAnnotationPreview(
            datasetId, localId, lang, originalLang, rights, source, media, content, type);
    AnnoPage savedAnnoPage = ftService.createAndSaveAnnoPage(annotationPreview);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Created new AnnoPage {}", savedAnnoPage);
    }
    return generateResponse(request, serializeJsonLd(savedAnnoPage), HttpStatus.OK);
  }

  @ApiOperation(value = "Replaces existing fulltext for a media resource with a new document")
  @PutMapping(
      value = "/presentation/{datasetId}/{localId}/annopage/{pageId}",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> replaceFullText(
      @PathVariable(value = WebConstants.REQUEST_VALUE_DATASET_ID) String datasetId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_LOCAL_ID) String localId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_PAGE_ID) String pageId,
      @RequestParam(value = WebConstants.REQUEST_VALUE_LANG) String lang,
      @RequestParam(
              value = WebConstants.REQUEST_VALUE_ORIGINAL_LANG,
              required = false,
              defaultValue = "false")
          boolean originalLang,
      @RequestParam(value = WebConstants.REQUEST_VALUE_RIGHTS) String rights,
      @RequestParam(value = WebConstants.REQUEST_VALUE_SOURCE, required = false) String source,
      @RequestBody(required = false) String content,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException, IOException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.UPDATE, request);
    }
    return replaceExistingFullText(
        datasetId, localId, pageId, lang, originalLang, rights, source, content, request);
  }

  private ResponseEntity<String> replaceExistingFullText(
      String datasetId,
      String localId,
      String pgId,
      String lang,
      boolean originalLang,
      String rights,
      String source,
      String content,
      HttpServletRequest request)
      throws IOException, EuropeanaApiException {
    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the PAGE_ID and LANG, if not then return a HTTP 404
     */
    TranslationAnnoPage annoPage = ftService.getAnnoPageByPgId(datasetId, localId, pgId, lang);

    if (annoPage == null) {
      throw new AnnoPageDoesNotExistException(
          "Annotation page does not exist for "
              + GeneralUtils.getTranslationAnnoPageUrl(datasetId, localId, pgId, lang));
    }
    // determine type
    SubtitleType type = null;
    if (!StringUtils.isEmpty(content)) {
      type = SubtitleType.getValueByMimetype(request.getContentType());
      if (type == null) {
        throw new MediaTypeNotSupportedException(
            "The content type " + request.getContentType() + " is not supported");
      }
    }
    AnnotationPreview annotationPreview =
        createAnnotationPreview(
            datasetId,
            localId,
            lang,
            originalLang,
            rights,
            source,
            annoPage.getTgtId(),
            content,
            type);
    TranslationAnnoPage updatedAnnoPage = ftService.updateAnnoPage(annotationPreview, annoPage);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Replaced AnnoPage {}", updatedAnnoPage);
    }
    return generateResponse(request, serializeJsonLd(updatedAnnoPage), HttpStatus.OK);
  }

  @ApiOperation(value = "Deletes the full-text associated to a media resource\n")
  @DeleteMapping(
      value = "/presentation/{datasetId}/{localId}/annopage/{pageId}",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> deleteFulltext(
      @PathVariable(value = WebConstants.REQUEST_VALUE_DATASET_ID) String datasetId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_LOCAL_ID) String localId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_PAGE_ID) String pageId,
      @RequestParam(value = WebConstants.REQUEST_VALUE_LANG, required = false) String lang,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.DELETE, request);
    }
    return deleteAnnoPage(datasetId, localId, pageId, lang, request);
  }

  private ResponseEntity<String> deleteAnnoPage(
      String datasetId, String localId, String pageId, String lang, HttpServletRequest request)
      throws AnnoPageDoesNotExistException {
    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the PAGE_ID and LANG (if provided), if not then return a HTTP 404
     */
    if (!ftService.doesTranslationExist(datasetId, localId, pageId, lang)) {
      throw new AnnoPageDoesNotExistException(
          "Annotation page does not exist for "
              + GeneralUtils.getTranslationAnnoPageUrl(datasetId, localId, pageId, lang));
    }

    /*
     * Delete the respective AnnotationPage(s) entry from MongoDB (if lang is omitted, the pages for
     * all languages will be deleted)
     */
    if (StringUtils.isNotEmpty(lang)) {
      ftService.deleteAnnoPages(datasetId, localId, pageId, lang);
    } else {
      ftService.deleteAnnoPages(datasetId, localId, pageId);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Deleted AnnoPage(s) for {}/{}/{}?lang={}", datasetId, localId, pageId, lang);
    }
    return noContentResponse(request);
  }

  // Creates Annotation preview object along with subtitles Items
  private AnnotationPreview createAnnotationPreview(
      String datasetId,
      String localId,
      String lang,
      boolean originalLang,
      String rights,
      String source,
      String media,
      String content,
      SubtitleType type)
      throws InvalidFormatException, SubtitleParsingException {
    // process subtitles if content is not empty
    List<SubtitleItem> subtitleItems = new ArrayList<>();
    if (!StringUtils.isEmpty(content)) {
      subtitleItems =
          subtitleService.parseSubtitle(
              new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), type);
    }
    String recordId = GeneralUtils.generateRecordId(datasetId, localId);
    return new Builder(recordId, type, subtitleItems)
        .setOriginalLang(originalLang)
        .setLanguage(lang)
        .setRights(rights)
        .setMedia(media)
        .setSource(source)
        .build();
  }
}