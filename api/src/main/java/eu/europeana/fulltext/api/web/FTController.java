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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.europeana.fulltext.api.config.FTDefinitions.*;
import static eu.europeana.fulltext.api.service.CacheUtils.IFMATCH;
import static eu.europeana.fulltext.api.service.CacheUtils.IFNONEMATCH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Rest controller that handles incoming fulltext requests
 * @author Lúthien
 * Created on 27-02-2018
 */
@RestController
@EnableWebMvc
@RequestMapping("/presentation")
public class FTController {

    private static final Logger LOG = LogManager.getLogger(FTController.class);
    private static final String ACCEPT = "Accept";
    private static final String CONTENTTYPE = "Content-Type";

    /* for parsing accept headers */
    private Pattern acceptProfilePattern = Pattern.compile("profile=\"(.*?)\"");

    private FTService fts;

    public  FTController(FTService ftService) {
        this.fts = ftService;
    }

    private String mediaTypeIIIFV2;
    private String mediaTypeIIIFV3;

    /**
     * Handles fetching a page (resource) with all its annotations
     * @return ResponseEntity
     */
    @GetMapping(value    = "/{datasetId}/{recordId}/annopage/{pageId}")
    public ResponseEntity<String> annopage(@PathVariable String datasetId,
                           @PathVariable String recordId,
                           @PathVariable String pageId,
                           @RequestParam(value = "format", required = false) String version,
                           HttpServletRequest request) throws SerializationException {
        LOG.debug("Retrieve Annopage: " + datasetId + "/" + recordId + "/" + pageId);
        boolean includeContext    = true;
        String acceptHeaderStatus = processAcceptHeader(request, version);
        HttpHeaders headers;
        if (StringUtils.equalsIgnoreCase(acceptHeaderStatus, "X")){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        } else {
            version = acceptHeaderStatus;
        }

        if (StringUtils.equalsIgnoreCase(acceptHeaderJsonOrLd(request), "JSON")){
            mediaTypeIIIFV2 = MEDIA_TYPE_IIIF_JSON_V2;
            mediaTypeIIIFV3 = MEDIA_TYPE_IIIF_JSON_V3;
            includeContext = false;
        } else {
            mediaTypeIIIFV2 = MEDIA_TYPE_IIIF_JSONLD_V2;
            mediaTypeIIIFV3 = MEDIA_TYPE_IIIF_JSONLD_V3;
        }

        AnnotationWrapper annotationPage;

        try {
            // first retrieve AnnoPage to do http caching processing
            AnnoPage                annoPage = fts.fetchAnnoPage(datasetId, recordId, pageId);
            ZonedDateTime           modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
            String                  eTag     = generateETag(datasetId + recordId + pageId, modified, version);
            ResponseEntity<String>  cached   = CacheUtils.checkCached(request, modified, eTag);

            if (null != cached){
                return cached;
            }

            headers = CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));

