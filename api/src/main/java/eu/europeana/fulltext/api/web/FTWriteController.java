package eu.europeana.fulltext.api.web;

import static eu.europeana.fulltext.WebConstants.*;
import static eu.europeana.fulltext.util.GeneralUtils.isValidAnnotationId;
import static eu.europeana.fulltext.util.HttpUtils.REQUEST_VERSION_2;
import static eu.europeana.fulltext.util.HttpUtils.addContentTypeToResponseHeader;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.service.authorization.AuthorizationService;
import eu.europeana.api.commons.web.controller.BaseRestController;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.api.commons.web.service.AbstractRequestPathMethodService;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.model.AnnotationWrapper;
import eu.europeana.fulltext.api.service.AnnotationApiRestService;
import eu.europeana.fulltext.api.service.CacheUtils;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.exception.InvalidUriException;
import eu.europeana.fulltext.exception.MediaTypeNotSupportedException;
import eu.europeana.fulltext.exception.SerializationException;
import eu.europeana.fulltext.exception.UnsupportedAnnotationException;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.DeleteAnnoSyncResponse;
import eu.europeana.fulltext.subtitles.DeleteAnnoSyncResponse.Status;
import eu.europeana.fulltext.subtitles.FulltextType;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import eu.europeana.fulltext.util.AnnotationUtils;
import eu.europeana.fulltext.util.GeneralUtils;
import io.swagger.annotations.ApiOperation;
import java.time.ZonedDateTime;
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
public class FTWriteController extends BaseRestController {

  private final FTSettings appSettings;
  private final AnnotationApiRestService annotationsApiRestService;

  private final FTService ftService;
  private final Predicate<String> annotationIdPattern;

  private final AuthorizationService ftAuthorizationService;
  private final AbstractRequestPathMethodService requestPathMethodService;

  private static final Logger LOG = LogManager.getLogger(FTWriteController.class);

  public FTWriteController(
      FTSettings appSettings,
      AnnotationApiRestService annotationsApiRestService,
      FTService ftService,
      AuthorizationService ftAuthorizationService,
      AbstractRequestPathMethodService requestPathMethodService) {
    this.appSettings = appSettings;
    this.annotationsApiRestService = annotationsApiRestService;
    this.ftService = ftService;
    annotationIdPattern =
        Pattern.compile(
                String.format(
                    GeneralUtils.ANNOTATION_ID_REGEX, appSettings.getAnnotationIdHostsPattern()))
            .asMatchPredicate();
    this.ftAuthorizationService = ftAuthorizationService;
    this.requestPathMethodService = requestPathMethodService;
  }

  @Override
  protected AuthorizationService getAuthorizationService() {
    return ftAuthorizationService;
  }

  @ApiOperation(value = "Propagate and synchronise with Annotations API")
  @PostMapping(
      value = "/fulltext/annosync",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> syncAnnotations(
      @RequestParam(value = REQUEST_VALUE_SOURCE) String source, HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException {
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

      AnnoPage annoPage = ftService.getShellAnnoPageBySource(source, false);

      DeleteAnnoSyncResponse response;
      if (annoPage == null) {
        // AnnoPage already deprecated, or doesn't exist
        response = new DeleteAnnoSyncResponse(source, Status.NOOP.getValue(), null);
      } else {
        ftService.deprecateAnnoPagesWithSources(Collections.singletonList(source));
        response = new DeleteAnnoSyncResponse(source, Status.DELETED.getValue(), annoPage);
      }
      return ResponseEntity.status(HttpStatus.ACCEPTED)
          .header(HttpHeaders.ALLOW, getMethodsForRequestPattern(request, requestPathMethodService))
          .body(ftService.serialise(response));
    }

    AnnotationItem item = itemOptional.get();
    String motivation = item.getMotivation();

    // motivation must be subtitling or transcribing
    if (!MOTIVATION_SUBTITLING.equals(motivation) && !MOTIVATION_TRANSCRIBING.equals(motivation)) {
      throw new UnsupportedAnnotationException(
          String.format(
              "Annotation motivation '%s' not supported for sync. Only subtitles or transcribing are supported",
              motivation));
    }

    AnnotationPreview annotationPreview =
            AnnotationUtils.createAnnotationPreview(item);
    AnnoPage annoPage = ftService.createAnnoPage(annotationPreview, true);

    // Morphia creates a new _id value if none exists, so we can't directly call save() – as this
    // could be an update.
    ftService.upsertAnnoPage(List.of(annoPage));

    return generateResponse(request, annoPage, HttpStatus.ACCEPTED);
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
      throws ApplicationAuthenticationException, EuropeanaApiException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.CREATE, request);
    }
    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the media URL, if so then return a HTTP 301 with the URL of the Annotation Page
     */
    String pageId = GeneralUtils.derivePageId(media);

    AnnoPage existingAnnoPage = ftService.getShellAnnoPageById(datasetId, localId, pageId, lang, true);

