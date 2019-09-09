package eu.europeana.fulltext.api.web;

import eu.europeana.fulltext.api.model.AnnotationWrapper;
import eu.europeana.fulltext.api.model.FullTextResource;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.europeana.fulltext.api.config.FTDefinitions.*;
import static eu.europeana.fulltext.api.service.CacheUtils.generateETag;
import static eu.europeana.fulltext.api.service.CacheUtils.generateSimpleETag;

/**
 * Rest controller that handles fulltext annotation page (annopage)- annotation- & resource requests
 * @author Lúthien
 * Created on 27-02-2018
 * Note that the eTag for the Fulltext response is created from a concatenation of:
 * - datasetId + recordId + pageId / AnnoId;
 * - modified date (toString()) of the fetched document (not for the Fulltext resource)
 * - the requested IIIF version (2 or 3) (not for the Fulltext resource); and the
 * - Fulltext API version as defined in the pom.xml
 */
@RestController
@RequestMapping("/presentation")
public class FTController {

    private static final Logger LOG                     = LogManager.getLogger(FTController.class);
    private static final String ACCEPT                  = "Accept";
    private static final String ACCEPT_JSON             = "Accept=" + MEDIA_TYPE_JSON;
    private static final String ACCEPT_JSONLD           = "Accept=" + MEDIA_TYPE_JSONLD;
    private static final String ACCEPT_VERSION_INVALID  = "Unknown profile or format version";
    private static final String CONTENT_TYPE            = "Content-Type";
    private static final Pattern acceptProfilePattern   = Pattern.compile("profile=\"(.*?)\"");

    private FTService fts;