            if ("3".equalsIgnoreCase(version)) {
                annotationPage = fts.generateAnnoPageV3(annoPage);
                headers.add(CONTENTTYPE, mediaTypeIIIFV3);
            } else {
                annotationPage = fts.generateAnnoPageV2(annoPage);
                headers.add(CONTENTTYPE, mediaTypeIIIFV2);
            }
        } catch (AnnoPageDoesNotExistException e) {
            LOG.warn(e.getMessage());
            return new ResponseEntity<>(fts.serializeResource(new JsonErrorResponse(e.getMessage())),
                                        HttpStatus.NOT_FOUND);
        }
        if (!includeContext){
            annotationPage.setContext(null);
        }
        return new ResponseEntity<>(fts.serializeResource(annotationPage),
                                    headers,
                                    HttpStatus.OK);
    }

    /**
     * Handles fetching a single annotation
     * @return ResponseEntity
     */
    @GetMapping(value = "/{datasetId}/{recordId}/anno/{annoID}")
    public ResponseEntity<String> annotation(@PathVariable String datasetId,
                             @PathVariable String recordId,
                             @PathVariable String annoID,
                             @RequestParam(value = "format", required = false) String version,
                             HttpServletRequest request) throws SerializationException {
        LOG.debug("Retrieve Annotation: " + datasetId + "/" + recordId + "/" + annoID);
        boolean includeContext = true;
        HttpHeaders headers;
        String acceptHeaderStatus = processAcceptHeader(request, version);

        if (StringUtils.equalsIgnoreCase(acceptHeaderStatus, "X")){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        } else {
            version = acceptHeaderStatus;
        }

        if (StringUtils.equalsIgnoreCase(acceptHeaderJsonOrLd(request), "JSON")){
            mediaTypeIIIFV2 = MEDIA_TYPE_IIIF_JSON_V2;
            mediaTypeIIIFV3 = MEDIA_TYPE_IIIF_JSON_V3;
            includeContext = false;
        } else {
            mediaTypeIIIFV2 = MEDIA_TYPE_IIIF_JSONLD_V2;
            mediaTypeIIIFV3 = MEDIA_TYPE_IIIF_JSONLD_V3;
        }

        AnnotationWrapper annotation;
        try {
            // first retrieve AnnoPage containing this Annotation to do http caching processing
            AnnoPage                annoPage = fts.fetchAPAnnotation(datasetId, recordId, annoID);
            ZonedDateTime           modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
            String                  eTag     = generateETag(datasetId + recordId + annoID, modified, version);
            ResponseEntity<String>  cached   = CacheUtils.checkCached(request, modified, eTag);

            if (cached != null) {
                return cached;
            }

            headers = CacheUtils.generateHeaders(request, eTag, CacheUtils.zonedDateTimeToString(modified));

            if ("3".equalsIgnoreCase(version)) {
                annotation = fts.generateAnnotationV3(annoPage, annoID);
                headers.add(CONTENTTYPE, mediaTypeIIIFV3);
            } else {
                annotation = fts.generateAnnotationV2(annoPage, annoID);
                headers.add(CONTENTTYPE, mediaTypeIIIFV2);
            }
        } catch (AnnoPageDoesNotExistException e) {
            LOG.warn(e.getMessage());
            return new ResponseEntity<>(fts.serializeResource(new JsonErrorResponse(e.getMessage())),
                                        HttpStatus.NOT_FOUND);
        }
        if (!includeContext){
            annotation.setContext(null);
        }
        return new ResponseEntity<>(fts.serializeResource(annotation),
                                    headers,
                                    HttpStatus.OK);
    }


    /**
     * Handles fetching a Fulltext Resource
     * @return ResponseEntity
     */
    @GetMapping(value = "/{datasetId}/{recordId}/{resId}")
    public ResponseEntity<String> fulltextJsonLd(@PathVariable String datasetId,
                                 @PathVariable String recordId,
                                 @PathVariable String resId,
                                 HttpServletRequest request) throws SerializationException {
        LOG.debug("Retrieve Resource: " + datasetId + "/" + recordId + "/" + resId);
        boolean includeContext = true;
        HttpHeaders headers = new HttpHeaders();

        if (StringUtils.equalsIgnoreCase(acceptHeaderJsonOrLd(request), "X")){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        } else if (StringUtils.containsIgnoreCase(request.getHeader(ACCEPT), "JSON")){
            headers.add(CONTENTTYPE, MEDIA_TYPE_JSON);
            includeContext = false;
        } else {
            headers.add(CONTENTTYPE, MEDIA_TYPE_JSONLD);
        }

        FullTextResource resource;
        try {
            resource = fts.getFullTextResource(datasetId, recordId, resId);
        } catch (ResourceDoesNotExistException e) {
            LOG.warn(e.getMessage());
            return new ResponseEntity<>(fts.serializeResource(new JsonErrorResponse(e.getMessage())),
                                        headers,
                                        HttpStatus.NOT_FOUND);
        }
        if (!includeContext){
            resource.setContext(null);
        }
        return new ResponseEntity<>(fts.serializeResource(resource),
                                    headers,
                                    HttpStatus.OK);
    }

    /**
     * For testing HEAD request performance (EA-1239)
     * This is currently also the method that is used in production, as it seems to be the fastest (together with count)
     * @return ResponseEntity
     */
    @RequestMapping(value    = {"/{datasetId}/{recordId}/annopage/{pageId}",
            "/{datasetId}/{recordId}/annopage-limitone/{pageId}"},
                    method   = RequestMethod.HEAD)
    public ResponseEntity annoPageHeadExistsOne(@PathVariable String datasetId,
                                              @PathVariable String recordId,
                                              @PathVariable String pageId) {
        if (fts.doesAnnoPageExistByLimitOne(datasetId, recordId, pageId)){
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }


    private String processAcceptHeader(HttpServletRequest request, String format) {
        String result = "0";
        String accept = request.getHeader(ACCEPT);
        if (validateAcceptFormat(accept) && StringUtils.isNotEmpty(accept)) {
            Matcher m = acceptProfilePattern.matcher(accept);
            if (m.find()) { // found a Profile parameter in the Accept header
                String profiles = m.group(1);
                if (profiles.toLowerCase(Locale.getDefault()).contains(MEDIA_TYPE_IIIF_V3)) {
                    result = "3";
                } else if (profiles.toLowerCase(Locale.getDefault()).contains(MEDIA_TYPE_IIIF_V2)) {
                    result = "2";
                } else {
                    result = "X"; // in case a Profile is found that matches neither version => HTTP 406
                }
            }
        }  else if (StringUtils.isNotEmpty(accept)) { // validateAcceptFormat(accept) = false => HTTP 406
            result = "X";
        }
        // if result == "0": request header is empty, or does not contain a Profile parameter
        if (StringUtils.equalsIgnoreCase(result, "0")) {
            if (StringUtils.isBlank(format)){
                result = "2";    // if format not given, fall back to default "2"
            } else {
                result = format; // else use the format parameter
            }
        }
        return result;
    }

    private String acceptHeaderJsonOrLd(HttpServletRequest request){
        String accept = request.getHeader(ACCEPT);
        String result = "JSONLD";
        if (StringUtils.containsIgnoreCase(accept, MEDIA_TYPE_JSON)){
            result = "JSON";
        } else if (!StringUtils.containsIgnoreCase(accept, MEDIA_TYPE_JSONLD) &&
                   !StringUtils.containsIgnoreCase(accept, "*/*")){
            result = "X";
        }
        return result;
    }

    private boolean validateAcceptFormat(String accept){
        return (StringUtils.containsIgnoreCase(accept, "*/*")) ||
               (StringUtils.containsIgnoreCase(accept, MEDIA_TYPE_JSON)) ||
               (StringUtils.containsIgnoreCase(accept, MEDIA_TYPE_JSONLD));
    }

    private String generateETag(String id, ZonedDateTime modified, String iiifVersion) {
        StringBuilder hashData = new StringBuilder(id);
        hashData.append(modified.toString());
        hashData.append(fts.getSettings().getAppVersion());
        hashData.append(iiifVersion);
        return CacheUtils.generateETag(hashData.toString(), true);
    }


    // ---- deprecated testing stuff ----

    /**
     * for testing HEAD request performance (EA-1239)
     * @return
     */
    @Deprecated
    @RequestMapping(value    = "/{datasetId}/{recordId}/annopage-countone/{pageId}",
                    method   = RequestMethod.HEAD,
                    produces = APPLICATION_JSON_VALUE)
    public ResponseEntity annoPageHeadCountOne(@PathVariable String datasetId,
                                       @PathVariable String recordId,
                                       @PathVariable String pageId) {
        if (fts.doesAnnoPageExistByCount(datasetId, recordId, pageId)){
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * for testing HEAD request performance (EA-1239)
     * @return
     */
    @Deprecated
    @RequestMapping(value    = "/{datasetId}/{recordId}/annopage-findone/{pageId}",
            method   = RequestMethod.HEAD,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity annoPageHeadFindOne(@PathVariable String datasetId,
                                       @PathVariable String recordId,
                                       @PathVariable String pageId) {
        if (fts.doesAnnoPageExistsByFindOne(datasetId, recordId, pageId)){
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * For testing retrieving the version from the pom file
     */
    @GetMapping(value = "/showversion")
    public ResponseEntity<String> showVersion() throws SerializationException {
        String response = "The version of this API is: " + fts.getSettings().getAppVersion();
        return new ResponseEntity<>(fts.serializeResource(response), HttpStatus.I_AM_A_TEAPOT);
    }

}
