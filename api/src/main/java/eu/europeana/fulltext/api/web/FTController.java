package eu.europeana.fulltext.api.web;

import eu.europeana.fulltext.api.model.AnnotationWrapper;
import eu.europeana.fulltext.api.model.FTResource;
import eu.europeana.fulltext.api.model.JsonErrorResponse;
import eu.europeana.fulltext.api.service.CacheUtils;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.ResourceDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.SerializationException;
import eu.europeana.fulltext.entity.AnnoPage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.ZonedDateTime;

import static eu.europeana.fulltext.RequestUtils.*;
import static eu.europeana.fulltext.api.config.FTDefinitions.*;
import static eu.europeana.fulltext.api.service.CacheUtils.generateETag;
import static eu.europeana.fulltext.api.service.CacheUtils.generateSimpleETag;

/**
 * Rest controller that handles fulltext annotation page (annopage)- annotation- & resource requests
 *
 * @author Lúthien
 * Created on 27-02-2018
 * Note that the eTag for the Fulltext response is created from a concatenation of:
 * - datasetId + localId + pageId / AnnoId;
 * - modified date (toString()) of the fetched document (not for the resource)
 * - the requested IIIF version (2 or 3) (not for the resource); and the
 * - Fulltext API version as defined in the pom.xml
 */
@RestController
@RequestMapping("/presentation")
public class FTController {

    private static final Logger LOG = LogManager.getLogger(FTController.class);

    private FTService fts;

    public  FTController(FTService ftService) {
        this.fts = ftService;
    }

    /**
     * Handles fetching a page (resource) with all its annotations
     * @param datasetId    identifier of the AnnoPage's dataset
     * @param localId      identifier of the AnnoPage's record
     * @param pageId       identifier of the AnnoPage
     * @param versionParam requested IIIF output format (2|3)
     * @param profile      when value = 'text', resources are dereferenced
     * @throws SerializationException when serialising to Json fails
     * @return response in json format
     */
    @GetMapping(value = "/{datasetId}/{localId}/annopage/{pageId}", headers = ACCEPT_JSON)
    public ResponseEntity<String> annoPageJson(@PathVariable String datasetId,
                                               @PathVariable String localId,
                                               @PathVariable String pageId,
                                               @RequestParam(value = "format", required = false) String versionParam,
                                               @RequestParam(value = "profile", required = false) String profile,
                                               HttpServletRequest request) throws SerializationException {
        return annoPage(datasetId, localId, pageId, versionParam, profile, request, true);
    }

    /**
     * Handles fetching a page (resource) with all its annotations
     * @param datasetId    identifier of the AnnoPage's dataset
     * @param localId      identifier of the AnnoPage's record
     * @param pageId       identifier of the AnnoPage
     * @param versionParam requested IIIF output format (2|3)
     * @param profile      when value = 'text', resources are dereferenced
     * @throws SerializationException when serialising to JsonLd fails
     * @return response in json-ld format
     */
    @GetMapping(value = "/{datasetId}/{localId}/annopage/{pageId}", headers = ACCEPT_JSONLD)
    public ResponseEntity<String> annoPageJsonLd(@PathVariable String datasetId,
                                                 @PathVariable String localId,
                                                 @PathVariable String pageId,
                                                 @RequestParam(value = "format", required = false) String versionParam,
                                                 @RequestParam(value = "profile", required = false) String profile,
                                                 HttpServletRequest request) throws SerializationException {
        return annoPage(datasetId, localId, pageId, versionParam, profile, request, false);
    }