    /**
     * This will set json-ld as the default type if there is no accept header specified or if it's *
     */
    @EnableWebMvc
    public class WebConfig implements WebMvcConfigurer {
        @Override
        public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
            configurer.defaultContentType(MediaType.valueOf(MEDIA_TYPE_JSONLD));
        }
    }


    public  FTController(FTService ftService) {
        this.fts = ftService;
    }

    /**
     * Handles fetching a page (resource) with all its annotations
     * @return response in json format
     */
    @GetMapping(value = "/{datasetId}/{recordId}/annopage/{pageId}", headers = ACCEPT_JSON)
    public ResponseEntity<String> annoPageJson(@PathVariable String datasetId,
                                               @PathVariable String recordId,
                                               @PathVariable String pageId,
                                               @RequestParam(value = "format", required = false) String versionParam,
                                               HttpServletRequest request) throws SerializationException {
        return annoPage(datasetId, recordId, pageId, versionParam, request, true);
    }

    /**
     * Handles fetching a page (resource) with all its annotations
     * @return response in json-ld format
     */
    @GetMapping(value = "/{datasetId}/{recordId}/annopage/{pageId}", headers = ACCEPT_JSONLD)
    public ResponseEntity<String> annoPageJsonLd(@PathVariable String datasetId,
                                                 @PathVariable String recordId,
                                                 @PathVariable String pageId,
                                                 @RequestParam(value = "format", required = false) String versionParam,
                                                 HttpServletRequest request) throws SerializationException {
        return annoPage(datasetId, recordId, pageId, versionParam, request, false);
    }

    private ResponseEntity<String> annoPage(String datasetId,
                                            String recordId,
                                            String pageId,
                                            String versionParam,
                                            HttpServletRequest request,
                                            boolean isJson) throws SerializationException {
        LOG.debug("Retrieve Annopage: " + datasetId + "/" + recordId + "/" + pageId);
        String requestVersion = getRequestVersion(request, versionParam);
        if (ACCEPT_VERSION_INVALID.equals(requestVersion)){
            return new ResponseEntity<>(ACCEPT_VERSION_INVALID, HttpStatus.NOT_ACCEPTABLE);
        }

        AnnotationWrapper annotationPage;
        HttpHeaders headers;
        try {
            AnnoPage                annoPage = fts.fetchAnnoPage(datasetId, recordId, pageId);
            ZonedDateTime           modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
            String                  eTag     = generateETag(datasetId + recordId + pageId,
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
                annotationPage = fts.generateAnnoPageV3(annoPage);
            } else {
                annotationPage = fts.generateAnnoPageV2(annoPage);
            }

        } catch (AnnoPageDoesNotExistException e) {
            LOG.debug(e.getMessage());
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
     * @return ResponseEntity
     */
    @RequestMapping(value    = {"/{datasetId}/{recordId}/annopage/{pageId}"},
                    method   = RequestMethod.HEAD)
    public ResponseEntity annoPageHeadExists(@PathVariable String datasetId,
                                             @PathVariable String recordId,
                                             @PathVariable String pageId) {
        if (fts.doesAnnoPageExist(datasetId, recordId, pageId)){
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Handles fetching a single annotation
     * @return response in json format
     */
    @GetMapping(value = "/{datasetId}/{recordId}/anno/{annoID}", headers = ACCEPT_JSON)
    public ResponseEntity<String> annotationJson(@PathVariable String datasetId,
                                                 @PathVariable String recordId,
                                                 @PathVariable String annoID,
                                                 @RequestParam(value = "format", required = false) String versionParam,
                                                 HttpServletRequest request) throws SerializationException {
        return annotation(datasetId, recordId, annoID, versionParam, request, true);
    }

    /**
     * Handles fetching a single annotation
     * @return response in json-ld format
     */
    @GetMapping(value = "/{datasetId}/{recordId}/anno/{annoID}", headers = ACCEPT_JSONLD)
    public ResponseEntity<String> annotationJsonLd(@PathVariable String datasetId,
                                                   @PathVariable String recordId,
                                                   @PathVariable String annoID,
                                                   @RequestParam(value = "format", required = false) String versionParam,
                                                   HttpServletRequest request) throws SerializationException {
        return annotation(datasetId, recordId, annoID, versionParam, request, false);
    }

    public ResponseEntity<String> annotation(String datasetId,
                                             String recordId,
                                             String annoID,
                                             String versionParam,
                                             HttpServletRequest request,
                                             boolean isJson) throws SerializationException {
        LOG.debug("Retrieve Annotation: " + datasetId + "/" + recordId + "/" + annoID);
        String requestVersion = getRequestVersion(request, versionParam);

        if (ACCEPT_VERSION_INVALID.equals(requestVersion)){
            return new ResponseEntity<>(ACCEPT_VERSION_INVALID, HttpStatus.NOT_ACCEPTABLE);
        }

        HttpHeaders headers;
        AnnotationWrapper annotation;
        try {
            AnnoPage                annoPage = fts.fetchAPAnnotation(datasetId, recordId, annoID);
            ZonedDateTime           modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
            String                  eTag     = generateETag(datasetId + recordId + annoID,
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
            LOG.debug(e.getMessage());
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
     * Handles fetching a Fulltext Resource.
     * @return response in json-ld format
     */
    @GetMapping(value = "/{datasetId}/{recordId}/{resId}", headers = ACCEPT_JSONLD,
                produces = MEDIA_TYPE_JSONLD + ";" + UTF_8)
    public ResponseEntity<String> resourceJsonLd(@PathVariable String datasetId,
                                                 @PathVariable String recordId,
                                                 @PathVariable String resId,
                                                 HttpServletRequest request) throws SerializationException {
        return resource(datasetId, recordId, resId, request, false);
    }

    /**
     * Handles fetching a Fulltext Resource
     * @return response in json format
     */
    @GetMapping(value = "/{datasetId}/{recordId}/{resId}", headers = ACCEPT_JSON,
                produces = MEDIA_TYPE_JSON + ";" + UTF_8)
    public ResponseEntity<String> resourceJson(@PathVariable String datasetId,
                                               @PathVariable String recordId,
                                               @PathVariable String resId,
                                               HttpServletRequest request) throws SerializationException {
        return resource(datasetId, recordId, resId, request, true);
    }

    private ResponseEntity<String> resource(String datasetId,
                                            String recordId,
                                            String resId,
                                            HttpServletRequest request, boolean isJson) throws SerializationException {
        LOG.debug("Retrieve Resource: " + datasetId + "/" + recordId + "/" + resId);
        HttpHeaders headers;
        FullTextResource resource;
        try {
            resource                = fts.fetchFullTextResource(datasetId, recordId, resId);
            ZonedDateTime modified  = CacheUtils.januarificator();
            String eTag             = generateSimpleETag(datasetId + recordId + resId +
                                                         resource.getLanguage() +
                                                         resource.getValue() +
                                                         fts.getSettings().getAppVersion(),
                                                         true);
            ResponseEntity<String> cached = CacheUtils.checkCached(request, modified, eTag);
            if (cached != null) {
                return cached;
            }

            headers = CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));
            headers.add(CONTENT_TYPE, isJson ? MEDIA_TYPE_JSON : MEDIA_TYPE_JSONLD);

        } catch (ResourceDoesNotExistException e) {
            LOG.debug(e.getMessage());
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
     * Retrieve the requested version from the accept header, or if not present from the format parameter. If nothing
     * is specified then 2 is returned as default
     * @return either version 2, 3 or ACCEPT_INVALID
     */
    private String getRequestVersion(HttpServletRequest request, String format) {
        String result = null;
        String accept = request.getHeader(ACCEPT);
        if (StringUtils.isNotEmpty(accept)) {
            Matcher m = acceptProfilePattern.matcher(accept);
            if (m.find()) { // found a Profile parameter in the Accept header
                String profiles = m.group(1);
                if (profiles.toLowerCase(Locale.getDefault()).contains(MEDIA_TYPE_IIIF_V3)) {
                    result = "3";
                } else if (profiles.toLowerCase(Locale.getDefault()).contains(MEDIA_TYPE_IIIF_V2)) {
                    result = "2";
                } else {
                    result = ACCEPT_VERSION_INVALID; // in case a Profile is found that matches neither version => HTTP 406
                }
            }
        }
        if (result == null) {
            // Request header is empty, or does not contain a Profile parameter
            if (StringUtils.isBlank(format)){
                result = "2";    // if format not given, fall back to default "2"
            } else if ("2".equals(format) || "3".equals(format)) {
                result = format; // else use the format parameter
            } else {
                result = ACCEPT_VERSION_INVALID;
            }

        }
        return result;
    }

    /**
     * For testing retrieving the version from the pom file
     */
    @GetMapping(value = "/showversion")
    public ResponseEntity<String> showVersion() throws SerializationException {
        String response = "The version of this API is: " + fts.getSettings().getAppVersion();
        return new ResponseEntity<>(fts.serialise(response), HttpStatus.I_AM_A_TEAPOT);
    }

}