    if (existingAnnoPage != null && !existingAnnoPage.isDeprecated()) {
      String redirectPath =
          String.format(
              "/presentation/%s/%s/annopage/%s", datasetId, localId, pageId);
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

    FulltextType type = FulltextType.getValueByMimetype(request.getContentType());
    if (type == null) {
      throw new MediaTypeNotSupportedException(
          "The content type " + request.getContentType() + " is not supported");
    }
    AnnotationPreview annotationPreview =
        AnnotationUtils.createAnnotationPreview(
            datasetId, localId, lang, originalLang, rights, source, media, content, type);
    AnnoPage createdAnnoPage = ftService.createAnnoPage(annotationPreview, false);

    // if AnnoPage was deprecated, this re-enables it
    createdAnnoPage.copyDbIdFrom(existingAnnoPage);

    ftService.saveAnnoPage(createdAnnoPage);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Created new AnnoPage {}", createdAnnoPage);
    }
    return generateResponse(request, createdAnnoPage, HttpStatus.OK);
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
      throws ApplicationAuthenticationException, EuropeanaApiException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.UPDATE, request);
    }
    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the PAGE_ID and LANG, if not then return a HTTP 404
     */
    AnnoPage annoPage = ftService.getAnnoPageByPgId(datasetId, localId, pageId, lang, true);

    if (annoPage == null) {
      throw new AnnoPageDoesNotExistException(
          "Annotation page does not exist for "
              + GeneralUtils.getAnnoPageUrl(datasetId, localId, pageId, lang));
    }
    // determine type
    FulltextType type = null;
    if (!StringUtils.isEmpty(content)) {
      type = FulltextType.getValueByMimetype(request.getContentType());
      if (type == null) {
        throw new MediaTypeNotSupportedException(
            "The content type " + request.getContentType() + " is not supported");
      }
    }
    AnnotationPreview annotationPreview =
        AnnotationUtils.createAnnotationPreview(
            datasetId,
            localId,
            lang,
            originalLang,
            rights,
            source,
            annoPage.getTgtId(),
            content,
            type);

    // if AnnoPage is deprecated, this re-enables it
    AnnoPage updatedAnnoPage = ftService.updateAnnoPage(annotationPreview, annoPage);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Replaced AnnoPage {}", updatedAnnoPage);
    }
    return generateResponse(request, updatedAnnoPage, HttpStatus.OK);
  }

  @ApiOperation(value = "Deprecates the full-text associated to a media resource\n")
  @DeleteMapping(
      value = "/presentation/{datasetId}/{localId}/annopage/{pageId}",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> deprecateFulltext(
      @PathVariable(value = WebConstants.REQUEST_VALUE_DATASET_ID) String datasetId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_LOCAL_ID) String localId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_PAGE_ID) String pageId,
      @RequestParam(value = WebConstants.REQUEST_VALUE_LANG, required = false) String lang,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.DELETE, request);
    }
    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the PAGE_ID and LANG (if provided), if not then return a HTTP 404
     * If present, check if the annoPages are deprecated already, respond with HTTP 410
     */
    ftService.checkForExistingAndDeprecatedAnnoPages(datasetId, localId, pageId, lang, true);

    /*
     * Deprecates the respective AnnotationPage(s) entry from MongoDB (if lang is omitted, the pages for
     * all languages will be deprecated)
     */
    if (StringUtils.isNotEmpty(lang)) {
      ftService.deprecateAnnoPages(datasetId, localId, pageId, lang);
    } else {
      ftService.deprecateAnnoPages(datasetId, localId, pageId);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Deprecated AnnoPage(s) for {}/{}/{}?lang={}", datasetId, localId, pageId, lang);
    }
    return noContentResponse(request);
  }



  protected ResponseEntity<String> generateResponse(
      HttpServletRequest request, AnnoPage annoPage, HttpStatus status)
      throws SerializationException {

    AnnotationWrapper annotationWrapper = ftService.generateAnnoPageV2(annoPage, true);
    // no context in json responses
    annotationWrapper.setContext(null);

    ZonedDateTime modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
    String requestVersion = REQUEST_VERSION_2;

    String eTag =
        CacheUtils.generateETag(
            annoPage.getDsId() + annoPage.getLcId() + annoPage.getPgId(),
            modified,
            requestVersion + appSettings.getAppVersion(),
            true);

    org.springframework.http.HttpHeaders headers =
        CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));
    addContentTypeToResponseHeader(headers, requestVersion, true);
    // overwrite Allow header populated in CacheUtils.generateHeaders
    headers.set(HttpHeaders.ALLOW, getMethodsForRequestPattern(request, requestPathMethodService));

    return ResponseEntity.status(status)
        .headers(headers)
        .body(ftService.serialise(annotationWrapper));
  }

  private ResponseEntity<String> noContentResponse(HttpServletRequest request) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add(HttpHeaders.ALLOW, getMethodsForRequestPattern(request, requestPathMethodService));
    return ResponseEntity.noContent().headers(headers).build();
  }
}