    private ResponseEntity<String> annoPage(String datasetId,
                                            String localId,
                                            String pageId,
                                            String versionParam,
                                            String profile,
                                            HttpServletRequest request,
                                            boolean isJson) throws SerializationException {
        LOG.debug("Retrieve Annopage: {}/{}/{}", datasetId, localId, pageId);
        String requestVersion = getRequestVersion(request, versionParam);
        if (ACCEPT_VERSION_INVALID.equals(requestVersion)){
            return new ResponseEntity<>(ACCEPT_VERSION_INVALID, HttpStatus.NOT_ACCEPTABLE);
        }

        AnnotationWrapper annotationPage;
        HttpHeaders headers;
        try {
            AnnoPage                annoPage = fts.fetchAnnoPage(datasetId, localId, pageId);
            ZonedDateTime           modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
            String                  eTag     = generateETag(datasetId + localId + pageId,
                                                            modified,
                                                            requestVersion + fts.getSettings().getAppVersion(),
                                                            true);
            ResponseEntity<String>  cached   = CacheUtils.checkCached(request, modified, eTag);
            if (null != cached){
                return cached;
            }

            headers = CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));
            addContentTypeToResponseHeader(headers, requestVersion, isJson);
            if ("3".equalsIgnoreCase(requestVersion)) {
                annotationPage = fts.generateAnnoPageV3(annoPage, StringUtils.equalsAnyIgnoreCase(profile, PROFILE_TEXT));
            } else {
                annotationPage = fts.generateAnnoPageV2(annoPage, StringUtils.equalsAnyIgnoreCase(profile, PROFILE_TEXT));
            }

        } catch (AnnoPageDoesNotExistException e) {
            LOG.debug(e);
            return new ResponseEntity<>(fts.serialise(new JsonErrorResponse(e.getMessage())),
                    HttpStatus.NOT_FOUND);
        }
        if (isJson) {
            annotationPage.setContext(null);
        }
        return new ResponseEntity<>(fts.serialise(annotationPage),
                headers,
                HttpStatus.OK);
    }

    /**
     * HTTP Head endpoint to check for existence of an AnnoPage
     * @param datasetId identifier of the AnnoPage's dataset
     * @param localId  identifier of the AnnoPage's record
     * @param pageId    identifier of the AnnoPage
     * @return ResponseEntity
     */
    @RequestMapping(value    = {"/{datasetId}/{localId}/annopage/{pageId}"},
                    method   = RequestMethod.HEAD)
    public ResponseEntity annoPageHeadExists(@PathVariable String datasetId,
                                             @PathVariable String localId,
                                             @PathVariable String pageId) {
        if (fts.doesAnnoPageExist(datasetId, localId, pageId)){
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Handles fetching a single annotation
     * @param datasetId    identifier of the dataset that contains the AnnoPage with this Annotation
     * @param localId      identifier of the record that contains the AnnoPage with this Annotation
     * @param annoID       identifier of the Annotation
     * @param versionParam requested IIIF output format (2|3)
     * @throws SerializationException when serialising to Json fails
     * @return response in json format
     */
    @GetMapping(value = "/{datasetId}/{localId}/anno/{annoID}", headers = ACCEPT_JSON)
    public ResponseEntity<String> annotationJson(@PathVariable String datasetId,
                                                 @PathVariable String localId,
                                                 @PathVariable String annoID,
                                                 @RequestParam(value = "format", required = false) String versionParam,
                                                 HttpServletRequest request) throws SerializationException {
        return annotation(datasetId, localId, annoID, versionParam, request, true);
    }

    /**
     * Handles fetching a single annotation
     * @param datasetId    identifier of the dataset that contains the AnnoPage with this Annotation
     * @param localId      identifier of the record that contains the AnnoPage with this Annotation
     * @param annoID       identifier of the Annotation
     * @param versionParam requested IIIF output format (2|3)
     * @throws SerializationException when serialising to JsonLd fails
     * @return response in json-ld format
     */
    @GetMapping(value = "/{datasetId}/{localId}/anno/{annoID}", headers = ACCEPT_JSONLD)
    public ResponseEntity<String> annotationJsonLd(@PathVariable String datasetId,
                                                   @PathVariable String localId,
                                                   @PathVariable String annoID,
                                                   @RequestParam(value = "format", required = false) String versionParam,
                                                   HttpServletRequest request) throws SerializationException {
        return annotation(datasetId, localId, annoID, versionParam, request, false);
    }

    private ResponseEntity<String> annotation(String datasetId,
                                              String localId,
                                              String annoID,
                                              String versionParam,
                                              HttpServletRequest request,
                                              boolean isJson) throws SerializationException {
        LOG.debug("Retrieve Annotation: {}/{}/{}", datasetId, localId, annoID);
        String requestVersion = getRequestVersion(request, versionParam);

        if (ACCEPT_VERSION_INVALID.equals(requestVersion)){
            return new ResponseEntity<>(ACCEPT_VERSION_INVALID, HttpStatus.NOT_ACCEPTABLE);
        }

        HttpHeaders headers;
        AnnotationWrapper annotation;
        try {
            AnnoPage                annoPage = fts.fetchAPAnnotation(datasetId, localId, annoID);
            ZonedDateTime           modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
            String                  eTag     = generateETag(datasetId + localId + annoID,
                                                            modified,
                                                            requestVersion + fts.getSettings().getAppVersion(),
                                                            true);
            ResponseEntity<String>  cached   = CacheUtils.checkCached(request, modified, eTag);
            if (cached != null) {
                return cached;
            }

            headers = CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));
            addContentTypeToResponseHeader(headers, requestVersion, isJson);

            if ("3".equalsIgnoreCase(requestVersion)) {
                annotation = fts.generateAnnotationV3(annoPage, annoID);
            } else {
                annotation = fts.generateAnnotationV2(annoPage, annoID);
            }
        } catch (AnnoPageDoesNotExistException e) {
            LOG.debug(e);
            return new ResponseEntity<>(fts.serialise(new JsonErrorResponse(e.getMessage())),
                                        HttpStatus.NOT_FOUND);
        }
        if (isJson){
            annotation.setContext(null);
        }
        return new ResponseEntity<>(fts.serialise(annotation),
                headers,
                HttpStatus.OK);
    }

    /**
     * Handles fetching a Resource in JSON-LD format
     * @param datasetId identifier of the dataset that contains the Annopage that refers to the Resource
     * @param localId   identifier of the record that contains the Annopage that refers to the Resource
     * @param resId     identifier of the Resource
     * @throws SerializationException when serialising to JsonLd fails
     * @return response in json-ld format
     */
    @GetMapping(value = "/{datasetId}/{localId}/{resId}", headers = ACCEPT_JSONLD,
                produces = MEDIA_TYPE_JSONLD + ";" + UTF_8)
    public ResponseEntity<String> resourceJsonLd(@PathVariable String datasetId,
                                                 @PathVariable String localId,
                                                 @PathVariable String resId,
                                                 HttpServletRequest request) throws SerializationException {
        return resource(datasetId, localId, resId, request, false);
    }

    /**
     * Handles fetching a Resource in JSON format
     * @param datasetId identifier of the dataset that contains the Annopage that refers to the Resource
     * @param localId   identifier of the record that contains the Annopage that refers to the Resource
     * @param resId     identifier of the Resource
     * @throws SerializationException when serialising to Json fails
     * @return response in json format
     */
    @GetMapping(value = "/{datasetId}/{localId}/{resId}", headers = ACCEPT_JSON,
                produces = MEDIA_TYPE_JSON + ";" + UTF_8)
    public ResponseEntity<String> resourceJson(@PathVariable String datasetId,
                                               @PathVariable String localId,
                                               @PathVariable String resId,
                                               HttpServletRequest request) throws SerializationException {
        return resource(datasetId, localId, resId, request, true);
    }

    private ResponseEntity<String> resource(String datasetId,
                                            String localId,
                                            String resId,
                                            HttpServletRequest request, boolean isJson) throws SerializationException {
        LOG.debug("Retrieve Resource: {}/{}/{}", datasetId, localId, resId);
        HttpHeaders headers;
        FTResource  resource;
        try {
            resource                = fts.fetchFTResource(datasetId, localId, resId);
            ZonedDateTime modified  = CacheUtils.januarificator();
            String eTag             = generateSimpleETag(datasetId + localId + resId +
                                                         resource.getLanguage() +
                                                         resource.getValue() +
                                                         fts.getSettings().getAppVersion(),
                    true);
            ResponseEntity<String> cached = CacheUtils.checkCached(request, modified, eTag);
            if (cached != null) {
                return cached;
            }

            headers = CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));
            headers.add(CONTENT_TYPE, (isJson ? MEDIA_TYPE_JSON : MEDIA_TYPE_JSONLD) + ";" + UTF_8);

        } catch (ResourceDoesNotExistException e) {
            LOG.debug(e);
            return new ResponseEntity<>(fts.serialise(new JsonErrorResponse(e.getMessage())),
                    HttpStatus.NOT_FOUND);
        }

        if (isJson) {
            resource.setContext(null);
        }
        return new ResponseEntity<>(fts.serialise(resource),
                headers,
                HttpStatus.OK);
    }

    // --- utils ---

    private void addContentTypeToResponseHeader(HttpHeaders headers, String version, boolean isJson) {
        if ("3".equalsIgnoreCase(version)) {
            if (isJson) {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSON_V3);
            } else {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSONLD_V3);
            }
        } else {
            if (isJson) {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSON_V2);
            } else {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSONLD_V2);
            }
        }
    }

    /**
     * For testing retrieving the version from the pom file
     *
     * @return String representing the API version
     * @throws SerializationException when serialising to a String fails
     */
    @GetMapping(value = "/showversion")
    public ResponseEntity<String> showVersion() throws SerializationException {
        String response = "The version of this API is: " + fts.getSettings().getAppVersion();
        return new ResponseEntity<>(fts.serialise(response), HttpStatus.I_AM_A_TEAPOT);
    }
}
