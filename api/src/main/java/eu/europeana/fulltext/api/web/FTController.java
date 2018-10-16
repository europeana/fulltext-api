package eu.europeana.fulltext.api.web;

import eu.europeana.fulltext.api.model.AnnotationWrapper;
import eu.europeana.fulltext.api.model.FullTextResource;
import eu.europeana.fulltext.api.model.JsonErrorResponse;
import eu.europeana.fulltext.api.service.CacheUtils;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.ResourceDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.SerializationException;
import eu.europeana.fulltext.common.entity.AnnoPage;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.europeana.fulltext.api.config.FTDefinitions.*;
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
    /* for parsing accept headers */
    private Pattern acceptProfilePattern = Pattern.compile("profile=\"(.*?)\"");

    private FTService fts;

    public FTController(FTService ftService) {
        this.fts = ftService;
    }

    String mediaTypeIIIFV2;
    String mediaTypeIIIFV3;

    /**
     * Handles fetching a page (resource) with all its annotations
     * @return
     */
    @GetMapping(value    = "/{datasetId}/{recordId}/annopage/{pageId}")
    public ResponseEntity<String> annopage(@PathVariable String datasetId,
                           @PathVariable String recordId,
                           @PathVariable String pageId,
                           @RequestParam(value = "format", required = false) String version,
                           HttpServletRequest request,
                           HttpServletResponse response) throws SerializationException {
        LOG.debug("Retrieve Annopage: " + datasetId + "/" + recordId + "/" + pageId);

        boolean includeContext = true;
        HttpHeaders headers = null;
        String acceptHeaderStatus = processAcceptHeader(request);

        if (StringUtils.equalsIgnoreCase(acceptHeaderStatus, "X")){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        } else if (StringUtils.isBlank(version)){
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
            AnnoPage annoPage      = fts.fetchAnnoPage(datasetId, recordId, pageId);
            ZonedDateTime modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
            String           eTag  = generateETag(datasetId + recordId + pageId, modified, version);
            headers                = CacheUtils.generateCacheHeaders("no-cache", eTag, modified, "Accept");
            ResponseEntity cached  = CacheUtils.checkCached(request, headers, modified, eTag);

            if (cached != null) {
                return cached;
            }

            if ("3".equalsIgnoreCase(version)) {
                annotationPage = fts.generateAnnoPageV3(annoPage);
                headers.add("Content-Type", mediaTypeIIIFV3);
            } else {
                annotationPage = fts.generateAnnoPageV2(annoPage);
                headers.add("Content-Type", mediaTypeIIIFV2);
            }
        } catch (AnnoPageDoesNotExistException e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(fts.serializeResource(new JsonErrorResponse(e.getMessage())),
                                        headers,
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
     * @return
     */
    @GetMapping(value = "/{datasetId}/{recordId}/anno/{annoID}")
    public ResponseEntity<String> annotation(@PathVariable String datasetId,
                             @PathVariable String recordId,
                             @PathVariable String annoID,
                             @RequestParam(value = "format", required = false) String version,
                             HttpServletRequest request,
                             HttpServletResponse response) throws SerializationException {
        LOG.debug("Retrieve Annotation: " + datasetId + "/" + recordId + "/" + annoID);

        boolean includeContext = true;
        HttpHeaders headers = null;

        String acceptHeaderStatus = processAcceptHeader(request);

        if (StringUtils.equalsIgnoreCase(acceptHeaderStatus, "X")){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        } else if (StringUtils.isBlank(version)){
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
            AnnoPage annoPage      = fts.fetchAPAnnotation(datasetId, recordId, annoID);
            ZonedDateTime modified = CacheUtils.dateToZonedUTC(annoPage.getModified());
            String           eTag  = generateETag(datasetId + recordId + annoID, modified, version);
            headers                = CacheUtils.generateCacheHeaders("no-cache", eTag, modified, "Accept");
            ResponseEntity cached  = CacheUtils.checkCached(request, headers, modified, eTag);

            if (cached != null) {
                return cached;
            }

            if ("3".equalsIgnoreCase(version)) {
                annotation = fts.generateAnnotationV3(annoPage, annoID);
                headers.add("Content-Type", mediaTypeIIIFV2);
            } else {
                annotation = fts.generateAnnotationV2(annoPage, annoID);
                headers.add("Content-Type", mediaTypeIIIFV3);
            }
        } catch (AnnoPageDoesNotExistException e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(fts.serializeResource(new JsonErrorResponse(e.getMessage())),
                                        headers,
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
     * @return
     */
    @GetMapping(value = "/{datasetId}/{recordId}/{resId}")
    public ResponseEntity<String> fulltextJsonLd(@PathVariable String datasetId,
                                 @PathVariable String recordId,
                                 @PathVariable String resId,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws SerializationException {
        LOG.debug("Retrieve Resource: " + datasetId + "/" + recordId + "/" + resId);
        boolean includeContext = true;
        HttpHeaders headers = new HttpHeaders();

        if (StringUtils.equalsIgnoreCase(acceptHeaderJsonOrLd(request), "X")){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        } else if (StringUtils.containsIgnoreCase(request.getHeader("Accept"), "JSON")){
            headers.add("Content-Type", MEDIA_TYPE_JSON);
            includeContext = false;
        } else {
            headers.add("Content-Type", MEDIA_TYPE_JSONLD);
        }

        FullTextResource resource;
        try {
            resource = fts.getFullTextResource(datasetId, recordId, resId);
        } catch (ResourceDoesNotExistException e) {
            LOG.error(e.getMessage(), e);
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
     * @return
     */
    @RequestMapping(value    = {"/{datasetId}/{recordId}/annopage/{pageId}",
            "/{datasetId}/{recordId}/annopage-limitone/{pageId}"},
                    method   = RequestMethod.HEAD)
    public ResponseEntity annoPageHead_existsOne(@PathVariable String datasetId,
                                              @PathVariable String recordId,
                                              @PathVariable String pageId) {
        if (fts.doesAnnoPageExistByLimitOne(datasetId, recordId, pageId)){
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }


    private String processAcceptHeader(HttpServletRequest request) {
        String result = "2"; // default version if no accept header is present or contains no Profile parameter
        String accept = request.getHeader("Accept");
        if (validateAcceptFormat(accept) && StringUtils.isNotEmpty(accept)) {
            Matcher m = acceptProfilePattern.matcher(accept);
            if (m.find()) {
                String profiles = m.group(1);
                if (profiles.toLowerCase(Locale.getDefault()).contains(MEDIA_TYPE_IIIF_V3)) {
                    result = "3";
                } else if (!profiles.toLowerCase(Locale.getDefault()).contains(MEDIA_TYPE_IIIF_V2)) {
                    result = "X"; // in case a Profile is found that matches neither version => HTTP 406
                }
            }
        }  else if (StringUtils.isNotEmpty(accept)) { // format not OK
            result = "X";
        } // implicit ELSE = request header empty, default to V2
        return result;
    }

    private String acceptHeaderJsonOrLd(HttpServletRequest request){
        String accept = request.getHeader("Accept");
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
    public ResponseEntity annoPageHead_countOne(@PathVariable String datasetId,
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
    public ResponseEntity annoPageHead_findOne(@PathVariable String datasetId,
                                       @PathVariable String recordId,
                                       @PathVariable String pageId) {
        if (fts.doesAnnoPageExistsByFindOne(datasetId, recordId, pageId)){
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

}
