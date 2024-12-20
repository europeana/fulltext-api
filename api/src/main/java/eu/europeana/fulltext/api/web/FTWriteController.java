package eu.europeana.fulltext.api.web;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.service.authorization.AuthorizationService;
import eu.europeana.api.commons.web.controller.BaseRestController;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.api.caching.CachingUtils;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.config.RequestPathServiceConfig;
import eu.europeana.fulltext.api.model.AnnotationWrapper;
import eu.europeana.fulltext.service.AnnotationApiRestService;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.exception.*;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.DeleteAnnoSyncResponse;
import eu.europeana.fulltext.subtitles.DeleteAnnoSyncResponse.Status;
import eu.europeana.fulltext.subtitles.FulltextType;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import eu.europeana.fulltext.util.AnnotationUtils;
import eu.europeana.fulltext.util.GeneralUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static eu.europeana.fulltext.WebConstants.*;
import static eu.europeana.fulltext.util.GeneralUtils.isValidAnnotationId;
import static eu.europeana.fulltext.util.RequestUtils.PROFILE_TEXT;
import static eu.europeana.fulltext.util.RequestUtils.extractProfiles;
import static eu.europeana.iiif.AcceptUtils.REQUEST_VERSION_2;
import static eu.europeana.iiif.AcceptUtils.addContentTypeToResponseHeader;
import static eu.europeana.fulltext.api.caching.CachingUtils.*;

@RestController
@Validated
public class FTWriteController extends BaseRestController {

  private final FTSettings appSettings;
  private final AnnotationApiRestService annotationsApiRestService;

  private final FTService ftService;
  private final Predicate<String> annotationIdPattern;

  private final AuthorizationService ftAuthorizationService;

  private final RequestPathServiceConfig requestPathMethodService;

  private static final Logger LOG = LogManager.getLogger(FTWriteController.class);

  public FTWriteController(
      FTSettings appSettings,
      AnnotationApiRestService annotationsApiRestService,
      FTService ftService,
      AuthorizationService ftAuthorizationService,
      RequestPathServiceConfig requestPathMethodService) {
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

  @Tag(name = "Synchronize annotations", description ="Propagate and synchronise with Annotations API")
  @PostMapping(
      value = "/fulltext/annosync",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> syncAnnotations(
      @RequestParam(value = REQUEST_VALUE_SOURCE) String source, HttpServletRequest request,
      @RequestParam(value = "profile", required = false) String profileParam
      )
      throws ApplicationAuthenticationException, EuropeanaApiException {
    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.UPDATE, request);
    }
    List<String> profiles = extractProfiles(profileParam);

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

    return generateResponse(request, annoPage, profiles, HttpStatus.ACCEPTED);
  }

  @Tag(name = "Submit new fulltext", description ="Submits a new fulltext document for a given Europeana ID (dataset + localID)")
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
      @RequestParam(value = "profile", required = false) String profileParam,
      @RequestBody String content,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.CREATE, request);
    }
    List<String> profiles = extractProfiles(profileParam);

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

    // using upsert as that saves the translation field as null for false values.
    // Also prevents null being saved in DB
    // this will be an update for deprecated AnnoPages
    ftService.upsertAnnoPage(List.of(createdAnnoPage));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Created new AnnoPage {}", createdAnnoPage);
    }
    return generateResponse(request, createdAnnoPage, profiles, HttpStatus.OK);
  }

  @Tag(name = "Replace fulltext", description = "Replaces existing fulltext for a media resource with a new document")
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
      @RequestParam(value = "profile", required = false) String profileParam,
      @RequestBody(required = false) String content,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.UPDATE, request);
    }
    List<String> profiles = extractProfiles(profileParam);
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

    // TODO still need to decide the appropriate order of thing for updating deprecated annopage
    //  Till then if AnnoPage is deprecated, content is mandatory in the request to update resource
    // if existing AnnoPage is deprecated then - resource is deleted from the Resource Collection and DBRef for resource as well
    // hence we can not update the rights of the resource
    // User needs to send the annotation body for the deprecated AnnoPages update
    if (annoPage.isDeprecated() && StringUtils.isEmpty(content)) {
      throw new AnnoPageGoneException(String.format("/%s/%s/annopage/%s", datasetId, localId, pageId),
              lang, "Send content to update the deprecated Annopage");
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

    AnnoPage updatedAnnoPage = ftService.updateAnnoPage(annotationPreview, annoPage);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Replaced AnnoPage {}", updatedAnnoPage);
    }
    return generateResponse(request, updatedAnnoPage, profiles, HttpStatus.OK);
  }

  @Tag(name = "Deprecate fulltext", description = "Deprecates the full-text associated to a media resource\n")
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
      HttpServletRequest request, AnnoPage annoPage, List<String> profiles,
      HttpStatus status)
      throws SerializationException {

    AnnotationWrapper annotationWrapper = ftService.generateAnnoPageV2(annoPage, null, profiles.contains(PROFILE_TEXT));

    ZonedDateTime modified = dateToZonedUTC(annoPage.getModified());
    String requestVersion = REQUEST_VERSION_2;

    String eTag = CachingUtils.generateETag(modified, requestVersion + appSettings.getAppVersion(), true);

    org.springframework.http.HttpHeaders headers =
        generateHeaders(request, eTag, zonedDateTimeToString(modified));
    addContentTypeToResponseHeader(headers, requestVersion, false);
    // overwrite Allow header populated in CacheUtils.generateHeaders
    headers.set(HttpHeaders.ALLOW, getMethodsForRequestPattern(request, requestPathMethodService));

    return ResponseEntity.status(status)
        .headers(headers)
        .body(ftService.serialise(annotationWrapper));
  }

  private ResponseEntity<String> noContentResponse(HttpServletRequest request) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add(HttpHeaders.ALLOW, getMethodsForRequestPattern(request, requestPathMethodService));
    addContentTypeToResponseHeader(headers, REQUEST_VERSION_2, false);
    return ResponseEntity.noContent().headers(headers).build();
  }
}
