package eu.europeana.fulltext.api.web;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.model.AnnotationWrapper;
import eu.europeana.fulltext.api.model.FTResource;
import eu.europeana.fulltext.api.model.info.SummaryManifest;
import eu.europeana.fulltext.api.service.CacheUtils;
import eu.europeana.fulltext.api.service.ControllerUtils;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.InvalidVersionException;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.iiif.AcceptUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_ANNOTATION_TYPES;
import static eu.europeana.fulltext.api.config.FTDefinitions.TEXT_ANNOTATION_TYPES;
import static eu.europeana.fulltext.api.service.CacheUtils.generateETag;
import static eu.europeana.fulltext.api.service.CacheUtils.generateSimpleETag;
import static eu.europeana.fulltext.util.RequestUtils.PROFILE_TEXT;
import static eu.europeana.fulltext.util.RequestUtils.extractProfiles;
import static eu.europeana.iiif.AcceptUtils.*;

/**
 * Rest controller that handles fulltext annotation page (annopage)- annotation- & resource requests
 *
 * @author Lúthien Created on 27-02-2018 Note that the eTag for the Fulltext response is created from a concatenation
 * of: - datasetId + localId + pageId / AnnoId; - modified date (toString()) of the fetched document (not for the
 * resource) - the requested IIIF version (2 or 3) (not for the resource); and the - Fulltext API version as defined in
 * the pom.xml
 */
@RestController
@Tag(name = "Full-text item", description = "Retrieve a page with annotations, an individual annotation or a full-text")
public class FTRetrievalController {

    private static final Set<AnnotationType> ALLOWED_ANNOTATION_TYPES = EnumSet.of(AnnotationType.PAGE,
        AnnotationType.BLOCK,
        AnnotationType.LINE,
        AnnotationType.WORD,
        AnnotationType.MEDIA,
        AnnotationType.CAPTION);

    private static final Logger LOG = LogManager.getLogger(FTRetrievalController.class);

    private final FTService fts;

    public FTRetrievalController(FTService ftService) {
        this.fts = ftService;
    }

    /**
     * Lists available AnnoPages for this record
     *
     * @param datasetId identifier of the AnnoPage's dataset
     * @param localId   identifier of the AnnoPage's record
     * @return result String containing requested info
     * @throws EuropeanaApiException when serialising to Json fails
     */
    @Tag(name="All Annopages (JSON)", description = "Lists available Annotation Pages for a given EuropeanaID (dataset + localID)")
    @GetMapping(value = "/presentation/{datasetId}/{localId}/annopage", headers = ACCEPT_JSON)
    public ResponseEntity<String> annoPageInfoJson(
        @PathVariable String datasetId,
        @PathVariable String localId,
        HttpServletRequest request) throws EuropeanaApiException {
        return getAnnoPageInfo(datasetId, localId, request, true);
    }

    /**
     * Lists available AnnoPages for this record
     *
     * @param dsId identifier of the AnnoPage's dataset
     * @param lcId identifier of the AnnoPage's record
     * @return result String containing requested info
     * @throws EuropeanaApiException when serialising to Json fails
     */
    @Tag(name = "All Annopages (JSON-LD)", description = "Lists available Annotation Pages for a given EuropeanaID (dataset + localID)")
    @GetMapping(value = "/presentation/{dsId}/{lcId}/annopage", headers = ACCEPT_JSONLD)
    public ResponseEntity<String> annoPageInfoJsonLd(
            @PathVariable String dsId,
            @PathVariable String lcId,
            HttpServletRequest request) throws EuropeanaApiException {
        return getAnnoPageInfo(dsId, lcId, request, false);
    }


