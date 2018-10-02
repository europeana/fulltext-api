package eu.europeana.fulltext.api.web;

import eu.europeana.fulltext.api.config.FTDefinitions;
import eu.europeana.fulltext.api.model.JsonErrorResponse;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.RecordParseException;
import eu.europeana.fulltext.api.service.exception.ResourceDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.SerializationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    public FTController(FTService FTService) {
        this.fts = FTService;
    }


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
        HttpHeaders headers = new HttpHeaders();
        if (!isAcceptHeaderOK(request)){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        String iiifVersion = version;
        if (iiifVersion == null) {
            iiifVersion = versionFromAcceptHeader(request);
        }

        String media_type_iiif_v2;
        String media_type_iiif_v3;

        if (StringUtils.containsIgnoreCase(request.getHeader("Accept"), MEDIA_TYPE_JSON)){
            media_type_iiif_v2 = MEDIA_TYPE_IIIF_JSON_V2;
            media_type_iiif_v3 = MEDIA_TYPE_IIIF_JSON_V3;
            includeContext = false;
        } else {
            media_type_iiif_v2 = MEDIA_TYPE_IIIF_JSONLD_V2;
            media_type_iiif_v3 = MEDIA_TYPE_IIIF_JSONLD_V3;
        }

        Object annotationPage;
        try {
            if ("3".equalsIgnoreCase(iiifVersion)) {
                annotationPage = fts.getAnnotationPageV3(datasetId, recordId, pageId, includeContext);
                headers.add("Content-Type", media_type_iiif_v3);
            } else {
                annotationPage = fts.getAnnotationPageV2(datasetId, recordId, pageId, includeContext);
                headers.add("Content-Type", media_type_iiif_v2);
            }
        } catch (AnnoPageDoesNotExistException e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(fts.serializeResource(new JsonErrorResponse(e.getMessage())),
                                        headers,
                                        HttpStatus.NOT_FOUND);
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
        HttpHeaders headers = new HttpHeaders();
        if (!isAcceptHeaderOK(request)){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        String iiifVersion = version;
        if (iiifVersion == null) {
            iiifVersion = versionFromAcceptHeader(request);
        }

        String media_type_iiif_v2;
        String media_type_iiif_v3;

        if (StringUtils.containsIgnoreCase(request.getHeader("Accept"), MEDIA_TYPE_JSON)){
            media_type_iiif_v2 = MEDIA_TYPE_IIIF_JSON_V2;
            media_type_iiif_v3 = MEDIA_TYPE_IIIF_JSON_V3;
            includeContext = false;
        } else {
            media_type_iiif_v2 = MEDIA_TYPE_IIIF_JSONLD_V2;
            media_type_iiif_v3 = MEDIA_TYPE_IIIF_JSONLD_V3;
        }

        Object annotation;
        try {
            if ("3".equalsIgnoreCase(iiifVersion)) {
                annotation = fts.getAnnotationV3(datasetId, recordId, annoID, includeContext);
                headers.add("Content-Type", media_type_iiif_v3);
            } else {
                annotation = fts.getAnnotationV2(datasetId, recordId, annoID, includeContext);
                headers.add("Content-Type", media_type_iiif_v2);
            }
        } catch (AnnoPageDoesNotExistException e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(fts.serializeResource(new JsonErrorResponse(e.getMessage())),
                                        headers,
                                        HttpStatus.NOT_FOUND);
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
        if (!isAcceptHeaderOK(request)){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        if (StringUtils.containsIgnoreCase(request.getHeader("Accept"), MEDIA_TYPE_JSON)){
            headers.add("Content-Type", MEDIA_TYPE_JSON);
            includeContext = false;
        } else {
            headers.add("Content-Type", MEDIA_TYPE_JSONLD);
        }

        Object resource;
        try {
            resource = fts.getFullTextResource(datasetId, recordId, resId, includeContext);
        } catch (ResourceDoesNotExistException e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(fts.serializeResource(new JsonErrorResponse(e.getMessage())),
                                        headers,
                                        HttpStatus.NOT_FOUND);
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


    private String versionFromAcceptHeader(HttpServletRequest request) {
        String result = "2"; // default version if no accept header is present
        String accept = request.getHeader("Accept");
        if (StringUtils.isNotEmpty(accept)) {
            Matcher m = acceptProfilePattern.matcher(accept);
            if (m.find()) {
                String profiles = m.group(1);
                if (profiles.toLowerCase(Locale.getDefault()).contains(MEDIA_TYPE_IIIF_V3)) {
                    result = "3";
                } else {
                    result = "2";
                }
            }
        }
        return result;
    }

    private boolean isAcceptHeaderOK(HttpServletRequest request){
        String accept = request.getHeader("Accept");
        return (StringUtils.isBlank(accept)) ||
               (StringUtils.containsIgnoreCase(accept, "*/*")) ||
               (StringUtils.containsIgnoreCase(accept, MEDIA_TYPE_JSON)) ||
               (StringUtils.containsIgnoreCase(accept, MEDIA_TYPE_JSONLD));
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