    private ResponseEntity<String> getAnnoPageInfo(String datasetId, String localId, HttpServletRequest request, boolean isJson)
        throws EuropeanaApiException {
        AnnoPage annoPage = fts.getSingleAnnoPage(datasetId, localId);
        ZonedDateTime modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
        String eTag = generateETag(datasetId + localId,
            modified,
            fts.getSettings().getAppVersion(),
            true);
        ResponseEntity<String> cached = CacheUtils.checkCached(request, modified, eTag);
        if (null != cached) {
            LOG.info("Returning cached object for {}, {}", datasetId, localId);
            return cached;
        }
        HttpHeaders headers = CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));
        // response generated by annoPageInfo is for version 3
        AcceptUtils.addContentTypeToResponseHeader(headers, REQUEST_VERSION_3, isJson);
        SummaryManifest apInfo = fts.collectionAnnoPageInfo(datasetId, localId);
        apInfo.setTextGranularity(setSummaryTextGranularity(annoPage.getAns().get(0).isMedia()));
        return new ResponseEntity<>(fts.serialise(apInfo), headers, HttpStatus.OK);
    }

    /**
     * Handles fetching the Annotation page (aka AnnoPage) with embedded annotations with boolean 'orig' == true
     *
     * @param datasetId       identifier of the AnnoPage's dataset
     * @param localId         identifier of the AnnoPage's record
     * @param pageId          identifier of the AnnoPage
     * @param lang            optional, in which language should the AnnoPage be
     * @param versionParam    optional, requested IIIF output format (2|3)
     * @param profile         optional, when value = 'text', resources are dereferenced
     * @param textGranularity optional, types of annotations that should be included (e.g. Block, Line, Page)
     * @return response in json format
     * @throws EuropeanaApiException when serialising to Json fails or an invalid parameter value is provided
     */
    @Tag(name = "Annopage (JSON)", description = "Retrieve a page with annotations")
    @GetMapping(value = "/presentation/{datasetId}/{localId}/annopage/{pageId}", headers = ACCEPT_JSON)
    public ResponseEntity<String> annoPageJson(
        @PathVariable String datasetId,
        @PathVariable String localId,
        @PathVariable String pageId,
        @RequestParam(value = "lang", required = false) String lang,
        @RequestParam(value = "format", required = false) String versionParam,
        @RequestParam(value = "profile", required = false) String profile,
        @RequestParam(value = "textGranularity", required = false) String textGranularity,
        HttpServletRequest request) throws EuropeanaApiException {
        return annoPage(datasetId, localId, pageId, lang, versionParam, profile, textGranularity, request, true);
    }

    /**
     * Handles fetching a page (resource) with all its annotations
     *
     * @param dsId       identifier of the AnnoPage's dataset
     * @param lcId         identifier of the AnnoPage's record
     * @param pgId          identifier of the AnnoPage
     * @param lang            optional, in which language should the AnnoPage be
     * @param versionParam    optional, requested IIIF output format (2|3)
     * @param profile         optional, when value = 'text', resources are dereferenced
     * @param textGranularity optional, specifies what annotations should be returned
     * @return response in json-ld format
     * @throws EuropeanaApiException when serialising to JsonLd fails or an invalid parameter value is provided
     */
    @Tag(name = "Annopage (JSON-LD)", description = "Retrieve a page with annotations")
    @GetMapping(value = "/presentation/{dsId}/{lcId}/annopage/{pgId}",
            headers = ACCEPT_JSONLD)
    public ResponseEntity<String> annoPageJsonLd(
        @PathVariable String dsId,
        @PathVariable String lcId,
        @PathVariable String pgId,
        @RequestParam(value = "lang", required = false) String lang,
        @RequestParam(value = "format", required = false) String versionParam,
        @RequestParam(value = "profile", required = false) String profile,
        @RequestParam(value = "textGranularity", required = false) String textGranularity,
        HttpServletRequest request) throws EuropeanaApiException {
        return annoPage(dsId, lcId, pgId, lang, versionParam, profile, textGranularity, request, false);
    }

    private ResponseEntity<String> annoPage(
        String datasetId,
        String localId,
        String pageId,
        String lang,
        String versionParam,
        String profileParam,
        String textGranularity,
        HttpServletRequest request,
        boolean isJson) throws EuropeanaApiException {
        LOG.debug("Retrieve Annopage: {}/{}/{} with language {}", datasetId, localId, pageId, lang);
        // validate the format
        String requestVersion = AcceptUtils.getRequestVersion(request, versionParam);
        if (StringUtils.isEmpty(requestVersion)) {
            throw new InvalidVersionException(ACCEPT_VERSION_INVALID);
        }
        AnnotationWrapper annotationPage;

        List<AnnotationType> textGranValues = ControllerUtils.validateTextGranularity(textGranularity,
            ALLOWED_ANNOTATION_TYPES);
        AnnoPage annoPage = fts.fetchAnnoPage(datasetId, localId, pageId, textGranValues, lang);
        ZonedDateTime modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
        String eTag = CacheUtils.generateETag(datasetId + localId + pageId,
            modified,
            requestVersion + fts.getSettings().getAppVersion(),
            true);
        ResponseEntity<String> cached = CacheUtils.checkCached(request, modified, eTag);
        if (null != cached) {
            return cached;
        }

        HttpHeaders headers = CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));
        AcceptUtils.addContentTypeToResponseHeader(headers, requestVersion, isJson);

        List<String> profiles = extractProfiles(profileParam);

        if ("3".equalsIgnoreCase(requestVersion)) {
            annotationPage = fts.generateAnnoPageV3(annoPage, textGranValues, profiles.contains(PROFILE_TEXT));
        } else {
            annotationPage = fts.generateAnnoPageV2(annoPage, textGranValues, profiles.contains(PROFILE_TEXT));
        }

        if (isJson) {
            annotationPage.setContext(null);
        }
        return new ResponseEntity<>(fts.serialise(annotationPage), headers, HttpStatus.OK);
    }

    /**
     * HTTP Head endpoint to check for existence of an AnnoPage
     *
     * @param datasetId    identifier of the AnnoPage's dataset
     * @param localId      identifier of the AnnoPage's record
     * @param pageId       identifier of the AnnoPage
     * @param lang         optional, in which language should the AnnoPage be
     * @param versionParam optional, requested IIIF output format (2|3)
     * @return ResponseEntity
     */
    @Tag(name = "Annopage exists (JSON)", description = "Check if a page with annotations exists")
    @RequestMapping(value = {"/presentation/{datasetId}/{localId}/annopage/{pageId}"},
        method = RequestMethod.HEAD,
        headers = ACCEPT_JSON)
    public ResponseEntity annoPageHeadExistsJson(
        @PathVariable String datasetId,
        @PathVariable String localId,
        @PathVariable String pageId,
        @RequestParam(value = "lang", required = false) String lang,
        @RequestParam(value = "format", required = false) String versionParam,
        HttpServletRequest request) throws EuropeanaApiException {
        return getAnnoPageHead(datasetId, localId, pageId, lang, versionParam, true, request);
    }

    /**
     * HTTP Head endpoint to check for existence of an AnnoPage
     *
     * @param dsId    identifier of the AnnoPage's dataset
     * @param lcId      identifier of the AnnoPage's record
     * @param pgId       identifier of the AnnoPage
     * @param lang         optional, in which language should the AnnoPage be
     * @param versionParam optional, requested IIIF output format (2|3
     * @return ResponseEntity
     */
    @Tag(name = "Annopage exists (JSON-LD)", description = "Check if a page with annotations exists")
    @RequestMapping(value = {"/presentation/{dsId}/{lcId}/annopage/{pgId}"},
        method = RequestMethod.HEAD,
        headers = ACCEPT_JSONLD)
    public ResponseEntity annoPageHeadExistsJsonld(
        @PathVariable String dsId,
        @PathVariable String lcId,
        @PathVariable String pgId,
        @RequestParam(value = "lang", required = false) String lang,
        @RequestParam(value = "format", required = false) String versionParam,
        HttpServletRequest request) throws EuropeanaApiException {
        return getAnnoPageHead(dsId, lcId, pgId, lang, versionParam, false, request);
    }

    private ResponseEntity getAnnoPageHead(
        String datasetId,
        String localId,
        String pageId,
        String lang,
        String versionParam,
        boolean isJson,
        HttpServletRequest request) throws InvalidVersionException {
        // validate the format
        String requestVersion = AcceptUtils.getRequestVersion(request, versionParam);
        if (StringUtils.isEmpty(requestVersion)) {
            throw new InvalidVersionException(ACCEPT_VERSION_INVALID);
        }
        HttpHeaders headers = new HttpHeaders();
        AcceptUtils.addContentTypeToResponseHeader(headers, requestVersion, isJson);
        if (fts.doesAnnoPageExist(datasetId, localId, pageId, lang, false)) {
            return new ResponseEntity<>(headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(headers, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Handles fetching a single annotation
     *
     * @param datasetId    identifier of the dataset that contains the AnnoPage with this Annotation
     * @param localId      identifier of the record that contains the AnnoPage with this Annotation
     * @param annoID       identifier of the Annotation
     * @param versionParam requested IIIF output format (2|3)
     * @return response in json format
     * @throws EuropeanaApiException when serialising to Json fails
     */
    @Tag(name = "Annotation (JSON)", description = "Retrieve a single annotation")
    @GetMapping(value = "/presentation/{datasetId}/{localId}/anno/{annoID}", headers = ACCEPT_JSON)
    public ResponseEntity<String> annotationJson(
        @PathVariable String datasetId,
        @PathVariable String localId,
        @PathVariable String annoID,
        @RequestParam(value = "format", required = false) String versionParam,
        HttpServletRequest request) throws EuropeanaApiException {
        return annotation(datasetId, localId, annoID, versionParam, request, true);
    }

    /**
     * Handles fetching a single annotation
     *
     * @param dsId    identifier of the dataset that contains the AnnoPage with this Annotation
     * @param lcId      identifier of the record that contains the AnnoPage with this Annotation
     * @param annoID       identifier of the Annotation
     * @param versionParam requested IIIF output format (2|3)
     * @return response in json-ld format
     * @throws EuropeanaApiException when serialising to JsonLd fails
     */
    @Tag(name = "Annotation (JSON-LD)", description = "Retrieve a single annotation")
    @GetMapping(value = "/presentation/{dsId}/{lcId}/anno/{annoID}", headers = ACCEPT_JSONLD)
    public ResponseEntity<String> annotationJsonLd(
        @PathVariable String dsId,
        @PathVariable String lcId,
        @PathVariable String annoID,
        @RequestParam(value = "format", required = false) String versionParam,
        HttpServletRequest request) throws EuropeanaApiException {
        return annotation(dsId, lcId, annoID, versionParam, request, false);
    }

    private ResponseEntity<String> annotation(
        String datasetId,
        String localId,
        String annoID,
        String versionParam,
        HttpServletRequest request,
        boolean isJson) throws EuropeanaApiException {
        LOG.debug("Retrieve Annotation: {}/{}/{}", datasetId, localId, annoID);
        // validate the format
        String requestVersion = AcceptUtils.getRequestVersion(request, versionParam);
        if (StringUtils.isEmpty(requestVersion)) {
            throw new InvalidVersionException(ACCEPT_VERSION_INVALID);
        }

        HttpHeaders headers;
        AnnotationWrapper annotation;
        AnnoPage annoPage = fts.fetchAPAnnotation(datasetId, localId, annoID);
        ZonedDateTime modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
        String eTag = generateETag(datasetId + localId + annoID,
            modified,
            requestVersion + fts.getSettings().getAppVersion(),
            true);
        ResponseEntity<String> cached = CacheUtils.checkCached(request, modified, eTag);
        if (cached != null) {
            return cached;
        }

        headers = CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));
        AcceptUtils.addContentTypeToResponseHeader(headers, requestVersion, isJson);

        if ("3".equalsIgnoreCase(requestVersion)) {
            annotation = fts.generateAnnotationV3(annoPage, annoID);
        } else {
            annotation = fts.generateAnnotationV2(annoPage, annoID);
        }

        if (isJson) {
            annotation.setContext(null);
        }
        return new ResponseEntity<>(fts.serialise(annotation), headers, HttpStatus.OK);
    }

    /**
     * Handles fetching a Resource in JSON-LD format
     *
     * @param datasetId identifier of the dataset that contains the Annopage that refers to the Resource
     * @param localId   identifier of the record that contains the Annopage that refers to the Resource
     * @param pageId     identifier of the Resource
     * @param lang       optional, in which language should the Resource be
     * @return response in json-ld format
     * @throws EuropeanaApiException when serialising to JsonLd fails
     */
    @Tag(name = "Fulltext resource (JSON-LD)", description = "Retrieve a full-text")
    @GetMapping(value = "/presentation/{datasetId}/{localId}/{pageId}",
        headers = ACCEPT_JSONLD,
        produces = MEDIA_TYPE_JSONLD + ';' + CHARSET_UTF_8)
    public ResponseEntity<String> resourceJsonLd(
        @PathVariable String datasetId,
        @PathVariable String localId,
        @PathVariable String pageId,
        @RequestParam(value = "lang", required = false) String lang,
        HttpServletRequest request) throws EuropeanaApiException {
        return resource(datasetId, localId, pageId, lang, request, false);
    }

    /**
     * Handles fetching a Resource in JSON format
     *
     * @param dsId identifier of the dataset that contains the Annopage that refers to the Resource
     * @param lcId   identifier of the record that contains the Annopage that refers to the Resource
     * @param pgId     identifier of the Resource
     * @return response in json format
     * @throws EuropeanaApiException when serialising to Json fails
     */
    @Tag(name = "Fulltext resource (JSON)", description = "Retrieve a full-text")
    @GetMapping(value = "/presentation/{dsId}/{lcId}/{pgId}",
        headers = ACCEPT_JSON,
        produces = MEDIA_TYPE_JSON + ';' + CHARSET_UTF_8)
    public ResponseEntity<String> resourceJson(
        @PathVariable String dsId,
        @PathVariable String lcId,
        @PathVariable String pgId,
        @RequestParam(value = "lang", required = false) String lang,
        HttpServletRequest request) throws EuropeanaApiException {
        return resource(dsId, lcId, pgId, lang, request, true);
    }

    private ResponseEntity<String> resource(
        String datasetId, String localId, String pageId, String lang,
        HttpServletRequest request, boolean isJson) throws
        EuropeanaApiException {
        LOG.debug("Retrieve Resource: {}/{}/{}", datasetId, localId, pageId);
        HttpHeaders headers;
        FTResource resource;

        resource = fts.fetchFTResource(datasetId, localId, pageId, lang);
        ZonedDateTime modified = CacheUtils.januarificator();
        String eTag = generateSimpleETag(datasetId
            + localId
            + pageId
            + resource.getLanguage()
            + resource.getValue()
            + fts.getSettings().getAppVersion(), true);
        ResponseEntity<String> cached = CacheUtils.checkCached(request, modified, eTag);
        if (cached != null) {
            return cached;
        }

        headers = CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));
        headers.add(CONTENT_TYPE, (isJson ? MEDIA_TYPE_JSON : MEDIA_TYPE_JSONLD) + ";" + CHARSET_UTF_8);

        if (isJson) {
            resource.setContext(null);
        }
        return new ResponseEntity<>(fts.serialise(resource), headers, HttpStatus.OK);
    }


    private static String[] setSummaryTextGranularity(boolean isMedia){
        List<AnnotationType> granularities;
        if (isMedia){
            granularities = List.copyOf(MEDIA_ANNOTATION_TYPES);
        } else {
            granularities = List.copyOf(TEXT_ANNOTATION_TYPES);
        }
        List<String> textGranularity = new ArrayList<>();
        for (AnnotationType granularity : granularities){
            textGranularity.add(granularity.getLowerCaseName());
        }
        return textGranularity.toArray(new String[0]);
    }

}
